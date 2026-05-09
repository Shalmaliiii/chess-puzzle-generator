package com.puzzlegenerator.chess.puzzle_service.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Nested
    @DisplayName("AnalysisResult")
    class AnalysisResultTest {

        @Test
        @DisplayName("builder creates object with all fields")
        void builderCreatesAllFields() {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("+1.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(20)
                    .build();

            assertEquals("e2e4", result.getBestMove());
            assertEquals("+1.5", result.getEvaluation());
            assertEquals(List.of("e2e4", "e7e5"), result.getPrincipalVariation());
            assertEquals(20, result.getDepth());
        }

        @Test
        @DisplayName("no-args constructor creates default object")
        void noArgsConstructor() {
            AnalysisResult result = new AnalysisResult();
            assertNull(result.getBestMove());
            assertNull(result.getEvaluation());
            assertNull(result.getPrincipalVariation());
            assertEquals(0, result.getDepth());
        }

        @Test
        @DisplayName("equals and hashCode work correctly")
        void equalsAndHashCode() {
            AnalysisResult a = AnalysisResult.builder().bestMove("e2e4").evaluation("M1").depth(20).build();
            AnalysisResult b = AnalysisResult.builder().bestMove("e2e4").evaluation("M1").depth(20).build();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("toString contains field values")
        void toStringContainsFields() {
            AnalysisResult result = AnalysisResult.builder().bestMove("e2e4").build();
            assertTrue(result.toString().contains("e2e4"));
        }
    }

    @Nested
    @DisplayName("GeneratedPuzzle")
    class GeneratedPuzzleTest {

        @Test
        @DisplayName("builder creates object with all fields")
        void builderCreatesAllFields() {
            GeneratedPuzzle puzzle = GeneratedPuzzle.builder()
                    .fen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
                    .solutionLine(List.of("h5f7"))
                    .mateIn(1)
                    .difficulty("BEGINNER")
                    .sideToMove("white")
                    .build();

            assertEquals("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4", puzzle.getFen());
            assertEquals(List.of("h5f7"), puzzle.getSolutionLine());
            assertEquals(1, puzzle.getMateIn());
            assertEquals("BEGINNER", puzzle.getDifficulty());
            assertEquals("white", puzzle.getSideToMove());
        }

        @Test
        @DisplayName("setters work correctly")
        void settersWork() {
            GeneratedPuzzle puzzle = new GeneratedPuzzle();
            puzzle.setFen("test-fen");
            puzzle.setMateIn(3);
            puzzle.setDifficulty("ADVANCED");
            puzzle.setSideToMove("black");
            puzzle.setSolutionLine(List.of("a1a2"));

            assertEquals("test-fen", puzzle.getFen());
            assertEquals(3, puzzle.getMateIn());
            assertEquals("ADVANCED", puzzle.getDifficulty());
            assertEquals("black", puzzle.getSideToMove());
            assertEquals(List.of("a1a2"), puzzle.getSolutionLine());
        }
    }

    @Nested
    @DisplayName("PuzzleGenerateEvent")
    class PuzzleGenerateEventTest {

        @Test
        @DisplayName("builder creates object with all fields")
        void builderCreatesAllFields() {
            PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                    .difficulty("INTERMEDIATE")
                    .count(5)
                    .requestedBy("admin")
                    .build();

            assertEquals("INTERMEDIATE", event.getDifficulty());
            assertEquals(5, event.getCount());
            assertEquals("admin", event.getRequestedBy());
        }

        @Test
        @DisplayName("allows null difficulty")
        void allowsNullDifficulty() {
            PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                    .difficulty(null)
                    .count(3)
                    .requestedBy("user")
                    .build();

            assertNull(event.getDifficulty());
        }
    }

    @Nested
    @DisplayName("PuzzleGeneratedEvent")
    class PuzzleGeneratedEventTest {

        @Test
        @DisplayName("builder creates object with all fields")
        void builderCreatesAllFields() {
            PuzzleGeneratedEvent event = PuzzleGeneratedEvent.builder()
                    .fen("test-fen")
                    .solutionLine(List.of("e2e4", "e7e5"))
                    .mateIn(2)
                    .difficulty("INTERMEDIATE")
                    .sideToMove("white")
                    .build();

            assertEquals("test-fen", event.getFen());
            assertEquals(List.of("e2e4", "e7e5"), event.getSolutionLine());
            assertEquals(2, event.getMateIn());
            assertEquals("INTERMEDIATE", event.getDifficulty());
            assertEquals("white", event.getSideToMove());
        }
    }

    @Nested
    @DisplayName("AnalysisRequest")
    class AnalysisRequestTest {

        @Test
        @DisplayName("default depth is 20")
        void defaultDepth() {
            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("test-fen")
                    .build();

            assertEquals("test-fen", request.getFen());
            assertEquals(20, request.getDepth());
        }

        @Test
        @DisplayName("custom depth overrides default")
        void customDepth() {
            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("test-fen")
                    .depth(15)
                    .build();

            assertEquals(15, request.getDepth());
        }
    }

    @Nested
    @DisplayName("AnalysisResponse")
    class AnalysisResponseTest {

        @Test
        @DisplayName("builder creates complete response")
        void builderCreatesResponse() {
            AnalysisResponse response = AnalysisResponse.builder()
                    .bestMove("e2e4")
                    .evaluation("+0.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(20)
                    .build();

            assertEquals("e2e4", response.getBestMove());
            assertEquals("+0.5", response.getEvaluation());
            assertEquals(20, response.getDepth());
        }
    }

    @Nested
    @DisplayName("ValidateMoveRequest")
    class ValidateMoveRequestTest {

        @Test
        @DisplayName("builder creates object with fen and move")
        void builderCreatesObject() {
            ValidateMoveRequest request = ValidateMoveRequest.builder()
                    .fen("starting-fen")
                    .move("e2e4")
                    .build();

            assertEquals("starting-fen", request.getFen());
            assertEquals("e2e4", request.getMove());
        }
    }

    @Nested
    @DisplayName("ValidateMoveResponse")
    class ValidateMoveResponseTest {

        @Test
        @DisplayName("builder creates object with all fields")
        void builderCreatesObject() {
            ValidateMoveResponse response = ValidateMoveResponse.builder()
                    .isBestMove(true)
                    .engineBestMove("e2e4")
                    .evaluation("+0.5")
                    .build();

            assertTrue(response.isBestMove());
            assertEquals("e2e4", response.getEngineBestMove());
            assertEquals("+0.5", response.getEvaluation());
        }

        @Test
        @DisplayName("isBestMove defaults to false")
        void isBestMoveDefaultsFalse() {
            ValidateMoveResponse response = new ValidateMoveResponse();
            assertFalse(response.isBestMove());
        }
    }

    @Nested
    @DisplayName("EngineHealthResponse")
    class EngineHealthResponseTest {

        @Test
        @DisplayName("builder creates response with all fields")
        void builderCreatesResponse() {
            EngineHealthResponse response = EngineHealthResponse.builder()
                    .status("UP")
                    .engineVersion("Stockfish 16.1")
                    .activeWorkers(1)
                    .build();

            assertEquals("UP", response.getStatus());
            assertEquals("Stockfish 16.1", response.getEngineVersion());
            assertEquals(1, response.getActiveWorkers());
        }
    }
}
