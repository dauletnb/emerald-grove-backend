package com.emeraldgrove.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ❶ Дубликат — твой текущий IllegalStateException
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(IllegalStateException e) {
        // предупреждение, не ошибка — это ожидаемая ситуация
        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DUPLICATE", e.getMessage(), HttpStatus.CONFLICT));
    }

    // ❷ Ресурс не найден — когда ищешь статью по id, а её нет
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND));
    }

    // ❸ Ошибки валидации — когда @Valid не прошёл на ArticleRequest
    // Spring бросает это исключение автоматически
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        // собираем все сообщения об ошибках в одну строку
        // например: "title: must not be blank, slug: must not be null"
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST));
    }

    // ❹ Неизвестная ошибка — последний рубеж, ловит всё остальное
    // ВАЖНО: логируем полный стектрейс через log.error, не log.warn
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error", e); // e передаём целиком — стектрейс попадёт в лог
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        // клиенту отдаём только "Internal server error" — детали не утекают
    }
}
