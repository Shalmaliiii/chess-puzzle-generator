package com.puzzlegenerator.chess.puzzle_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PuzzleServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(PuzzleServiceApplication.class);

	public static void main(String[] args) {
		logger.info("Starting PuzzleServiceApplication...");
		try {
			SpringApplication.run(PuzzleServiceApplication.class, args);
			logger.info("PuzzleServiceApplication started successfully.");
		} catch (Exception ex) {
			logger.error("PuzzleServiceApplication failed to start.", ex);
			throw ex;
		}
	}

	@Bean
	public ApplicationRunner applicationRunner() {
		return args -> {
			logger.info("Puzzle Service Application is running with arguments: {}", args.getSourceArgs());
		};
	}
}
