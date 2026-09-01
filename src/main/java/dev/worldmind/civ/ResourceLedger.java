package dev.worldmind.civ;
public final class ResourceLedger {
  private double food=100, materials=100, wealth=50;
  public double food(){return food;} public double materials(){return materials;} public double wealth(){return wealth;}
  public void addFood(double v){food=Math.max(0,food+v);} public void addMaterials(double v){materials=Math.max(0,materials+v);} public void addWealth(double v){wealth=Math.max(0,wealth+v);}
  public double scarcity(){return Math.max(0, Math.min(1, 1.0 - food/100.0));}
}
