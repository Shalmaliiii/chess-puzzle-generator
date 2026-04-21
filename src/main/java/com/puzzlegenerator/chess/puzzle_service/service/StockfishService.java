package com.puzzlegenerator.chess.puzzle_service.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true", matchIfMissing = true)
public class StockfishService {

    private static final Logger logger =
            LoggerFactory.getLogger(StockfishService.class);

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    @Value("${stockfish.path}")
    private String stockfishPath;

    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;

    @PostConstruct
    public void startEngine() {

        logger.info("Starting Stockfish engine at path: {}", stockfishPath);

        ProcessBuilder processBuilder = new ProcessBuilder(stockfishPath);

        try {
            engineProcess = processBuilder.start();
        } catch (IOException e) {
            throw new StockfishException(
                    "Failed to start Stockfish process at path: " + stockfishPath, e);
        }

        if (!engineProcess.isAlive()) {
            throw new StockfishException("Stockfish engine failed to start (process is not alive).");
        }

        reader = new BufferedReader(
                new InputStreamReader(engineProcess.getInputStream()));

        writer = new BufferedWriter(
                new OutputStreamWriter(engineProcess.getOutputStream()));

        try {
            initializeEngine();
        } catch (IOException e) {
            // Ensure we don't leak a partially-initialized engine process.
            cleanup();
            throw new StockfishException("Failed to initialize Stockfish engine.", e);
        } catch (RuntimeException e) {
            cleanup();
            throw e;
        }

        logger.info("Stockfish engine initialized successfully.");
    }

    private void initializeEngine() throws IOException {

        sendCommand("uci");
        waitFor("uciok");

        sendCommand("isready");
        waitFor("readyok");
    }

    private void sendCommand(String command) throws IOException {

        logger.info("Sending command to Stockfish: {}", command);

        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String expected) throws IOException {

        String line;

        while ((line = reader.readLine()) != null) {

            logger.debug("Stockfish: {}", line);

            if (line.contains(expected)) {
                logger.info("Received confirmation from engine: {}", expected);
                return;
            }
        }

        throw new StockfishException(
                "Did not receive expected response from Stockfish: " + expected
                        + " (engine stream closed unexpectedly).");
    }

    public String getBestMove(String fen) {

        logger.info("Analyzing position: {}", fen);

        try {
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
        } catch (IOException e) {
            throw new StockfishException(
                    "I/O error while communicating with Stockfish for FEN: " + fen, e);
        }

        // Stream closed before a bestmove line arrived — surface this instead of
        // returning a sentinel string that callers are likely to treat as success.
        throw new StockfishException(
                "Stockfish closed its output stream before returning a best move for FEN: " + fen);
    }

    @PreDestroy
    public void shutdown() {

        logger.info("Shutting down Stockfish engine...");

        cleanup();
    }

    private void cleanup() {

        closeQuietly("writer", writer);
        closeQuietly("reader", reader);

        Process process = engineProcess;
        if (process == null) {
            return;
        }

        if (!process.isAlive()) {
            logger.info("Stockfish engine process already exited with code {}.", process.exitValue());
            return;
        }

        process.destroy();

        try {
            if (!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn(
                        "Stockfish engine did not exit within {}s after destroy(); forcing shutdown.",
                        SHUTDOWN_TIMEOUT_SECONDS);
                process.destroyForcibly();
                if (!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    logger.error("Stockfish engine did not exit even after destroyForcibly().");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for Stockfish engine to shut down.", e);
            process.destroyForcibly();
        }
    }

    private static void closeQuietly(String name, AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            // Closing streams can fail if the underlying process has already died;
            // log at warn rather than swallowing silently so operators can diagnose issues.
            logger.warn("Failed to close Stockfish {}: {}", name, e.toString());
        }
    }
}
