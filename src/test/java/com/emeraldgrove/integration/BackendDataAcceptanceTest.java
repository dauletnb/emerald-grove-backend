package com.emeraldgrove.integration;

import com.emeraldgrove.dto.SyncArticleNoteRequest;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.enums.NoteType;
import com.emeraldgrove.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Интеграционные тесты для проверки корректности принятия данных с frontend
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Backend Data Acceptance Tests")
public class BackendDataAcceptanceTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleRepository articleRepository;

    private MockMvc mockMvc;

    private static final String VALID_EXTENSION_ID = "valid-extension-id-123";
    private static final String VALID_TOKEN = "integration-test-extension-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        articleRepository.deleteAll();
    }

    @Test
    @DisplayName("Должен корректно принять валидные данные с frontend")
    void shouldAcceptValidDataFromFrontend() throws Exception {
        // Создаем запрос, идентичный тому, что отправляет frontend
        SyncArticleRequest request = new SyncArticleRequest(
                "frontend-article-123",
                "https://example.com/test-article",
                "Test Article from Frontend",
                "This is a test description from frontend",
                List.of(
                        new SyncArticleNoteRequest(
                                "note-1",
                                NoteType.IDEA,
                                "Test idea from frontend",
                                System.currentTimeMillis()
                        ),
                        new SyncArticleNoteRequest(
                                "note-2",
                                NoteType.TASK,
                                "Test task from frontend",
                                System.currentTimeMillis()
                        ),
                        new SyncArticleNoteRequest(
                                "note-3",
                                NoteType.QUESTION,
                                "Test question from frontend",
                                System.currentTimeMillis()
                        )
                )
        );

        String requestBody = objectMapper.writeValueAsString(request);
        System.out.println("Отправляемые данные (как от frontend):\n" + requestBody);

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.articleId").exists())
                .andExpect(jsonPath("$.article").exists())
                .andExpect(jsonPath("$.article.articleId").exists())
                .andExpect(jsonPath("$.article.createdAt").isNumber())
                .andExpect(jsonPath("$.article.updatedAt").isNumber())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Должен обработать запрос с пустыми заметками")
    void shouldHandleRequestWithEmptyNotes() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "empty-notes-article",
                "https://example.com/empty-notes",
                "Article with Empty Notes",
                "Description without notes",
                List.of()
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Должен обработать запрос с null заметками")
    void shouldHandleRequestWithNullNotes() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "null-notes-article",
                "https://example.com/null-notes",
                "Article with Null Notes",
                "Description with null notes",
                null
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Должен отклонить запрос с невалидным URL")
    void shouldRejectInvalidUrl() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "invalid-url-article",
                "", // Пустой URL
                "Article with Invalid URL",
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Должен отклонить запрос с невалидным заголовком")
    void shouldRejectInvalidHeaders() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "test-article",
                "https://example.com/test",
                "Test Article",
                "Description",
                List.of()
        );

        // Тест без Authorization заголовка
        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // В server-side тестах без браузерного CORS-контекста запрос с валидным токеном проходит
        mockMvc.perform(post("/api/articles/sync")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать длинный контент")
    void shouldHandleLongContent() throws Exception {
        String longDescription = "A".repeat(1000); // 1000 символов
        String longNoteContent = "B".repeat(500); // 500 символов

        SyncArticleRequest request = new SyncArticleRequest(
                "long-content-article",
                "https://example.com/long-content",
                "Article with Long Content",
                longDescription,
                List.of(
                        new SyncArticleNoteRequest(
                                "long-note",
                                NoteType.IDEA,
                                longNoteContent,
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Должен отклонить слишком длинный title")
    void shouldRejectTooLongTitle() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "long-title-article",
                "https://example.com/long-title",
                "T".repeat(256),
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Должен отклонить слишком длинный description")
    void shouldRejectTooLongDescription() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "long-description-article",
                "https://example.com/long-description",
                "Valid title",
                "D".repeat(5001),
                List.of()
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Должен отклонить слишком длинный note id")
    void shouldRejectTooLongNoteId() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "long-note-id-article",
                "https://example.com/long-note-id",
                "Valid title",
                "Description",
                List.of(
                        new SyncArticleNoteRequest(
                                "n".repeat(65),
                                NoteType.IDEA,
                                "Valid content",
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Должен отклонить слишком длинный note content")
    void shouldRejectTooLongNoteContent() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "long-note-content-article",
                "https://example.com/long-note-content",
                "Valid title",
                "Description",
                List.of(
                        new SyncArticleNoteRequest(
                                "valid-note-id",
                                NoteType.IDEA,
                                "C".repeat(2001),
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать специальные символы")
    void shouldHandleSpecialCharacters() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "special-chars-article",
                "https://example.com/special-chars",
                "Статья со специальными символами: !@#$%^&*()_+-=[]{}|;':\",./<>?",
                "Описание с эмодзи: 🚀 🔥 💯 и Unicode: 中文 русский",
                List.of(
                        new SyncArticleNoteRequest(
                                "special-note",
                                NoteType.QUESTION,
                                "Вопрос с символами: How to handle \"quotes\" and 'apostrophes'?",
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Должен обрабатывать обновление существующей статьи")
    void shouldHandleExistingArticleUpdate() throws Exception {
        // Сначала создаем статью
        SyncArticleRequest createRequest = new SyncArticleRequest(
                "update-test-article",
                "https://example.com/update-test",
                "Original Title",
                "Original Description",
                List.of()
        );

        // Создаем статью
        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        // Теперь обновляем ту же статью
        SyncArticleRequest updateRequest = new SyncArticleRequest(
                "update-test-article", // тот же externalId
                "https://example.com/update-test", // тот же URL
                "Updated Title",
                "Updated Description",
                List.of(
                        new SyncArticleNoteRequest(
                                "new-note",
                                NoteType.TASK,
                                "New note added in update",
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPDATED"))
                .andExpect(jsonPath("$.article.updatedAt").isNumber());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать невалидные данные заметок")
    void shouldHandleInvalidNoteData() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "invalid-notes-article",
                "https://example.com/invalid-notes",
                "Article with Invalid Notes",
                "Description",
                List.of(
                        new SyncArticleNoteRequest(
                                "", // Пустой ID
                                NoteType.IDEA,
                                "Note with empty ID",
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Должен очищать XSS-контент перед сохранением")
    void shouldSanitizeXssPayloadBeforePersisting() throws Exception {
        SyncArticleRequest request = new SyncArticleRequest(
                "xss-article",
                "https://example.com/xss",
                "<script>alert('title')</script>Safe title",
                "<img src=x onerror=alert('desc')>Safe description",
                List.of(
                        new SyncArticleNoteRequest(
                                "xss-note",
                                NoteType.IDEA,
                                "<script>alert('note')</script>Hello <b>world</b>",
                                System.currentTimeMillis()
                        )
                )
        );

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        Article savedArticle = articleRepository.findByExternalId("xss-article").orElseThrow();

        assertEquals("Safe title", savedArticle.getTitle());
        assertEquals("Safe description", savedArticle.getDescription());
        assertEquals(1, savedArticle.getNotes().size());
        assertEquals("Hello world", savedArticle.getNotes().get(0).getContent());
    }
}
