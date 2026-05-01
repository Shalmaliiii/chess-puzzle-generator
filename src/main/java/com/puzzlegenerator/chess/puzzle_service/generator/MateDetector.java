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
        boolean isMate = result.getEvaluation().startsWith("M");
        log.debug("Evaluation '{}' is forced mate: {}", result.getEvaluation(), isMate);
        return isMate;
    }

    public int getMateInN(AnalysisResult result) {
        if (!isForcedMate(result)) {
            return -1;
        }
        try {
            return Integer.parseInt(result.getEvaluation().substring(1));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse mate-in-N from evaluation: {}", result.getEvaluation());
            return -1;
        }
    }
}
