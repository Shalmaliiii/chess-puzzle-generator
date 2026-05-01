package com.puzzlegenerator.chess.puzzle_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"stockfish.enabled=false",
	"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class PuzzleServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
