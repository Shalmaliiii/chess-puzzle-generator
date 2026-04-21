package com.puzzlegenerator.chess.puzzle_service.service;

/**
 * Thrown when the Stockfish engine fails to start, respond, or shut down cleanly.
 * Used to propagate engine errors to callers instead of returning sentinel values.
 */
public class StockfishException extends RuntimeException {

    public StockfishException(String message) {
        super(message);
    }

    public StockfishException(String message, Throwable cause) {
        super(message, cause);
    }
}
