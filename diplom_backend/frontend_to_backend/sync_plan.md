# План реализации `SyncController`

## 1. Создать `SyncController`
- **Файл:** `src/main/java/com/emeraldgrove/controller/SyncController.java`
- **Путь:** `@RequestMapping("/api/sync")`
- **Зависимости:** `ArticleService`, `CollectionService`, `ControllerUtil`
- **Swagger:** `@Tag(name = "Sync")`

Перенести 6 эндпоинтов:

| Метод | Новый путь | Источник |
|-------|-----------|----------|
| `POST` | `/api/sync/articles` | `ArticleController.syncArticle` |
| `POST` | `/api/sync/articles/deletions` | `ArticleController.syncDeletedArticles` |
| `POST` | `/api/sync/collections` | `CollectionController.syncCollections` |
| `POST` | `/api/sync/collections/deletions` | `CollectionController.syncDeletedCollections` |
| `POST` | `/api/sync/collections/links` | `CollectionController.syncCollectionLinks` |
| `POST` | `/api/sync/collections/links/deletions` | `CollectionController.syncDeletedCollectionLinks` |

## 2. Очистить `ArticleController`
- Удалить методы `syncArticle` и `syncDeletedArticles`.
- Удалить неиспользуемые DTO-импорты (`SyncArticleRequestDto`, `SyncArticleResponseDto`, `ArticleDeletionSyncRequestDto`, `SyncBatchResponseDto`).
- Удалить инжекцию `CollectionService`, если `getArticleCollectionIds` тоже переносится (опционально).
- Обновить `@Tag` description.

## 3. Очистить `CollectionController`
- Удалить методы `syncCollections`, `syncDeletedCollections`, `syncCollectionLinks`, `syncDeletedCollectionLinks`.
- Удалить неиспользуемые DTO-импорты (`SyncBatchResponseDto`, `CollectionSyncDto`, `ExternalIdDeletionRequestDto`, `CollectionLinkBatchSyncResponseDto`, `CollectionLinkSyncDto`, `CollectionLinkDeletionDto`).
- Обновить `@Tag` description.

## 4. Обновить Frontend
- Заменить `POST /api/articles/sync` → `POST /api/sync/articles`
- Заменить `POST /api/articles/sync/deletions` → `POST /api/sync/articles/deletions`
- Заменить `POST /api/collections/sync` → `POST /api/sync/collections`
- Заменить `POST /api/collections/sync/deletions` → `POST /api/sync/collections/deletions`
- Заменить `POST /api/collections/links/sync` → `POST /api/sync/collections/links`
- Заменить `POST /api/collections/links/sync/deletions` → `POST /api/sync/collections/links/deletions`

## 5. Тестирование
- Скомпилировать проект (`mvn compile`).
- Проверить Swagger UI: эндпоинты отображаются в группе **Sync**.
- Запустить интеграционные тесты синхронизации (если есть).
- Проверить frontend: синхронизация статей и коллекций работает корректно.

## Приоритет
Создание `SyncController` и очистка старых контроллеров — **высокий**, можно выполнить без изменений в сервисном слое.
