import os
import uuid
import asyncio
import functools
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse

from fastapi import FastAPI, Depends, File, Form, UploadFile, Header
import logging
from sqlalchemy.orm import Session
from sqlalchemy.sql import func
from typing import List, Optional
from datetime import datetime

from database.connection import get_db, create_tables
from database.models import TouristObject, User, Visit
from app import schemas
from app.auth import get_current_user, verify_firebase_token
from app.translations import get_text, localize_tourist_object, localize_visit
from app.vision_service import (
    VisionServiceError,
    google_vision_detections,
    choose_best_match,
    effective_radius,
    is_ai_match_success,
    nearby_objects,
)
from fastapi import HTTPException, status

logging.basicConfig(level=logging.INFO, format="%(levelname)s:     %(message)s")
logger = logging.getLogger(__name__)
POINTS_PER_VISIT = 10
MAX_IMAGE_BYTES = 8 * 1024 * 1024

create_tables()

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

app = FastAPI(
    title="Vision100 API",
    description="API за интелигентна мобилна система за дигитализация на 100-те национални туристически обекта.",
    version="1.0.0"
)

app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

db_write_lock = asyncio.Lock()

@app.get("/health", tags=["System"])
def health_check(accept_language: Optional[str] = Header(None)):
    logger.info("Health check requested")
    return {"status": "ok", "message": get_text(accept_language, "health_ok")}

@app.get("/api/objects", response_model=List[schemas.TouristObjectResponse], tags=["Tourist Objects"])
def get_tourist_objects(
    skip: int = 0,
    limit: int = 250,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    accept_language: Optional[str] = Header(None),
):
    logger.info("Fetching tourist objects. user_id=%s skip=%s limit=%s", current_user.id, skip, limit)
    logger.info("Accept-Language header: %s", accept_language)
    objects = db.query(TouristObject).offset(skip).limit(limit).all()
    visited_object_ids = {
        object_id
        for (object_id,) in db.query(Visit.object_id)
        .filter(
            Visit.user_id == current_user.id,
            Visit.is_verified.is_(True),
        )
        .distinct()
        .all()
    }
    logger.info("Fetched tourist objects count=%s", len(objects))
    return [
        localize_tourist_object(obj, accept_language, 1 if obj.id in visited_object_ids else 0)
        for obj in objects
    ]

@app.get("/api/visits/me", response_model=List[schemas.VisitResponse], tags=["Visits"])
def get_my_visits(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    accept_language: Optional[str] = Header(None)
):
    visits = (
        db.query(Visit)
        .filter(Visit.user_id == current_user.id, Visit.is_verified.is_(True))
        .order_by(Visit.visited_at.desc())
        .all()
    )
    return [localize_visit(visit, accept_language) for visit in visits]

@app.get("/api/visits/{visit_id}/photo", tags=["Visits"])
def get_visit_photo(
    visit_id: int, 
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    accept_language: Optional[str] = Header(None)
):
    visit = db.query(Visit).filter(Visit.id == visit_id).first()
    if not visit or not visit.photo_url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=get_text(accept_language, "photo_not_found")
        )
    
    file_path = visit.photo_url.lstrip("/")
    
    if not os.path.exists(file_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=get_text(accept_language, "photo_missing_disk")
        )
        
    return FileResponse(file_path)

@app.post("/api/checkins/verify", response_model=schemas.CheckInResponse, tags=["Check-in"])
async def verify_check_in(
    photo: UploadFile = File(...),
    latitude: float = Form(...),
    longitude: float = Form(...),
    gps_accuracy: Optional[float] = Form(None),
    timestamp: Optional[int] = Form(None),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    accept_language: Optional[str] = Header(None),
):
    if not photo.content_type or not photo.content_type.startswith("image/"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=get_text(accept_language, "photo_must_be_image"),
        )

    image_bytes = await photo.read()
    if not image_bytes:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=get_text(accept_language, "photo_empty"))

    logger.info("Check-in initiated. user_id=%s lat=%s lon=%s image_size_bytes=%s", current_user.id, latitude, longitude, len(image_bytes))

    if len(image_bytes) > MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=get_text(accept_language, "photo_too_large", max_size=MAX_IMAGE_BYTES // (1024 * 1024)),
        )

    file_extension = ".jpg"
    if photo.filename and "." in photo.filename:
        file_extension = f".{photo.filename.split('.')[-1]}"
    saved_filename = f"{uuid.uuid4().hex}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, saved_filename)
    with open(file_path, "wb") as f:
        f.write(image_bytes)
        
    photo_url_path = f"/uploads/{saved_filename}"

    all_objects = db.query(TouristObject).all()
    candidates = nearby_objects(all_objects, latitude, longitude, gps_accuracy)
    candidate_for_response = candidates[0][0] if candidates else None
    distance_for_response = candidates[0][1] if candidates else None
    radius_for_response = (
        effective_radius(candidates[0][0], gps_accuracy)
        if candidates
        else None
    )

    logger.info("GPS candidates found: %s", len(candidates))

    if not candidates:
        logger.info(
            "Check-in rejected by GPS. user_id=%s lat=%s lon=%s",
            current_user.id,
            latitude,
            longitude,
        )

        return schemas.CheckInResponse(
            verified=False,
            reason=get_text(accept_language, "gps_outside_radius"),
            object=None,
            total_points=current_user.total_points,
            distance_meters=distance_for_response,
            effective_radius_meters=radius_for_response,
        )

    try:
        logger.info("Sending photo to Vision API for analysis...")
        loop = asyncio.get_running_loop()
        detections = await loop.run_in_executor(
            None, 
            functools.partial(google_vision_detections, image_bytes, lat=latitude, lng=longitude)
        )
        logger.info("Vision API successfully returned %s preliminary detections.", len(detections))
    except VisionServiceError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=get_text(accept_language, "vision_service_error"))

    detection_response = [
        schemas.AIDetectionResponse(label=item.label, score=item.score, source=item.source)
        for item in detections[:10]
    ]

    best_match = choose_best_match(candidates, detections) if detections else None
    target_object = best_match.tourist_object if best_match else candidate_for_response
    is_success = bool(detections and best_match and is_ai_match_success(best_match))

    async with db_write_lock:
        verified_visit = db.query(Visit).filter(
            Visit.user_id == current_user.id,
            Visit.object_id == target_object.id,
            Visit.is_verified == True
        ).first()

        if verified_visit:
            try:
                os.remove(file_path)
            except OSError:
                pass
            return schemas.CheckInResponse(
                verified=True,
                reason=get_text(accept_language, "already_verified"),
                object=localize_tourist_object(target_object, accept_language),
                visit=localize_visit(verified_visit, accept_language),
                already_visited=True,
                points_awarded=0,
                total_points=current_user.total_points,
                distance_meters=best_match.distance_meters if best_match else distance_for_response,
                effective_radius_meters=effective_radius(target_object, gps_accuracy),
                ai_confidence=best_match.ai_confidence if best_match else None,
                ai_match_score=best_match.match_score if best_match else None,
                ai_matched_label=best_match.matched_label if best_match else None,
                ai_detected_label=best_match.matched_detection if best_match else None,
                detections=detection_response,
            )

        unverified_count = db.query(Visit).filter(
            Visit.user_id == current_user.id,
            Visit.object_id == target_object.id,
            Visit.is_verified == False
        ).count()

        if unverified_count >= 5:
            try:
                os.remove(file_path)
            except OSError:
                pass
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=get_text(accept_language, "max_attempts_reached")
            )

        visit_time = datetime.fromtimestamp(timestamp / 1000.0) if timestamp else func.now()

        visit = Visit(
            user_id=current_user.id,
            object_id=target_object.id,
            latitude=latitude,
            longitude=longitude,
            gps_accuracy=gps_accuracy,
            visited_at=visit_time,
            photo_url=photo_url_path,
            ai_confidence=best_match.ai_confidence if best_match else None,
            ai_matched_label=best_match.matched_detection if best_match else None,
            is_verified=is_success,
            points_awarded=POINTS_PER_VISIT if is_success else 0,
        )

        db.add(visit)
        if is_success:
            current_user.total_points += POINTS_PER_VISIT
        db.commit()
        db.refresh(visit)
        if is_success:
            db.refresh(current_user)

    if not detections:
        return schemas.CheckInResponse(
            verified=False,
            reason=get_text(accept_language, "ai_no_useful_labels"),
            object=localize_tourist_object(target_object, accept_language),
            total_points=current_user.total_points,
            distance_meters=distance_for_response,
            effective_radius_meters=radius_for_response,
            detections=detection_response,
        )
    elif not is_success:
        return schemas.CheckInResponse(
            verified=False,
            reason=get_text(accept_language, "ai_not_match"),
            object=localize_tourist_object(target_object, accept_language),
            total_points=current_user.total_points,
            distance_meters=best_match.distance_meters,
            effective_radius_meters=effective_radius(target_object, gps_accuracy),
            ai_confidence=best_match.ai_confidence,
            ai_match_score=best_match.match_score,
            ai_matched_label=best_match.matched_label,
            ai_detected_label=best_match.matched_detection,
            detections=detection_response,
        )
    else:
        return schemas.CheckInResponse(
            verified=True,
            reason=get_text(accept_language, "visit_verified"),
            object=localize_tourist_object(target_object, accept_language),
            visit=localize_visit(visit, accept_language),
            already_visited=False,
            points_awarded=POINTS_PER_VISIT,
            total_points=current_user.total_points,
            distance_meters=best_match.distance_meters,
            effective_radius_meters=effective_radius(target_object, gps_accuracy),
            ai_confidence=best_match.ai_confidence,
            ai_match_score=best_match.match_score,
            ai_matched_label=best_match.matched_label,
            ai_detected_label=best_match.matched_detection,
            detections=detection_response,
        )

@app.get("/api/leaderboard", response_model=List[schemas.LeaderboardUser], tags=["Users"])
def get_leaderboard(
    limit: int = 50,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    limit = max(1, min(limit, 100))
    return db.query(User).order_by(User.total_points.desc(), User.display_name.asc()).limit(limit).all()

@app.post("/api/auth/sync", response_model=schemas.UserResponse, tags=["Auth"])
async def sync_user(
    user_data: schemas.UserSync,
    token_data: dict = Depends(verify_firebase_token),
    db: Session = Depends(get_db)
):
    uid = token_data.get("uid")
    email = token_data.get("email")
    sign_in_provider = token_data.get("firebase", {}).get("sign_in_provider", "")
    logger.info("Sync requested. uid=%s email=%s provider=%s display_name=%s", uid, email, sign_in_provider, user_data.display_name)
    
    user = db.query(User).filter(User.firebase_uid == uid).first()
    
    if user:
        logger.info("Existing user found. user_id=%s display_name=%s last_login will be updated", user.id, user.display_name)
        user.last_login = func.now()
    else:
        requested_name = user_data.display_name.strip() if user_data.display_name else None
        final_name = (requested_name or token_data.get("name", "Unknown User")).strip()
        logger.info("Creating new user. final_name=%s provider=%s", final_name, sign_in_provider)
        
        if sign_in_provider == "password":
            if db.query(User).filter(User.display_name == final_name).first():
                try:
                    from firebase_admin import auth
                    auth.delete_user(uid)
                    logger.info("Deleted user %s from Firebase Auth due to registration failure.", uid)
                except Exception as e:
                    logger.error("Failed to delete user %s from Firebase Auth: %s", uid, e)
                raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username is already taken. Please choose another one.")
        else:
            base_name = final_name[:90]
            counter = 2
            if db.query(User).filter(User.display_name == final_name).first():
                logger.info("Auto-adjusting Google display_name because it is taken. current_name=%s", final_name)
                final_name = f"{base_name} {counter}"
                while db.query(User).filter(User.display_name == final_name).first():
                    counter += 1
                    final_name = f"{base_name} {counter}"

        final_avatar = user_data.avatar_url or token_data.get("picture", "")
        
        user = User(
            firebase_uid=uid,
            email=email,
            display_name=final_name,
            avatar_url=final_avatar,
            last_login=func.now()
        )
        db.add(user)
    
    async with db_write_lock:
        db.commit()
        db.refresh(user)
        logger.info("Sync completed successfully. user_id=%s display_name=%s last_login=%s", user.id, user.display_name, user.last_login)
    
    return user

@app.get("/api/users/me", response_model=schemas.UserResponse, tags=["Users"])
def get_current_user_info(current_user: User = Depends(get_current_user)):
    logger.info("/api/users/me requested. user_id=%s display_name=%s", current_user.id, current_user.display_name)
    return current_user

@app.put("/api/users/me/name", response_model=schemas.UserResponse, tags=["Users"])
async def update_username(
    name_data: schemas.UserNameUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    new_name = name_data.display_name.strip()
    logger.info("Username update requested. user_id=%s old_name=%s new_name=%s", current_user.id, current_user.display_name, new_name)
    
    if new_name == current_user.display_name:
        logger.info("Username update skipped because name is unchanged. user_id=%s", current_user.id)
        return current_user
        
    name_exists = db.query(User).filter(User.display_name == new_name).first()
    if name_exists:
        logger.warning("Username update rejected because name is taken. new_name=%s", new_name)
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="This username is already taken.")
        
    current_user.display_name = new_name
    async with db_write_lock:
        db.commit()
        db.refresh(current_user)
        logger.info("Username updated successfully. user_id=%s new_name=%s", current_user.id, current_user.display_name)
    
    return current_user