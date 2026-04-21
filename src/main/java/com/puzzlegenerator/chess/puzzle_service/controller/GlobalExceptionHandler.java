package com.puzzlegenerator.chess.puzzle_service.controller;

import com.puzzlegenerator.chess.puzzle_service.service.StockfishException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Central translation layer between service-level exceptions and HTTP responses.
 * Without this, runtime exceptions would be rendered as generic 500s with stack
 * traces, hiding the underlying failure mode from callers and from logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StockfishException.class)
    public ResponseEntity<Map<String, Object>> handleStockfish(StockfishException ex) {
        logger.error("Stockfish engine error", ex);
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, "stockfish_error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Bad request", ex);
        return errorResponse(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        logger.error("Unhandled exception", ex);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal_error",
                "An unexpected error occurred.");
    }

    private static ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status, String error, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message == null ? "" : message);
        return ResponseEntity.status(status).body(body);
    }
}
