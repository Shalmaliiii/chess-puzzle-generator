package com.puzzlegenerator.chess.puzzle_service.kafka;

import com.puzzlegenerator.chess.puzzle_service.dto.PuzzleGeneratedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnBean(KafkaTemplate.class)
public class PuzzleGeneratedProducer {

    private static final String TOPIC = "puzzle.generated";

    private final KafkaTemplate<String, PuzzleGeneratedEvent> kafkaTemplate;

    public PuzzleGeneratedProducer(KafkaTemplate<String, PuzzleGeneratedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(PuzzleGeneratedEvent event) {
        log.info("Publishing puzzle generated event for FEN: {}", event.getFen());
        kafkaTemplate.send(TOPIC, event.getFen(), event);
    }
}
