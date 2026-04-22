package com.puzzlegenerator.chess.puzzle_service.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true", matchIfMissing = true)
public class StockfishService {

    private static final Logger logger =
            LoggerFactory.getLogger(StockfishService.class);

    // Strict FEN validator: 8 ranks of piece placement, side to move, castling
    // rights, en passant target, halfmove clock, fullmove number. Rejects any
    // control characters (including CR/LF) that could be used to inject
    // additional UCI commands into the engine's stdin.
    private static final Pattern FEN_PATTERN = Pattern.compile(
            "^([rnbqkpRNBQKP1-8]+/){7}[rnbqkpRNBQKP1-8]+ [wb] (-|[KQkq]{1,4}) (-|[a-h][36]) \\d{1,3} \\d{1,4}$");

    static boolean isValidFen(String fen) {
        if (fen == null || fen.length() > 100) {
            return false;
        }
        return FEN_PATTERN.matcher(fen).matches();
    }

    @Value("${stockfish.path}")
    private String stockfishPath;

    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;

    @PostConstruct
    public void startEngine() throws Exception {

        logger.info("Starting Stockfish engine...");

        ProcessBuilder processBuilder =
                new ProcessBuilder(stockfishPath);

        engineProcess = processBuilder.start();

        if (!engineProcess.isAlive()) {
            throw new IllegalStateException("Stockfish engine failed to start.");
        }

        reader = new BufferedReader(
                new InputStreamReader(engineProcess.getInputStream()));

        writer = new BufferedWriter(
                new OutputStreamWriter(engineProcess.getOutputStream()));

        initializeEngine();

        logger.info("Stockfish engine initialized successfully.");
    }

    private void initializeEngine() throws Exception {

        sendCommand("uci");
        waitFor("uciok");

        sendCommand("isready");
        waitFor("readyok");
    }

    private void sendCommand(String command) throws Exception {

        logger.info("Sending command to Stockfish: {}", command);

        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String expected) throws Exception {

        String line;

        while ((line = reader.readLine()) != null) {

            logger.debug("Stockfish: {}", line);

            if (line.contains(expected)) {
                logger.info("Received confirmation from engine: {}", expected);
                return;
            }
        }

        throw new IllegalStateException(
                "Did not receive expected response from Stockfish: " + expected);
    }

    public String getBestMove(String fen) throws Exception {

        if (!isValidFen(fen)) {
            logger.warn("Rejected invalid FEN input");
            throw new IllegalArgumentException("Invalid FEN");
        }

        logger.info("Analyzing position: {}", fen);
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go depth 15");

        String line;

        while ((line = reader.readLine()) != null) {

            logger.debug("Stockfish: {}", line);

            if (line.startsWith("bestmove")) {
                logger.info("Best move found: {}", line);
                return line;
            }
        }

        return "No move found";
    }

    @PreDestroy
    public void shutdown() {

        logger.info("Shutting down Stockfish engine...");

        if (engineProcess != null && engineProcess.isAlive()) {
            engineProcess.destroy();
        }
    }
}