# Анализ текущей архитектуры контроллеров и рекомендации по рефакторингу

> Дата анализа: 2026-05-11

## Текущая структура контроллеров

| Контроллер | Префикс | Кол-во эндпоинтов | Основная зона ответственности |
|------------|---------|-------------------|-------------------------------|
| `ArticleController` | `/api/articles` | 7 | Статьи, синхронизация статей, AI-аналитика, заметки |
| `CollectionController` | `/api/collections` | 11 | Коллекции, связи статья-коллекция, синхронизация коллекций |
| `AuthController` | `/api/auth` | 5 | Аутентификация, регистрация, токены |

---

## Выявленные архитектурные проблемы

### 1. Смешение зон ответственности в `ArticleController`

**Проблема:**
В одном контроллере смешаны три различных доменных контекста:

- **CRUD операции со статьями:** `GET /api/articles`, `DELETE /api/articles/{externalId}`
- **Синхронизация статей:** `POST /api/articles/sync`, `POST /api/articles/sync/deletions`
- **AI-аналитика:** `GET /api/articles/{externalId}/ai`, `POST /api/articles/{externalId}/ai/retry`
- **Заметки:** `DELETE /api/articles/{externalId}/notes/{noteId}`
- **Перекрестное обращение к `CollectionService`:** `GET /api/articles/{articleExternalId}/collections`

**Последствия:**
- Нарушение **Single Responsibility Principle** — контроллер отвечает за слишком многое.
- Сложность тестирования — чтобы протестировать AI-эндпоинты, приходится мокать и статьи, и синхронизацию.
- Размытая граница API — клиенту сложно понять, какие эндпоинты вызывать для конкретной задачи.
- Swagger-тег `@Tag(name = "Articles")` объединяет несвязанные операции (sync, AI, заметки).

---

### 2. Смешение зон ответственности в `CollectionController`

**Проблема:**
Аналогично `ArticleController`, здесь смешаны:

- **CRUD коллекций:** `POST /api/collections`, `PUT /api/collections/{externalId}`, `DELETE /api/collections/{externalId}`, `GET /api/collections`, `GET /api/collections/{externalId}`
- **Связи статья-коллекция:** `POST/DELETE /api/collections/{collectionExternalId}/articles/{articleExternalId}`, `GET /api/collections/{externalId}/articles`
- **Синхронизация коллекций и связей:** 4 эндпоинта на `.../sync` и `.../sync/deletions`

**Последствия:**
- При изменении логики синхронизации затрагивается контроллер коллекций.
- Batch-операции синхронизации (которые вызываются фоново) смешаны с интерактивными CRUD-операциями.

---

### 3. Перекрестная зависимость контроллеров

**Проблема:**
`ArticleController` инжектирует `CollectionService` только ради одного метода:

```java
@GetMapping("/{articleExternalId}/collections")
public ResponseEntity<List<String>> getArticleCollectionIds(...) {
    return ResponseEntity.ok(collectionService.getArticleCollectionIds(...));
}
```

**Последствия:**
- Контроллер статей "знает" о существовании сервиса коллекций.
- Нарушение границ Bounded Context.
- При удалении или изменении `CollectionService` придется править `ArticleController`.

---

## Рекомендации по переносу

### Рекомендация 1: Создать `SyncController` (ВЫСОКИЙ приоритет, **обязательно**)

**Обоснование:**
Синхронизация — это отдельный доменный контекст. Frontend вызывает sync-эндпоинты в фоновом режиме (service worker / background sync), в то время как CRUD-эндпоинты вызываются пользователем напрямую. Разделение позволит:
- Независимо версионировать API синхронизации.
- Применять разные политики rate-limiting (sync обычно тяжелее).
- Упростить тестирование — sync-тесты отдельно от CRUD-тестов.

**Эндпоинты для переноса:**

| Из | В | Метод |
|----|---|-------|
| `ArticleController` | `SyncController` | `POST /api/sync/articles` |
| `ArticleController` | `SyncController` | `POST /api/sync/articles/deletions` |
| `CollectionController` | `SyncController` | `POST /api/sync/collections` |
| `CollectionController` | `SyncController` | `POST /api/sync/collections/deletions` |
| `CollectionController` | `SyncController` | `POST /api/sync/collections/links` |
| `CollectionController` | `SyncController` | `POST /api/sync/collections/links/deletions` |

**Не требует изменений сервисов:**
`SyncController` будет использовать существующие `ArticleService` и `CollectionService` — нужно только перенести вызовы из контроллеров.

---

### Рекомендация 2: Создать `AiController` (СРЕДНИЙ приоритет, **опционально**)

**Обоснование:**
AI-эндпоинты (`getAiResult`, `retryAiAnalysis`) привязаны к статье, но представляют отдельный поддомен. Если планируется расширение AI-функциональности (анализ коллекций, summary и т.д.), вынос в отдельный контроллер оправдан.

**Варианты:**

- **Вариант A (рекомендуется):** `AiController` с базовым путем `/api/ai`
  - `GET /api/ai/articles/{externalId}` — получить результат анализа
  - `POST /api/ai/articles/{externalId}/retry` — повторить анализ

- **Вариант B:** Оставить в `ArticleController`, но вынести AI-логику из `ArticleService` в отдельный `AiArticleService`.

**Решение:** На текущий момент (2 эндпоинта) оставить в `ArticleController` допустимо. Если AI-функциональность расширится — вынести обязательно.

---

### Рекомендация 3: Создать `ArticleCollectionLinkController` (НИЗКИЙ приоритет, **опционально**)

**Обоснование:**
Связи "статья-коллекция" — это отдельная сущность (`ArticleCollectionLink`). Вынесение в отдельный контроллер позволит:
- Убрать перекрестную зависимость `ArticleController -> CollectionService`.
- Объединить все операции со связями в одном месте.

**Эндпоинты для переноса:**

| Из | В | Метод |
|----|---|-------|
| `CollectionController` | `ArticleCollectionLinkController` | `POST /api/collection-links` |
| `CollectionController` | `ArticleCollectionLinkController` | `DELETE /api/collection-links` |
| `CollectionController` | `ArticleCollectionLinkController` | `GET /api/collections/{externalId}/articles` |
| `ArticleController` | `ArticleCollectionLinkController` | `GET /api/articles/{externalId}/collections` |

**Решение:** Можно отложить. Пока проект маленький, хранение связей в `CollectionController` допустимо.

---

## Итоговая рекомендуемая структура

| Контроллер | Префикс | Эндпоинты |
|------------|---------|-----------|
| `ArticleController` | `/api/articles` | `GET /`, `DELETE /{externalId}`, `DELETE /{externalId}/notes/{noteId}` |
| `CollectionController` | `/api/collections` | `POST /`, `PUT /{externalId}`, `DELETE /{externalId}`, `GET /`, `GET /{externalId}` |
| `SyncController` | `/api/sync` | `POST /articles`, `POST /articles/deletions`, `POST /collections`, `POST /collections/deletions`, `POST /collections/links`, `POST /collections/links/deletions` |
| `AiController` | `/api/ai` | `GET /articles/{externalId}`, `POST /articles/{externalId}/retry` |
| `AuthController` | `/api/auth` | (без изменений) |

---

## Чек-лист реализации

1. [ ] Создать `SyncController` с 6 эндпоинтами.
2. [ ] Удалить sync-методы из `ArticleController` и `CollectionController`.
3. [ ] Обновить Swagger/OpenAPI теги (`@Tag`) для нового контроллера.
4. [ ] Обновить frontend: заменить `POST /api/articles/sync` → `POST /api/sync/articles` и т.д.
5. [ ] Добавить тесты для `SyncController`.
6. [ ] (Опционально) Создать `AiController` и перенести 2 AI-эндпоинта.

## Приоритет

- **Высокий:** `SyncController` — улучшает разделение ответственности и упрощает поддержку фоновой синхронизации.
- **Средний:** `AiController` — актуален при расширении AI-функциональности.
- **Низкий:** `ArticleCollectionLinkController` — можно отложить до роста проекта.
