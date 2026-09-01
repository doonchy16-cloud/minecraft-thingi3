package dev.worldmind.region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;

public final class GlobalEvolutionScheduler {
    public List<RegionState> selectDue(Collection<RegionState> regions, long nowTick, int budget) {
        if (budget <= 0 || regions.isEmpty()) return List.of();
        List<RegionState> due = new ArrayList<>();
        for (RegionState state : regions) {
            if (nowTick - state.lastEvaluationTick() >= 2400L) due.add(state);
        }
        due.sort(Comparator.comparingDouble((RegionState r) -> r.priority(nowTick)).reversed()
                .thenComparing(r -> r.key().stableId()));
        return List.copyOf(due.subList(0, Math.min(budget, due.size())));
    }
}
