package dev.worldmind.sim;

import dev.worldmind.ai.AIAdviceSource;
import dev.worldmind.ai.AIAdvisoryMixer;
import dev.worldmind.ai.AIPlaceContextBuilder;
import dev.worldmind.ai.AIPlaceEvaluation;
import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.core.EvolutionDecision;
import dev.worldmind.core.EvolutionType;
import dev.worldmind.state.PlaceRecord;
import dev.worldmind.state.TransformationPlan;
import dev.worldmind.state.WorldmindState;

public final class PlaceSimulationService {
    private final WorldmindConfig config;
    private final AIAdviceSource aiAdvice;
    private final AIAdvisoryMixer aiMixer = new AIAdvisoryMixer();

    public PlaceSimulationService(WorldmindConfig config) {
        this(config, AIAdviceSource.none());
    }

    public PlaceSimulationService(WorldmindConfig config, AIAdviceSource aiAdvice) {
        this.config = config.validated();
        this.aiAdvice = aiAdvice == null ? AIAdviceSource.none() : aiAdvice;
    }

    public int simulateDuePlaces(WorldmindState state) {
        SimulationBudget budget = new SimulationBudget(config.simulationUnitsPerTick());
        int planned = 0;
        long now = state.worldTicks();
        long minTicks = Math.round(config.minimumAbsenceDays() * 24_000.0);
        for (PlaceRecord place : state.places().values()) {
            if (!budget.tryConsume()) break;
            if (state.hasPendingPlanForPlace(place.id())) continue;
            long absence = Math.max(0L, now - place.lastPresenceTick());
            if (absence < minTicks) continue;
            if (now - place.lastEvaluationTick() < 24_000L) continue;

            AIPlaceEvaluation evaluation = AIPlaceContextBuilder.evaluate(place, now, config.minimumAbsenceDays());
            // A non-blocking external request may be in progress. Defer only while it is actually in flight;
            // timeout/failure clears this gate and deterministic Worldmind resumes on the next cycle.
            if (aiAdvice.planningPending(place.id())) continue;

            EvolutionDecision decision = aiMixer.mix(evaluation.context(), evaluation.deterministicDecision(),
                    aiAdvice.adviceFor(place.id()));
            place.markEvaluated(now);
            if (decision.type() != EvolutionType.STASIS) {
                state.addPlan(TransformationPlan.pending(place.id(), decision.type(), decision.decisionSeed(),
                        decision.intensity(), now));
                planned++;
            }
        }
        return planned;
    }
}
