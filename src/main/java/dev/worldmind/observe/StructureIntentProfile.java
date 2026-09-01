package dev.worldmind.observe;

/** Coarse semantic build intent. The schema is deliberately extensible for future deep analyzers. */
public record StructureIntentProfile(
        String purpose,
        String dominantPalette,
        double defensiveIntent,
        double unfinishedIntent,
        double expansionIntent,
        double architecturalConfidence) {
    public StructureIntentProfile {
        purpose = purpose == null ? "unknown" : purpose;
        dominantPalette = dominantPalette == null ? "mixed" : dominantPalette;
        defensiveIntent = clamp(defensiveIntent);
        unfinishedIntent = clamp(unfinishedIntent);
        expansionIntent = clamp(expansionIntent);
        architecturalConfidence = clamp(architecturalConfidence);
    }
    public static StructureIntentProfile unknown(){return new StructureIntentProfile("unknown","mixed",0,0,0,0);}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
}
