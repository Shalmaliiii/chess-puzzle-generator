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
public class PuzzleGeneratedEvent {
    private String fen;
    private List<String> solutionLine;
    private int mateIn;
    private String difficulty;
    private String sideToMove;
}
