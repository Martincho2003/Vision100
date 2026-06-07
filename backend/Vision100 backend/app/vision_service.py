import json
import logging
import math
import os
import re
from dataclasses import dataclass
from difflib import SequenceMatcher
from typing import Iterable, Optional

from database.models import TouristObject


logger = logging.getLogger(__name__)

AI_PROVIDER = "google"
MIN_AI_CONFIDENCE = 0.55
MIN_TEXT_MATCH = 0.72
MAX_GPS_ACCURACY_BONUS_METERS = 100.0


@dataclass(frozen=True)
class Detection:
    label: str
    score: float
    source: str


@dataclass(frozen=True)
class ObjectMatch:
    tourist_object: TouristObject
    distance_meters: float
    matched_label: Optional[str]
    matched_detection: Optional[str]
    ai_confidence: float
    match_score: float
    detection_source: Optional[str]

    @property
    def combined_score(self) -> float:
        return self.ai_confidence * self.match_score


class VisionServiceError(RuntimeError):
    pass


def distance_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000.0
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    return 2 * radius * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def effective_radius(tourist_object: TouristObject, gps_accuracy: Optional[float]) -> float:
    accuracy_bonus = min(max(gps_accuracy or 0.0, 0.0), MAX_GPS_ACCURACY_BONUS_METERS)
    base_radius = 500.0
    return base_radius + accuracy_bonus


def object_terms(tourist_object: TouristObject) -> list[str]:
    terms = [
        tourist_object.name_bg,
        tourist_object.name_en,
        tourist_object.category_bg,
        tourist_object.category_en,
        tourist_object.region_bg,
        tourist_object.region_en,
    ]

    if tourist_object.ai_labels:
        try:
            parsed = json.loads(tourist_object.ai_labels)
            if isinstance(parsed, list):
                terms.extend(str(item) for item in parsed)
        except json.JSONDecodeError:
            terms.append(tourist_object.ai_labels)

    split_terms: list[str] = []
    for term in terms:
        if not term:
            continue
        split_terms.append(term)
        split_terms.extend(part.strip() for part in re.split(r"[;/|,]", term) if part.strip())

    return list(dict.fromkeys(split_terms))


def normalize_text(value: str) -> str:
    value = value.lower()
    value = value.replace("&", " and ")
    value = re.sub(r"[^a-z0-9\u0400-\u04ff]+", " ", value)
    return re.sub(r"\s+", " ", value).strip()


def text_similarity(left: str, right: str) -> float:
    left_norm = normalize_text(left)
    right_norm = normalize_text(right)
    if not left_norm or not right_norm:
        return 0.0
    if left_norm == right_norm:
        return 1.0
    if left_norm in right_norm or right_norm in left_norm:
        shorter = min(len(left_norm), len(right_norm))
        longer = max(len(left_norm), len(right_norm))
        return max(0.82, shorter / longer)
    return SequenceMatcher(None, left_norm, right_norm).ratio()


def analyze_image(image_bytes: bytes, filename: Optional[str] = None, lat: Optional[float] = None, lng: Optional[float] = None) -> list[Detection]:
    if AI_PROVIDER == "mock":
        return _mock_detections(filename)
    return _google_vision_detections(image_bytes, lat, lng)


def _mock_detections(filename: Optional[str]) -> list[Detection]:
    stem = os.path.splitext(os.path.basename(filename or ""))[0]
    label = stem.replace("_", " ").replace("-", " ").strip()
    if not label:
        label = "Rila Monastery"
    return [Detection(label=label, score=0.99, source="mock")]


def _google_vision_detections(image_bytes: bytes, lat: Optional[float] = None, lng: Optional[float] = None) -> list[Detection]:
    try:
        from google.cloud import vision
    except ImportError as exc:
        raise VisionServiceError(
            "google-cloud-vision is not installed. Install requirements.txt or set VISION100_AI_PROVIDER=mock."
        ) from exc

    try:
        client = vision.ImageAnnotatorClient()
        image = vision.Image(content=image_bytes)
        
        image_context = None
        if lat is not None and lng is not None:
            delta = 0.005
            lat_long_rect = vision.LatLongRect(
                min_lat_lng={"latitude": lat - delta, "longitude": lng - delta},
                max_lat_lng={"latitude": lat + delta, "longitude": lng + delta},
            )
            image_context = vision.ImageContext(lat_long_rect=lat_long_rect)

        request = vision.AnnotateImageRequest(
            image=image,
            image_context=image_context,
            features=[
                vision.Feature(type_=vision.Feature.Type.LANDMARK_DETECTION, max_results=10),
                vision.Feature(type_=vision.Feature.Type.LABEL_DETECTION, max_results=20),
                vision.Feature(type_=vision.Feature.Type.WEB_DETECTION, max_results=10),
                vision.Feature(type_=vision.Feature.Type.TEXT_DETECTION),
            ],
        )
        response = client.annotate_image(request=request)
    except Exception as exc:
        logger.exception("Google Vision request failed")
        raise VisionServiceError(f"Google Vision request failed: {exc}") from exc

    if response.error.message:
        raise VisionServiceError(response.error.message)

    detections: list[Detection] = []
    for landmark in response.landmark_annotations:
        detections.append(Detection(landmark.description, float(landmark.score or 0.0), "landmark"))
    for label in response.label_annotations:
        detections.append(Detection(label.description, float(label.score or 0.0), "label"))
    if response.web_detection:
        for best_guess in response.web_detection.best_guess_labels:
            if best_guess.label:
                detections.append(Detection(best_guess.label, 0.75, "web_best_guess"))
        for entity in response.web_detection.web_entities:
            if entity.description:
                detections.append(Detection(entity.description, float(entity.score or 0.0), "web_entity"))

    if response.text_annotations:
        full_text = response.text_annotations[0].description
        if full_text:
            for line in full_text.split('\n'):
                line = line.strip()
                if line:
                    detections.append(Detection(line, 0.85, "text"))

    logger.info(f"Google Vision API returned {len(detections)} raw detections.")
    for d in detections:
        logger.info(f"Vision Detection: '{d.label}' (Score: {d.score:.2f}, Source: {d.source})")

    return _dedupe_detections(detections)


def _dedupe_detections(detections: Iterable[Detection]) -> list[Detection]:
    best_by_label: dict[str, Detection] = {}
    for detection in detections:
        key = normalize_text(detection.label)
        if not key:
            continue
        current = best_by_label.get(key)
        if current is None or detection.score > current.score:
            best_by_label[key] = detection
    return sorted(best_by_label.values(), key=lambda item: item.score, reverse=True)


def nearby_objects(
    tourist_objects: Iterable[TouristObject],
    latitude: float,
    longitude: float,
    gps_accuracy: Optional[float],
) -> list[tuple[TouristObject, float]]:
    nearby: list[tuple[TouristObject, float]] = []
    for tourist_object in tourist_objects:
        distance = distance_meters(latitude, longitude, tourist_object.latitude, tourist_object.longitude)
        if distance <= effective_radius(tourist_object, gps_accuracy):
            nearby.append((tourist_object, distance))
    return sorted(nearby, key=lambda item: item[1])


def match_object(
    tourist_object: TouristObject,
    distance: float,
    detections: list[Detection],
) -> ObjectMatch:
    best_label = None
    best_detection = None
    best_source = None
    best_ai_confidence = 0.0
    best_match_score = 0.0

    for term in object_terms(tourist_object):
        for detection in detections:
            score = text_similarity(term, detection.label)
            if score > best_match_score or (score == best_match_score and detection.score > best_ai_confidence):
                best_label = term
                best_detection = detection.label
                best_source = detection.source
                best_ai_confidence = detection.score
                best_match_score = score

    return ObjectMatch(
        tourist_object=tourist_object,
        distance_meters=distance,
        matched_label=best_label,
        matched_detection=best_detection,
        ai_confidence=best_ai_confidence,
        match_score=best_match_score,
        detection_source=best_source,
    )


def choose_best_match(
    candidates: list[tuple[TouristObject, float]],
    detections: list[Detection],
) -> Optional[ObjectMatch]:
    if not candidates:
        return None

    matches = [match_object(tourist_object, distance, detections) for tourist_object, distance in candidates]
    return max(matches, key=lambda item: (item.combined_score, item.match_score, item.ai_confidence, -item.distance_meters))


def is_ai_match_success(match: ObjectMatch) -> bool:
    return match.ai_confidence >= MIN_AI_CONFIDENCE and match.match_score >= MIN_TEXT_MATCH
