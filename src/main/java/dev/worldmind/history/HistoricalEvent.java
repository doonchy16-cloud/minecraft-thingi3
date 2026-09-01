package dev.worldmind.history;
import java.util.List;
public record HistoricalEvent(String id,String type,long tick,double significance,List<String> causes,List<String> actors) {}
