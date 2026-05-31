import json
from database.connection import SessionLocal, create_tables, engine
from database.models import TouristObject, Base

def seed():
    Base.metadata.drop_all(bind=engine)
    create_tables()
    db = SessionLocal()
    try:
        with open('100_nto_bts_official_only.json', 'r', encoding='utf-8') as f:
            raw_objects = json.load(f)
        objects = []
        for data in raw_objects:
            obj = TouristObject(
                number=str(data['number']),
                name_bg=data['name_bg'],
                name_en=data.get('name_en', ''),
                description=data.get('description', ''),
                region=data.get('region', ''),
                category=data.get('category', ''),
                latitude=float(data.get('latitude', 0)),
                longitude=float(data.get('longtitude', 0)),
                ai_labels=json.dumps(data.get('ai_labels', []))
            )
            objects.append(obj)
        db.add_all(objects)
        db.commit()
        print(f'Създадени {len(objects)} туристически обекти.')
    except Exception as e:
        db.rollback()
        print(f'Грешка при създаване на туристически обекти: {e}')
        raise
    finally:
        db.close()

if __name__ == '__main__':
    seed()

