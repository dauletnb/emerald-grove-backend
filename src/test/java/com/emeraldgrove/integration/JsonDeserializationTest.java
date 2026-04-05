package com.emeraldgrove.integration;

import com.emeraldgrove.dto.SyncArticleNoteRequest;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.enums.NoteType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки корректности JSON десериализации данных с frontend
 */
@SpringBootTest
@DisplayName("JSON Deserialization Tests")
public class JsonDeserializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Должен корректно десериализовать JSON от frontend")
    void shouldDeserializeFrontendJson() throws JsonProcessingException {
        // JSON, который отправляет frontend (articles-fixed.ts)
        String frontendJson = """
            {
              "externalId": "test-123",
              "url": "https://example.com/test",
              "title": "Test Article",
              "description": "Test description",
              "notes": [
                {
                  "id": "note-1",
                  "type": "IDEA",
                  "content": "Test idea",
                  "createdAt": 1712160000000
                },
                {
                  "id": "note-2",
                  "type": "TASK",
                  "content": "Test task",
                  "createdAt": 1712160000001
                },
                {
                  "id": "note-3",
                  "type": "QUESTION",
                  "content": "Test question",
                  "createdAt": 1712160000002
                }
              ]
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(frontendJson, SyncArticleRequest.class);

        assertNotNull(request);
        assertEquals("test-123", request.externalId());
        assertEquals("https://example.com/test", request.url());
        assertEquals("Test Article", request.title());
        assertEquals("Test description", request.description());
        
        assertNotNull(request.notes());
        assertEquals(3, request.notes().size());
        
        // Проверяем первую заметку
        SyncArticleNoteRequest note1 = request.notes().get(0);
        assertEquals("note-1", note1.id());
        assertEquals(NoteType.IDEA, note1.type());
        assertEquals("Test idea", note1.content());
        assertEquals(1712160000000L, note1.createdAt());
        
        // Проверяем вторую заметку
        SyncArticleNoteRequest note2 = request.notes().get(1);
        assertEquals("note-2", note2.id());
        assertEquals(NoteType.TASK, note2.type());
        assertEquals("Test task", note2.content());
        assertEquals(1712160000001L, note2.createdAt());
        
        // Проверяем третью заметку
        SyncArticleNoteRequest note3 = request.notes().get(2);
        assertEquals("note-3", note3.id());
        assertEquals(NoteType.QUESTION, note3.type());
        assertEquals("Test question", note3.content());
        assertEquals(1712160000002L, note3.createdAt());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать JSON с null полями")
    void shouldHandleJsonWithNullFields() throws JsonProcessingException {
        String jsonWithNulls = """
            {
              "externalId": null,
              "url": "https://example.com/test",
              "title": "Test Article",
              "description": null,
              "notes": null
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(jsonWithNulls, SyncArticleRequest.class);

        assertNotNull(request);
        assertNull(request.externalId());
        assertEquals("https://example.com/test", request.url());
        assertEquals("Test Article", request.title());
        assertNull(request.description());
        assertNull(request.notes());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать JSON с пустыми массивами")
    void shouldHandleJsonWithEmptyArrays() throws JsonProcessingException {
        String jsonWithEmptyArray = """
            {
              "externalId": "test-123",
              "url": "https://example.com/test",
              "title": "Test Article",
              "description": "Test description",
              "notes": []
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(jsonWithEmptyArray, SyncArticleRequest.class);

        assertNotNull(request);
        assertEquals("test-123", request.externalId());
        assertNotNull(request.notes());
        assertTrue(request.notes().isEmpty());
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при невалидном типе заметки")
    void shouldThrowExceptionForInvalidNoteType() {
        String jsonWithInvalidType = """
            {
              "externalId": "test-123",
              "url": "https://example.com/test",
              "title": "Test Article",
              "description": "Test description",
              "notes": [
                {
                  "id": "note-1",
                  "type": "INVALID_TYPE",
                  "content": "Test note",
                  "createdAt": 1712160000000
                }
              ]
            }
            """;

        JsonProcessingException exception = assertThrows(JsonProcessingException.class, () -> {
            objectMapper.readValue(jsonWithInvalidType, SyncArticleRequest.class);
        });

        assertTrue(exception.getMessage().contains("INVALID_TYPE"));
    }

    @Test
    @DisplayName("Должен корректно сериализовать ответ для frontend")
    void shouldSerializeResponseForFrontend() throws JsonProcessingException {
        // Создаем объект ответа, который бэкенд отправит frontend
        SyncArticleResponse response = new SyncArticleResponse(
                com.emeraldgrove.enums.SyncStatus.CREATED,
                123L,
                null
        );

        String json = objectMapper.writeValueAsString(response);
        
        assertNotNull(json);
        assertTrue(json.contains("\"status\":\"CREATED\""));
        assertTrue(json.contains("\"articleId\":123"));
        assertTrue(json.contains("\"article\":null"));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать JSON с экранированными символами")
    void shouldHandleJsonWithEscapedCharacters() throws JsonProcessingException {
        String jsonWithEscapedChars = """
            {
              "externalId": "test-123",
              "url": "https://example.com/test",
              "title": "Article with \\"quotes\\" and 'apostrophes'",
              "description": "Description with \\n newlines and \\t tabs",
              "notes": [
                {
                  "id": "note-1",
                  "type": "IDEA",
                  "content": "Content with \\"quotes\\" and \\n newlines",
                  "createdAt": 1712160000000
                }
              ]
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(jsonWithEscapedChars, SyncArticleRequest.class);

        assertNotNull(request);
        assertEquals("Article with \"quotes\" and 'apostrophes'", request.title());
        assertEquals("Description with \n newlines and \t tabs", request.description());
        
        SyncArticleNoteRequest note = request.notes().get(0);
        assertEquals("Content with \"quotes\" and \n newlines", note.content());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать Unicode символы")
    void shouldHandleUnicodeCharacters() throws JsonProcessingException {
        String jsonWithUnicode = """
            {
              "externalId": "unicode-test",
              "url": "https://example.com/unicode",
              "title": "Статья на русском",
              "description": "Описание с эмодзи: 🚀 🔥 💯 и китайскими иероглифами: 中文",
              "notes": [
                {
                  "id": "unicode-note",
                  "type": "QUESTION",
                  "content": "Вопрос с Unicode: How to handle 中文?",
                  "createdAt": 1712160000000
                }
              ]
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(jsonWithUnicode, SyncArticleRequest.class);

        assertNotNull(request);
        assertEquals("Статья на русском", request.title());
        assertEquals("Описание с эмодзи: 🚀 🔥 💯 и китайскими иероглифами: 中文", request.description());
        
        SyncArticleNoteRequest note = request.notes().get(0);
        assertEquals("Вопрос с Unicode: How to handle 中文?", note.content());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать большие числа")
    void shouldHandleLargeNumbers() throws JsonProcessingException {
        String jsonWithLargeNumbers = """
            {
              "externalId": "large-numbers-test",
              "url": "https://example.com/large-numbers",
              "title": "Test with Large Numbers",
              "description": "Test description",
              "notes": [
                {
                  "id": "note-1",
                  "type": "TASK",
                  "content": "Note with large timestamp",
                  "createdAt": 1712160000000123
                }
              ]
            }
            """;

        SyncArticleRequest request = objectMapper.readValue(jsonWithLargeNumbers, SyncArticleRequest.class);

        assertNotNull(request);
        SyncArticleNoteRequest note = request.notes().get(0);
        assertEquals(1712160000000123L, note.createdAt());
    }
}
