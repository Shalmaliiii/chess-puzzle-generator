package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockfishServiceFenValidationTest {

    @Test
    void acceptsStandardStartingPosition() {
        assertTrue(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    }

    @Test
    void acceptsMidGamePosition() {
        assertTrue(StockfishService.isValidFen(
                "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"));
    }

    @Test
    void rejectsNull() {
        assertFalse(StockfishService.isValidFen(null));
    }

    @Test
    void rejectsEmpty() {
        assertFalse(StockfishService.isValidFen(""));
    }

    @Test
    void rejectsFenWithEmbeddedNewlineInjection() {
        // Attempt to inject an arbitrary UCI command by smuggling a newline
        // into the FEN string.
        String malicious = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\nquit";
        assertFalse(StockfishService.isValidFen(malicious));
    }

    @Test
    void rejectsFenWithCarriageReturn() {
        String malicious = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\rgo";
        assertFalse(StockfishService.isValidFen(malicious));
    }

    @Test
    void rejectsFenWithInvalidPieceCharacter() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/XNBQKBNR w KQkq - 0 1"));
    }

    @Test
    void rejectsFenWithMissingFields() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"));
    }

    @Test
    void rejectsFenWithInvalidSideToMove() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1"));
    }
}
