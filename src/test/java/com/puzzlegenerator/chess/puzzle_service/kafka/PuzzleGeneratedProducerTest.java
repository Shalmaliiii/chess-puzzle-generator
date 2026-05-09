package com.puzzlegenerator.chess.puzzle_service.kafka;

import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGeneratedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuzzleGeneratedProducerTest {

    @Mock
    private KafkaTemplate<String, PuzzleGeneratedEvent> kafkaTemplate;

    @InjectMocks
    private PuzzleGeneratedProducer producer;

    @Test
    @DisplayName("publishes event to correct topic with FEN as key")
    void publishesToCorrectTopic() {
        PuzzleGeneratedEvent event = PuzzleGeneratedEvent.builder()
                .fen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
                .solutionLine(List.of("h5f7"))
                .mateIn(1)
                .difficulty("BEGINNER")
                .sideToMove("white")
                .build();

        producer.publish(event);

        verify(kafkaTemplate).send(
                eq("puzzle.generated"),
                eq(event.getFen()),
                eq(event)
        );
    }

    @Test
    @DisplayName("sends event with all fields populated")
    void sendsEventWithAllFields() {
        PuzzleGeneratedEvent event = PuzzleGeneratedEvent.builder()
                .fen("test-fen")
                .solutionLine(List.of("e2e4", "e7e5", "d1h5"))
                .mateIn(2)
                .difficulty("INTERMEDIATE")
                .sideToMove("white")
                .build();

        producer.publish(event);

        ArgumentCaptor<PuzzleGeneratedEvent> captor = ArgumentCaptor.forClass(PuzzleGeneratedEvent.class);
        verify(kafkaTemplate).send(anyString(), anyString(), captor.capture());

        PuzzleGeneratedEvent captured = captor.getValue();
        assertEquals("test-fen", captured.getFen());
        assertEquals(List.of("e2e4", "e7e5", "d1h5"), captured.getSolutionLine());
        assertEquals(2, captured.getMateIn());
        assertEquals("INTERMEDIATE", captured.getDifficulty());
        assertEquals("white", captured.getSideToMove());
    }

    @Test
    @DisplayName("uses FEN as the Kafka message key")
    void usesFenAsKey() {
        String fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1";
        PuzzleGeneratedEvent event = PuzzleGeneratedEvent.builder()
                .fen(fen)
                .solutionLine(List.of("e1e8"))
                .mateIn(1)
                .difficulty("BEGINNER")
                .sideToMove("white")
                .build();

        producer.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any());

        assertEquals(fen, keyCaptor.getValue());
    }
}
