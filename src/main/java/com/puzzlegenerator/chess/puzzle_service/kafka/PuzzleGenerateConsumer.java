package com.puzzlegenerator.chess.puzzle_service.kafka;

import com.puzzlegenerator.chess.puzzle_service.dto.GeneratedPuzzle;
import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGenerateEvent;
import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGeneratedEvent;
import com.puzzlegenerator.chess.puzzle_service.generator.PuzzleGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class PuzzleGenerateConsumer {

    private final PuzzleGeneratorService puzzleGeneratorService;
    private final PuzzleGeneratedProducer puzzleGeneratedProducer;

    public PuzzleGenerateConsumer(PuzzleGeneratorService puzzleGeneratorService,
                                  PuzzleGeneratedProducer puzzleGeneratedProducer) {
        this.puzzleGeneratorService = puzzleGeneratorService;
        this.puzzleGeneratedProducer = puzzleGeneratedProducer;
    }

    @KafkaListener(topics = "puzzle.generate", groupId = "engine-service")
    public void consume(PuzzleGenerateEvent event) {
        log.info("Received puzzle generate event: difficulty={}, count={}, requestedBy={}",
                event.getDifficulty(), event.getCount(), event.getRequestedBy());

        try {
            List<GeneratedPuzzle> puzzles = puzzleGeneratorService.generatePuzzles(
                    event.getDifficulty(), event.getCount());

            for (GeneratedPuzzle puzzle : puzzles) {
                PuzzleGeneratedEvent generatedEvent = PuzzleGeneratedEvent.builder()
                        .fen(puzzle.getFen())
                        .solutionLine(puzzle.getSolutionLine())
                        .mateIn(puzzle.getMateIn())
                        .difficulty(puzzle.getDifficulty())
                        .sideToMove(puzzle.getSideToMove())
                        .build();

                puzzleGeneratedProducer.publish(generatedEvent);
            }

            log.info("Published {} puzzle generated events", puzzles.size());
        } catch (Exception e) {
            log.error("Failed to process puzzle generate event: {}", e.getMessage(), e);
        }
    }
}
