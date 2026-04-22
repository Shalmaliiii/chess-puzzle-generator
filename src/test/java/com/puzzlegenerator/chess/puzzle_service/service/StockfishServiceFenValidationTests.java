package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockfishServiceFenValidationTests {

    @Test
    void acceptsStartingPositionFen() {
        assertTrue(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    }

    @Test
    void acceptsMidGameFenWithEnPassant() {
        assertTrue(StockfishService.isValidFen(
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2"));
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
    void rejectsNewlineInjection() {
        // Classic UCI command-injection attempt: a newline would let the
        // appended text be interpreted as a separate command by the engine.
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\nquit"));
    }

    @Test
    void rejectsCarriageReturnInjection() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\rsetoption name foo value bar"));
    }

    @Test
    void rejectsWrongNumberOfRanks() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1"));
    }

    @Test
    void rejectsInvalidSideToMove() {
        assertFalse(StockfishService.isValidFen(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1"));
    }

    @Test
    void rejectsOverlyLongInput() {
        String longInput = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
                + " ".repeat(200);
        assertFalse(StockfishService.isValidFen(longInput));
    }
}
