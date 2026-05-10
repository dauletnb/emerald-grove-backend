# План реализации переноса из Frontend в Backend

## Цель

Перенести критичную бизнес-логику и синхронизируемые данные из локального слоя Frontend в Backend так, чтобы:

- backend стал canonical source of truth после sync;
- offline-first сценарий остался рабочим;
- данные корректно синхронизировались между устройствами;
- frontend перестал хранить критичные sync-поля только локально.

---

## Текущее состояние проекта

### Уже реализовано

- backend на Spring Boot 4, Java 21, JPA, PostgreSQL, Flyway;
- JWT-аутентификация и защищенные `/api/**` endpoints;
- CRUD и bulk sync для коллекций;
- связи article <-> collection;
- базовый sync одной статьи;
- хранение заметок к статье;
- AI endpoints для статьи.

### Основной gap

Коллекции уже работают по bulk/offline-first паттерну, а статьи пока нет. Поэтому именно модуль `articles` сейчас является главным узким местом переноса.

---

## Критичные выводы по backend

### 1. Коллекции уже являются эталонным паттерном

`CollectionController` и `CollectionServiceImpl` уже задают нужную модель:

- online CRUD отдельно;
- offline bulk sync отдельно;
- `externalId` используется как главный идентификатор;
- после sync frontend должен делать повторный pull server state.

Эту же модель нужно перенести на статьи.

### 2. Article sync пока неполный

Сейчас backend поддерживает только:

- `POST /api/articles/sync` для одной статьи;
- `GET /api/articles`;
- `DELETE /api/articles/{externalId}`;
- операции по AI и заметкам.

Но отсутствуют:

- bulk sync статей;
- bulk sync удалений;
- sync полей `isFavorite` и `isReadLater`;
- серверная фильтрация system sections;
- conflict resolution по клиентским временным меткам.

### 3. Структура Article на backend не покрывает frontend-модель

В entity `Article` сейчас нет:

- `isFavorite`;
- `isReadLater`.

Это означает, что часть пользовательского состояния хранится только в IndexedDB и теряется при синхронизации между устройствами.

### 4. Sync-ответы пока слишком "немые"

Для коллекций backend может silently skip некоторые связи, если нет зависимых сущностей. Для текущего этапа это допустимо, но для статей и следующего шага миграции лучше перейти к более подробным sync-ответам:

- что создано;
- что обновлено;
- что пропущено;
- почему пропущено.

---

## Порядок реализации

## Этап 1. Синхронизировать article flags

### Что делаем

Добавляем в backend поддержку:

- `isFavorite`;
- `isReadLater`.

### Изменения

#### База данных

- новая миграция Flyway:
  - добавить в `emerald_grove.article` поля `is_favorite boolean not null default false`;
  - добавить `is_read_later boolean not null default false`.

#### Entity / DTO

- обновить `Article`;
- обновить `SyncArticleRequestDto`;
- обновить `ArticleSyncDto`;
- обновить `SyncArticlePayloadResponseDto`.

#### Service

- в `ArticleServiceImpl`:
  - сохранять флаги при create/update;
  - возвращать их во всех article payloads.

### Критерий готовности

- после sync статьи флаги сохраняются в БД;
- `GET /api/articles` возвращает эти флаги;
- frontend больше не обязан восстанавливать их только из локального слоя.

### Приоритет

Самый высокий. Без этого перенос article-состояния нельзя считать завершенным.

---

## Этап 2. Добавить bulk sync для статей

### Что делаем

Вводим отдельный bulk endpoint по аналогии с коллекциями.

### API

- `POST /api/articles/sync/bulk`

### Изменения

#### Controller / Service

- расширить `ArticleController`;
- расширить `ArticleService` и `ArticleServiceImpl`;
- реализовать пакетную обработку списка `SyncArticleRequestDto`.

#### Поведение

Для каждой статьи:

- если статья найдена по `externalId`, обновляем;
- если `externalId` нет, но статья найдена по `url + userId`, аккуратно обновляем;
- иначе создаем новую.

### Важное решение

Bulk sync должен использовать ту же логику, что и одиночный sync, а не дублировать ее. Лучше вынести общий private-метод, чтобы не получить расхождение поведения.

### Критерий готовности

- можно отправить пакет статей одной операцией;
- backend обрабатывает create/update консистентно;
- после bulk sync frontend может сделать единый pull актуального server state.

### Приоритет

Высокий. Это базовая часть offline-first для статей.

---

## Этап 3. Добавить bulk sync удалений

### Что делаем

Добавляем отдельные endpoints для синхронизации удалений, накопленных оффлайн.

### API

- `POST /api/articles/sync/deletions`
- `POST /api/collections/sync/deletions`
- `POST /api/collections/links/sync/deletions`

### Изменения

#### DTO

- завести простой request DTO с массивом `externalId` или специализированные DTO для links.

#### Service

- удалить все найденные сущности пользователя;
- отсутствующие записи обрабатывать идемпотентно, без падения всего батча.

### Критерий готовности

- оффлайн-удаления не "возвращаются" после следующего sync;
- локальное и серверное состояние не расходятся после reconnect.

### Приоритет

Высокий. Без этого offline-first модель остается неполной.

---

## Этап 4. Добавить server-side фильтрацию для system sections

### Что делаем

Расширяем `GET /api/articles`, чтобы backend умел отдавать:

- favorites;
- read later;
- recent.

### API

- `GET /api/articles?filter=favorites`
- `GET /api/articles?filter=readLater`
- `GET /api/articles?filter=recent&days=7`

### Изменения

#### Repository

- добавить методы выборки по флагам;
- добавить выборку recent по `createdAt` или `updatedAt` согласно продуктовой логике.

#### Controller / Service

- принимать query params;
- валидировать допустимые значения filter;
- не ломать текущее поведение `GET /api/articles` без параметров.

### Критерий готовности

- frontend может получать системные разделы с сервера;
- секции `FAVORITES` и `READ_LATER` больше не зависят только от локальной фильтрации.

### Приоритет

Средний. Это важно для UX, но не блокирует базовый перенос.

---

## Этап 5. Добавить conflict resolution по клиентским меткам времени

### Что делаем

В sync DTO добавляем клиентскую временную метку последнего изменения и вводим понятное правило разрешения конфликта.

### Базовое правило

На первом шаге достаточно стратегии `last write wins`:

- если `clientUpdatedAt > server.updatedAt`, принимаем клиентские изменения;
- иначе оставляем серверное состояние.

### Изменения

- добавить `clientUpdatedAt` в article sync DTO;
- при необходимости аналогично расширить collections и links;
- вернуть в ответе результат по каждой записи: accepted / skipped / conflicted.

### Риск

Это изменение затрагивает контракт синхронизации сильнее других. Его лучше делать после того, как базовые bulk-потоки уже заработают стабильно.

### Критерий готовности

- синхронизация становится предсказуемой при работе с нескольких устройств;
- backend может объяснить, почему запись была принята или отклонена.

### Приоритет

Средний-высокий.

---

## Этап 6. Сделать sync-ответы подробными

### Что делаем

Переходим от `void`/минимальных ответов к result DTO для bulk-операций.

### Новый формат ответа должен содержать

- список созданных сущностей;
- список обновленных сущностей;
- список пропущенных сущностей;
- причину пропуска;
- при конфликте статус записи.

### Где особенно важно

- `collections/links/sync`;
- `articles/sync/bulk`;
- deletion sync endpoints.

### Критерий готовности

- frontend может осознанно повторять sync только для проблемных записей;
- пользователь получает понятную диагностику вместо "200 OK, но часть данных пропала".

### Приоритет

Средний.

---

## Этап 7. Уточнить модель синхронизации AI status

### Что проверить

- должен ли `aiStatus` считаться только серверным полем;
- должен ли он возвращаться в общем `GET /api/articles`;
- нужен ли отдельный pull/polling endpoint для статусов AI.

### Рекомендуемое решение

Оставить `aiStatus` серверным полем и не позволять frontend изменять его через обычный article sync. Backend должен сам быть владельцем этого статуса.

### Критерий готовности

- статус AI одинаково виден на всех устройствах;
- обычный article sync не ломает жизненный цикл AI-задач.

### Приоритет

Ниже, чем article flags и bulk sync.

---

## Файлы backend, которые почти точно будут затронуты

- `src/main/java/com/emeraldgrove/entity/Article.java`
- `src/main/java/com/emeraldgrove/dto/SyncArticleRequestDto.java`
- `src/main/java/com/emeraldgrove/dto/ArticleSyncDto.java`
- `src/main/java/com/emeraldgrove/dto/SyncArticlePayloadResponseDto.java`
- `src/main/java/com/emeraldgrove/service/ArticleService.java`
- `src/main/java/com/emeraldgrove/service/impl/ArticleServiceImpl.java`
- `src/main/java/com/emeraldgrove/controller/ArticleController.java`
- `src/main/java/com/emeraldgrove/repository/ArticleRepository.java`
- `src/main/java/com/emeraldgrove/controller/CollectionController.java`
- `src/main/java/com/emeraldgrove/service/CollectionService.java`
- `src/main/java/com/emeraldgrove/service/impl/CollectionServiceImpl.java`
- `src/main/resources/db/migration/*`
- `src/test/java/com/emeraldgrove/controller/*`
- `src/test/java/com/emeraldgrove/service/*`

---

## Рекомендуемая очередность внедрения

1. Добавить `isFavorite` и `isReadLater` в БД, entity и DTO.
2. Обновить одиночный article sync под новые поля.
3. Добавить bulk sync статей.
4. Добавить bulk sync удалений.
5. Расширить `GET /api/articles` фильтрацией.
6. Добавить conflict resolution.
7. Сделать verbose bulk responses.
8. Доработать AI sync-модель.

---

## Минимальный рабочий scope следующего шага

Если идти небольшими безопасными итерациями, лучший следующий инкремент:

1. Миграция БД для article flags.
2. Обновление entity и DTO.
3. Обновление `ArticleServiceImpl`.
4. Тесты на create/update/get articles с новыми флагами.

Это даст первый завершенный кусок переноса без резкого изменения API.
