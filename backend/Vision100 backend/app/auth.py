import os
import logging
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
import firebase_admin
from firebase_admin import credentials, auth

from database.connection import get_db
from database.models import User

logger = logging.getLogger(__name__)

cred_path = "./firebase-credentials.json"

if os.path.exists(cred_path):
    cred = credentials.Certificate(cred_path)
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin initialized from %s", cred_path)
else:
    logger.warning("Firebase credentials file not found at %s", cred_path)

security = HTTPBearer()

def verify_firebase_token(
    cred: HTTPAuthorizationCredentials = Depends(security)
) -> dict:
    token = cred.credentials
    logger.info("Verifying Firebase token. token_prefix=%s token_length=%s", token[:12], len(token))
    try:
        decoded_token = auth.verify_id_token(token, clock_skew_seconds=60)
        if not decoded_token.get("uid"):
            raise ValueError("Token does not contain UID")
        logger.info(
            "Firebase token verified successfully. uid=%s email=%s",
            decoded_token.get("uid"),
            decoded_token.get("email"),
        )
        return decoded_token
    except Exception as e:
        logger.exception("Firebase token verification failed")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid or expired token: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )


def get_current_user(
    token_data: dict = Depends(verify_firebase_token),
    db: Session = Depends(get_db)
) -> User:
    uid = token_data.get("uid")
    logger.info("Looking up current user in DB. uid=%s", uid)
    user = db.query(User).filter(User.firebase_uid == uid).first()
    
    if not user:
        logger.warning("User not found in DB for uid=%s", uid)
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found in the database. Please register first."
        )
        
    logger.info("Current user found. user_id=%s display_name=%s", user.id, user.display_name)
    return user
