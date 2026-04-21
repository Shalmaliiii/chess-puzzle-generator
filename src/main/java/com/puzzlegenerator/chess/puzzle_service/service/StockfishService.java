package com.puzzlegenerator.chess.puzzle_service.service;

import com.puzzlegenerator.chess.puzzle_service.uci.UciEngineIO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true", matchIfMissing = true)
public class StockfishService {

    private static final Logger logger = LoggerFactory.getLogger(StockfishService.class);

    @Value("${stockfish.path}")
    private String stockfishPath;

    private UciEngineIO engine;

    @PostConstruct
    public void startEngine() throws Exception {

        logger.info("Starting Stockfish engine...");

        Process process = new ProcessBuilder(stockfishPath).start();

        if (!process.isAlive()) {
            throw new IllegalStateException("Stockfish engine failed to start.");
        }

        engine = new UciEngineIO(process);

        initializeEngine();

        logger.info("Stockfish engine initialized successfully.");
    }

    private void initializeEngine() throws Exception {
        engine.sendCommand("uci");
        engine.waitFor("uciok");

        engine.sendCommand("isready");
        engine.waitFor("readyok");
    }

    public String getBestMove(String fen) throws Exception {

        logger.info("Analyzing position: {}", fen);
        engine.sendCommand("ucinewgame");
        engine.sendCommand("position fen " + fen);
        engine.sendCommand("go depth 15");

        String line = engine.readUntil(l -> l.startsWith("bestmove"));
        logger.info("Best move found: {}", line);
        return line;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Stockfish engine...");
        if (engine != null) {
            engine.close();
        }
    }
}
