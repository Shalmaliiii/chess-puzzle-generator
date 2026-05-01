package com.puzzlegenerator.chess.puzzle_service.controller;

import com.puzzlegenerator.chess.puzzle_service.dto.*;
import com.puzzlegenerator.chess.puzzle_service.service.StockfishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/engine")
@ConditionalOnBean(StockfishService.class)
public class EngineController {

    private final StockfishService stockfishService;

    public EngineController(StockfishService stockfishService) {
        this.stockfishService = stockfishService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) throws Exception {
        log.info("Received analysis request for FEN: {}", request.getFen());

        int depth = request.getDepth() > 0 ? request.getDepth() : 20;
        AnalysisResult result = stockfishService.analyzePosition(request.getFen(), depth);

        AnalysisResponse response = AnalysisResponse.builder()
                .bestMove(result.getBestMove())
                .evaluation(result.getEvaluation())
                .principalVariation(result.getPrincipalVariation())
                .depth(result.getDepth())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-move")
    public ResponseEntity<ValidateMoveResponse> validateMove(@RequestBody ValidateMoveRequest request) throws Exception {
        log.info("Validating move {} for FEN: {}", request.getMove(), request.getFen());

        AnalysisResult result = stockfishService.analyzePosition(request.getFen(), 20);

        boolean isBestMove = request.getMove().equals(result.getBestMove());

        ValidateMoveResponse response = ValidateMoveResponse.builder()
                .isBestMove(isBestMove)
                .engineBestMove(result.getBestMove())
                .evaluation(result.getEvaluation())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<EngineHealthResponse> health() {
        log.info("Engine health check requested");

        EngineHealthResponse response = EngineHealthResponse.builder()
                .status(stockfishService.isRunning() ? "UP" : "DOWN")
                .engineVersion(stockfishService.getEngineVersion())
                .activeWorkers(1)
                .build();

        return ResponseEntity.ok(response);
    }
}
