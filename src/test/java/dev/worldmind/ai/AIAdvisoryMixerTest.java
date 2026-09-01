package dev.worldmind.ai;

import static org.junit.jupiter.api.Assertions.*;
import dev.worldmind.core.EvolutionDecision;
import dev.worldmind.core.EvolutionType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AIAdvisoryMixerTest {
    private final AIAdvisoryMixer mixer = new AIAdvisoryMixer();
    private AIPlaceContext context(boolean sealed) {
        return new AIPlaceContext("p","homestead",.9,"wood",0,0,0,10,.4,.5,.4,.2,sealed,EvolutionType.RECLAMATION,.5);
    }
    private AIValidatedAdvice advice() { return new AIValidatedAdvice("p",EvolutionType.CONSTRUCTIVE,.95,.15,"reason",""); }

    @Test void safetyStasisReasonsAreImmutable() {
        for (String reason : new String[]{"worldseal","absence-below-threshold","low-place-confidence"}) {
            var d = new EvolutionDecision(EvolutionType.STASIS,0,1,reason);
            assertEquals(d, mixer.mix(context(false), d, Optional.of(advice())));
        }
    }

    @Test void sealedContextIsImmutableEvenIfRationaleIsWrong() {
        var d = new EvolutionDecision(EvolutionType.RECLAMATION,.5,1,"contextual-weighted-causality");
        assertEquals(d, mixer.mix(context(true), d, Optional.of(advice())));
    }

    @Test void eligibleDecisionCanBeBoundedlyInfluenced() {
        var d = new EvolutionDecision(EvolutionType.RECLAMATION,.90,123,"contextual-weighted-causality");
        var out = mixer.mix(context(false), d, Optional.of(advice()));
        assertEquals(EvolutionType.CONSTRUCTIVE, out.type());
        assertEquals(1.0, out.intensity(), .0001);
        assertEquals(123, out.decisionSeed());
        assertTrue(out.rationale().contains("ai-advisory"));
    }

    @Test void absentAdviceIsIdentity() {
        var d = new EvolutionDecision(EvolutionType.DECAY,.4,7,"contextual-weighted-causality");
        assertEquals(d, mixer.mix(context(false), d, Optional.empty()));
    }
}
