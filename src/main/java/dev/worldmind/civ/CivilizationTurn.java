package dev.worldmind.civ;
import java.util.List;
public record CivilizationTurn(List<CivilizationEvent> events,int populationDelta,double migrationPressure) {}
