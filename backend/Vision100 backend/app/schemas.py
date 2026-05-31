from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class TouristObjectBase(BaseModel):
    number: str
    name: str
    description: Optional[str] = None
    region: Optional[str] = None
    category: Optional[str] = None
    latitude: float
    longitude: float

class TouristObjectResponse(TouristObjectBase):
    id: int

    class Config:
        from_attributes = True

class UserSync(BaseModel):
    display_name: Optional[str] = None
    avatar_url: Optional[str] = None

class UserNameUpdate(BaseModel):
    display_name: str

class UserBase(BaseModel):
    firebase_uid: str
    email: str
    display_name: str
    avatar_url: Optional[str] = None

class UserResponse(UserBase):
    id: int
    total_points: int
    created_at: datetime
    updated_at: Optional[datetime] = None
    last_login: Optional[datetime] = None

    class Config:
        from_attributes = True

class AIDetectionResponse(BaseModel):
    label: str
    score: float
    source: str


class VisitResponse(BaseModel):
    id: int
    user_id: int
    object_id: int
    visited_at: datetime
    latitude: float
    longitude: float
    gps_accuracy: Optional[float] = None
    photo_url: Optional[str] = None
    ai_confidence: Optional[float] = None
    ai_matched_label: Optional[str] = None
    is_verified: bool
    points_awarded: int
    tourist_object: TouristObjectResponse

    class Config:
        from_attributes = True


class CheckInResponse(BaseModel):
    verified: bool
    reason: str
    object: Optional[TouristObjectResponse] = None
    visit: Optional[VisitResponse] = None
    already_visited: bool = False
    points_awarded: int = 0
    total_points: int
    distance_meters: Optional[float] = None
    effective_radius_meters: Optional[float] = None
    ai_confidence: Optional[float] = None
    ai_match_score: Optional[float] = None
    ai_matched_label: Optional[str] = None
    ai_detected_label: Optional[str] = None
    detections: List[AIDetectionResponse] = []


class LeaderboardUser(BaseModel):
    id: int
    display_name: str
    avatar_url: Optional[str] = None
    total_points: int
