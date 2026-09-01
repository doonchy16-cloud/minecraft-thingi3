package dev.worldmind.anomaly;
public final class AnomalyScheduler {
 public boolean shouldManifest(double pressure,long seed){if(pressure<=0)return false; long x=mix(seed^Double.doubleToLongBits(pressure)); double unit=(x>>>11)*0x1.0p-53; double chance=Math.min(0.0005,Math.pow(Math.max(0,Math.min(1,pressure)),3)*0.0005); return unit<chance;}
 private static long mix(long z){z=(z^(z>>>33))*0xff51afd7ed558ccdl;z=(z^(z>>>33))*0xc4ceb9fe1a85ec53l;return z^(z>>>33);}
}
