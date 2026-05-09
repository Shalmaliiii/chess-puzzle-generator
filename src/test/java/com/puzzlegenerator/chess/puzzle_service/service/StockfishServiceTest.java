package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockfishServiceTest {

    private final StockfishService service = new StockfishService();

    private String invokeParseScore(String infoLine) throws Exception {
        Method method = StockfishService.class.getDeclaredMethod("parseScore", String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, infoLine);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeParsePv(String infoLine) throws Exception {
        Method method = StockfishService.class.getDeclaredMethod("parsePv", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(service, infoLine);
    }

    private int invokeParseDepth(String infoLine) throws Exception {
        Method method = StockfishService.class.getDeclaredMethod("parseDepth", String.class);
        method.setAccessible(true);
        return (int) method.invoke(service, infoLine);
    }

    @Nested
    @DisplayName("parseScore")
    class ParseScore {

        @Test
        @DisplayName("parses positive mate score")
        void positiveMateScore() throws Exception {
            String line = "info depth 20 seldepth 7 multipv 1 score mate 3 nodes 12345 pv e2e4 e7e5 d1h5";
            assertEquals("M3", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses negative mate score")
        void negativeMateScore() throws Exception {
            String line = "info depth 20 seldepth 7 multipv 1 score mate -2 nodes 12345 pv e2e4 e7e5";
            assertEquals("M-2", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses mate-in-1 score")
        void mateInOneScore() throws Exception {
            String line = "info depth 1 score mate 1 pv h5f7";
            assertEquals("M1", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses positive centipawn score")
        void positiveCentipawnScore() throws Exception {
            String line = "info depth 20 seldepth 30 score cp 150 nodes 999 pv e2e4";
            assertEquals("+1.5", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses negative centipawn score")
        void negativeCentipawnScore() throws Exception {
            String line = "info depth 20 seldepth 30 score cp -250 nodes 999 pv e2e4";
            assertEquals("-2.5", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses zero centipawn score")
        void zeroCentipawnScore() throws Exception {
            String line = "info depth 20 seldepth 30 score cp 0 nodes 999 pv e2e4";
            assertEquals("+0.0", invokeParseScore(line));
        }

        @Test
        @DisplayName("parses small centipawn score")
        void smallCentipawnScore() throws Exception {
            String line = "info depth 15 score cp 35 pv d2d4";
            assertEquals("+0.4", invokeParseScore(line));
        }

        @Test
        @DisplayName("returns 0.0 for line without score")
        void noScoreReturnsDefault() throws Exception {
            String line = "info depth 5 nodes 100";
            assertEquals("0.0", invokeParseScore(line));
        }

        @Test
        @DisplayName("mate score takes priority if both mate and cp present (mate first)")
        void mateScoreTakesPriority() throws Exception {
            String line = "info depth 20 score mate 2 score cp 100 pv e2e4";
            assertEquals("M2", invokeParseScore(line));
        }
    }

    @Nested
    @DisplayName("parsePv")
    class ParsePv {

        @Test
        @DisplayName("parses single move PV")
        void singleMovePv() throws Exception {
            String line = "info depth 20 score mate 1 pv h5f7";
            List<String> pv = invokeParsePv(line);
            assertEquals(List.of("h5f7"), pv);
        }

        @Test
        @DisplayName("parses multi-move PV")
        void multiMovePv() throws Exception {
            String line = "info depth 20 score mate 3 pv e2e4 e7e5 d1h5 b8c6 h5f7";
            List<String> pv = invokeParsePv(line);
            assertEquals(List.of("e2e4", "e7e5", "d1h5", "b8c6", "h5f7"), pv);
        }

        @Test
        @DisplayName("returns empty list when no pv in line")
        void noPvReturnsEmpty() throws Exception {
            String line = "info depth 5 score cp 100 nodes 200";
            List<String> pv = invokeParsePv(line);
            assertTrue(pv.isEmpty());
        }

        @Test
        @DisplayName("handles PV with promotion moves")
        void pvWithPromotion() throws Exception {
            String line = "info depth 20 score mate 1 pv a7a8q";
            List<String> pv = invokeParsePv(line);
            assertEquals(List.of("a7a8q"), pv);
        }
    }

    @Nested
    @DisplayName("parseDepth")
    class ParseDepth {

        @Test
        @DisplayName("parses depth from info line")
        void parsesDepth() throws Exception {
            String line = "info depth 25 seldepth 30 score cp 100 pv e2e4";
            assertEquals(25, invokeParseDepth(line));
        }

        @Test
        @DisplayName("parses single digit depth")
        void singleDigitDepth() throws Exception {
            String line = "info depth 5 score cp 50 pv e2e4";
            assertEquals(5, invokeParseDepth(line));
        }

        @Test
        @DisplayName("returns 0 when no depth token")
        void noDepthReturnsZero() throws Exception {
            String line = "info score cp 100 pv e2e4";
            assertEquals(0, invokeParseDepth(line));
        }

        @Test
        @DisplayName("returns 0 for malformed depth value")
        void malformedDepthReturnsZero() throws Exception {
            String line = "info depth abc score cp 100 pv e2e4";
            assertEquals(0, invokeParseDepth(line));
        }

        @Test
        @DisplayName("depth at end of tokens with no following value returns 0")
        void depthAtEndReturnsZero() throws Exception {
            String line = "info depth";
            assertEquals(0, invokeParseDepth(line));
        }
    }

    @Nested
    @DisplayName("isRunning and getEngineVersion")
    class StatusMethods {

        @Test
        @DisplayName("isRunning returns false when engine not started")
        void isRunningFalseWhenNotStarted() {
            assertFalse(service.isRunning());
        }

        @Test
        @DisplayName("getEngineVersion returns unknown when not initialized")
        void defaultEngineVersion() {
            assertEquals("unknown", service.getEngineVersion());
        }
    }
}
