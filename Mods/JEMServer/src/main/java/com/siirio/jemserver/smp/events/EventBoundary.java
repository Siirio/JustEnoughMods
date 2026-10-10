package com.siirio.jemserver.smp.events;

import java.util.UUID;

public record EventBoundary(UUID id,String dimension,String type,int minX,int minY,int minZ,int maxX,int maxY,int maxZ,int color,boolean active,boolean passable) {}
