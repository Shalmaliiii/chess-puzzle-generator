package com.puzzlegenerator.chess.puzzle_service.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DifficultyClassifier {

    public static final String BEGINNER = "BEGINNER";
    public static final String INTERMEDIATE = "INTERMEDIATE";
    public static final String ADVANCED = "ADVANCED";
    public static final String MASTER = "MASTER";

    public String classify(int mateIn) {
        String difficulty;
        if (mateIn == 1) {
            difficulty = BEGINNER;
        } else if (mateIn == 2) {
            difficulty = INTERMEDIATE;
        } else if (mateIn == 3) {
            difficulty = ADVANCED;
        } else {
            difficulty = MASTER;
        }
        log.debug("Mate-in-{} classified as {}", mateIn, difficulty);
        return difficulty;
    }

    public boolean matchesDifficulty(int mateIn, String targetDifficulty) {
        if (targetDifficulty == null) {
            return true;
        }
        return classify(mateIn).equalsIgnoreCase(targetDifficulty);
    }
}
