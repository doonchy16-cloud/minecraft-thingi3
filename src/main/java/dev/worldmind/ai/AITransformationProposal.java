package dev.worldmind.ai;

/** Raw untrusted response from an external intelligence provider. */
public record AITransformationProposal(
        String placeId,
        String recommendation,
        double confidence,
        double intensityAdjustment,
        String reason,
        String styleHint) {
    public AITransformationProposal {
        placeId = placeId == null ? "" : placeId;
        recommendation = recommendation == null ? "" : recommendation;
        reason = reason == null ? "" : reason;
        styleHint = styleHint == null ? "" : styleHint;
    }
}
