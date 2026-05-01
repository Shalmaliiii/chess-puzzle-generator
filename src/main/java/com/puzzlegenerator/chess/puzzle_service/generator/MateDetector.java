package com.puzzlegenerator.chess.puzzle_service.generator;

import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MateDetector {

    public boolean isForcedMate(AnalysisResult result) {
        if (result == null || result.getEvaluation() == null) {
            return false;
        }
        String eval = result.getEvaluation();
        if (!eval.startsWith("M")) {
            return false;
        }
        int mateIn = parseMateValue(eval);
        boolean isMate = mateIn > 0;
        log.debug("Evaluation '{}' is forced mate: {}", eval, isMate);
        return isMate;
    }

    public int getMateInN(AnalysisResult result) {
        if (!isForcedMate(result)) {
            return -1;
        }
        return parseMateValue(result.getEvaluation());
    }

    private int parseMateValue(String evaluation) {
        try {
            return Integer.parseInt(evaluation.substring(1));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse mate-in-N from evaluation: {}", evaluation);
            return -1;
        }
    }
}
