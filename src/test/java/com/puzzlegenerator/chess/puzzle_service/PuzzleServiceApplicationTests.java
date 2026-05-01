package com.puzzlegenerator.chess.puzzle_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"stockfish.enabled=false"
})
class PuzzleServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
