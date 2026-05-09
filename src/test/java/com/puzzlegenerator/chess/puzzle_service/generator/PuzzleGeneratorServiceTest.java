package com.puzzlegenerator.chess.puzzle_service.generator;

import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import com.puzzlegenerator.chess.puzzle_service.dto.GeneratedPuzzle;
import com.puzzlegenerator.chess.puzzle_service.service.StockfishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuzzleGeneratorServiceTest {

    @Mock
    private StockfishService stockfishService;

    private FenGenerator fenGenerator;
    private MateDetector mateDetector;
    private DifficultyClassifier difficultyClassifier;
    private PuzzleGeneratorService puzzleGeneratorService;

    @BeforeEach
    void setUp() {
        fenGenerator = new FenGenerator();
        mateDetector = new MateDetector();
        difficultyClassifier = new DifficultyClassifier();
        puzzleGeneratorService = new PuzzleGeneratorService(
                stockfishService, fenGenerator, mateDetector, difficultyClassifier);
    }

    @Nested
    @DisplayName("generatePuzzles")
    class GeneratePuzzles {

        @Test
        @DisplayName("generates puzzles when Stockfish returns mate evaluations")
        void generatesPuzzlesWithMateResults() throws Exception {
            AnalysisResult mateResult = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateResult);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 5);

            assertFalse(puzzles.isEmpty());
            assertTrue(puzzles.size() <= 5);
            for (GeneratedPuzzle puzzle : puzzles) {
                assertNotNull(puzzle.getFen());
                assertNotNull(puzzle.getSolutionLine());
                assertTrue(puzzle.getMateIn() > 0);
                assertNotNull(puzzle.getDifficulty());
                assertNotNull(puzzle.getSideToMove());
            }
        }

        @Test
        @DisplayName("returns empty list when no positions yield forced mates")
        void returnsEmptyWhenNoMates() throws Exception {
            AnalysisResult noMateResult = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("+1.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(noMateResult);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 5);

            assertTrue(puzzles.isEmpty());
        }

        @Test
        @DisplayName("filters by difficulty when specified")
        void filtersByDifficulty() throws Exception {
            AnalysisResult mateIn1 = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateIn1);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles("BEGINNER", 10);

            for (GeneratedPuzzle puzzle : puzzles) {
                assertEquals("BEGINNER", puzzle.getDifficulty());
                assertEquals(1, puzzle.getMateIn());
            }
        }

        @Test
        @DisplayName("filters out non-matching difficulty")
        void filtersNonMatchingDifficulty() throws Exception {
            AnalysisResult mateIn1 = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateIn1);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles("MASTER", 5);

            assertTrue(puzzles.isEmpty(), "Mate-in-1 should not match MASTER difficulty");
        }

        @Test
        @DisplayName("respects count limit")
        void respectsCountLimit() throws Exception {
            AnalysisResult mateResult = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateResult);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 3);

            assertEquals(3, puzzles.size());
        }

        @Test
        @DisplayName("handles Stockfish exceptions gracefully")
        void handlesStockfishException() throws Exception {
            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenThrow(new RuntimeException("Stockfish crashed"));

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 5);

            assertTrue(puzzles.isEmpty());
        }

        @Test
        @DisplayName("correctly extracts side to move from FEN")
        void correctSideToMove() throws Exception {
            AnalysisResult mateResult = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateResult);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 40);

            boolean hasWhite = puzzles.stream().anyMatch(p -> "white".equals(p.getSideToMove()));
            boolean hasBlack = puzzles.stream().anyMatch(p -> "black".equals(p.getSideToMove()));

            assertTrue(hasWhite, "Should have puzzles with white to move");
            assertTrue(hasBlack, "Should have puzzles with black to move");
        }

        @Test
        @DisplayName("skips positions with negative mate evaluations")
        void skipsNegativeMate() throws Exception {
            AnalysisResult negativeMate = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("M-3")
                    .principalVariation(List.of("e2e4"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(negativeMate);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 5);

            assertTrue(puzzles.isEmpty());
        }

        @Test
        @DisplayName("generates correct difficulty labels for various mate depths")
        void correctDifficultyLabels() throws Exception {
            AnalysisResult mateIn2 = AnalysisResult.builder()
                    .bestMove("f3g5")
                    .evaluation("M2")
                    .principalVariation(List.of("f3g5", "e7e6", "g5f7"))
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateIn2);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 5);

            for (GeneratedPuzzle puzzle : puzzles) {
                assertEquals("INTERMEDIATE", puzzle.getDifficulty());
                assertEquals(2, puzzle.getMateIn());
            }
        }

        @Test
        @DisplayName("requesting zero puzzles returns empty list")
        void zeroCountReturnsEmpty() throws Exception {
            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 0);

            assertTrue(puzzles.isEmpty());
            verifyNoInteractions(stockfishService);
        }

        @Test
        @DisplayName("solution line is populated from principal variation")
        void solutionLinePopulated() throws Exception {
            List<String> pv = List.of("h5f7", "e8f7", "d1d7");
            AnalysisResult mateResult = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M2")
                    .principalVariation(pv)
                    .depth(25)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(25)))
                    .thenReturn(mateResult);

            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(null, 1);

            assertFalse(puzzles.isEmpty());
            assertEquals(pv, puzzles.get(0).getSolutionLine());
        }
    }
}
