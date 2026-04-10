package com.puzzlegenerator.chess.puzzle_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chess")
public class ChessController {
    private static final Logger logger = LoggerFactory.getLogger(ChessController.class);

    @GetMapping("/ping")
    public String ping() {
        logger.info("/ping : Chess Service Live");
        return "Chess Service Live";
    }
}
