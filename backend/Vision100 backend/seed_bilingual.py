import json
from database.connection import SessionLocal, create_tables, engine
from database.models import TouristObject

def seed():
    create_tables()
    db = SessionLocal()
    try:
        with open('100_nto_bilingual_updated.json', 'r', encoding='utf-8') as f:
            raw_objects = json.load(f)
            
        added = 0
        updated = 0
        
        for data in raw_objects:
            obj_number = int(data.get('number', 0))
            name = str(data['name_bg'])
            existing = db.query(TouristObject).filter_by(name_bg=name).first()
            
            if existing:
                existing.name_bg = data.get('name_bg', '')
                existing.name_en = data.get('name_en', '')
                existing.description_bg = data.get('description_bg', '')
                existing.description_en = data.get('description_en', '')
                existing.region_bg = data.get('region_bg', '')
                existing.region_en = data.get('region_en', '')
                existing.category_bg = data.get('category_bg', '')
                existing.category_en = data.get('category_en', '')
                existing.latitude = float(data.get('latitude', 0))
                existing.longitude = float(data.get('longtitude', 0))
                existing.ai_labels = json.dumps(data.get('ai_labels', []))
                updated += 1
            else:
                obj = TouristObject(
                    number=obj_number,
                    name_bg=data.get('name_bg', ''),
                    name_en=data.get('name_en', ''),
                    description_bg=data.get('description_bg', ''),
                    description_en=data.get('description_en', ''),
                    region_bg=data.get('region_bg', ''),
                    region_en=data.get('region_en', ''),
                    category_bg=data.get('category_bg', ''),
                    category_en=data.get('category_en', ''),
                    latitude=float(data.get('latitude', 0)),
                    longitude=float(data.get('longtitude', 0)),
                    ai_labels=json.dumps(data.get('ai_labels', []))
                )
                db.add(obj)
                added += 1
                
        db.commit()
        print(f'Успешно добавени {added} и обновени {updated} туристически обекта.')
    except Exception as e:
        db.rollback()
        print(f'Грешка при запис на туристически обекти: {e}')
        raise
    finally:
        db.close()

if __name__ == '__main__':
    seed()