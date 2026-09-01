package dev.worldmind.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChronologyPolicyTest {
    @Test void pausedNeverAdvances() {
        assertEquals(0, new ChronologyPolicy().compute(ChronologyMode.PAUSED, 86_400_000L).abstractTicks());
    }

    @Test void livingAdvancesMoreThanCappedForLongAbsence() {
        ChronologyPolicy p = new ChronologyPolicy();
        long living = p.compute(ChronologyMode.LIVING, 14L * 86_400_000L).abstractTicks();
        long capped = p.compute(ChronologyMode.CAPPED_LIVING, 14L * 86_400_000L).abstractTicks();
        assertTrue(living > capped);
        assertTrue(capped > 0);
    }

    @Test void cappedModeCompressesAndCaps() {
        ChronologyPolicy p = new ChronologyPolicy();
        long week = p.compute(ChronologyMode.CAPPED_LIVING, 7L * 86_400_000L).abstractTicks();
        long year = p.compute(ChronologyMode.CAPPED_LIVING, 365L * 86_400_000L).abstractTicks();
        assertTrue(year >= week);
        assertTrue(year <= ChronologyPolicy.MAX_CATCH_UP_TICKS);
    }
}
