package com.puzzlegenerator.chess.puzzle_service.generator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class FenGenerator {

    private static final Random RANDOM = new Random();

    private static final List<String> MATE_POSITIONS = Arrays.asList(
            // Mate-in-1 positions
            "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            "rnbqkbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2",
            "r1bqk2r/pppp1Qpp/2n2n2/2b1p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4",
            "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
            "6k1/5ppp/8/8/8/4B3/5PPP/6K1 w - - 0 1",
            "r1b1k2r/ppppqppp/2n2n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 1",
            "k7/8/1K6/8/8/8/8/7R w - - 0 1",
            "k7/8/2K5/8/8/8/8/1R6 w - - 0 1",
            "7k/5K2/6R1/8/8/8/8/8 w - - 0 1",
            "6k1/4Rppp/8/8/8/8/5PPP/6K1 w - - 0 1",

            // Mate-in-2 positions
            "2bqkbn1/2pppp2/np2N3/r3P1p1/p2N2B1/5Q2/PPPPKPP1/RNB2r2 w - - 0 1",
            "r1bq2r1/b4pk1/p1pp1p2/1p2pP2/1P2P1PB/3P4/1PPQ2P1/R3K2R w KQ - 0 1",
            "3r1rk1/1pp1b1pp/p7/4Np2/2Pp4/3P2Q1/PP3RPP/5RK1 w - - 0 1",
            "r3k2r/ppp2Npp/1b5n/4p2b/2B1P2q/BQP2P2/P5PP/RN5K w kq - 0 1",
            "r2qk2r/pb4pp/1n2Pb2/2B2Q2/p1p5/2P5/PPP2PPP/RN2R1K1 w kq - 0 1",
            "1rb4r/pkPp3p/1b1P3n/1Q6/N3Pp2/8/P1P3PP/7K w - - 0 1",
            "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 0 1",
            "r1b1kb1r/pppp1ppp/5q2/4n3/3KP3/2N3PN/PPP4P/R1BQ1B1R b kq - 0 1",
            "r1bqr3/ppp1B1kp/1b4p1/n2B4/3NQ3/2P5/PP3PPP/R3K2R w KQ - 0 1",
            "r4rk1/pppb3p/2n1Bppn/8/2BP2q1/1QN1P3/PP3PP1/R3K2R w KQ - 0 1",

            // Mate-in-3 positions
            "r2qr1k1/ppp2ppp/3b1n2/3pN1N1/2PP4/2PB4/P4PPP/R2Q1RK1 w - - 0 1",
            "r5rk/5p1p/5R2/4B3/8/8/7P/7K w - - 0 1",
            "2r3k1/p4p2/3Rp2p/1p2P1pK/8/1P4P1/P3Q2P/1q6 w - - 0 1",
            "rn1qkb1r/pp2pppp/5n2/3p1b2/3P4/2N1P3/PP3PPP/R1BQKBNR w KQkq - 0 1",
            "2kr1b1r/ppp1pppp/2n2n2/8/Q2Pq3/4BN2/PP2PPPP/R3KB1R w KQ - 0 1",
            "rn3rk1/pbppq1pp/1p2pb2/4N2Q/3PN3/3B4/PPP2PPP/R3K2R w KQ - 0 1",
            "r2q1rk1/ppp2ppp/2np4/2b1p1B1/2B1P1n1/3P1N2/PPP2PPP/RN1QR1K1 w - - 0 1",
            "r1bq1rk1/ppp2ppp/2n1pn2/3p2B1/1bPP4/2NBPN2/PP3PPP/R2QK2R w KQ - 0 1",
            "5rk1/1p1q2pp/p2p4/2pP2QN/8/1P4P1/P4PKP/4r3 w - - 0 1",
            "6rk/5Npp/8/3Q4/8/8/8/7K w - - 0 1",

            // Mate-in-4+ positions
            "r1bk3r/ppppq2p/5Qp1/4P3/3B4/8/PPP2PPP/R3K2R w KQ - 0 1",
            "2rr3k/pp3pp1/1nnqbN1p/3pN3/2pP4/2P3Q1/PPB4P/R4RK1 w - - 0 1",
            "r1b1k1nr/p2p1ppp/n2B4/1p1NPN1P/6P1/3P1Q2/P1P1K3/q5b1 w kq - 0 1",
            "r3r1k1/pp1q1pp1/2p2n1p/3p1B2/3P1bN1/2P5/PPQ2PPP/R3R1K1 w - - 0 1",
            "r2qk2r/ppp1bppp/2n1b3/3np3/8/1P1P1NP1/PBP1QPBP/RN2R1K1 w kq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "5rk1/pp4pp/4p3/2R3Q1/3n4/2q5/P4PPP/5RK1 w - - 0 1",
            "2kr3r/p1ppqpb1/bn2Qnp1/3PN3/1p2P3/2N5/PPPBBPPP/R3K2R w KQ - 0 1",
            "1k1r4/pp1b1R2/3q2pp/4p3/2B5/4Q3/PPP2B2/2K5 w - - 0 1",
            "r3k2r/1ppn3p/2q1q1n1/8/2q1Pp2/6R1/p1p2QPP/1R4K1 b kq - 0 1"
        );

    private static final List<String> TACTICAL_SEEDS = Arrays.asList(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r1bqkbnr/pppppppp/2n5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2"
    );

    public String getRandomMatePosition() {
        int idx = RANDOM.nextInt(MATE_POSITIONS.size());
        log.debug("Selected mate position at index {}", idx);
        return MATE_POSITIONS.get(idx);
    }

    public String getRandomTacticalPosition() {
        int idx = RANDOM.nextInt(TACTICAL_SEEDS.size());
        log.debug("Selected tactical seed at index {}", idx);
        return TACTICAL_SEEDS.get(idx);
    }

    public List<String> getAllMatePositions() {
        return MATE_POSITIONS;
    }
}
