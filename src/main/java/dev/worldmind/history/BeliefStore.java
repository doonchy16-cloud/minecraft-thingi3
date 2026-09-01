package dev.worldmind.history;
import java.util.*;
public final class BeliefStore {
 private final Map<String,ActorBelief> beliefs=new LinkedHashMap<>();
 public ActorBelief record(String actor,String event,BeliefLayer layer,String narrative,double confidence){ActorBelief b=new ActorBelief(actor,event,layer,narrative,c(confidence),0);beliefs.put(k(actor,event,layer),b);return b;}
 public ActorBelief distort(String actor,String event,BeliefLayer layer,long seed){ActorBelief prior=findAny(actor,event).orElse(new ActorBelief(actor,event,layer,"an event occurred",.3,0)); String[] prefixes={"Legend says ","Officially, ","It is remembered that ","Some insist that "}; String n=prefixes[Math.floorMod((int)(seed^prior.narrative().hashCode()),prefixes.length)]+prior.narrative(); ActorBelief b=new ActorBelief(actor,event,layer,n,Math.max(.15,prior.confidence()*.82),prior.revision()+1);beliefs.put(k(actor,event,layer),b);return b;}
 public Optional<ActorBelief> get(String a,String e,BeliefLayer l){return Optional.ofNullable(beliefs.get(k(a,e,l)));}
 public Collection<ActorBelief> all(){return List.copyOf(beliefs.values());}
 private Optional<ActorBelief> findAny(String a,String e){return beliefs.values().stream().filter(b->b.actorId().equals(a)&&b.eventId().equals(e)).max(Comparator.comparingLong(ActorBelief::revision));}
 private static String k(String a,String e,BeliefLayer l){return a+"|"+e+"|"+l;} private static double c(double v){return Math.max(0,Math.min(1,v));}
}
