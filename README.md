# Deepal Code Gen — Android-приложение для генерации инженерного кода

Android-порт скрипта `deepal-engineer-code.py`: по VIN автомобиля Deepal S05 генерирует инженерный код вида `*#XXXXXXXX#*`.

## О программе

Deepal Code Gen — утилита для владельцев Deepal S05: приложение (и Python-скрипт) берут VIN автомобиля и по последним 8 символам вычисляют инженерный код, набираемый на головном устройстве. Работает полностью офлайн — весь расчёт выполняется на устройстве.

- Репозиторий: [github.com/free4fan/deepal-code-generator](https://github.com/free4fan/deepal-code-generator)
- Загрузки (APK): [страница релизов](https://github.com/free4fan/deepal-code-generator/releases)

## Алгоритм

Повторяет Python-скрипт v3.1 (ядро алгоритма идентично v3.0.1; добавлена валидация символов VIN). Обе реализации покрывают общими тестовыми фикстурами `tests/fixtures.txt`:

1. Берутся последние 8 символов VIN (регистр не важен)
2. **B2D-маппинг**: каждая буква заменяется цифрой по таблице `ABCDEFGHIJKLMNOPQRSTUVWXYZ → 01234567899876543210012345`
3. **Shuffle**: цифры переставляются в порядке `[2, 5, 1, 4, 7, 0, 3, 6]`
4. Результат оборачивается в `*#...#*`

Пример: `LS6A3E2R5PA123456` → `*#14036425#*`

Валидация: минимум 8 символов; допустимы `0-9` и `A-Z` без `I`, `O`, `Q` (как в реальных VIN). Нежелательный символ — ошибка с указанием позиции.

## Возможности

- Ввод VIN (17 символов, счётчик символов, только заглавные) + проверка допустимости символов
- Генерация кода, копирование в буфер обмена (Snackbar-уведомление)
- Три темы: светлая / тёмная / системная
- Языки интерфейса: русский / английский (на основе `AppCompatDelegate.setApplicationLocales`; старый выбор в SharedPreferences мигрируется при первом запуске)
- Material 3 (MaterialCardView, TextInputLayout, MaterialButtonToggleGroup, DynamicColors)
- Работает полностью офлайн, без сети и запросов к API

## Использование

1. Запустите приложение
2. Введите VIN автомобиля в поле ввода (17 символов, регистр не важен)
3. Нажмите **«Сгенерировать код»** — внизу появится код вида `*#XXXXXXXX#*`
4. Нажмите **«Копировать»**, чтобы сохранить код в буфер обмена
5. Наберите код на головном устройстве автомобиля (например, в приложении звонка) и подтвердите

Дополнительно:

- **«Очистить»** — сбросить VIN и результат
- **Тема** — переключение светлой / тёмной / системной темы
- **Язык** — русский / английский (выбор запоминается)
- Код можно сгенерировать и из клавиатуры: введите VIN и нажмите **Done/Готово**

Код генерируется только по последним 8 символам VIN — первые 9 цифр автомобиля значения не имеют.

## Требования

- Android 5.0+ (minSdk 21), targetSdk 34
- Сборка: JDK 17 (JDK 21 тоже работает), Android SDK (platform 34, build-tools 34.0.0), Gradle 8.5 (wrapper: `gradlew`)

## Сборка

```bash
cd codegen-app

# пароли keystore — один раз:
cp keystore.properties.example keystore.properties
#   и впишите storePassword/keyPassword (или задайте env DEEPAL_KEYSTORE_PASSWORD / DEEPAL_KEY_PASSWORD)

./gradlew :app:testReleaseUnitTest   # unit-тесты алгоритма
./gradlew :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk ../deepal_code_gen.apk
```

Важно:

- `keystore.jks` лежит в `codegen-app/` (бэкап ключа подписи). Пароли к нему — только в `keystore.properties` (вне git) или в переменных окружения.
- `keystore.properties` и `local.properties` заигнорены в git.

Python-скрипт:

```bash
python3 deepal-engineer-code.py LS6A3E2R5PA123456     # код по VIN
python3 deepal-engineer-code.py                        # интерактивный режим (q — выход)
python3 deepal-engineer-code.py --selftest             # сверка с tests/fixtures.txt
```

## Установка

```bash
adb install -r deepal_code_gen.apk
```

APK подписан release-ключом из `codegen-app/keystore.jks`; для установки на устройство достаточно этой подписи (`*.apk.idsig` здесь не используется — он нужен только для Google Play).

## Структура

```
deepal-code-generator/
├── deepal-engineer-code.py     # исходный алгоритм (Python v3.1)
├── tests/fixtures.txt          # общие эталонные VIN→код для Python и Android-тестов
├── codegen-app/                # Android-проект
│   ├── keystore.jks            # ключ подписи (бэкап; пароли — в keystore.properties)
│   └── app/src/
│       ├── main/java/com/deepal/codegen/
│       │   ├── CodeGen.java    # алгоритм + валидация (вынесен в отдельный класс)
│       │   └── MainActivity.java
│       ├── main/res/           # UI (Material 3), строки ru/en, темы
│       └── test/               # unit-тесты CodeGen + копия fixtures
```

## Другие найденные коды (Deepal S05)

Набираются в приложении «Телефон» на головном устройстве: панель набора → код целиком (включая `*` и `#`) → кнопка «Позвонить». Вызов не инициируется, код перехватывается локально.

| Код | Что открывает |
|---|---|
| `111111#*#` | **DynoMode** — режим роликового стенда/динамометра, отключает вмешательство ESP/ABS (для сервисных станций) |
| `88888888***` | **SmartLog** — страница сбора и выгрузки журналов |

В инженерном меню (`*#XXXXXXXX#*`) есть вкладка «Другие тесты» (MiscTest) с заводскими кнопками: получение SN/certificate, запуск нативных настроек, список приложений, перезагрузка системы. Пароль этих кнопок дублируется в шагах инструкции приложения (`help_steps` в strings.xml) — при публикации APK учитывайте, что он раскрыт.
