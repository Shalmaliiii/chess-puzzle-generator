package com.puzzlegenerator.chess.puzzle_service.generator;

import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import com.puzzlegenerator.chess.puzzle_service.dto.GeneratedPuzzle;
import com.puzzlegenerator.chess.puzzle_service.service.StockfishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true", matchIfMissing = true)
public class PuzzleGeneratorService {

    private final StockfishService stockfishService;
    private final FenGenerator fenGenerator;
    private final MateDetector mateDetector;
    private final DifficultyClassifier difficultyClassifier;

    public PuzzleGeneratorService(StockfishService stockfishService,
                                  FenGenerator fenGenerator,
                                  MateDetector mateDetector,
                                  DifficultyClassifier difficultyClassifier) {
        this.stockfishService = stockfishService;
        this.fenGenerator = fenGenerator;
        this.mateDetector = mateDetector;
        this.difficultyClassifier = difficultyClassifier;
    }

    public List<GeneratedPuzzle> generatePuzzles(String difficulty, int count) {
        log.info("Generating {} puzzles with difficulty: {}", count, difficulty);

        List<GeneratedPuzzle> puzzles = new ArrayList<>();
        List<String> positions = fenGenerator.getAllMatePositions();

        for (String fen : positions) {
            if (puzzles.size() >= count) {
                break;
            }

            try {
                AnalysisResult result = stockfishService.analyzePosition(fen, 25);

                if (!mateDetector.isForcedMate(result)) {
                    continue;
                }

                int mateIn = mateDetector.getMateInN(result);
                if (mateIn <= 0) {
                    continue;
                }

                if (!difficultyClassifier.matchesDifficulty(mateIn, difficulty)) {
                    continue;
                }

                String sideToMove = extractSideToMove(fen);

                GeneratedPuzzle puzzle = GeneratedPuzzle.builder()
                        .fen(fen)
                        .solutionLine(result.getPrincipalVariation())
                        .mateIn(mateIn)
                        .difficulty(difficultyClassifier.classify(mateIn))
                        .sideToMove(sideToMove)
                        .build();

                puzzles.add(puzzle);
                log.info("Generated puzzle: mate-in-{} ({}), FEN: {}", mateIn, puzzle.getDifficulty(), fen);

            } catch (Exception e) {
                log.warn("Failed to analyze position {}: {}", fen, e.getMessage());
            }
        }

        log.info("Generated {} puzzles out of {} requested", puzzles.size(), count);
        return puzzles;
    }

    private String extractSideToMove(String fen) {
        String[] parts = fen.split("\\s+");
        if (parts.length >= 2) {
            return "w".equals(parts[1]) ? "WHITE" : "BLACK";
        }
        return "unknown";
    }
}
