# План реализации исправлений проблем удаления

> Статус: **выполнена диагностика + frontend-исправления** (2026-05-11)  
> Цель: устранить ситуацию, при которой DELETE-запросы к API не приводят к физическому удалению записей из PostgreSQL или записи воскресают при синхронизации.
>
> **Frontend-правки реализованы.** Backend-правки (JPA-связи, логирование) остаются в `Emerald-Grove-Backend`.

---

## Результаты диагностики (выполнено 2026-05-11)

- **`ON DELETE CASCADE` присутствует** на всех критичных FK.
- **Миграции V1–V3 применены корректно**, `flyway_schema_history` в порядке.
- **В БД нет осиротевших записей**; по статьям и коллекциям БД пуста.
- **Главная гипотеза (отсутствие CASCADE) НЕ ПОДТВЕРДИЛАСЬ.**
- **DELETE-запросы с frontend уходят корректно** (`deleteArticleFromServer`, `deleteNoteFromServer`, `deleteCollectionOnServer`).
- **Удаление коллекций в UI отсутствует полностью** — функции есть, но не вызываются.

### Результаты диагностики Этапа 1 (выполнено 2026-05-11, online-режим)

| Проверка | Результат |
|----------|-----------|
| Логи backend после удаления статьи | `Hibernate: delete from emerald_grove.article where id=?` — **DELETE выполняется**, `ROLLBACK` отсутствует |
| Повторный fetch после удаления | `SELECT ... FROM article` возвращает **0 записей** — статья **не воскресает** |
| Network DevTools (Library page) | **`DELETE /api/articles/{id}` уходит корректно**, backend возвращает `204 No Content` |
| `POST /api/articles/sync/deletions` (batch) | Не наблюдался в момент удаления; удаление происходит через **прямой DELETE**, а не batch-sync |

**Вывод:** В online-режиме удаление работает **полностью корректно**: frontend шлёт `DELETE`, backend возвращает `204`, запись физически удаляется из БД (`delete from article`), и при последующем `fetchArticlesFromServer` статья **не воскресает** (0 записей).

**Результаты offline-теста (выполнено 2026-05-11):**

| Проверка | Результат |
|----------|-----------|
| Удаление статьи без сети | Статья исчезает из UI локально |
| Возвращение интернета + обновление Library | `GET /api/articles` возвращает `200`, удалённая статья **отсутствует** в ответе |
| Повторный fetch | Статья **не воскресает** |

**Вывод:** Offline-сценарий тоже работает корректно. `skippedCount`-проблема не воспроизвелась в текущих условиях, но защитная проверка в коде всё равно нужна (регрессия).

**Оставшиеся риски (требуют исправления):**
1. ✅ **Frontend:** `clearPendingArticleDeletions` — добавлена проверка `skippedCount` в `syncArticlesWithServer` и `syncPendingCollections`.
2. ✅ **Frontend:** UI удаления коллекций — реализована кнопка и `handleDeleteCollection`.
3. **Backend:** JPA-связи в `Article`, `deleteCollection` без предзагрузки `articleLinks`, `deleteNote` через `save()` — защита от будущих регрессий (репозиторий `Emerald-Grove-Backend`).
4. **Frontend (отложено):** фильтрация "воскрешённых" записей после fetch (3.3) — не критично, т.к. не воспроизводится в текущих условиях.
5. **Frontend (отложено):** optimistic rollback при ошибке удаления (3.4) — можно реализовать при необходимости.

---

## Этап 1. Диагностика API (критический, блокирует всё остальное)

| Шаг | Действие | Как выполнить |
|-----|----------|---------------|
| 1.1 | Включить полное логирование SQL на backend | `logging.level.org.hibernate.SQL=DEBUG`, `spring.jpa.properties.hibernate.format_sql=true` |
| 1.2 | Создать тестовую статью через API | `POST /api/articles/sync` |
| 1.3 | Вызвать DELETE статьи через API напрямую | `DELETE /api/articles/{externalId}` |
| 1.4 | Проверить логи backend | В логах должен быть `DELETE FROM article ...`, НЕ должно быть `ROLLBACK` или `ConstraintViolationException` |
| 1.5 | Проверить БД после DELETE | `SELECT COUNT(*) FROM article WHERE external_id = '...'` должен вернуть `0` |
| 1.6 | Проверить поведение frontend | Открыть DevTools расширения → Network, выполнить удаление, убедиться что `DELETE` уходит и возвращает `204` |

**Результат этапа:** понятно, на каком уровне (frontend / backend / БД) происходит сбой. **Не приступать к правкам кода без результата этого этапа.**

---

## Этап 2. Исправления backend

### 2.1. Добавление недостающих JPA-связей (защита от регрессий)

**Приоритет: высокий**

| Шаг | Действие | Файл |
|-----|----------|------|
| 2.1.1 | Добавить `@OneToMany` на `List<AiJob>` в `Article` | `entity/Article.java` |
| 2.1.2 | Добавить `@OneToMany` на `List<AiResult>` в `Article` | `entity/Article.java` |
| 2.1.3 | Добавить `@OneToMany` на `List<ArticleCollectionLink>` в `Article` | `entity/Article.java` |
| 2.1.4 | Убрать `orphanRemoval = true` из `ArticleCollection.articleLinks` (или оставить, но быть уверенным в `@EntityGraph`) | `entity/ArticleCollection.java` |

**Стратегия:** оставить `ON DELETE CASCADE` на уровне БД, а в JPA использовать `cascade` + `orphanRemoval` только для `notes`.

### 2.2. Исправление `CollectionServiceImpl.deleteCollection()`

**Приоритет: высокий**

| Шаг | Действие | Файл |
|-----|----------|------|
| 2.2.1 | Добавить `@EntityGraph(attributePaths = "articleLinks")` или отдельный метод `findByExternalIdAndUserIdWithLinks` | `repository/ArticleCollectionRepository.java` |
| 2.2.2 | Использовать новый метод в `deleteCollection` | `service/impl/CollectionServiceImpl.java` |

**Альтернатива:** полагаться только на БД-каскад, убрав `cascade`/`orphanRemoval` из `ArticleCollection.articleLinks`.

### 2.3. Исправление `ArticleServiceImpl.deleteNote()`

**Приоритет: средний**

| Шаг | Действие | Файл |
|-----|----------|------|
| 2.3.1 | Создать `ArticleNoteRepository` с методом `deleteByExternalIdAndArticleExternalId` | `repository/ArticleNoteRepository.java` |
| 2.3.2 | Заменить текущую реализацию `deleteNote` на прямой вызов репозитория | `service/impl/ArticleServiceImpl.java` |

### 2.4. Добавление логирования в backend

**Приоритет: низкий (удобство отладки)**

| Шаг | Действие | Файл |
|-----|----------|------|
| 2.4.1 | Добавить `log.info(...)` / `log.debug(...)` в `deleteArticle`, `deleteCollection`, `deleteNote`, `syncDeletedArticles`, `syncDeletedCollections` | `ArticleServiceImpl.java`, `CollectionServiceImpl.java` |

---

## Этап 3. Исправления frontend (на основе анализа кода)

### 3.1. Логирование удаления в DevTools (для отладки)

**Приоритет: высокий (временное, для диагностики)**

| Шаг | Действие | Файл |
|-----|----------|------|
| 3.1.1 | Добавить `console.log` перед `deleteArticleFromServer`, `deleteNoteFromServer`, `deleteCollectionOnServer` с выводом ID и URL | `shared/api/articles.ts`, `shared/api/collections.ts` |
| 3.1.2 | Добавить `console.log` с `response.status` после каждого `DELETE` | `shared/api/articles.ts`, `shared/api/collections.ts` |
| 3.1.3 | Добавить `console.log` в `syncArticlesWithServer` и `syncPendingCollections` с выводом списка pending deletions | `library/LibraryApp.tsx` |

**После стабилизации:** логи можно понижать до `console.debug` или убрать.

### 3.2. Исправление синхронизации удалённых статей (критично) ✅ Выполнено

**Приоритет: высокий**

| Шаг | Действие | Файл | Статус |
|-----|----------|------|--------|
| 3.2.1 | Перед `clearPendingArticleDeletions([id])` в `handleDelete` проверить, что `response.ok === true` | `library/LibraryApp.tsx` | ✅ Уже работало |
| 3.2.2 | В `syncArticlesWithServer` перед `clearPendingArticleDeletions` проверить `result.skippedCount === 0` | `library/LibraryApp.tsx` | ✅ Реализовано |
| 3.2.3 | То же самое для `syncPendingCollections` — проверить `skippedCount` перед `clearPendingCollectionDeletions` и `clearPendingCollectionLinkDeletions` | `library/LibraryApp.tsx` | ✅ Реализовано |

**Результат:** если backend вернул `SKIPPED`, frontend не очищает очередь удалений, и они повторно отправятся при следующей синхронизации.

### 3.3. Фильтрация "воскрешённых" записей после fetch (отложено)

**Приоритет: высокий → отложено**

| Шаг | Действие | Файл | Статус |
|-----|----------|------|--------|
| 3.3.1 | В `syncArticlesWithServer` после `fetchArticlesFromServer` отфильтровать `remoteArticles`, исключая ID из `deletedArticles` | `library/LibraryApp.tsx` | ⏸ Отложено (не воспроизводится) |
| 3.3.2 | Аналогично для коллекций и линков | `library/LibraryApp.tsx` | ⏸ Отложено |

**Почему отложено:** диагностика показала, что записи **не воскресают** ни в online, ни в offline-режиме. Реализация остаётся как защита от регрессии на будущее.

### 3.4. Optimistic rollback при ошибке удаления статьи (отложено)

**Приоритет: средний → отложено**

| Шаг | Действие | Файл | Статус |
|-----|----------|------|--------|
| 3.4.1 | Если `deleteArticleFromServer` упал с ошибкой, вернуть статью в `setArticles` | `library/LibraryApp.tsx` | ⏸ Отложено |
| 3.4.2 | Показать явное сообщение пользователю | `library/LibraryApp.tsx` | ⏸ Отложено |

**Альтернатива (проще):** сначала вызывать `deleteArticleFromServer`, и только при успехе удалять из IndexedDB и UI.

### 3.5. UI для удаления коллекций ✅ Выполнено

**Приоритет: средний**

| Шаг | Действие | Файл | Статус |
|-----|----------|------|--------|
| 3.5.1 | Добавить кнопку удаления (иконку корзины) рядом с каждой пользовательской коллекцией в сайдбаре | `library/LibraryApp.tsx` | ✅ Реализовано |
| 3.5.2 | Добавить `handleDeleteCollection(collectionId)` в `LibraryApp.tsx` | `library/LibraryApp.tsx` | ✅ Реализовано |
| 3.5.3 | `handleDeleteCollection` должен удалять из IndexedDB, сервера и UI | `library/LibraryApp.tsx` | ✅ Реализовано |

### 3.6. Фоновая синхронизация pending deletions

**Приоритет: низкий**

| Шаг | Действие | Файл |
|-----|----------|------|
| 3.6.1 | Добавить в `background.ts` обработчик периодической синхронизации (`chrome.alarms` или `setInterval`) для `syncArticlesWithServer` / `syncPendingCollections` | `background/background.ts` |
| 3.6.2 | Либо вызывать `syncArticlesWithServer` по событию `chrome.runtime.onMessage` с типом `SYNC_PENDING` | `background/background.ts` |

**Почему:** сейчас pending deletions обрабатываются только при открытии `LibraryApp`. Если пользователь удалил статью и закрыл вкладку — удаление может не дойти до сервера.

---

## Этап 4. Тестирование (критический)

### 4.1. Backend (ручное через curl/Postman)

| Шаг | Действие | Ожидаемый результат |
|-----|----------|-------------------|
| 4.1.1 | POST `/api/articles/sync` — создать статью с заметками и связями | `201 Created` |
| 4.1.2 | DELETE `/api/articles/{externalId}` — удалить статью | `204 No Content`, в БД 0 записей в `article`, `article_note`, `ai_job`, `ai_result`, `article_collection_link` |
| 4.1.3 | DELETE `/api/collections/{externalId}` — удалить коллекцию со статьями | `204 No Content`, коллекция удалена, статьи остаются |
| 4.1.4 | DELETE `/api/articles/{articleId}/notes/{noteId}` — удалить заметку | `204 No Content`, запись исчезла из `article_note` |
| 4.1.5 | POST `/api/articles/sync/deletions` — batch-удаление | `204` или `200` с корректным `skippedCount` |

### 4.2. Frontend (ручное через DevTools расширения)

| Шаг | Действие | Ожидаемый результат |
|-----|----------|-------------------|
| 4.2.1 | Сохранить статью → удалить → проверить Network | `DELETE` уходит, возвращает `204` |
| 4.2.2 | Перезагрузить LibraryApp (или подождать синхронизации) | Удалённая статья **не воскресает** |
| 4.2.3 | Создать коллекцию → удалить через UI | `DELETE /api/collections/{id}` уходит, коллекция исчезает и не возвращается |
| 4.2.4 | Удалить заметку | `DELETE` уходит, заметка не появляется после `fetchArticlesFromServer` |
| 4.2.5 | Отключить сеть → удалить статью → включить сеть → открыть LibraryApp | Статья удаляется на сервере после восстановления связи |

---

## Этап 5. Финализация

| Шаг | Действие |
|-----|----------|
| 5.1 | Убедиться, что backend `application.properties` использует `ddl-auto=validate` |
| 5.2 | Убрать/понизить `console.log` в frontend, оставив только осмысленные `console.warn` при ошибках |
| 5.3 | Обновить `deletion_problems.md`: отметить, какие гипотезы подтвердились и как исправлены |
| 5.4 | Смержить ветку с фиксами, убедиться что CI/CD проходит |

---

## Зависимости между этапами

```
Этап 1 (Диагностика)
    |
    ├──> Этап 2 (Backend-исправления) ──> Этап 4 (Тестирование) ──> Этап 5 (Финализация)
    |
    ├──> Этап 3 (Frontend-исправления) ──> Этап 4 (Тестирование) ──> Этап 5 (Финализация)
```

**Этап 1 обязателен.** Без него правки в коде могут быть избыточными или недостаточными.

