package com.puzzlegenerator.chess.puzzle_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChessController.class)
class ChessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ping_returnsLiveMessage() throws Exception {
        mockMvc.perform(get("/chess/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Chess Service Live"));
    }

    @Test
    void ping_unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/chess/unknown"))
                .andExpect(status().isNotFound());
    }
}
