package dev.worldmind.history;
public record ActorBelief(String actorId,String eventId,BeliefLayer layer,String narrative,double confidence,long revision) {}
