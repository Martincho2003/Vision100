import sqlite3

def migrate():
    conn = sqlite3.connect("vision100.db")
    cursor = conn.cursor()

    try:
        cursor.execute('PRAGMA foreign_keys=OFF;')
        
        print("Създаване на нова таблица tourist_objects_new с обновената схема...")
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS tourist_objects_new (
                id INTEGER PRIMARY KEY,
                number VARCHAR(10) NOT NULL,
                name_bg VARCHAR(200) NOT NULL,
                name_en VARCHAR(200) NOT NULL,
                description TEXT,
                region VARCHAR(100),
                category VARCHAR(50),
                latitude FLOAT NOT NULL,
                longitude FLOAT NOT NULL,
                ai_labels TEXT
            )
        ''')

        print("Прехвърляне на данните от старата към новата таблица...")
        cursor.execute('''
            INSERT INTO tourist_objects_new (
                id, number, name_bg, name_en, description, region,
                category, latitude, longitude, ai_labels
            )
            SELECT
                id, CAST(number AS VARCHAR), name_bg, name_en, description, region,
                category, latitude, longitude, ai_labels
            FROM tourist_objects
        ''')

        print("Изтриване на старата таблица...")
        cursor.execute('DROP TABLE tourist_objects')

        print("Преименуване на новата таблица на tourist_objects...")
        cursor.execute('ALTER TABLE tourist_objects_new RENAME TO tourist_objects')

        print("Възстановяване на индексите...")
        cursor.execute('CREATE INDEX IF NOT EXISTS ix_tourist_objects_id ON tourist_objects (id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS ix_tourist_objects_number ON tourist_objects (number)')

        conn.commit()
        print("✅ Миграцията (migrate_db_01.py) премина успешно!")
        
    except sqlite3.OperationalError as e:
        conn.rollback()
        print(f"❌ Грешка при миграцията: {e}")
        print("Ако колоните вече са премахнати, възможно е миграцията да е изпълнена.")
    finally:
        cursor.execute('PRAGMA foreign_keys=ON;')
        conn.close()

if __name__ == "__main__":
    migrate()
