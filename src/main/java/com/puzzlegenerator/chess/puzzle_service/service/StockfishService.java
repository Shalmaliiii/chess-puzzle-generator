package com.puzzlegenerator.chess.puzzle_service.service;

import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true", matchIfMissing = true)
public class StockfishService {

    @Value("${stockfish.path}")
    private String stockfishPath;

    private Process engineProcess;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String engineVersion = "unknown";

    @PostConstruct
    public void startEngine() throws Exception {
        log.info("Starting Stockfish engine...");

        ProcessBuilder processBuilder = new ProcessBuilder(stockfishPath);
        engineProcess = processBuilder.start();

        if (!engineProcess.isAlive()) {
            throw new IllegalStateException("Stockfish engine failed to start.");
        }

        reader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));

        initializeEngine();
        log.info("Stockfish engine initialized successfully.");
    }

    private void initializeEngine() throws Exception {
        sendCommand("uci");
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("Stockfish: {}", line);
            if (line.startsWith("id name")) {
                engineVersion = line.substring("id name ".length()).trim();
            }
            if (line.contains("uciok")) {
                log.info("Received confirmation from engine: uciok");
                break;
            }
        }

        sendCommand("isready");
        waitFor("readyok");
    }

    private void sendCommand(String command) throws Exception {
        log.debug("Sending command to Stockfish: {}", command);
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String expected) throws Exception {
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("Stockfish: {}", line);
            if (line.contains(expected)) {
                log.info("Received confirmation from engine: {}", expected);
                return;
            }
        }
        throw new IllegalStateException("Did not receive expected response from Stockfish: " + expected);
    }

    public synchronized String getBestMove(String fen) throws Exception {
        log.info("Analyzing position: {}", fen);
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go depth 15");

        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("Stockfish: {}", line);
            if (line.startsWith("bestmove")) {
                log.info("Best move found: {}", line);
                return line;
            }
        }
        return "No move found";
    }

    public synchronized AnalysisResult analyzePosition(String fen, int depth) throws Exception {
        log.info("Analyzing position at depth {}: {}", depth, fen);

        sendCommand("ucinewgame");
        sendCommand("isready");
        waitFor("readyok");
        sendCommand("position fen " + fen);
        sendCommand("go depth " + depth);

        String bestMove = null;
        String evaluation = "0.0";
        List<String> principalVariation = new ArrayList<>();
        int reachedDepth = 0;

        String lastScore = null;
        List<String> lastPv = new ArrayList<>();
        int lastDepth = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("Stockfish: {}", line);

            if (line.startsWith("info") && line.contains(" pv ")) {
                lastScore = parseScore(line);
                lastPv = parsePv(line);
                lastDepth = parseDepth(line);
            }

            if (line.startsWith("bestmove")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    bestMove = parts[1];
                }
                evaluation = lastScore != null ? lastScore : "0.0";
                principalVariation = lastPv;
                reachedDepth = lastDepth;
                break;
            }
        }

        return AnalysisResult.builder()
                .bestMove(bestMove)
                .evaluation(evaluation)
                .principalVariation(principalVariation)
                .depth(reachedDepth)
                .build();
    }

    private String parseScore(String infoLine) {
        if (infoLine.contains("score mate ")) {
            int idx = infoLine.indexOf("score mate ");
            String rest = infoLine.substring(idx + "score mate ".length());
            String mateValue = rest.split("\\s+")[0];
            int mateIn = Integer.parseInt(mateValue);
            return "M" + mateIn;
        }
        if (infoLine.contains("score cp ")) {
            int idx = infoLine.indexOf("score cp ");
            String rest = infoLine.substring(idx + "score cp ".length());
            String cpValue = rest.split("\\s+")[0];
            int cp = Integer.parseInt(cpValue);
            double eval = cp / 100.0;
            return (eval >= 0 ? "+" : "") + String.format("%.1f", eval);
        }
        return "0.0";
    }

    private List<String> parsePv(String infoLine) {
        int pvIdx = infoLine.indexOf(" pv ");
        if (pvIdx == -1) {
            return new ArrayList<>();
        }
        String pvStr = infoLine.substring(pvIdx + " pv ".length()).trim();
        return Arrays.asList(pvStr.split("\\s+"));
    }

    private int parseDepth(String infoLine) {
        String[] tokens = infoLine.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if ("depth".equals(tokens[i])) {
                try {
                    return Integer.parseInt(tokens[i + 1]);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public boolean isRunning() {
        return engineProcess != null && engineProcess.isAlive();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Stockfish engine...");
        if (engineProcess != null && engineProcess.isAlive()) {
            engineProcess.destroy();
        }
    }
}
