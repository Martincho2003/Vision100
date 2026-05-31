import sqlite3

def migrate():
    conn = sqlite3.connect("vision100.db")
    cursor = conn.cursor()

    try:
        cursor.execute('PRAGMA foreign_keys=OFF;')
        
        print("Изтриване на старите посещения (visits)...")
        cursor.execute('DROP TABLE IF EXISTS visits')
        
        print("Изтриване на старата таблица с обекти (tourist_objects)...")
        cursor.execute('DROP TABLE IF EXISTS tourist_objects')

        conn.commit()
        print("✅ Таблиците са изтрити успешно. Можете да стартирате seed_bilingual.py, за да ги създаде наново с правилната схема.")
        
    except sqlite3.OperationalError as e:
        conn.rollback()
        print(f"❌ Грешка: {e}")
    finally:
        cursor.execute('PRAGMA foreign_keys=ON;')
        conn.close()

if __name__ == "__main__":
    migrate()
