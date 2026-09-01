package dev.worldmind.civ;
public record CapabilityProfile(double agriculture,double construction,double defense,double logistics,double knowledge) {
  public CapabilityProfile { agriculture=c(agriculture);construction=c(construction);defense=c(defense);logistics=c(logistics);knowledge=c(knowledge);}
  public static CapabilityProfile baseline(){return new CapabilityProfile(.25,.2,.15,.15,.2);} private static double c(double v){return Math.max(0,Math.min(1,v));}
}
