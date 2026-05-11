# Анализ проблем удаления (статьи, заметки, коллекции)

> Дата анализа: 2026-05-11
> Предположение: при вызове DELETE-методов контроллеров записи физически остаются в PostgreSQL.

---

## 1. Скрытая зависимость от БД-каскада (высокий риск)

**Где:** `ArticleServiceImpl.deleteArticle()`, `CollectionServiceImpl.deleteCollection()`

**Проблема:**
- Сущность `Article` **не объявляет** JPA-связей `OneToMany` на `AiJob`, `AiResult`, `ArticleCollectionLink`.
- `ArticleCollection` объявляет `@OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)` на `articleLinks`, но репозиторий `findByExternalIdAndUserId` **не использует** `@EntityGraph(attributePaths = "articleLinks")`.
- Удаление полагается исключительно на `ON DELETE CASCADE` в PostgreSQL.

**Почему это опасно:**
- Если таблицы были созданы Hibernate ДО включения Flyway (например, при `ddl-auto=update/create`), а затем Flyway включили с `baseline-on-migrate=true`, миграции V1/V2 с `ON DELETE CASCADE` могли **не примениться**.
- Внешние ключи существуют, но **без** `ON DELETE CASCADE`.
- Hibernate не знает об этих связях на уровне JPA (у `Article` нет `OneToMany` на `ArticleCollectionLink`), поэтому не выполняет каскад сам.
- При `repository.delete(article)` PostgreSQL выбрасывает `ConstraintViolationException`, Spring откатывает транзакцию.
- **Результат:** контроллер может вернуть 500 (или ошибка глобально обработана), а запись в БД остается, т.к. транзакция откатилась.

**Как подтвердить:**
```sql
-- Проверить правила удаления для FK
SELECT tc.constraint_name, ccu.column_name, rc.delete_rule
FROM information_schema.table_constraints tc
JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
JOIN information_schema.referential_constraints rc ON tc.constraint_name = rc.constraint_name
WHERE tc.table_schema = 'emerald_grove' AND tc.constraint_type = 'FOREIGN KEY';
```
- Если `delete_rule != 'CASCADE'` — это подтвержденная причина.

**Проверить Flyway:**
```sql
SELECT * FROM emerald_grove.flyway_schema_history ORDER BY installed_rank;
```

---

## Результаты диагностики (2026-05-11)

### 1. Проверка `delete_rule` для всех FK

```
 constraint_name            | from_table         | from_col | delete_rule
----------------------------+--------------------+----------+-------------
 ai_jobs_article_id_fkey    | article            | id       | CASCADE
 ai_results_article_id_fkey | article            | id       | CASCADE
 fk_link_article            | article            | id       | CASCADE
 fk_article_notes_article   | article            | id       | CASCADE
 fk_link_collection         | article_collection | id       | CASCADE
 refresh_tokens_user_id_fkey| user               | id       | CASCADE
 articles_user_id_fkey      | user               | id       | NO ACTION
 fk_collection_user         | user               | id       | CASCADE
```

**Вывод:** `ON DELETE CASCADE` **присутствует** на всех критичных FK (`article_note`, `ai_job`, `ai_result`, `article_collection_link`).
Гипотеза об отсутствии каскада **НЕ ПОДТВЕРДИЛАСЬ**.

### 2. Проверка `flyway_schema_history`

| installed_rank | version | description               | script                        | success |
|----------------|---------|---------------------------|-------------------------------|---------|
| 1              | 1       | create articles           | V1__create_articles.sql       | true    |
| 2              | 2       | create collections tables | V2__create_collections_tables.sql | true |
| 3              | 3       | add article flags         | V3__add_article_flags.sql     | true    |

**Вывод:** Все миграции применены корректно.

### 3. Проверка осиротевших записей

| Таблица                  | Количество осиротевших записей |
|--------------------------|--------------------------------|
| `article_note`           | 0                              |
| `ai_job`                 | 0                              |
| `article_collection_link`| 0                              |
| `article`                | 0 (всего 0 статей в БД)        |
| `article_collection`     | 0 (всего 0 коллекций в БД)     |

**Вывод:** В БД **нет осиротевших записей**, БД полностью пуста по статьям и коллекциям (есть только 1 пользователь).

### Итог диагностики

- Проблема "оставшихся записей" **не подтверждена** на уровне БД.
- `ON DELETE CASCADE` работает корректно, миграции применены.
- Скорее всего, предположение пользователя основано на код-ревью, а не на фактическом наблюдении.
- Если фронтенд "видит" записи после удаления — причина скорее в **синхронизации** (frontend не отправляет DELETE, или повторно создаёт записи при следующем sync), либо в **ошибках на уровне API** (транзакции, исключения), а не в схеме БД.

---

## 2. Избыточность каскадного удаления (JPA + БД)

**Где:** `Article.notes`, `ArticleCollection.articleLinks`

**Проблема:**
- `Article` имеет `@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)` на `notes`.
- `ArticleCollection` имеет аналогичную связь на `articleLinks`.
- При этом SQL-миграции (`V1__create_articles.sql`, `V2__create_collections_tables.sql`) на те же FK настроили `ON DELETE CASCADE`.

**Последствия:**
- Hibernate при `delete()` может сам сгенерировать `DELETE` для дочерних записей, а затем PostgreSQL тоже выполнит каскад.
- Это дублирование и лишние SQL-запросы.
- Смешивание стратегий делает код хрупким: изменение в одном месте (JPA или БД) может сломать удаление.

**Рекомендация:**
- Выбрать одну стратегию: либо каскад на уровне JPA (удобнее), либо на уровне БД (быстрее).

---

## 3. Удаление ArticleCollection без предзагрузки связей

**Где:** `CollectionServiceImpl.deleteCollection()`

```java
@Override
@Transactional
public void deleteCollection(String externalId, Long userId) {
    collectionRepository.delete(getOwnedCollection(externalId, userId));
}
```

**Проблема:**
- `getOwnedCollection` вызывает `findByExternalIdAndUserId`, который **не загружает** `articleLinks`.
- Hibernate при `delete()` с `orphanRemoval = true` на неинициализированной LAZY-коллекции может:
  1. Выполнить доп. `SELECT` для загрузки links, затем удалить по одному.
  2. Проигнорировать JPA-cascade и выполнить только `DELETE FROM article_collection`, полагаясь на БД.
- Поведение зависит от версии Hibernate. Это **неявное поведение**.

**Рекомендация:**
- Добавить `@EntityGraph(attributePaths = "articleLinks")` или отдельный метод загрузки коллекции со связями перед удалением.
- Либо убрать JPA-cascade из `ArticleCollection` и полагаться только на БД (при условии п. 1).

---

## 4. Удаление заметки через save() вместо явного delete()

**Где:** `ArticleServiceImpl.deleteNote()`

```java
@Override
@Transactional
public void deleteNote(String articleExternalId, String noteExternalId, Long userId) {
    Article article = articleRepository.findByExternalIdAndUserId(articleExternalId, userId)
        .orElseThrow(...);
    boolean removed = article.getNotes().removeIf(note -> note.getExternalId().equals(noteExternalId));
    if (!removed) { throw ...; }
    articleRepository.save(article);
}
```

**Проблема:**
- Удаление косвенное: через `removeIf` + `orphanRemoval`, а не `repository.delete(note)`.
- `save(article)` избыточен (entity уже managed).
- В текущем коде работает благодаря `@EntityGraph(attributePaths = "notes")`, но подход менее надежен, чем прямое удаление.

**Рекомендация:**
- Создать `ArticleNoteRepository.deleteByExternalIdAndArticleExternalId(...)` и вызывать напрямую.

---

## 5. Недостаточное логирование

**Где:** `ArticleServiceImpl`, `CollectionServiceImpl`

**Проблема:**
- Методы `deleteArticle`, `deleteCollection`, `deleteNote`, `syncDeletedArticles`, `syncDeletedCollections` не логируют факт удаления.
- При откате транзакции или `ConstraintViolationException` сложно понять из логов, что пошло не так.
- `spring.jpa.show-sql=true` включено, но без логов сервиса отладка затруднена.

**Рекомендация:**
- Добавить `log.info(...)` / `log.debug(...)` перед каждым вызовом `repository.delete(...)`.

---

## 6. Проверочный чек-лист

1. **Проверить структуру FK в БД** (см. SQL в п. 1).
   - Если `delete_rule = 'CASCADE'` — БД-каскад работает, предположение требует дальнейшей проверки.
   - Если `delete_rule = 'NO ACTION'` или отсутствует — **подтвержденная причина**.

2. **Проверить `flyway_schema_history`:**
   ```sql
   SELECT * FROM emerald_grove.flyway_schema_history ORDER BY installed_rank;
   ```
   - Если таблицы существуют, а записей о V1/V2 нет — миграции не применялись.

3. **Включить детальное логирование SQL:**
   ```properties
   spring.jpa.properties.hibernate.format_sql=true
   logging.level.org.hibernate.SQL=DEBUG
   logging.level.org.hibernate.orm.jdbc.bind=TRACE
   ```
   - Выполнить `DELETE /api/articles/{id}` и посмотреть, есть ли `DELETE`, и не следует ли `ROLLBACK`.

4. **Ручной тест через psql:**
   - Найти `external_id` статьи.
   - Выполнить `DELETE /api/articles/{externalId}`.
   - Проверить:
     ```sql
     SELECT COUNT(*) FROM emerald_grove.article_collection_link WHERE article_id = ...;
     SELECT COUNT(*) FROM emerald_grove.ai_job WHERE article_id = ...;
     SELECT COUNT(*) FROM emerald_grove.article_note WHERE article_id = ...;
     ```
   - Если счетчик > 0 — связи остались.

5. **Проверить HTTP-ответ:**
   - Если фронтенд получает `204`, но запись остается — транзакция, скорее всего, **откатилась** (см. логи на `ConstraintViolationException`).
   - Если приходит `500` — проблема 100% в orphaned FK или отсутствии `ON DELETE CASCADE`.

---

## 7. Результаты frontend-диагностики (2026-05-11)

### 7.1. Online-режим

| Проверка | Результат |
|----------|-----------|
| `DELETE /api/articles/{id}` уходит из frontend | ✅ **Да**, виден в DevTools страницы Library |
| HTTP-статус ответа backend | ✅ **`204 No Content`** |
| Статья физически удаляется из БД | ✅ **Да**, `Hibernate: delete from article where id=?` |
| Статья воскресает после fetch | ✅ **Нет**, `SELECT` возвращает 0 записей |

### 7.2. Offline-режим

| Проверка | Результат |
|----------|-----------|
| Удаление без сети | ✅ Статья исчезает из UI локально |
| Возвращение интернета + синхронизация | ✅ `GET /api/articles` не содержит удалённую статью |
| Batch-deletion (`POST /api/articles/sync/deletions`) | ✅ Не требуется; прямой DELETE сработал при наличии сети |

### 7.3. Выполненные frontend-исправления

1. ✅ **`skippedCount` проверка** — добавлена в `syncArticlesWithServer`, `syncPendingCollections` (коллекции и линки).
2. ✅ **UI удаления коллекций** — реализована кнопка в sidebar и `handleDeleteCollection`.

### Итог диагностики

- **Гипотеза об отсутствии `ON DELETE CASCADE`** — **ОПРОВЕРГНУТА**. Каскад присутствует на всех FK, миграции применены корректно.
- **Гипотеза о неотправке DELETE с frontend** — **ОПРОВЕРГНУТА**. Запросы уходят корректно, backend возвращает 204, записи удаляются.
- **Реальная проблема** — отсутствие UI для удаления коллекций (исправлено) и отсутствие защиты `skippedCount` (исправлено).
- Backend-риски (JPA-связи, `deleteCollection`, `deleteNote`, логирование) — требуют фикса в `Emerald-Grove-Backend` как защита от регрессий.

---

## Итог (устаревший, см. п. 7)

- ~~**Наиболее вероятная причина** "оставшихся записей" — **отсутствие `ON DELETE CASCADE` в реальной схеме БД**~~ — **ОПРОВЕРГНУТО**.
- Второй риск — **LAZY `articleLinks` без `@EntityGraph`** при удалении `ArticleCollection` — требует проверки в backend.
- Третий риск — архитектурный: **JPA-сущности не отражают все зависимости** — защита от регрессий в backend.
