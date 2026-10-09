package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.*;

public final class BossStaging {
    private static final Map<UUID,BoundingBox> BOUNDS=new HashMap<>();
    public static BoundingBox bounds(LivingEntity boss) { return BOUNDS.computeIfAbsent(boss.getUUID(),id->discover(boss)); }
    private static BoundingBox discover(LivingEntity boss) {
        var level=(ServerLevel)boss.level();var pos=boss.blockPosition();
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk!=null) {
            var starts=new ArrayList<net.minecraft.world.level.levelgen.structure.StructureStart>(chunk.getAllStarts().values());
            for(var reference:chunk.getAllReferences().entrySet()) for(long value:reference.getValue()) {
                var cp=new ChunkPos(value);var origin=level.getChunkSource().getChunkNow(cp.x,cp.z);
                if(origin!=null) { var start=origin.getStartForStructure(reference.getKey());if(start!=null) starts.add(start); }
            }
            var box=starts.stream().filter(s->s.isValid() && s.getBoundingBox().isInside(pos)).map(s->s.getBoundingBox()).min(Comparator.comparingLong(b->(long)b.getXSpan()*b.getZSpan()));
            if(box.isPresent()) return box.get();
        }
        int radius=EventRules.STAGING_RADIUS.get();
        return new BoundingBox(pos.getX()-radius,pos.getY()-radius,pos.getZ()-radius,pos.getX()+radius,pos.getY()+radius,pos.getZ()+radius);
    }
    public static boolean contains(LivingEntity boss,net.minecraft.server.level.ServerPlayer player) { return boss.level()==player.level() && bounds(boss).isInside(player.blockPosition()); }
    public static void remove(UUID id) { BOUNDS.remove(id); }
    public static void clear() { BOUNDS.clear(); }
    private BossStaging() {}
}
