package dev.worldmind.sim;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.ai.*;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.core.EvolutionType;
import dev.worldmind.state.PlaceKind;
import dev.worldmind.state.PlaceRecord;
import dev.worldmind.state.WorldmindState;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceSimulationAIAdviceTest {
    private static PlaceRecord place() {
        return PlaceRecord.create("minecraft:overworld", 0, 64, 0, 10, new UUID(1,2), .95, PlaceKind.HOMESTEAD, 0);
    }

    @Test void cachedAdviceInfluencesEligibleDecision() {
        WorldmindConfig cfg = WorldmindConfig.defaults();
        WorldmindState state = new WorldmindState();
        state.setWorldTicks(10 * 24000L);
        PlaceRecord p = place(); state.upsertPlace(p);
        AIPlaceEvaluation eval = AIPlaceContextBuilder.evaluate(p, state.worldTicks(), cfg.minimumAbsenceDays());
        EvolutionType desired = eval.deterministicDecision().type() == EvolutionType.DECAY ? EvolutionType.CONSTRUCTIVE : EvolutionType.DECAY;
        AIValidatedAdvice advice = new AIValidatedAdvice(p.id(), desired, .95, .10, "bounded", "");
        AIAdviceSource source = id -> Optional.of(advice);
        int planned = new PlaceSimulationService(cfg, source).simulateDuePlaces(state);
        assertEquals(1, planned);
        assertEquals(desired, state.plans().values().iterator().next().type());
    }

    @Test void noAdviceMatchesDeterministicEvaluation() {
        WorldmindConfig cfg = WorldmindConfig.defaults();
        WorldmindState state = new WorldmindState();
        state.setWorldTicks(10 * 24000L);
        PlaceRecord p = place(); state.upsertPlace(p);
        AIPlaceEvaluation eval = AIPlaceContextBuilder.evaluate(p, state.worldTicks(), cfg.minimumAbsenceDays());
        int planned = new PlaceSimulationService(cfg, AIAdviceSource.none()).simulateDuePlaces(state);
        if (eval.deterministicDecision().type() == EvolutionType.STASIS) assertEquals(0, planned);
        else {
            assertEquals(1, planned);
            assertEquals(eval.deterministicDecision().type(), state.plans().values().iterator().next().type());
        }
    }

    @Test void inflightAdviceDefersWithoutConsumingEvaluationClock() {
        WorldmindConfig cfg = WorldmindConfig.defaults();
        WorldmindState state = new WorldmindState();
        state.setWorldTicks(10 * 24000L);
        PlaceRecord p = place(); state.upsertPlace(p);
        long before = p.lastEvaluationTick();
        AIAdviceSource waiting = new AIAdviceSource() {
            public Optional<AIValidatedAdvice> adviceFor(String id) { return Optional.empty(); }
            public boolean planningPending(String id) { return true; }
        };
        assertEquals(0, new PlaceSimulationService(cfg, waiting).simulateDuePlaces(state));
        assertEquals(before, p.lastEvaluationTick());
        assertTrue(state.plans().isEmpty());
    }
}
