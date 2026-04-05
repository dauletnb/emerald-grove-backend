package com.emeraldgrove.integration;

import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.enums.NoteType;
import com.emeraldgrove.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки корректности схемы базы данных и ограничений
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Database Schema Validation Tests")
public class DatabaseSchemaValidationTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
    }

    @Test
    @DisplayName("Должна существовать таблица articles с правильной структурой")
    void shouldHaveCorrectArticlesTableStructure() {
        // Проверяем структуру таблицы articles используя H2 специфичный запрос
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable, character_maximum_length " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'ARTICLES' " +
                "ORDER BY ordinal_position"
        );

        assertFalse(columns.isEmpty(), "Таблица articles должна существовать");

        // Проверяем обязательные колонки
        var columnMap = columns.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row.get("column_name"),
                        row -> row
                ));

        // Проверяем наличие ключевых полей
        assertTrue(columnMap.containsKey("ID"), "Поле id должно существовать");
        assertTrue(columnMap.containsKey("EXTERNAL_ID"), "Поле external_id должно существовать");
        assertTrue(columnMap.containsKey("URL"), "Поле url должно существовать");
        assertTrue(columnMap.containsKey("TITLE"), "Поле title должно существовать");
        assertTrue(columnMap.containsKey("DESCRIPTION"), "Поле description должно существовать");
        assertTrue(columnMap.containsKey("IS_READ"), "Поле is_read должно существовать");
        assertTrue(columnMap.containsKey("CREATED_AT"), "Поле created_at должно существовать");

        // Проверяем ограничения NOT NULL
        assertEquals("NO", columnMap.get("URL").get("is_nullable"), "URL не может быть NULL");
        assertEquals("NO", columnMap.get("TITLE").get("is_nullable"), "Title не может быть NULL");
        assertEquals("NO", columnMap.get("IS_READ").get("is_nullable"), "IsRead не может быть NULL");
        assertEquals("NO", columnMap.get("CREATED_AT").get("is_nullable"), "CreatedAt не может быть NULL");

        // Проверяем длину полей
        assertEquals(36L, columnMap.get("EXTERNAL_ID").get("character_maximum_length"));
        assertEquals(2048L, columnMap.get("URL").get("character_maximum_length"));
    }

    @Test
    @DisplayName("Должна существовать таблица article_notes с правильной структурой")
    void shouldHaveCorrectArticleNotesTableStructure() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable, character_maximum_length " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'ARTICLE_NOTES' " +
                "ORDER BY ordinal_position"
        );

        assertFalse(columns.isEmpty(), "Таблица article_notes должна существовать");

        var columnMap = columns.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row.get("column_name"),
                        row -> row
                ));

        // Проверяем наличие ключевых полей
        assertTrue(columnMap.containsKey("ID"), "Поле id должно существовать");
        assertTrue(columnMap.containsKey("EXTERNAL_ID"), "Поле external_id должно существовать");
        assertTrue(columnMap.containsKey("ARTICLE_ID"), "Поле article_id должно существовать");
        assertTrue(columnMap.containsKey("TYPE"), "Поле type должно существовать");
        assertTrue(columnMap.containsKey("CONTENT"), "Поле content должно существовать");
        assertTrue(columnMap.containsKey("CLIENT_CREATED_AT"), "Поле client_created_at должно существовать");
        assertTrue(columnMap.containsKey("CREATED_AT"), "Поле created_at должно существовать");

        // Проверяем ограничения NOT NULL
        assertEquals("NO", columnMap.get("EXTERNAL_ID").get("is_nullable"));
        assertEquals("NO", columnMap.get("ARTICLE_ID").get("is_nullable"));
        assertEquals("NO", columnMap.get("TYPE").get("is_nullable"));
        assertEquals("NO", columnMap.get("CONTENT").get("is_nullable"));
        assertEquals("NO", columnMap.get("CLIENT_CREATED_AT").get("is_nullable"));

        // Проверяем длину полей
        assertEquals(64L, columnMap.get("EXTERNAL_ID").get("character_maximum_length"));
        assertEquals(32L, columnMap.get("TYPE").get("character_maximum_length"));
    }

    @Test
    @DisplayName("Должны существовать foreign key constraints")
    void shouldHaveForeignKeyConstraints() {
        // Проверяем наличие foreign key constraint
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT tc.constraint_name, tc.constraint_type, " +
                "kcu.column_name, ccu.table_name AS foreign_table_name " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.key_column_usage AS kcu " +
                "ON tc.constraint_name = kcu.constraint_name " +
                "JOIN information_schema.constraint_column_usage AS ccu " +
                "ON ccu.constraint_name = tc.constraint_name " +
                "WHERE tc.table_name = 'ARTICLE_NOTES' " +
                "AND tc.constraint_type = 'FOREIGN KEY'"
        );

        assertFalse(constraints.isEmpty(), "Должен существовать foreign key constraint");

        var constraint = constraints.get(0);
        assertEquals("ARTICLE_ID", constraint.get("column_name"));
        assertEquals("ARTICLES", constraint.get("foreign_table_name"));
    }

    @Test
    @DisplayName("Должны существовать unique constraints")
    void shouldHaveUniqueConstraints() {
        // Проверяем unique constraint для URL
        List<Map<String, Object>> uniqueConstraints = jdbcTemplate.queryForList(
                "SELECT tc.constraint_name, kcu.column_name " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.key_column_usage AS kcu " +
                "ON tc.constraint_name = kcu.constraint_name " +
                "WHERE tc.table_name = 'ARTICLES' " +
                "AND tc.constraint_type = 'UNIQUE'"
        );

        assertFalse(uniqueConstraints.isEmpty(), "Должен существовать unique constraint для articles");

        // Проверяем unique constraint для article_notes
        List<Map<String, Object>> noteUniqueConstraints = jdbcTemplate.queryForList(
                "SELECT tc.constraint_name, kcu.column_name " +
                "FROM information_schema.table_constraints AS tc " +
                "JOIN information_schema.key_column_usage AS kcu " +
                "ON tc.constraint_name = kcu.constraint_name " +
                "WHERE tc.table_name = 'ARTICLE_NOTES' " +
                "AND tc.constraint_type = 'UNIQUE'"
        );

        assertFalse(noteUniqueConstraints.isEmpty(), "Должен существовать unique constraint для article_notes");
    }

    @Test
    @DisplayName("Должен корректно работать cascade delete")
    void shouldWorkCascadeDelete() {
        // Создаем статью с заметками
        Article article = Article.builder()
                .externalId("cascade-test")
                .url("https://example.com/cascade")
                .title("Cascade Test")
                .description("Description")
                .build();

        ArticleNote note = ArticleNote.builder()
                .externalId("cascade-note")
                .type(NoteType.IDEA)
                .content("Note content")
                .clientCreatedAt(System.currentTimeMillis())
                .article(article)
                .build();
        article.getNotes().add(note);

        Article saved = articleRepository.save(article);
        Long articleId = saved.getId();

        // Проверяем, что данные сохранены
        assertTrue(articleRepository.existsById(articleId));
        assertEquals(1, saved.getNotes().size());

        // Проверяем количество записей в article_notes
        Integer noteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emerald_grove.article_notes WHERE ARTICLE_ID = ?",
                Integer.class,
                articleId
        );
        assertEquals(1, noteCount.intValue());

        // Удаляем статью
        articleRepository.delete(saved);

        // Проверяем, что заметки тоже удалены (cascade)
        Integer noteCountAfterDelete = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emerald_grove.article_notes WHERE ARTICLE_ID = ?",
                Integer.class,
                articleId
        );
        assertEquals(0, noteCountAfterDelete.intValue());
    }

    @Test
    @DisplayName("Должна корректно работать нумерация ID")
    @Transactional
    void shouldWorkCorrectIdSequencing() {
        // Создаем несколько статей
        Article article1 = Article.builder()
                .externalId("seq-test-1")
                .url("https://example.com/seq1")
                .title("Article 1")
                .description("Description 1")
                .build();

        Article article2 = Article.builder()
                .externalId("seq-test-2")
                .url("https://example.com/seq2")
                .title("Article 2")
                .description("Description 2")
                .build();

        Article saved1 = articleRepository.save(article1);
        Article saved2 = articleRepository.save(article2);

        // Проверяем, что ID разные и последовательные
        assertNotEquals(saved1.getId(), saved2.getId());
        assertTrue(saved2.getId() > saved1.getId());

        // Проверяем, что ID начинаются с 1 и увеличиваются
        assertTrue(saved1.getId() >= 1);
        assertTrue(saved2.getId() >= 2);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать NULL значения в optional полях")
    @Transactional
    void shouldHandleNullValuesInOptionalFields() {
        Article article = Article.builder()
                .externalId("null-test")
                .url("https://example.com/null")
                .title("Article with nulls")
                .description(null) // NULL в optional поле
                .build();

        Article saved = articleRepository.save(article);

        assertNotNull(saved.getId());
        assertEquals("null-test", saved.getExternalId());
        assertEquals("https://example.com/null", saved.getUrl());
        assertEquals("Article with nulls", saved.getTitle());
        assertNull(saved.getDescription()); // Должно остаться NULL
        assertFalse(saved.getIsRead()); // Default value
    }
}
