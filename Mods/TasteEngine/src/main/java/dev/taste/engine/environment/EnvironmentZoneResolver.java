package dev.taste.engine.environment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EnvironmentZoneResolver {
    public record Zone(String id,double x,double y,double z,double radius,int priority,EnvironmentSnapshot environment) { }
    private final List<Zone> zones=new ArrayList<>();
    public void replace(List<Zone> next,int maximum){zones.clear();zones.addAll(next.stream().sorted(Comparator.comparingInt(Zone::priority).reversed()).limit(maximum).toList());}
    public Zone resolve(double x,double y,double z){for(Zone zone:zones){double dx=zone.x()-x,dy=zone.y()-y,dz=zone.z()-z;if(dx*dx+dy*dy+dz*dz<=zone.radius()*zone.radius())return zone;}return null;}
}
