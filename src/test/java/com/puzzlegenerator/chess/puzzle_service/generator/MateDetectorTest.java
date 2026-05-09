package com.puzzlegenerator.chess.puzzle_service.generator;

import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MateDetectorTest {

    private MateDetector mateDetector;

    @BeforeEach
    void setUp() {
        mateDetector = new MateDetector();
    }

    @Nested
    @DisplayName("isForcedMate")
    class IsForcedMate {

        @Test
        @DisplayName("returns true for mate-in-1")
        void mateInOne() {
            AnalysisResult result = AnalysisResult.builder()
                    .evaluation("M1")
                    .bestMove("e2e4")
                    .principalVariation(List.of("e2e4"))
                    .depth(20)
                    .build();
            assertTrue(mateDetector.isForcedMate(result));
        }

        @ParameterizedTest
        @ValueSource(strings = {"M1", "M2", "M3", "M5", "M10", "M25"})
        @DisplayName("returns true for various positive mate evaluations")
        void variousPositiveMates(String eval) {
            AnalysisResult result = AnalysisResult.builder().evaluation(eval).build();
            assertTrue(mateDetector.isForcedMate(result));
        }

        @ParameterizedTest
        @ValueSource(strings = {"M-1", "M-2", "M-5"})
        @DisplayName("returns false for negative mate evaluations (opponent has mate)")
        void negativeMate(String eval) {
            AnalysisResult result = AnalysisResult.builder().evaluation(eval).build();
            assertFalse(mateDetector.isForcedMate(result));
        }

        @ParameterizedTest
        @ValueSource(strings = {"M0"})
        @DisplayName("returns false for mate-in-0 (edge case)")
        void mateInZero(String eval) {
            AnalysisResult result = AnalysisResult.builder().evaluation(eval).build();
            assertFalse(mateDetector.isForcedMate(result));
        }

        @Test
        @DisplayName("returns false for null result")
        void nullResult() {
            assertFalse(mateDetector.isForcedMate(null));
        }

        @Test
        @DisplayName("returns false for null evaluation")
        void nullEvaluation() {
            AnalysisResult result = AnalysisResult.builder().evaluation(null).build();
            assertFalse(mateDetector.isForcedMate(result));
        }

        @ParameterizedTest
        @ValueSource(strings = {"+3.5", "-1.2", "0.0", "+0.5"})
        @DisplayName("returns false for centipawn evaluations")
        void centipawnEvaluation(String eval) {
            AnalysisResult result = AnalysisResult.builder().evaluation(eval).build();
            assertFalse(mateDetector.isForcedMate(result));
        }

        @Test
        @DisplayName("returns false for malformed mate string")
        void malformedMateString() {
            AnalysisResult result = AnalysisResult.builder().evaluation("Mabc").build();
            assertFalse(mateDetector.isForcedMate(result));
        }

        @Test
        @DisplayName("returns false for empty evaluation string")
        void emptyEvaluation() {
            AnalysisResult result = AnalysisResult.builder().evaluation("").build();
            assertFalse(mateDetector.isForcedMate(result));
        }
    }

    @Nested
    @DisplayName("getMateInN")
    class GetMateInN {

        @ParameterizedTest
        @CsvSource({"M1,1", "M2,2", "M3,3", "M5,5", "M10,10"})
        @DisplayName("returns correct mate-in-N value for forced mates")
        void correctMateValue(String eval, int expected) {
            AnalysisResult result = AnalysisResult.builder().evaluation(eval).build();
            assertEquals(expected, mateDetector.getMateInN(result));
        }

        @Test
        @DisplayName("returns -1 for non-mate evaluation")
        void nonMateEvaluation() {
            AnalysisResult result = AnalysisResult.builder().evaluation("+3.5").build();
            assertEquals(-1, mateDetector.getMateInN(result));
        }

        @Test
        @DisplayName("returns -1 for null result")
        void nullResult() {
            assertEquals(-1, mateDetector.getMateInN(null));
        }

        @Test
        @DisplayName("returns -1 for negative mate (opponent has mate)")
        void negativeMate() {
            AnalysisResult result = AnalysisResult.builder().evaluation("M-3").build();
            assertEquals(-1, mateDetector.getMateInN(result));
        }

        @Test
        @DisplayName("returns -1 for malformed evaluation")
        void malformedEvaluation() {
            AnalysisResult result = AnalysisResult.builder().evaluation("Mxyz").build();
            assertEquals(-1, mateDetector.getMateInN(result));
        }
    }
}
