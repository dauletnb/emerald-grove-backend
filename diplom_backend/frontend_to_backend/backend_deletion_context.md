# Контекст: анализ проблем удаления (backend)

> Создано: 2026-05-11  
> Назначение: быстрое восстановление контекста при возвращении к backend после работы с frontend.

---

## Что анализировалось

Предположение: при вызове DELETE через контроллеры записи (статьи, заметки, коллекции) физически остаются в PostgreSQL.

## Ключевые файлы анализа

- Полный анализ проблем: `diplom_backend/frontend_to_backend/deletion_problems.md`
- План исправлений: `diplom_backend/frontend_to_backend/deletion_fix_plan.md`

---

## Результаты диагностики БД (2026-05-11)

| Проверка | Результат |
|----------|-----------|
| `ON DELETE CASCADE` на FK | **Есть** на всех критичных FK (`article_note`, `ai_job`, `ai_result`, `article_collection_link`) |
| `flyway_schema_history` | Миграции V1–V3 применены успешно |
| Осиротевшие записи | **0** во всех таблицах |
| Статей/коллекций в БД | **0** (БД пуста, только 1 пользователь) |

**Главная гипотеза (отсутствие CASCADE) НЕ ПОДТВЕРДИЛАСЬ.**

---

## Обнаруженные проблемы в коде (требуют фикса)

1. **`Article` не имеет JPA-связей** `OneToMany` на `AiJob`, `AiResult`, `ArticleCollectionLink`. Код полагается только на БД-каскад.
2. **`CollectionServiceImpl.deleteCollection()`** удаляет `ArticleCollection` без предзагрузки `articleLinks` (LAZY + orphanRemoval = неявное поведение Hibernate).
3. **`ArticleServiceImpl.deleteNote()`** удаляет заметку через `removeIf` + `save()`, а не прямым `repository.delete()`.
4. **Нет `ArticleNoteRepository`** — заметки не имеют собственного репозитория.
5. **Нет логирования** в методах удаления (`deleteArticle`, `deleteCollection`, `deleteNote`, sync-методы).

---

## Следующие шаги (если проблема подтвердится на frontend)

1. Проверить логи backend при реальном DELETE-запросе (создать статью, удалить, проверить `DELETE` и отсутствие `ROLLBACK`).
2. Реализовать профилактические правки:
   - Добавить недостающие JPA-связи в `Article.java`.
   - Добавить `ArticleNoteRepository` и переделать `deleteNote()`.
   - Исправить `deleteCollection()` — либо `@EntityGraph`, либо убрать JPA-cascade.
   - Добавить логирование.

---

## Важные фрагменты кода

### Article.java — отсутствуют связи
```java
// Нет OneToMany на AiJob, AiResult, ArticleCollectionLink
@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ArticleNote> notes = new ArrayList<>();
```

### CollectionServiceImpl.deleteCollection() — LAZY без EntityGraph
```java
@Override
@Transactional
public void deleteCollection(String externalId, Long userId) {
    collectionRepository.delete(getOwnedCollection(externalId, userId));
}
```

### ArticleServiceImpl.deleteNote() — косвенное удаление
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

### ArticleCollection.java — JPA-cascade + БД-cascade (дублирование)
```java
@OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ArticleCollectionLink> articleLinks = new ArrayList<>();
```

---

## Контакт с frontend

Если на frontend обнаружено, что DELETE-запросы:
- **Не уходят** → проблема в frontend (не вызывается API).
- **Уходят, но возвращают ошибку** → проверить логи backend (возможно, `ConstraintViolationException` или другая ошибка).
- **Уходят, возвращают 204, но записи остаются** → транзакция откатывается тихо (проверить глобальный exception handler).

