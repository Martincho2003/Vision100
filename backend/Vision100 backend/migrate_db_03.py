import sqlite3


TABLES_TO_DROP = ("messages", "event_members", "events")


def migrate():
    conn = sqlite3.connect("vision100.db")
    cursor = conn.cursor()

    try:
        cursor.execute("PRAGMA foreign_keys=OFF;")

        for table_name in TABLES_TO_DROP:
            print(f"Removing table if it exists: {table_name}")
            cursor.execute(f"DROP TABLE IF EXISTS {table_name}")

        conn.commit()
        print("Migration migrate_db_03.py completed successfully.")

    except sqlite3.Error as exc:
        conn.rollback()
        print(f"Migration failed: {exc}")
        raise
    finally:
        cursor.execute("PRAGMA foreign_keys=ON;")
        conn.close()


if __name__ == "__main__":
    migrate()
