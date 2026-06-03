import json
import time
import urllib.request
import urllib.parse
import ssl

INPUT_FILE = "100_nto_bilingual.json"
OUTPUT_FILE = "100_nto_bilingual_updated.json"

def get_coordinates(query):
    """
    Търси координати чрез OpenStreetMap (Nominatim) API.
    Nominatim е безплатно, но изисква User-Agent и максимум 1 заявка в секунда.
    """
    url = f"https://nominatim.openstreetmap.org/search?q={urllib.parse.quote(query)}&format=json&limit=1"
    
    # Nominatim изисква персонализиран User-Agent
    req = urllib.request.Request(
        url, 
        headers={'User-Agent': 'Vision100_Geocoding_Script_v2.0 (student_project)'}
    )
    
    # Игнориране на евентуални SSL грешки в локална среда
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    
    try:
        response = urllib.request.urlopen(req, context=ctx)
        result = json.loads(response.read().decode('utf-8'))
        
        if result:
            return float(result[0]['lat']), float(result[0]['lon'])
    except urllib.error.HTTPError as e:
        if e.code == 429:
            print("\n[!] ВНИМАНИЕ: Получихме грешка 429 (Too Many Requests). Сървърът ни блокира временно!")
            print("[!] Изчакваме 60 секунди преди следващ опит...")
            time.sleep(10)
            return get_coordinates(query) # Опитваме отново след паузата
        print(f"  [!] HTTP грешка: {e.code}")
    except Exception as e:
        print(f"  [!] Мрежова грешка при заявката: {e}")
        
    return None, None

def update_locations():
    print(f"Четене на данни от {INPUT_FILE}...")
    try:
        with open(INPUT_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Грешка: Файлът {INPUT_FILE} не е намерен.")
        return

    updated_count = 0
    not_found_count = 0
    
    print(f"Намерени {len(data)} обекта. Започва търсене на координати...\n")

    for i, obj in enumerate(data, 1):
        name = obj.get("name_bg", "").split(' - ')[0] # Опитваме се да вземем само основното име, ако има тирета
        region = obj.get("region_bg", "")
        
        # Съставяме заявки - първо по-специфична, ако не стане - по-обща
        queries_to_try = [
            f"{name}, {region}, България",
            f"{name}, България",
        ]
        
        found = False
        for query in queries_to_try:
            # print(f"[{i}/{len(data)}] Търсене за: {query}")
            lat, lon = get_coordinates(query)
            
            if lat and lon:
                obj['latitude'] = lat
                obj['longtitude'] = lon
                print(f"[УСПЕХ] Обект {i}: {name} -> ({lat}, {lon})")
                updated_count += 1
                found = True
                
                # Запазваме прогреса веднага, за да не губим данни при спиране
                with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
                    json.dump(data, f, ensure_ascii=False, indent=4)
                break
                
            # Задължителна пауза от 2 секунди според правилата на Nominatim
            time.sleep(2.0)
            
        if not found:
            not_found_count += 1
            print(f"[НЕ НАМЕРЕН] Обект {i}: '{name}' - запазваме със старите координати.")
            
            # Запазваме прогреса веднага, дори и да не сме намерили нови
            with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=4)
            
    # Запазване в НОВ файл, за да не повредим оригинала при грешка
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
        
    print(f"\n--- ПРИКЛЮЧЕНО ---")
    print(f"Обновени обекти: {updated_count}")
    print(f"Ненамерени обекти: {not_found_count}")
    print(f"Резултатът е записан в новия файл: {OUTPUT_FILE}")
    print("Прегледайте го и ако сте доволни, преименувайте го на 100_nto_bilingual.json")

if __name__ == "__main__":
    update_locations()
