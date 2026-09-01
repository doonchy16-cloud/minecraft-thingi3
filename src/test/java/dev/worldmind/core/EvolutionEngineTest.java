package dev.worldmind.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvolutionEngineTest {
    private static PlaceSnapshot snapshot(double absenceDays, boolean protectedPlace, double confidence) {
        return new PlaceSnapshot(UUID.fromString("00000000-0000-0000-0000-000000000001"), absenceDays,
                protectedPlace, confidence, 0.65, 0.55, 0.50, 0.35);
    }

    @Test void underFiveDaysStaysStill() {
        assertEquals(EvolutionType.STASIS, new EvolutionEngine(5.0).decide(snapshot(4.99, false, 0.95), 42L).type());
    }

    @Test void protectedPlaceStaysStill() {
        assertEquals(EvolutionType.STASIS, new EvolutionEngine(5.0).decide(snapshot(200, true, 0.95), 42L).type());
    }

    @Test void decisionIsDeterministicForSameInputs() {
        EvolutionEngine engine = new EvolutionEngine(5.0);
        assertEquals(engine.decide(snapshot(30, false, 0.95), 991L), engine.decide(snapshot(30, false, 0.95), 991L));
    }

    @Test void longHighConfidenceAbsenceCanEvolve() {
        EvolutionEngine engine = new EvolutionEngine(5.0);
        boolean found = false;
        for (long seed = 0; seed < 128; seed++) {
            if (engine.decide(snapshot(30, false, 0.95), seed).type() != EvolutionType.STASIS) { found = true; break; }
        }
        assertTrue(found);
    }
}
