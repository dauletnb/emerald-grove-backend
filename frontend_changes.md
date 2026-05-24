# Backend Fixes — Timestamps

## Проблема

Фронтенд отправляет `createdAt` заметок (и статей) в формате **epoch milliseconds** (`Date.now()` в JS → число вида `1716537600000`). При каждом sync через `POST /api/articles/sync` даты заметок сдвигаются на +5 часов (UTC+5, таймзона Almaty). Это значит, что бэкенд применяет timezone offset при сохранении или возврате timestamps.

### Симптомы

Пользователь создал 5 заметок в 09:24–09:26 (Almaty, UTC+5). После sync бэкенд вернул:

| Заметка | Ожидаемое время | Возвращённое время | Сдвиг |
|---------|----------------|--------------------|-------|
| тест    | 09:24:52       | 08:24:52           | −1h   |
| тест2   | 09:25:07       | 13:25:07           | +4h   |
| тест3   | 09:25:26       | 18:25:26           | +9h   |
| тест    | 09:25:53       | 23:25:53           | +14h  |
| тест3   | 09:26:05       | 04:26:05 (+1 day)  | +19h  |

Каждая следующая заметка сдвигается на **+5 часов** больше. Это означает, что при каждом round-trip (фронт → бэк → фронт) к каждой заметке прибавляется timezone offset.

## Корневая причина (вероятная)

В Java-коде timestamp заметки хранится или конвертируется через `LocalDateTime` **без явного указания timezone**. При этом:

1. Фронтенд отправляет `createdAt: 1716537600000` (epoch ms, UTC)
2. Бэкенд конвертирует в `LocalDateTime` используя системную timezone (UTC+5):
   ```java
   // НЕПРАВИЛЬНО — применяет серверную timezone
   LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAt), ZoneId.systemDefault())
   ```
3. При сериализации обратно в JSON бэкенд конвертирует `LocalDateTime` → epoch ms, снова применяя offset:
   ```java
   // НЕПРАВИЛЬНО — двойная конвертация timezone
   localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
   ```
4. Каждый sync добавляет ещё один offset (+5h) к каждой заметке

## Что нужно исправить

### 1. Использовать `Instant` вместо `LocalDateTime` для timestamps

В entity-классах заметок (например `ArticleNote.java` или `Note.java`):

```java
// БЫЛО (неправильно)
@Column(name = "created_at")
private LocalDateTime createdAt;

// СТАЛО (правильно)
@Column(name = "created_at")
private Instant createdAt;
```

### 2. Сериализация: epoch milliseconds

При маппинге DTO → JSON убедиться, что `createdAt` возвращается как epoch **milliseconds** (число), а не ISO-строка и не epoch seconds:

```java
// В DTO / response mapper
public Long getCreatedAt() {
    return createdAt != null ? createdAt.toEpochMilli() : null;
}
```

### 3. Десериализация: принимать epoch milliseconds

При получении `createdAt` от фронтенда:

```java
// В DTO / request mapper
public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt != null ? Instant.ofEpochMilli(createdAt) : null;
}
```

### 4. Убрать `ZoneId.systemDefault()` из всех конвертаций timestamps

Grep по проекту:

```
ZoneId.systemDefault()
TimeZone.getDefault()
LocalDateTime.ofInstant
```

Заменить все вхождения на явный `ZoneOffset.UTC` или использовать `Instant` напрямую.

### 5. PostgreSQL: тип колонки `TIMESTAMP WITH TIME ZONE`

Убедиться, что колонки дат в БД используют `TIMESTAMPTZ`, а не `TIMESTAMP`:

```sql
-- Проверить и при необходимости мигрировать (Flyway)
ALTER TABLE article_notes
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
```

### 6. JVM timezone (опционально, как страховка)

В `application.properties` или при старте приложения:

```properties
# application.properties
spring.jackson.time-zone=UTC
```

Или в `main()`:

```java
TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
```

### 7. Jackson конфигурация

Убедиться, что Jackson сериализует `Instant` как числа (epoch ms):

```properties
spring.jackson.serialization.write-dates-as-timestamps=true
spring.jackson.serialization.write-date-timestamps-as-nanoseconds=false
spring.jackson.deserialization.read-date-timestamps-as-nanoseconds=false
```

## Затронутые эндпоинты

| Метод | URL | Что проверить |
|-------|-----|---------------|
| POST | `/api/articles/sync` | Приём и возврат `notes[].createdAt` |
| GET | `/api/articles` | Возврат `notes[].createdAt` |
| GET | `/api/articles/{id}` | Возврат `notes[].createdAt` |

## Затронутые файлы (предположительно)

- `Note.java` / `ArticleNote.java` — entity
- `NoteDto.java` / `ArticleNoteDto.java` — DTO
- `ArticleMapper.java` — маппинг entity ↔ DTO
- `ArticleSyncService.java` / `ArticleServiceImpl.java` — бизнес-логика sync
- `application.properties` — Jackson/timezone настройки
- Flyway-миграция — если нужно менять тип колонки

## Как проверить после исправления

1. Создать заметку на фронтенде
2. Посмотреть в DevTools → Network → POST `/api/articles/sync` → Request Body → `notes[0].createdAt` (должно быть ~13-значное число)
3. Посмотреть Response Body → `article.notes[0].createdAt` — **должно совпадать** с отправленным значением
4. Создать ещё одну заметку, повторить проверку — обе заметки должны сохранить свои оригинальные `createdAt`

## Фронтенд-защита (уже реализована)

На фронтенде добавлена `normalizeTimestamp()` — если бэкенд вернёт epoch seconds (< 1e12), они автоматически умножаются на 1000. Это временная страховка, но **корневое исправление должно быть на бэкенде**.
