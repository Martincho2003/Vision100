TRANSLATIONS = {
    "bg": {
        "health_ok": "Vision100 API работи.",
        "photo_not_found": "Снимката за това посещение не е намерена.",
        "photo_missing_disk": "Файлът със снимката физически липсва на сървъра.",
        "photo_must_be_image": "Изпратеният файл трябва да е снимка.",
        "photo_empty": "Изпратената снимка е празна.",
        "photo_too_large": "Снимката е твърде голяма. Максималният размер е {max_size} MB.",
        "tourist_object_not_found": "Туристическият обект не е намерен.",
        "vision_service_error": "Разпознаването на обекти е временно недостъпно поради грешка в Vision API.",
        "gps_outside_radius": "GPS локацията е извън позволения радиус за избрания/най-близкия обект.",
        "ai_no_useful_labels": "Изкуственият интелект не откри полезни обекти или забележителности в снимката.",
        "ai_not_match": "Резултатът от AI не съвпада с туристическия обект близо до изпратената GPS локация.",
        "already_verified": "Туристическият обект вече е верифициран от този потребител.",
        "max_attempts_reached": "Достигнахте максималния брой опити за този обект. Администратор ще прегледа снимките ви ръчно за одобрение.",
        "visit_verified": "Посещението е верифицирано успешно."
    },
    "en": {
        "health_ok": "Vision100 API is running.",
        "photo_not_found": "The photo for this visit could not be found.",
        "photo_missing_disk": "The photo file is physically missing from the server.",
        "photo_must_be_image": "The uploaded file must be an image.",
        "photo_empty": "The uploaded photo is empty.",
        "photo_too_large": "Photo is too large. Max size is {max_size} MB.",
        "tourist_object_not_found": "Tourist object not found.",
        "vision_service_error": "Image recognition is temporarily unavailable due to a Vision API error.",
        "gps_outside_radius": "GPS location is outside the allowed radius for the selected/nearest tourist object.",
        "ai_no_useful_labels": "AI did not detect useful labels or landmarks in the image.",
        "ai_not_match": "AI result does not match the tourist object near the submitted GPS location.",
        "already_verified": "Tourist object is already verified for this user.",
        "max_attempts_reached": "You have reached the maximum number of attempts for this object. An administrator will review your photos manually.",
        "visit_verified": "Visit verified successfully."
    }
}

def get_text(accept_language: str, key: str, **kwargs) -> str:
    lang_key = "bg"
    
    if accept_language:
        primary_lang = accept_language.split(",")[0].split("-")[0].strip().lower()
        if primary_lang in TRANSLATIONS:
            lang_key = primary_lang
            
    text = TRANSLATIONS[lang_key].get(key, TRANSLATIONS["bg"].get(key, key))
    if kwargs:
        return text.format(**kwargs)
    return text

def get_language(accept_language: str) -> str:
    if accept_language:
        primary_lang = accept_language.split(",")[0].split("-")[0].strip().lower()
        if primary_lang in TRANSLATIONS:
            return primary_lang
    return "bg"

def localize_tourist_object(obj, accept_language: str) -> dict:
    lang = get_language(accept_language)
    return {
        "id": obj.id,
        "number": obj.number,
        "name": getattr(obj, f"name_{lang}", obj.name_bg),
        "description": getattr(obj, f"description_{lang}", obj.description_bg),
        "region": getattr(obj, f"region_{lang}", obj.region_bg),
        "category": getattr(obj, f"category_{lang}", obj.category_bg),
        "latitude": obj.latitude,
        "longitude": obj.longitude
    }

def localize_visit(visit, accept_language: str) -> dict:
    if not visit:
        return None
    v_dict = visit.__dict__.copy()
    if visit.tourist_object:
        v_dict["tourist_object"] = localize_tourist_object(visit.tourist_object, accept_language)
    return v_dict
