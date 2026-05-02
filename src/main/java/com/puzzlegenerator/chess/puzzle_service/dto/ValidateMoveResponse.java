package com.puzzlegenerator.chess.puzzle_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateMoveResponse {
    private boolean isBestMove;
    private String engineBestMove;
    private String evaluation;
}
