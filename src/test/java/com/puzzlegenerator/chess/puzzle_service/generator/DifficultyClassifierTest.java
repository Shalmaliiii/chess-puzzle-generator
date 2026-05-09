package com.puzzlegenerator.chess.puzzle_service.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyClassifierTest {

    private DifficultyClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new DifficultyClassifier();
    }

    @Nested
    @DisplayName("classify")
    class Classify {

        @Test
        @DisplayName("mate-in-1 is BEGINNER")
        void mateInOneIsBeginner() {
            assertEquals(DifficultyClassifier.BEGINNER, classifier.classify(1));
        }

        @Test
        @DisplayName("mate-in-2 is INTERMEDIATE")
        void mateInTwoIsIntermediate() {
            assertEquals(DifficultyClassifier.INTERMEDIATE, classifier.classify(2));
        }

        @Test
        @DisplayName("mate-in-3 is ADVANCED")
        void mateInThreeIsAdvanced() {
            assertEquals(DifficultyClassifier.ADVANCED, classifier.classify(3));
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 5, 6, 10, 20, 100})
        @DisplayName("mate-in-4 and above is MASTER")
        void mateInFourPlusIsMaster(int mateIn) {
            assertEquals(DifficultyClassifier.MASTER, classifier.classify(mateIn));
        }

        @Test
        @DisplayName("mate-in-0 edge case returns MASTER (falls to else branch)")
        void mateInZero() {
            assertEquals(DifficultyClassifier.MASTER, classifier.classify(0));
        }

        @Test
        @DisplayName("negative mate value returns MASTER (falls to else branch)")
        void negativeMate() {
            assertEquals(DifficultyClassifier.MASTER, classifier.classify(-1));
        }
    }

    @Nested
    @DisplayName("matchesDifficulty")
    class MatchesDifficulty {

        @Test
        @DisplayName("null target matches any difficulty")
        void nullTargetMatchesAll() {
            assertTrue(classifier.matchesDifficulty(1, null));
            assertTrue(classifier.matchesDifficulty(2, null));
            assertTrue(classifier.matchesDifficulty(3, null));
            assertTrue(classifier.matchesDifficulty(5, null));
        }

        @ParameterizedTest
        @CsvSource({
                "1, BEGINNER",
                "2, INTERMEDIATE",
                "3, ADVANCED",
                "4, MASTER",
                "5, MASTER"
        })
        @DisplayName("returns true when mate-in-N matches target difficulty")
        void matchesCorrectDifficulty(int mateIn, String target) {
            assertTrue(classifier.matchesDifficulty(mateIn, target));
        }

        @ParameterizedTest
        @CsvSource({
                "1, INTERMEDIATE",
                "1, ADVANCED",
                "1, MASTER",
                "2, BEGINNER",
                "2, ADVANCED",
                "3, BEGINNER",
                "4, BEGINNER"
        })
        @DisplayName("returns false when mate-in-N does not match target difficulty")
        void doesNotMatchWrongDifficulty(int mateIn, String target) {
            assertFalse(classifier.matchesDifficulty(mateIn, target));
        }

        @Test
        @DisplayName("case-insensitive matching works")
        void caseInsensitive() {
            assertTrue(classifier.matchesDifficulty(1, "beginner"));
            assertTrue(classifier.matchesDifficulty(1, "Beginner"));
            assertTrue(classifier.matchesDifficulty(2, "intermediate"));
            assertTrue(classifier.matchesDifficulty(3, "advanced"));
            assertTrue(classifier.matchesDifficulty(4, "master"));
        }
    }
}
