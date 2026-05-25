package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockfishServiceTest {

    private static final String START_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static StockfishService withStreams(BufferedReader reader, BufferedWriter writer)
            throws Exception {
        StockfishService service = new StockfishService();
        setField(service, "reader", reader);
        setField(service, "writer", writer);
        return service;
    }

    @Test
    void getBestMove_returnsBestMoveLineAndSendsUciCommands() throws Exception {
        String engineOutput = String.join("\n",
                "info depth 1 score cp 23",
                "info depth 2 score cp 25",
                "bestmove e2e4 ponder e7e5",
                "");
        StringWriter rawWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(rawWriter);
        BufferedReader reader = new BufferedReader(new StringReader(engineOutput));

        StockfishService service = withStreams(reader, writer);

        String best = service.getBestMove(START_FEN);

        assertEquals("bestmove e2e4 ponder e7e5", best);

        writer.flush();
        String sent = rawWriter.toString();
        assertTrue(sent.contains("ucinewgame"), "expected ucinewgame to be sent, got: " + sent);
        assertTrue(sent.contains("position fen " + START_FEN),
                "expected FEN to be sent, got: " + sent);
        assertTrue(sent.contains("go depth 15"), "expected search command, got: " + sent);
    }

    @Test
    void getBestMove_returnsNoMoveFoundWhenStreamEndsWithoutBestMove() throws Exception {
        String engineOutput = "info depth 1\ninfo depth 2\n";
        BufferedReader reader = new BufferedReader(new StringReader(engineOutput));
        BufferedWriter writer = new BufferedWriter(new StringWriter());

        StockfishService service = withStreams(reader, writer);

        assertEquals("No move found", service.getBestMove(START_FEN));
    }

    @Test
    void getBestMove_skipsNonBestMoveLinesBeforeReturning() throws Exception {
        String engineOutput = String.join("\n",
                "info string hello",
                "info depth 10 score cp 12",
                "bestmove g1f3",
                "");
        BufferedReader reader = new BufferedReader(new StringReader(engineOutput));
        BufferedWriter writer = new BufferedWriter(new StringWriter());

        StockfishService service = withStreams(reader, writer);

        assertEquals("bestmove g1f3", service.getBestMove(START_FEN));
    }

    @Test
    void shutdown_destroysProcessWhenAlive() throws Exception {
        StockfishService service = new StockfishService();
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        setField(service, "engineProcess", process);

        service.shutdown();

        verify(process).destroy();
    }

    @Test
    void shutdown_doesNothingWhenProcessNull() {
        StockfishService service = new StockfishService();
        assertDoesNotThrow(service::shutdown);
    }

    @Test
    void shutdown_doesNotDestroyWhenProcessAlreadyDead() throws Exception {
        StockfishService service = new StockfishService();
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(false);
        setField(service, "engineProcess", process);

        service.shutdown();

        verify(process, never()).destroy();
    }

    @Test
    void startEngine_throwsWhenBinaryDoesNotExist() throws Exception {
        StockfishService service = new StockfishService();
        setField(service, "stockfishPath", "/nonexistent/definitely-not-stockfish-xyz");

        assertThrows(Exception.class, service::startEngine);
    }

    @Test
    void startEngine_throwsWhenEngineExitsWithoutUciHandshake() throws Exception {
        // /bin/true exits immediately without speaking UCI, so either the
        // handshake write fails (IOException) or waitFor("uciok") runs off
        // the end of the closed stream (IllegalStateException). Either way,
        // startEngine must surface an exception rather than return silently.
        StockfishService service = new StockfishService();
        setField(service, "stockfishPath", "/bin/true");

        assertThrows(Exception.class, service::startEngine);
    }
}
