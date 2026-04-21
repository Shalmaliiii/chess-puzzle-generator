package com.puzzlegenerator.chess.puzzle_service.uci;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.function.Predicate;

/**
 * Wraps the stdin/stdout streams of a UCI-compatible chess engine process and
 * centralizes the protocol-level read/write primitives so they can be shared
 * by any service that drives such an engine.
 */
public class UciEngineIO implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(UciEngineIO.class);

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    public UciEngineIO(Process process) {
        this.process = process;
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    public void sendCommand(String command) throws IOException {
        logger.info("Sending command to engine: {}", command);
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    /**
     * Reads lines from the engine, logging each at debug level, until {@code match}
     * returns true. Returns the matching line.
     *
     * @throws IllegalStateException if the stream ends before a match is found.
     */
    public String readUntil(Predicate<String> match) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            logger.debug("Engine: {}", line);
            if (match.test(line)) {
                return line;
            }
        }
        throw new IllegalStateException("Engine stream closed before expected response was received.");
    }

    /**
     * Convenience wrapper around {@link #readUntil(Predicate)} that waits for a
     * line containing the given token (e.g. {@code uciok}, {@code readyok}).
     */
    public String waitFor(String expected) throws IOException {
        String line = readUntil(l -> l.contains(expected));
        logger.info("Received confirmation from engine: {}", expected);
        return line;
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    @Override
    public void close() {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }
}
