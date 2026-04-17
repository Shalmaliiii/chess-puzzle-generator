package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockfishServiceTest {

    private static final String STARTING_POSITION =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Test
    void validateFen_acceptsStartingPosition() {
        assertEquals(STARTING_POSITION, StockfishService.validateFen(STARTING_POSITION));
    }

    @Test
    void validateFen_trimsSurroundingWhitespace() {
        assertEquals(STARTING_POSITION,
                StockfishService.validateFen("  " + STARTING_POSITION + "  "));
    }

    @Test
    void validateFen_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> StockfishService.validateFen(null));
    }

    @Test
    void validateFen_rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> StockfishService.validateFen(""));
        assertThrows(IllegalArgumentException.class, () -> StockfishService.validateFen("   "));
    }

    @Test
    void validateFen_rejectsEmbeddedNewline() {
        // A newline-based payload would otherwise be interpreted by Stockfish
        // as a separate UCI command.
        String payload = STARTING_POSITION + "\nquit";
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(payload));
    }

    @Test
    void validateFen_rejectsCarriageReturn() {
        String payload = STARTING_POSITION + "\rquit";
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(payload));
    }

    @Test
    void validateFen_rejectsMalformedPieceLetters() {
        String bad = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w KQkq - 0 1";
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(bad));
    }

    @Test
    void validateFen_rejectsWrongNumberOfRanks() {
        String bad = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1";
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(bad));
    }

    @Test
    void validateFen_rejectsInvalidSideToMove() {
        String bad = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1";
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(bad));
    }

    @Test
    void validateFen_rejectsOverlyLongInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append('a');
        }
        assertThrows(IllegalArgumentException.class,
                () -> StockfishService.validateFen(sb.toString()));
    }
}
