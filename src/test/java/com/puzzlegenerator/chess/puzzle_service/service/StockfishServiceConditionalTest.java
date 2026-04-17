package com.puzzlegenerator.chess.puzzle_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the bug fixed in commit fb7430c ("Fixed Test Case").
 *
 * <p>Before the fix, {@link StockfishService} was registered as an unconditional
 * Spring bean whose {@code @PostConstruct startEngine()} method eagerly spawned
 * the Stockfish binary at context startup. When the binary was unavailable
 * (e.g. in CI or local unit tests), {@code ProcessBuilder.start()} threw an
 * {@code IOException} and the Spring application context failed to load,
 * causing {@code PuzzleServiceApplicationTests.contextLoads()} to fail.
 *
 * <p>The fix adds
 * {@code @ConditionalOnProperty(name = "stockfish.enabled", havingValue = "true",
 * matchIfMissing = true)} to {@link StockfishService} and sets
 * {@code stockfish.enabled=false} on the Spring Boot test, so the bean is not
 * instantiated during tests that do not have Stockfish available.
 *
 * <p>This test guards against the regression by asserting that when
 * {@code stockfish.enabled=false} the Spring context loads without a
 * {@link StockfishService} bean. If the {@code @ConditionalOnProperty}
 * annotation is removed, the context would instead attempt to start the
 * Stockfish process and fail to load (reproducing the original bug), which
 * would cause this test to fail.
 */
@SpringBootTest(properties = "stockfish.enabled=false")
class StockfishServiceConditionalTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsWithoutStockfishServiceWhenDisabled() {
        assertThat(applicationContext.getBeansOfType(StockfishService.class))
                .as("StockfishService must not be instantiated when stockfish.enabled=false; "
                        + "otherwise the @PostConstruct would attempt to spawn the Stockfish binary "
                        + "and fail the context load (regression of commit fb7430c).")
                .isEmpty();
    }
}
