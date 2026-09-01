package dev.worldmind.ai;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.core.EvolutionType;
import org.junit.jupiter.api.Test;

class AIProposalValidatorTest {
    private final AIProposalValidator validator = new AIProposalValidator();

    private AIPlaceContext context(boolean sealed, double placeConfidence, double absenceDays) {
        return new AIPlaceContext("place-1", "homestead", placeConfidence, "wood", 0.1, 0.2, 0.3,
                absenceDays, 0.4, 0.5, 0.4, 0.2, sealed, EvolutionType.RECLAMATION, 0.55);
    }

    @Test void acceptsAllowlistedProposalAndClampsAdjustment() {
        var p = new AITransformationProposal("place-1", "CONSTRUCTIVE", 0.91, 0.9, "reason", "style");
        var out = validator.validate(context(false, 0.8, 10), p, 5.0).orElseThrow();
        assertEquals(EvolutionType.CONSTRUCTIVE, out.recommendation());
        assertEquals(0.15, out.intensityAdjustment(), 0.0001);
    }

    @Test void rejectsMismatchedPlaceAndLowConfidence() {
        assertTrue(validator.validate(context(false, .8, 10),
                new AITransformationProposal("other", "DECAY", .9, 0, "", ""), 5).isEmpty());
        assertTrue(validator.validate(context(false, .8, 10),
                new AITransformationProposal("place-1", "DECAY", .69, 0, "", ""), 5).isEmpty());
    }

    @Test void deterministicSafetyGatesOutrankAi() {
        var p = new AITransformationProposal("place-1", "CONSTRUCTIVE", .99, .15, "", "");
        assertTrue(validator.validate(context(true, .9, 10), p, 5).isEmpty());
        assertTrue(validator.validate(context(false, .54, 10), p, 5).isEmpty());
        assertTrue(validator.validate(context(false, .9, 4.9), p, 5).isEmpty());
    }

    @Test void rejectsNonFiniteNumbersAndUnknownRecommendation() {
        assertTrue(validator.validate(context(false, .9, 10),
                new AITransformationProposal("place-1", "CAST_LAVA", .99, 0, "", ""), 5).isEmpty());
        assertTrue(validator.validate(context(false, .9, 10),
                new AITransformationProposal("place-1", "DECAY", Double.NaN, 0, "", ""), 5).isEmpty());
        assertTrue(validator.validate(context(false, .9, 10),
                new AITransformationProposal("place-1", "DECAY", .9, Double.NaN, "", ""), 5).isEmpty());
    }

    @Test void sanitizesTextLengths() {
        var p = new AITransformationProposal("place-1", "BLENDED", .9, 0,
                "r".repeat(500), "s".repeat(500));
        var out = validator.validate(context(false, .9, 10), p, 5).orElseThrow();
        assertEquals(240, out.reason().length());
        assertEquals(120, out.styleHint().length());
    }
}
