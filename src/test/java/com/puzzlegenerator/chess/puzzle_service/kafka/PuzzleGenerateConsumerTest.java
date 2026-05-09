package com.puzzlegenerator.chess.puzzle_service.kafka;

import com.puzzlegenerator.chess.puzzle_service.dto.GeneratedPuzzle;
import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGenerateEvent;
import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGeneratedEvent;
import com.puzzlegenerator.chess.puzzle_service.generator.PuzzleGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuzzleGenerateConsumerTest {

    @Mock
    private PuzzleGeneratorService puzzleGeneratorService;

    @Mock
    private PuzzleGeneratedProducer puzzleGeneratedProducer;

    @InjectMocks
    private PuzzleGenerateConsumer consumer;

    @Test
    @DisplayName("processes event and publishes generated puzzles")
    void processesEventAndPublishes() {
        GeneratedPuzzle puzzle1 = GeneratedPuzzle.builder()
                .fen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
                .solutionLine(List.of("h5f7"))
                .mateIn(1)
                .difficulty("BEGINNER")
                .sideToMove("white")
                .build();

        GeneratedPuzzle puzzle2 = GeneratedPuzzle.builder()
                .fen("6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1")
                .solutionLine(List.of("e1e8"))
                .mateIn(1)
                .difficulty("BEGINNER")
                .sideToMove("white")
                .build();

        when(puzzleGeneratorService.generatePuzzles("BEGINNER", 2))
                .thenReturn(List.of(puzzle1, puzzle2));

        PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                .difficulty("BEGINNER")
                .count(2)
                .requestedBy("admin")
                .build();

        consumer.consume(event);

        verify(puzzleGeneratedProducer, times(2)).publish(any(PuzzleGeneratedEvent.class));

        ArgumentCaptor<PuzzleGeneratedEvent> captor = ArgumentCaptor.forClass(PuzzleGeneratedEvent.class);
        verify(puzzleGeneratedProducer, times(2)).publish(captor.capture());

        List<PuzzleGeneratedEvent> publishedEvents = captor.getAllValues();
        assertEquals(2, publishedEvents.size());

        assertEquals(puzzle1.getFen(), publishedEvents.get(0).getFen());
        assertEquals(puzzle1.getSolutionLine(), publishedEvents.get(0).getSolutionLine());
        assertEquals(puzzle1.getMateIn(), publishedEvents.get(0).getMateIn());
        assertEquals(puzzle1.getDifficulty(), publishedEvents.get(0).getDifficulty());
        assertEquals(puzzle1.getSideToMove(), publishedEvents.get(0).getSideToMove());

        assertEquals(puzzle2.getFen(), publishedEvents.get(1).getFen());
    }

    @Test
    @DisplayName("publishes nothing when no puzzles generated")
    void publishesNothingWhenEmpty() {
        when(puzzleGeneratorService.generatePuzzles("MASTER", 5))
                .thenReturn(List.of());

        PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                .difficulty("MASTER")
                .count(5)
                .requestedBy("admin")
                .build();

        consumer.consume(event);

        verify(puzzleGeneratedProducer, never()).publish(any());
    }

    @Test
    @DisplayName("handles generator service exception gracefully")
    void handlesExceptionGracefully() {
        when(puzzleGeneratorService.generatePuzzles(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Generation failed"));

        PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                .difficulty("BEGINNER")
                .count(1)
                .requestedBy("admin")
                .build();

        assertDoesNotThrow(() -> consumer.consume(event));
        verify(puzzleGeneratedProducer, never()).publish(any());
    }

    @Test
    @DisplayName("passes null difficulty from event correctly")
    void passesNullDifficulty() {
        when(puzzleGeneratorService.generatePuzzles(null, 3))
                .thenReturn(List.of());

        PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                .difficulty(null)
                .count(3)
                .requestedBy("user1")
                .build();

        consumer.consume(event);

        verify(puzzleGeneratorService).generatePuzzles(null, 3);
    }

    @Test
    @DisplayName("event fields are mapped correctly to generated event")
    void eventFieldsMappedCorrectly() {
        GeneratedPuzzle puzzle = GeneratedPuzzle.builder()
                .fen("test-fen")
                .solutionLine(List.of("a1a2", "b1b2", "c1c2"))
                .mateIn(3)
                .difficulty("ADVANCED")
                .sideToMove("black")
                .build();

        when(puzzleGeneratorService.generatePuzzles("ADVANCED", 1))
                .thenReturn(List.of(puzzle));

        PuzzleGenerateEvent event = PuzzleGenerateEvent.builder()
                .difficulty("ADVANCED")
                .count(1)
                .requestedBy("admin")
                .build();

        consumer.consume(event);

        ArgumentCaptor<PuzzleGeneratedEvent> captor = ArgumentCaptor.forClass(PuzzleGeneratedEvent.class);
        verify(puzzleGeneratedProducer).publish(captor.capture());

        PuzzleGeneratedEvent published = captor.getValue();
        assertEquals("test-fen", published.getFen());
        assertEquals(List.of("a1a2", "b1b2", "c1c2"), published.getSolutionLine());
        assertEquals(3, published.getMateIn());
        assertEquals("ADVANCED", published.getDifficulty());
        assertEquals("black", published.getSideToMove());
    }
}
