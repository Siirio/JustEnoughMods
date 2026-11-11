package dev.taste.engine.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnvironmentZoneStore {
    private final Map<String,EnvironmentMessages.Zone> zones=new LinkedHashMap<>();
    private long revision;
    public synchronized void replace(List<EnvironmentMessages.Zone> next,int maximum){if(next.size()>maximum)throw new IllegalArgumentException("Too many environment zones");zones.clear();for(var zone:next)zones.put(zone.id(),zone);revision++;}
    public synchronized EnvironmentMessages.Snapshot snapshot(){return new EnvironmentMessages.Snapshot(revision,List.copyOf(zones.values()));}
}
