package dev.worldmind.civ;
public record CultureProfile(double militarism,double trade,double tradition,double innovation) {
  public CultureProfile { militarism=clamp(militarism); trade=clamp(trade); tradition=clamp(tradition); innovation=clamp(innovation); }
  public static CultureProfile baseline(){return new CultureProfile(.3,.5,.5,.4);} private static double clamp(double v){return Math.max(0,Math.min(1,v));}
}
