# Vision100

Vision100 е мобилно приложение за дигитално отчитане на посещения в
"100-те национални туристически обекта". Идеята е проста: вместо хартиена
книжка и печати, потребителят влиза в профила си,
прави снимка на място и приложението изпраща снимката заедно с GPS координати
към сървъра.

Бекендът проверява две неща:

- дали подадената локация е в радиус около туристическия обект;
- дали Google Vision разпознава в снимката същия или достатъчно близък обект.

При успешно съвпадение посещението се записва, снимката се пази локално на
сървъра, а потребителят получава точки за класацията.

## Какво има в текущата версия

- вход и регистрация чрез Firebase Auth - имейл/парола и Google вход;
- списък с туристически обекти, търсене, филтри и карта;
- smart check-in със снимка, GPS координати и AI проверка;
- потребителски профил, история на посещенията и общ брой точки;
- глобална класация по точки;
- български и английски интерфейс;
- FastAPI backend със SQLite база и SQLAlchemy модели.

Модулът за събития и чат е част от по-широката идея за проекта, но в текущия
код не е активна функционалност.

## Структура

```text
Vision100/
+-- android/
|   +-- Vision100/             # Android приложението
+-- backend/
    +-- Vision100 backend/     # FastAPI backend, база, seed скриптове
```

По-важните файлове:

- `backend/Vision100 backend/app/main.py` - API маршрути;
- `backend/Vision100 backend/app/vision_service.py` - логика за Google Vision,
  GPS радиус и съпоставяне на резултатите;
- `backend/Vision100 backend/database/models.py` - SQLAlchemy моделите;
- `android/Vision100/app/src/main/java/com/example/vision100/network/ApiService.kt`
  - Retrofit интерфейсът и адресът на backend-а;
- `android/Vision100/app/src/main/java/com/example/vision100/ui/screens/`
  - Compose екраните.

## Backend

Изисквания:

- Python 3.12+;
- Firebase service account файл;
- Google Cloud проект с включен Cloud Vision API;
- инсталирани Python зависимости от `requirements.txt`.

Стартиране под Windows PowerShell:

```powershell
cd "backend\Vision100 backend"

py -m venv vision
.\vision\Scripts\Activate.ps1

python -m pip install --upgrade pip
pip install -r requirements.txt
```

За Firebase Admin SDK backend-ът очаква файл:

```text
backend/Vision100 backend/firebase-credentials.json
```

За Google Vision може да се използва същият service account, ако има нужните
права, или отделен credentials файл. За текущата PowerShell сесия:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = "$PWD\firebase-credentials.json"
```

Ако базата липсва или трябва да се създаде наново:

```powershell
python seed_bilingual.py
```

Сървърът се пуска така:

```powershell
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

След стартиране:

- Swagger документация: `http://127.0.0.1:8000/docs`
- health check: `http://127.0.0.1:8000/health`

Основните API маршрути са:

- `GET /health`
- `GET /api/objects`
- `POST /api/auth/sync`
- `GET /api/users/me`
- `PUT /api/users/me/name`
- `POST /api/checkins/verify`
- `GET /api/visits/me`
- `GET /api/visits/{visit_id}/photo`
- `GET /api/leaderboard`

## Android приложение

Изисквания:

- Android Studio;
- JDK, съвместим с използвания Android Gradle Plugin;
- Android SDK с compile SDK 36;
- Firebase конфигурация за Android приложението.

Проектът се отваря от:

```text
android/Vision100
```

Firebase конфигурацията трябва да бъде в:

```text
android/Vision100/app/google-services.json
```

Адресът на backend-а е зададен в:

```kotlin
// android/Vision100/app/src/main/java/com/example/vision100/network/ApiService.kt
private const val BASE_URL
```

Този адрес трябва да сочи към машината, на която върви FastAPI сървърът.
За Android Emulator обикновено се използва:

```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

За реален телефон се използва локалният IP адрес на компютъра, например:

```kotlin
private const val BASE_URL = "http://192.168.0.2:8000/"
```

Важно е телефонът и компютърът да са в една мрежа, а firewall-ът да допуска
връзки към порт `8000`.

Build от команден ред:

```powershell
cd "android\Vision100"
.\gradlew.bat assembleDebug
```

Най-удобно за разработка е приложението да се стартира през Android Studio,
защото оттам се виждат Logcat съобщенията, разрешенията за камера/локация и
Firebase грешките при вход.

## Данни и локални файлове

SQLite базата се намира в backend папката като `vision100.db`. При seed-ване се
използва `100_nto_bilingual.json`, където са описани обектите с координати,
описания и AI labels. Качените снимки от check-in се записват в `uploads/`.

Следните файлове са локални и не трябва да се качват публично:

- `firebase-credentials.json`
- `google-services.json`
- `vision100.db`
- `uploads/`
- `.env` файлове