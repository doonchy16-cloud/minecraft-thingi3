package dev.worldmind.civ;
import java.util.*;
public final class CivilizationEngine {
  public CivilizationTurn advance(CivilizationState c,double days,long seed){
    SplittableRandom r=new SplittableRandom(seed^c.id().hashCode()); List<CivilizationEvent> events=new ArrayList<>();
    double scarcity=c.resources().scarcity(); int delta=0;
    if(scarcity>.55){c.setMigrationPressure(Math.min(1,c.migrationPressure()+.25)); delta=-(int)Math.max(1,Math.round(c.population()*.02*Math.min(days/10.0,1))); events.add(new CivilizationEvent("scarcity","Food pressure drives migration",.55+scarcity*.35));}
    else if(days>=10 && r.nextDouble()<.55){delta=Math.max(1,(int)Math.round(c.population()*.01)); events.add(new CivilizationEvent("growth","Population and settlement capacity expand",.35));}
    if(c.threat()>.55){events.add(new CivilizationEvent("fortification","Threat pressure drives defensive investment",.45+c.threat()*.3));}
    if(c.cohesion()<.35 && c.population()>40){events.add(new CivilizationEvent("fracture","Low cohesion creates a breakaway movement",.75));}
    if(c.migrationPressure()>.65){events.add(new CivilizationEvent("migration","Population begins establishing a successor settlement",.65));}
    c.adjustPopulation(delta); return new CivilizationTurn(List.copyOf(events),delta,c.migrationPressure());
  }
}
