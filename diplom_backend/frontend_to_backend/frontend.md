# План изменений во Frontend

> Дата: 2026-05-11
> Контекст: в backend созданы `SyncController` (`/api/sync/...`) и `AiController` (`/api/ai/...`). Необходимо обновить все HTTP-запросы со старых путей на новые.

---

## 1. Обновить URL синхронизации статей

**Старые пути:**
- `POST /api/articles/sync`
- `POST /api/articles/sync/deletions`

**Новые пути:**
- `POST /api/sync/articles`
- `POST /api/sync/articles/deletions`

**Где искать:**
- Сервис/модуль синхронизации статей (например, `articleSyncService.ts`, `api/articles.ts`, `syncService.ts`).
- Service Worker / Background Sync — если используется `sync` event для отложенной отправки.
- Redux-thunks / Zustand-actions / Vuex-actions, отвечающие за отправку статей на сервер.
- Любые хуки (`useSyncArticles`, `useArticleSync`), вызывающие sync-методы.

**Что менять:**
- Заменить строку endpoint в конфигурации API-клиента.
- Проверить, что `Content-Type: application/json` и тело запроса (`SyncArticleRequestDto`, `ArticleDeletionSyncRequestDto`) остаются без изменений.

---

## 2. Обновить URL синхронизации коллекций

**Старые пути:**
- `POST /api/collections/sync`
- `POST /api/collections/sync/deletions`

**Новые пути:**
- `POST /api/sync/collections`
- `POST /api/sync/collections/deletions`

**Где искать:**
- Сервис синхронизации коллекций (например, `collectionSyncService.ts`, `api/collections.ts`).
- Фоновая синхронизация коллекций в Service Worker.
- State-management actions, отправляющие batch-запросы коллекций.

---

## 3. Обновить URL синхронизации связей (collection links)

**Старые пути:**
- `POST /api/collections/links/sync`
- `POST /api/collections/links/sync/deletions`

**Новые пути:**
- `POST /api/sync/collections/links`
- `POST /api/sync/collections/links/deletions`

**Где искать:**
- Сервис связей статья-коллекция.
- Batch-операции добавления/удаления статей из коллекций при синхронизации.
- Offline-queue, если удаления связей кэшируются и отправляются позже.

---

## 4. Обновить URL AI-аналитики статей

**Старые пути:**
- `GET /api/articles/{externalId}/ai`
- `POST /api/articles/{externalId}/ai/retry`

**Новые пути:**
- `GET /api/ai/articles/{externalId}`
- `POST /api/ai/articles/{externalId}/retry`

**Где искать:**
- Компонент отображения AI-результата (например, `ArticleDetail`, `AiPanel`, `AnalysisTab`).
- Кнопка "Повторить анализ" и её обработчик.
- API-сервис для AI-запросов.

---

## 5. Проверить Service Worker / Background Sync

**Проблема:**
Service Worker может хранить старые URL в `sync` tag-ах или в IndexedDB для отложенных запросов.

**Что проверить:**
- Все `registration.sync.register('...')` — убедиться, что логика обработки `sync` event отправляет на новые пути.
- Если offline-queue хранит URL как строки в IndexedDB — мигрировать записи или очистить queue (если допустимо).
- Функция-обработчик `fetch` или `sync` event в SW, которая читает URL из очереди и выполняет `fetch()`.

---

## 6. Проверить переменные окружения / конфиг API

**Где искать:**
- `.env`, `.env.local`, `.env.production` — если base URL или пути задаются через env.
- Константы API: `API_ENDPOINTS`, `ROUTES`, `URLS` и т.п.
- API-клиент (Axios instance / Fetch wrapper) — убедиться, что нет хардкода старых путей.

---

## 7. Тестирование

### 7.1. Online-режим
- [ ] Добавить статью → `POST /api/sync/articles` возвращает `201/200`.
- [ ] Удалить статью (online) → `DELETE /api/articles/{id}` работает как раньше.
- [ ] Синхронизация коллекций → `POST /api/sync/collections` возвращает `SyncBatchResponseDto`.
- [ ] Синхронизация связей → `POST /api/sync/collections/links` корректно применяет/пропускает записи.
- [ ] Получить AI-результат → `GET /api/ai/articles/{id}` возвращает `ArticleAiResponseDto`.
- [ ] Повторить анализ → `POST /api/ai/articles/{id}/retry` возвращает `202 Accepted`.

### 7.2. Offline-режим
- [ ] Добавить статью offline → запрос кладётся в очередь.
- [ ] Вернуть интернет → queued-запрос отправляется на `POST /api/sync/articles`.
- [ ] Проверить Network tab: **нет** запросов на старые пути (`/api/articles/sync`).

### 7.3. Swagger / API-документация
- [ ] Проверить, что frontend-типы (`SyncArticleRequestDto`, `CollectionSyncDto` и т.д.) совпадают с backend-DTO.
- [ ] Если используется генерация клиента (OpenAPI Generator) — перегенерировать клиент с нового Swagger.

---

## 8. Чек-лист замены URL

| # | Старый URL | Новый URL | Статус |
|---|-----------|-----------|--------|
| 1 | `POST /api/articles/sync` | `POST /api/sync/articles` | [ ] |
| 2 | `POST /api/articles/sync/deletions` | `POST /api/sync/articles/deletions` | [ ] |
| 3 | `POST /api/collections/sync` | `POST /api/sync/collections` | [ ] |
| 4 | `POST /api/collections/sync/deletions` | `POST /api/sync/collections/deletions` | [ ] |
| 5 | `POST /api/collections/links/sync` | `POST /api/sync/collections/links` | [ ] |
| 6 | `POST /api/collections/links/sync/deletions` | `POST /api/sync/collections/links/deletions` | [ ] |
| 7 | `GET /api/articles/{externalId}/ai` | `GET /api/ai/articles/{externalId}` | [ ] |
| 8 | `POST /api/articles/{externalId}/ai/retry` | `POST /api/ai/articles/{externalId}/retry` | [ ] |

---

## Примечание

- Все **CRUD-эндпоинты** (`GET /api/articles`, `DELETE /api/articles/{id}`, `POST /api/collections` и т.д.) **не изменились** — трогать их не нужно.
- Изменились **sync-эндпоинты** (batch-операции, фоновая синхронизация) и **AI-эндпоинты**.
- Если используется TypeScript — после обновления URL убедиться, что `fetch` / axios-вызовы не ломаются из-за типизации response (типы DTO не менялись).
