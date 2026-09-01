package dev.worldmind.civ;
import java.util.*;
public final class CivilizationState {
  private final String id,name,dimension; private int population; private long foundedTick; private long lastUpdatedTick; private ResourceLedger resources=new ResourceLedger();
  private CultureProfile culture=CultureProfile.baseline(); private CapabilityProfile capabilities=CapabilityProfile.baseline();
  private double threat, cohesion=.8, migrationPressure; private final Set<String> settlements=new LinkedHashSet<>();
  private CivilizationState(String id,String name,String dimension,int population,long tick){this.id=id;this.name=name;this.dimension=dimension;this.population=Math.max(1,population);this.foundedTick=tick;this.lastUpdatedTick=tick;}
  public static CivilizationState found(String name,String dimension,int population,long tick){return new CivilizationState(UUID.nameUUIDFromBytes((name+dimension+tick).getBytes()).toString(),name,dimension,population,tick);}
  public String id(){return id;} public String name(){return name;} public String dimension(){return dimension;} public int population(){return population;} public ResourceLedger resources(){return resources;} public long lastUpdatedTick(){return lastUpdatedTick;} public void markUpdated(long tick){lastUpdatedTick=Math.max(lastUpdatedTick,tick);}
  public CultureProfile culture(){return culture;} public CapabilityProfile capabilities(){return capabilities;} public double threat(){return threat;} public double cohesion(){return cohesion;} public double migrationPressure(){return migrationPressure;}
  public void setThreat(double v){threat=c(v);} public void setCohesion(double v){cohesion=c(v);} public void setMigrationPressure(double v){migrationPressure=c(v);} public void adjustPopulation(int delta){population=Math.max(1,population+delta);} public Set<String> settlements(){return Set.copyOf(settlements);} public void addSettlement(String id){settlements.add(id);}
  public void evolveCulture(CultureProfile p){culture=p;} public void evolveCapabilities(CapabilityProfile p){capabilities=p;} private static double c(double v){return Math.max(0,Math.min(1,v));}
}
