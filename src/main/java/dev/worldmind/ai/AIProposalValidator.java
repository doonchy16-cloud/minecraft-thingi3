package dev.worldmind.ai;

import dev.worldmind.core.EvolutionType;
import java.util.Locale;
import java.util.Optional;

public final class AIProposalValidator {
    public static final double MIN_AI_CONFIDENCE = 0.70;
    public static final double MIN_PLACE_CONFIDENCE = 0.55;
    public static final double MAX_INTENSITY_ADJUSTMENT = 0.15;

    public Optional<AIValidatedAdvice> validate(AIPlaceContext context, AITransformationProposal proposal,
            double minimumAbsenceDays) {
        if (context == null || proposal == null) return Optional.empty();
        if (!context.placeId().equals(proposal.placeId())) return Optional.empty();
        if (context.sealed()) return Optional.empty();
        if (context.placeConfidence() < MIN_PLACE_CONFIDENCE) return Optional.empty();
        if (context.absenceDays() < Math.max(0.0, minimumAbsenceDays)) return Optional.empty();
        if (!Double.isFinite(proposal.confidence()) || proposal.confidence() < MIN_AI_CONFIDENCE || proposal.confidence() > 1.0) {
            return Optional.empty();
        }
        if (!Double.isFinite(proposal.intensityAdjustment())) return Optional.empty();

        EvolutionType recommendation;
        try {
            recommendation = EvolutionType.valueOf(proposal.recommendation().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        double adjustment = Math.max(-MAX_INTENSITY_ADJUSTMENT,
                Math.min(MAX_INTENSITY_ADJUSTMENT, proposal.intensityAdjustment()));
        return Optional.of(new AIValidatedAdvice(
                context.placeId(), recommendation, proposal.confidence(), adjustment,
                sanitize(proposal.reason(), 240), sanitize(proposal.styleHint(), 120)));
    }

    private static String sanitize(String text, int max) {
        if (text == null || text.isEmpty()) return "";
        String clean = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        if (clean.length() > max) clean = clean.substring(0, max);
        return clean;
    }
}
