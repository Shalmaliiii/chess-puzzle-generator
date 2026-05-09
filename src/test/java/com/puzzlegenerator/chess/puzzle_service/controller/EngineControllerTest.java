package com.puzzlegenerator.chess.puzzle_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisRequest;
import com.puzzlegenerator.chess.puzzle_service.dto.AnalysisResult;
import com.puzzlegenerator.chess.puzzle_service.dto.ValidateMoveRequest;
import com.puzzlegenerator.chess.puzzle_service.service.StockfishService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EngineController.class)
class EngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockfishService stockfishService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/engine/analyze")
    class Analyze {

        @Test
        @DisplayName("returns analysis result for valid request")
        void validAnalysisRequest() throws Exception {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("+0.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(20)
                    .build();

            when(stockfishService.analyzePosition(anyString(), anyInt()))
                    .thenReturn(result);

            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                    .depth(20)
                    .build();

            mockMvc.perform(post("/api/engine/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bestMove").value("e2e4"))
                    .andExpect(jsonPath("$.evaluation").value("+0.5"))
                    .andExpect(jsonPath("$.depth").value(20))
                    .andExpect(jsonPath("$.principalVariation").isArray())
                    .andExpect(jsonPath("$.principalVariation[0]").value("e2e4"));
        }

        @Test
        @DisplayName("uses default depth 20 when depth is 0")
        void defaultDepthWhenZero() throws Exception {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("d2d4")
                    .evaluation("+0.3")
                    .principalVariation(List.of("d2d4"))
                    .depth(20)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(20)))
                    .thenReturn(result);

            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                    .depth(0)
                    .build();

            mockMvc.perform(post("/api/engine/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bestMove").value("d2d4"));
        }

        @Test
        @DisplayName("returns mate evaluation in analysis response")
        void mateEvaluation() throws Exception {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("h5f7")
                    .evaluation("M1")
                    .principalVariation(List.of("h5f7"))
                    .depth(15)
                    .build();

            when(stockfishService.analyzePosition(anyString(), anyInt()))
                    .thenReturn(result);

            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
                    .depth(15)
                    .build();

            mockMvc.perform(post("/api/engine/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluation").value("M1"))
                    .andExpect(jsonPath("$.bestMove").value("h5f7"));
        }

        @Test
        @DisplayName("returns 500 when Stockfish throws exception")
        void stockfishException() throws Exception {
            when(stockfishService.analyzePosition(anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Engine crashed"));

            AnalysisRequest request = AnalysisRequest.builder()
                    .fen("invalid-fen")
                    .depth(10)
                    .build();

            mockMvc.perform(post("/api/engine/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/engine/validate-move")
    class ValidateMove {

        @Test
        @DisplayName("returns true when move matches best move")
        void moveMatchesBest() throws Exception {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("+0.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(20)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(20)))
                    .thenReturn(result);

            ValidateMoveRequest request = ValidateMoveRequest.builder()
                    .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                    .move("e2e4")
                    .build();

            mockMvc.perform(post("/api/engine/validate-move")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bestMove").value(true))
                    .andExpect(jsonPath("$.engineBestMove").value("e2e4"))
                    .andExpect(jsonPath("$.evaluation").value("+0.5"));
        }

        @Test
        @DisplayName("returns false when move does not match best move")
        void moveDoesNotMatch() throws Exception {
            AnalysisResult result = AnalysisResult.builder()
                    .bestMove("e2e4")
                    .evaluation("+0.5")
                    .principalVariation(List.of("e2e4", "e7e5"))
                    .depth(20)
                    .build();

            when(stockfishService.analyzePosition(anyString(), eq(20)))
                    .thenReturn(result);

            ValidateMoveRequest request = ValidateMoveRequest.builder()
                    .fen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                    .move("a2a3")
                    .build();

            mockMvc.perform(post("/api/engine/validate-move")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bestMove").value(false))
                    .andExpect(jsonPath("$.engineBestMove").value("e2e4"));
        }
    }

    @Nested
    @DisplayName("GET /api/engine/health")
    class Health {

        @Test
        @DisplayName("returns UP when engine is running")
        void engineRunning() throws Exception {
            when(stockfishService.isRunning()).thenReturn(true);
            when(stockfishService.getEngineVersion()).thenReturn("Stockfish 16.1");

            mockMvc.perform(get("/api/engine/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.engineVersion").value("Stockfish 16.1"))
                    .andExpect(jsonPath("$.activeWorkers").value(1));
        }

        @Test
        @DisplayName("returns DOWN when engine is not running")
        void engineNotRunning() throws Exception {
            when(stockfishService.isRunning()).thenReturn(false);
            when(stockfishService.getEngineVersion()).thenReturn("unknown");

            mockMvc.perform(get("/api/engine/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.engineVersion").value("unknown"));
        }
    }
}
