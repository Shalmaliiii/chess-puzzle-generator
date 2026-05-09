package com.puzzlegenerator.chess.puzzle_service.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FenGeneratorTest {

    private FenGenerator fenGenerator;

    @BeforeEach
    void setUp() {
        fenGenerator = new FenGenerator();
    }

    @Nested
    @DisplayName("getRandomMatePosition")
    class GetRandomMatePosition {

        @Test
        @DisplayName("returns a non-null, non-empty FEN string")
        void returnsValidFen() {
            String fen = fenGenerator.getRandomMatePosition();
            assertNotNull(fen);
            assertFalse(fen.isEmpty());
        }

        @Test
        @DisplayName("returned FEN has valid structure (at least 2 space-separated parts)")
        void returnsFenWithValidStructure() {
            String fen = fenGenerator.getRandomMatePosition();
            String[] parts = fen.split("\\s+");
            assertTrue(parts.length >= 2, "FEN should have at least position and side-to-move");
        }

        @Test
        @DisplayName("returned FEN has valid side to move (w or b)")
        void returnsValidSideToMove() {
            String fen = fenGenerator.getRandomMatePosition();
            String[] parts = fen.split("\\s+");
            assertTrue("w".equals(parts[1]) || "b".equals(parts[1]),
                    "Side to move should be 'w' or 'b', got: " + parts[1]);
        }

        @RepeatedTest(20)
        @DisplayName("always returns a FEN from the known mate positions list")
        void returnsFromKnownPositions() {
            List<String> allPositions = fenGenerator.getAllMatePositions();
            String fen = fenGenerator.getRandomMatePosition();
            assertTrue(allPositions.contains(fen),
                    "Returned FEN should be from the known mate positions list");
        }

        @Test
        @DisplayName("produces some variety over multiple calls (randomness)")
        void producesVariety() {
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                seen.add(fenGenerator.getRandomMatePosition());
            }
            assertTrue(seen.size() > 1, "Should produce at least 2 different positions over 100 calls");
        }
    }

    @Nested
    @DisplayName("getRandomTacticalPosition")
    class GetRandomTacticalPosition {

        @Test
        @DisplayName("returns a non-null, non-empty FEN string")
        void returnsValidFen() {
            String fen = fenGenerator.getRandomTacticalPosition();
            assertNotNull(fen);
            assertFalse(fen.isEmpty());
        }

        @Test
        @DisplayName("returned FEN has valid structure")
        void returnsFenWithValidStructure() {
            String fen = fenGenerator.getRandomTacticalPosition();
            String[] parts = fen.split("\\s+");
            assertTrue(parts.length >= 2);
        }
    }

    @Nested
    @DisplayName("getAllMatePositions")
    class GetAllMatePositions {

        @Test
        @DisplayName("returns non-empty list")
        void returnsNonEmptyList() {
            List<String> positions = fenGenerator.getAllMatePositions();
            assertNotNull(positions);
            assertFalse(positions.isEmpty());
        }

        @Test
        @DisplayName("contains expected number of positions (40 total)")
        void containsExpectedCount() {
            List<String> positions = fenGenerator.getAllMatePositions();
            assertEquals(40, positions.size(),
                    "Should have 10 mate-in-1 + 10 mate-in-2 + 10 mate-in-3 + 10 mate-in-4+");
        }

        @Test
        @DisplayName("all positions have valid FEN structure")
        void allPositionsHaveValidFen() {
            List<String> positions = fenGenerator.getAllMatePositions();
            for (String fen : positions) {
                String[] parts = fen.split("\\s+");
                assertTrue(parts.length >= 2,
                        "FEN should have at least 2 parts: " + fen);
                assertTrue("w".equals(parts[1]) || "b".equals(parts[1]),
                        "Side to move should be 'w' or 'b' in: " + fen);
            }
        }

        @Test
        @DisplayName("position board part has 8 ranks separated by '/'")
        void boardPartHasEightRanks() {
            List<String> positions = fenGenerator.getAllMatePositions();
            for (String fen : positions) {
                String board = fen.split("\\s+")[0];
                String[] ranks = board.split("/");
                assertEquals(8, ranks.length,
                        "Board should have 8 ranks: " + fen);
            }
        }
    }
}
