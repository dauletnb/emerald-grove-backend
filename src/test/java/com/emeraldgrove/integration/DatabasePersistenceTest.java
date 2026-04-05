package com.emeraldgrove.integration;

import com.emeraldgrove.dto.SyncArticleNoteRequest;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.enums.NoteType;
import com.emeraldgrove.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для проверки корректности записи данных в базу данных
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Database Persistence Tests")
public class DatabasePersistenceTest {

    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом
        articleRepository.deleteAll();
    }

    @Test
    @DisplayName("Должен корректно сохранять статью с заметками")
    @Transactional
    void shouldSaveArticleWithNotes() {
        // Создаем тестовые данные
        long note1CreatedAt = System.currentTimeMillis();
        long note2CreatedAt = note1CreatedAt + 1;
        long note3CreatedAt = note2CreatedAt + 1;

        SyncArticleRequest request = new SyncArticleRequest(
                "test-external-id-123",
                "https://example.com/test-article",
                "Test Article Title",
                "Test article description with sufficient content",
                List.of(
                        new SyncArticleNoteRequest(
                                "note-1",
                                NoteType.IDEA,
                                "Test idea content",
                                note1CreatedAt
                        ),
                        new SyncArticleNoteRequest(
                                "note-2",
                                NoteType.TASK,
                                "Test task content",
                                note2CreatedAt
                        ),
                        new SyncArticleNoteRequest(
                                "note-3",
                                NoteType.QUESTION,
                                "Test question content",
                                note3CreatedAt
                        )
                )
        );

        // Создаем entity из DTO
        Article article = Article.builder()
                .externalId(request.externalId())
                .url(request.url())
                .title(request.title())
                .description(request.description())
                .build();

        // Добавляем заметки
        for (SyncArticleNoteRequest noteRequest : request.notes()) {
            ArticleNote note = ArticleNote.builder()
                    .externalId(noteRequest.id())
                    .type(noteRequest.type())
                    .content(noteRequest.content())
                    .clientCreatedAt(noteRequest.createdAt())
                    .article(article)
                    .build();
            article.getNotes().add(note);
        }

        // Сохраняем в базу
        Article savedArticle = articleRepository.save(article);

        // Проверяем, что статья сохранена
        assertNotNull(savedArticle.getId());
        assertEquals("test-external-id-123", savedArticle.getExternalId());
        assertEquals("https://example.com/test-article", savedArticle.getUrl());
        assertEquals("Test Article Title", savedArticle.getTitle());
        assertEquals("Test article description with sufficient content", savedArticle.getDescription());
        assertNotNull(savedArticle.getCreatedAt());
        assertFalse(savedArticle.getIsRead());

        // Проверяем, что заметки сохранены
        assertEquals(3, savedArticle.getNotes().size());
        
        ArticleNote savedNote1 = savedArticle.getNotes().get(0);
        assertEquals("note-1", savedNote1.getExternalId());
        assertEquals(NoteType.IDEA, savedNote1.getType());
        assertEquals("Test idea content", savedNote1.getContent());
        assertEquals(note1CreatedAt, savedNote1.getClientCreatedAt());
        assertEquals(savedArticle, savedNote1.getArticle());

        ArticleNote savedNote2 = savedArticle.getNotes().get(1);
        assertEquals("note-2", savedNote2.getExternalId());
        assertEquals(NoteType.TASK, savedNote2.getType());
        assertEquals("Test task content", savedNote2.getContent());

        ArticleNote savedNote3 = savedArticle.getNotes().get(2);
        assertEquals("note-3", savedNote3.getExternalId());
        assertEquals(NoteType.QUESTION, savedNote3.getType());
        assertEquals("Test question content", savedNote3.getContent());
    }

    @Test
    @DisplayName("Должен корректно обновлять существующую статью")
    @Transactional
    void shouldUpdateExistingArticle() {
        // Сначала создаем статью
        Article originalArticle = Article.builder()
                .externalId("update-test-id")
                .url("https://example.com/original")
                .title("Original Title")
                .description("Original Description")
                .build();

        Article saved = articleRepository.saveAndFlush(originalArticle);
        Long articleId = saved.getId();
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

        assertNotNull(originalCreatedAt);
        assertNotNull(originalUpdatedAt);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Thread interrupted while waiting to verify updatedAt");
        }

        // Теперь обновляем
        saved.setTitle("Updated Title");
        saved.setDescription("Updated Description");
        saved.setIsRead(true);

        // Добавляем новую заметку
        ArticleNote newNote = ArticleNote.builder()
                .externalId("new-note")
                .type(NoteType.IDEA)
                .content("New note content")
                .clientCreatedAt(System.currentTimeMillis())
                .article(saved)
                .build();
        saved.getNotes().add(newNote);

        // Сохраняем обновления
        Article updated = articleRepository.saveAndFlush(saved);

        // Проверяем обновления
        assertEquals(articleId, updated.getId()); // ID не должен измениться
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Description", updated.getDescription());
        assertTrue(updated.getIsRead());
        assertEquals(1, updated.getNotes().size());
        assertEquals("new-note", updated.getNotes().get(0).getExternalId());
        assertEquals(originalCreatedAt, updated.getCreatedAt());
        assertNotNull(updated.getUpdatedAt());
        assertFalse(updated.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать null externalId")
    @Transactional
    void shouldHandleNullExternalId() {
        Article article = Article.builder()
                .externalId(null) // null externalId
                .url("https://example.com/null-external")
                .title("Article with null externalId")
                .description("Description")
                .build();

        Article saved = articleRepository.save(article);

        assertNotNull(saved.getId());
        assertNull(saved.getExternalId());
        assertEquals("https://example.com/null-external", saved.getUrl());
    }

    @Test
    @DisplayName("Должен корректно сохранять статью без заметок")
    @Transactional
    void shouldSaveArticleWithoutNotes() {
        Article article = Article.builder()
                .externalId("no-notes-test")
                .url("https://example.com/no-notes")
                .title("Article without notes")
                .description("Description")
                .build();

        Article saved = articleRepository.save(article);

        assertNotNull(saved.getId());
        assertNotNull(saved.getNotes());
        assertTrue(saved.getNotes().isEmpty());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать длинный контент")
    @Transactional
    void shouldHandleLongContent() {
        String longTitle = "A".repeat(100);
        String longDescription = "B".repeat(1000);
        String longNoteContent = "C".repeat(500);

        Article article = Article.builder()
                .externalId("long-content-test")
                .url("https://example.com/long-content")
                .title(longTitle)
                .description(longDescription)
                .build();

        ArticleNote note = ArticleNote.builder()
                .externalId("long-note")
                .type(NoteType.IDEA)
                .content(longNoteContent)
                .clientCreatedAt(System.currentTimeMillis())
                .article(article)
                .build();
        article.getNotes().add(note);

        Article saved = articleRepository.save(article);

        assertEquals(longTitle, saved.getTitle());
        assertEquals(longDescription, saved.getDescription());
        assertEquals(1, saved.getNotes().size());
        assertEquals(longNoteContent, saved.getNotes().get(0).getContent());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать специальные символы и Unicode")
    @Transactional
    void shouldHandleSpecialCharactersAndUnicode() {
        String titleWithSpecialChars = "Статья со специальными символами: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        String descriptionWithUnicode = "Описание с эмодзи: 🚀 🔥 💯 и Unicode: 中文 русский";
        String noteContent = "Заметка с Unicode: How to handle 中文?";

        Article article = Article.builder()
                .externalId("unicode-test")
                .url("https://example.com/unicode")
                .title(titleWithSpecialChars)
                .description(descriptionWithUnicode)
                .build();

        ArticleNote note = ArticleNote.builder()
                .externalId("unicode-note")
                .type(NoteType.QUESTION)
                .content(noteContent)
                .clientCreatedAt(System.currentTimeMillis())
                .article(article)
                .build();
        article.getNotes().add(note);

        Article saved = articleRepository.save(article);

        assertEquals(titleWithSpecialChars, saved.getTitle());
        assertEquals(descriptionWithUnicode, saved.getDescription());
        assertEquals(noteContent, saved.getNotes().get(0).getContent());
    }

    @Test
    @DisplayName("Должен корректно работать с уникальными constraint'ами")
    @Transactional
    void shouldHandleUniqueConstraints() {
        // Создаем первую статью
        Article article1 = Article.builder()
                .externalId("unique-test")
                .url("https://example.com/unique")
                .title("First Article")
                .description("Description")
                .build();

        Article saved1 = articleRepository.save(article1);

        // Пытаемся создать статью с тем же URL (должно быть запрещено)
        Article article2 = Article.builder()
                .externalId("different-id")
                .url("https://example.com/unique") // тот же URL
                .title("Second Article")
                .description("Description")
                .build();

        // Проверяем, что findByUrl находит первую статью
        var foundByUrl = articleRepository.findByUrl("https://example.com/unique");
        assertTrue(foundByUrl.isPresent());
        assertEquals(saved1.getId(), foundByUrl.get().getId());

        // Проверяем findByExternalId
        var foundByExternalId = articleRepository.findByExternalId("unique-test");
        assertTrue(foundByExternalId.isPresent());
        assertEquals(saved1.getId(), foundByExternalId.get().getId());
    }

    @Test
    @DisplayName("Должен корректно удалять заметки при orphanRemoval")
    @Transactional
    void shouldRemoveNotesOnOrphanRemoval() {
        // Создаем статью с заметками
        Article article = Article.builder()
                .externalId("orphan-test")
                .url("https://example.com/orphan")
                .title("Article with notes")
                .description("Description")
                .build();

        ArticleNote note1 = ArticleNote.builder()
                .externalId("note-1")
                .type(NoteType.IDEA)
                .content("Note 1")
                .clientCreatedAt(System.currentTimeMillis())
                .article(article)
                .build();

        ArticleNote note2 = ArticleNote.builder()
                .externalId("note-2")
                .type(NoteType.TASK)
                .content("Note 2")
                .clientCreatedAt(System.currentTimeMillis())
                .article(article)
                .build();

        article.getNotes().add(note1);
        article.getNotes().add(note2);

        Article saved = articleRepository.save(article);
        assertEquals(2, saved.getNotes().size());

        // Удаляем одну заметку
        saved.getNotes().remove(0);
        Article updated = articleRepository.save(saved);

        // Проверяем, что заметка удалена из базы
        assertEquals(1, updated.getNotes().size());
        assertEquals("note-2", updated.getNotes().get(0).getExternalId());
    }

    @Test
    @DisplayName("Должен корректно устанавливаться createdAt timestamp")
    @Transactional
    void shouldSetCreatedAtTimestamp() {
        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);

        Article article = Article.builder()
                .externalId("timestamp-test")
                .url("https://example.com/timestamp")
                .title("Timestamp Test")
                .description("Description")
                .build();

        Article saved = articleRepository.save(article);

        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(saved.getCreatedAt().isAfter(beforeSave));
        assertTrue(saved.getCreatedAt().isBefore(afterSave));
        assertFalse(saved.getUpdatedAt().isBefore(saved.getCreatedAt()));
    }
}
