package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Companion to {@link StockfishServiceConditionalTest}. Verifies that when
 * {@code stockfish.enabled=true} the {@link StockfishService} bean is actually
 * registered and its {@code @PostConstruct} runs successfully.
 *
 * <p>Because enabling the bean causes it to spawn the real Stockfish process,
 * this test is gated on Stockfish actually being available on the executing
 * machine. When Stockfish is not installed (for example, on CI runners that
 * have not provisioned the binary), the test is skipped rather than failed —
 * matching the guarantee that tests which require Stockfish only run when
 * Stockfish is enabled and available.
 */
@SpringBootTest(properties = {
        "stockfish.enabled=true",
        "stockfish.path=stockfish"
})
@EnabledIf("isStockfishAvailable")
class StockfishServiceEnabledTest {

    @Autowired(required = false)
    private StockfishService stockfishService;

    @Test
    void stockfishServiceBeanIsRegisteredWhenEnabled() {
        assertThat(stockfishService)
                .as("StockfishService bean should be registered when stockfish.enabled=true")
                .isNotNull();
    }

    /**
     * Probes whether a Stockfish binary is available on the system so that this
     * test class is only executed when the engine can actually be launched.
     * Any failure to start the process (binary missing, not executable, etc.)
     * disables the test.
     */
    @SuppressWarnings("unused")
    static boolean isStockfishAvailable() {
        Process process = null;
        try {
            process = new ProcessBuilder("stockfish")
                    .redirectErrorStream(true)
                    .start();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }
}
