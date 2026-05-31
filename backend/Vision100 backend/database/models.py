from sqlalchemy import (
    Column, Integer, String, Float, Boolean,
    DateTime, ForeignKey, Text, UniqueConstraint
)
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database.base import Base


class User(Base):
    __tablename__ = "users"

    id              = Column(Integer, primary_key=True, index=True)
    firebase_uid    = Column(String(128), unique=True, nullable=False, index=True)
    email           = Column(String(255), unique=True, nullable=False)
    display_name    = Column(String(100), unique=True, nullable=False)
    avatar_url      = Column(String(512), nullable=True)
    total_points    = Column(Integer, default=0, nullable=False)
    created_at      = Column(DateTime(timezone=True), server_default=func.now())
    updated_at      = Column(DateTime(timezone=True), onupdate=func.now())
    last_login      = Column(DateTime(timezone=True), nullable=True)

    visits          = relationship("Visit",   back_populates="user", cascade="all, delete-orphan")

    def __repr__(self):
        return f"<User id={self.id} email={self.email}>"


class TouristObject(Base):
    __tablename__ = "tourist_objects"

    id              = Column(Integer, primary_key=True, index=True)
    number          = Column(String(10), index=True, nullable=False)
    name_bg         = Column(String(200), nullable=False)
    name_en         = Column(String(200), nullable=False)
    description_bg  = Column(Text, nullable=True)
    description_en  = Column(Text, nullable=True)
    region_bg       = Column(String(100), nullable=True)
    region_en       = Column(String(100), nullable=True)
    category_bg     = Column(String(50), nullable=True)
    category_en     = Column(String(50), nullable=True)
    latitude        = Column(Float, nullable=False)
    longitude       = Column(Float, nullable=False)
    ai_labels       = Column(Text, nullable=True)

    visits          = relationship("Visit", back_populates="tourist_object")

    def __repr__(self):
        return f"<TouristObject #{self.number} {self.name_bg}>"


class Visit(Base):
    __tablename__ = "visits"

    id              = Column(Integer, primary_key=True, index=True)
    user_id         = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    object_id       = Column(Integer, ForeignKey("tourist_objects.id"), nullable=False, index=True)
    visited_at      = Column(DateTime(timezone=True), server_default=func.now())

    latitude        = Column(Float, nullable=False)
    longitude       = Column(Float, nullable=False)
    gps_accuracy    = Column(Float, nullable=True)
    photo_url       = Column(String(512), nullable=True)
    ai_confidence   = Column(Float, nullable=True)
    ai_matched_label= Column(String(200), nullable=True)
    is_verified     = Column(Boolean, default=False, nullable=False)
    points_awarded  = Column(Integer, default=0, nullable=False)

    __table_args__ = (
        UniqueConstraint("user_id", "object_id", name="uq_user_object"),
    )

    user            = relationship("User",          back_populates="visits")
    tourist_object  = relationship("TouristObject", back_populates="visits")

    def __repr__(self):
        return f"<Visit user={self.user_id} object={self.object_id} verified={self.is_verified}>"

