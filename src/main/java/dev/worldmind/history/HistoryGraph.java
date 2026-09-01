package dev.worldmind.history;
import java.nio.charset.StandardCharsets; import java.util.*;
public final class HistoryGraph {
 private final Map<String,HistoricalEvent> events=new LinkedHashMap<>(); private final BeliefStore beliefs=new BeliefStore();
 public HistoricalEvent record(String type,long tick,double significance,List<String> causes,List<String> actors){String basis=type+"|"+tick+"|"+causes+"|"+actors+"|"+events.size();String id=UUID.nameUUIDFromBytes(basis.getBytes(StandardCharsets.UTF_8)).toString();HistoricalEvent e=new HistoricalEvent(id,type,tick,c(significance),List.copyOf(causes),List.copyOf(actors));events.put(id,e);return e;}
 public Optional<HistoricalEvent> event(String id){return Optional.ofNullable(events.get(id));} public Collection<HistoricalEvent> events(){return List.copyOf(events.values());} public BeliefStore beliefs(){return beliefs;}
 public List<HistoricalEvent> consequencesOf(String cause){return events.values().stream().filter(e->e.causes().contains(cause)).toList();} private static double c(double v){return Math.max(0,Math.min(1,v));}
}
