package com.puzzlegenerator.chess.puzzle_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private String bestMove;
    private String evaluation;
    private List<String> principalVariation;
    private int depth;
}
