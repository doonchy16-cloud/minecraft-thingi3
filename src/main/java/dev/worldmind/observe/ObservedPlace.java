package dev.worldmind.observe;
import dev.worldmind.state.PlaceKind;
public record ObservedPlace(int x,int y,int z,int radius,double confidence,PlaceKind kind,boolean sealed,StructureIntentProfile structure) {}
