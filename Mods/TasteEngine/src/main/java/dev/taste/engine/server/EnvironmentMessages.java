package dev.taste.engine.server;

import java.util.List;

public final class EnvironmentMessages {
    private EnvironmentMessages() { }
    public record Hello(int protocol,boolean supportsZones) { }
    public record Zone(String id,double x,double y,double z,double radius,int priority,float fog,float red,float green,float blue) { }
    public record Snapshot(long revision,List<Zone> zones){public Snapshot{zones=List.copyOf(zones);}}
}
