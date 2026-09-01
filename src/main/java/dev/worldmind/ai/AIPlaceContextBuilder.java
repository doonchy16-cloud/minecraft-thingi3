package dev.worldmind.ai;

import dev.worldmind.core.EvolutionDecision;
import dev.worldmind.core.EvolutionEngine;
import dev.worldmind.core.PlaceSnapshot;
import dev.worldmind.observe.StructureIntentProfile;
import dev.worldmind.state.PlaceKind;
import dev.worldmind.state.PlaceRecord;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Single source of truth for the deterministic semantic evidence also exposed to AI. */
public final class AIPlaceContextBuilder {
    private AIPlaceContextBuilder() {}

    public static AIPlaceEvaluation evaluate(PlaceRecord place, long nowTick, double minimumAbsenceDays) {
        if (place == null) throw new IllegalArgumentException("place");
        long absence = Math.max(0L, nowTick - place.lastPresenceTick());
        double absenceDays = absence / 24_000.0;
        UUID placeUuid = stableUuid(place.id());
        double nature = unit(place.id(), 11);
        double settlement = place.kind() == PlaceKind.HOMESTEAD ? 0.68 : 0.42;
        double fragility = 0.42 + unit(place.id(), 29) * 0.35;
        double threat = unit(place.id(), 47) * 0.65;
        PlaceSnapshot snapshot = new PlaceSnapshot(placeUuid, absenceDays, place.sealed(), place.confidence(),
                nature, settlement, fragility, threat);
        long seed = mix(nowTick ^ placeUuid.getMostSignificantBits() ^ placeUuid.getLeastSignificantBits());
        EvolutionDecision deterministic = new EvolutionEngine(minimumAbsenceDays).decide(snapshot, seed);
        StructureIntentProfile structure = place.structureProfile();
        AIPlaceContext context = new AIPlaceContext(
                place.id(), place.kind().name().toLowerCase(java.util.Locale.ROOT), place.confidence(),
                structure.dominantPalette(), structure.defensiveIntent(), structure.unfinishedIntent(),
                structure.expansionIntent(), absenceDays, nature, settlement, fragility, threat, place.sealed(),
                deterministic.type(), deterministic.intensity());
        return new AIPlaceEvaluation(context, deterministic);
    }

    private static UUID stableUuid(String text) { return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)); }
    private static double unit(String text, int salt) { return (mix(text.hashCode() * 31L + salt) >>> 11) * 0x1.0p-53; }
    private static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdl;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53l;
        return z ^ (z >>> 33);
    }
}
