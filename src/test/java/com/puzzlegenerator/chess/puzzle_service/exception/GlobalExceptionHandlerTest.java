package com.puzzlegenerator.chess.puzzle_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handles IllegalArgumentException with 400 status")
    void handlesIllegalArgument() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Invalid FEN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("Invalid FEN", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("handles IllegalStateException with 503 status")
    void handlesIllegalState() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalState(new IllegalStateException("Engine not available"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().get("status"));
        assertEquals("Service Unavailable", response.getBody().get("error"));
        assertEquals("Engine not available", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handles generic Exception with 500 status and generic message")
    void handlesGenericException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("Something broke"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }

    @Test
    @DisplayName("response body contains timestamp field")
    void responseContainsTimestamp() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("test"));

        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("timestamp"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("response body preserves field ordering (LinkedHashMap)")
    void responseFieldOrdering() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("test"));

        assertNotNull(response.getBody());
        var keys = response.getBody().keySet().stream().toList();
        assertEquals("timestamp", keys.get(0));
        assertEquals("status", keys.get(1));
        assertEquals("error", keys.get(2));
        assertEquals("message", keys.get(3));
    }
}
