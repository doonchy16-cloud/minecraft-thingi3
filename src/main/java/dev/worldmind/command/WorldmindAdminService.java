package dev.worldmind.command;

import dev.worldmind.config.WorldmindConfig;
import dev.worldmind.history.HistoricalEvent;
import dev.worldmind.region.RegionKey;
import dev.worldmind.region.SpatialField;
import dev.worldmind.region.SpatialSignal;
import dev.worldmind.sim.WorldSimulationResult;
import dev.worldmind.sim.WorldSimulationService;
import dev.worldmind.state.WorldmindState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Command-independent admin/testing operations so chronology tests do not depend on Brigadier. */
public final class WorldmindAdminService {
    public AdvanceResult advance(WorldmindState state, WorldmindConfig config, int days) {
        int safeDays = Math.max(1, Math.min(365, days));
        long added = safeDays * 24000L;
        int regions = 0, plans = 0, civs = 0, history = 0;
        WorldSimulationService service = new WorldSimulationService(config);
        for (int day = 0; day < safeDays; day++) {
            state.setWorldTicks(state.worldTicks() + 24000L);
            int passes = Math.max(1, Math.min(128,
                    (int)Math.ceil(state.regions().size() / (double)Math.max(1, config.globalRegionsPerCycle())) + 1));
            for (int i=0; i<passes; i++) {
                WorldSimulationResult result = service.advance(state);
                regions += result.regionsEvaluated();
                plans += result.regionalPlans();
                civs += result.civilizationsAdvanced();
                history += result.historyEvents();
                if (result.regionsEvaluated() == 0 && result.civilizationsAdvanced() == 0) break;
            }
        }
        return new AdvanceResult(added, regions, plans, civs, history);
    }

    public String status(WorldmindState state) {
        long pending = state.regionPlans().values().stream().filter(p -> p.status() == dev.worldmind.state.PlanStatus.PENDING).count();
        return "Worldmind status: day=" + format(state.worldTicks()/24000.0)
                + " knownRegions=" + state.regions().size()
                + " pendingRegionalPlans=" + pending
                + " places=" + state.places().size()
                + " civilizations=" + state.civilizations().size()
                + " historyEvents=" + state.history().events().size();
    }

    public String inspect(WorldmindState state, String dimension, int blockX, int blockZ) {
        RegionKey key = RegionKey.fromBlock(dimension, blockX, blockZ);
        var region = state.region(key).orElse(null);
        if (region == null) return "Worldmind inspect: unknown region " + key.stableId();
        var snapshot = region.snapshot(state.worldTicks());
        var field = region.spatial();
        return "Worldmind inspect " + key.stableId()
                + " physicalAgeDays=" + format(snapshot.elapsedDays())
                + " protection=" + format(region.protectionCoverage())
                + " forest=" + format(strongest(field, SpatialSignal.FOREST))
                + " vegetation=" + format(strongest(field, SpatialSignal.VEGETATION))
                + " route=" + format(strongest(field, SpatialSignal.ROUTE))
                + " farm=" + format(strongest(field, SpatialSignal.FARM))
                + " settlement=" + format(strongest(field, SpatialSignal.SETTLEMENT))
                + " reclamation=" + format(strongest(field, SpatialSignal.RECLAMATION))
                + " pending=" + state.pendingRegionPlan(key).map(p -> p.process()+"/"+p.outcome()+" target="+p.targetMutations()).orElse("none");
    }

    public String history(WorldmindState state, int limit) {
        List<HistoricalEvent> all = new ArrayList<>(state.history().events());
        if (all.isEmpty()) return "Worldmind history: no events recorded.";
        int count = Math.max(1, Math.min(20, limit));
        int start = Math.max(0, all.size() - count);
        StringBuilder out = new StringBuilder("Worldmind history (latest ").append(all.size()-start).append("):");
        for (int i=start; i<all.size(); i++) {
            HistoricalEvent e = all.get(i);
            out.append("\n").append(format(e.tick()/24000.0)).append("d ").append(e.type())
                    .append(" sig=").append(format(e.significance()));
        }
        return out.toString();
    }

    private static double strongest(SpatialField field, SpatialSignal signal) {
        int i = field.strongest(signal);
        return field.cell(field.x(i), field.z(i)).signal(signal);
    }
    private static String format(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    public record AdvanceResult(long ticksAdded, int regionsEvaluated, int regionalPlans,
                                int civilizationsAdvanced, int historyEvents) {}
}
