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

    // Basic FEN validator: 6 space-separated fields. The first field is ranks
    // made of piece letters (pnbrqkPNBRQK) and digits 1-8 separated by '/'.
    // The remaining fields carry side-to-move, castling, en-passant, halfmove
    // and fullmove counters. This is intentionally strict to prevent callers
    // from smuggling newlines or additional UCI commands through the FEN.
    private static final Pattern FEN_PATTERN = Pattern.compile(
            "^([pnbrqkPNBRQK1-8]+/){7}[pnbrqkPNBRQK1-8]+"
                    + " [wb]"
                    + " (-|[KQkq]{1,4})"
                    + " (-|[a-h][1-8])"
                    + " \\d{1,3}"
                    + " \\d{1,4}$");

    private static final int MAX_FEN_LENGTH = 100;

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

        String sanitizedFen = validateFen(fen);

        logger.info("Analyzing position: {}", sanitizedFen);
        sendCommand("ucinewgame");
        sendCommand("position fen " + sanitizedFen);
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

    /**
     * Validate a FEN string before it is forwarded to the Stockfish engine.
     *
     * <p>The Stockfish process speaks the UCI protocol over stdin where
     * commands are delimited by newlines. Concatenating unvalidated user input
     * into the {@code position fen ...} command would allow a caller to embed
     * newline characters and smuggle arbitrary UCI commands (e.g. {@code
     * setoption}, {@code quit}) into the engine. Rejecting anything that does
     * not match the expected FEN grammar prevents that injection vector.
     */
    static String validateFen(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN must not be null");
        }

        String trimmed = fen.trim();

        if (trimmed.isEmpty() || trimmed.length() > MAX_FEN_LENGTH) {
            throw new IllegalArgumentException(
                    "FEN length must be between 1 and " + MAX_FEN_LENGTH + " characters");
        }

        if (trimmed.indexOf('\n') >= 0 || trimmed.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("FEN must not contain newline characters");
        }

        if (!FEN_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid FEN string");
        }

        return trimmed;
    }

    @PreDestroy
    public void shutdown() {

        logger.info("Shutting down Stockfish engine...");

        if (engineProcess != null && engineProcess.isAlive()) {
            engineProcess.destroy();
        }
    }
}
