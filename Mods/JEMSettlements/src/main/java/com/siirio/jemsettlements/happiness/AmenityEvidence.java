package com.siirio.jemsettlements.happiness;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/** One scoring owner shared by background scan and interactive evidence projection. */
public final class AmenityEvidence {
    private final java.util.Map<String,java.util.Set<String>> distinct=new java.util.LinkedHashMap<>();
    private final java.util.Map<String,Integer> caps=new java.util.HashMap<>();
    public CompoundTag observe(ServerLevel level,BlockPos pos,BlockState state) {
        CompoundTag facts=new CompoundTag();
        AmenityRegistry.get(state.getBlock()).ifPresent(value->{
            facts.putString("Category",value.category());facts.putInt("Cap",value.cap());
            boolean active=!value.requiresLight()||state.getLightEmission(level,pos)>0&&level.getBrightness(LightLayer.BLOCK,pos)>0;
            facts.putBoolean("Active",active);if(!active)return;
            var seen=distinct.computeIfAbsent(value.category(),ignored->new java.util.LinkedHashSet<>());
            String key=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock())+"#"+state.getValues();
            facts.putBoolean("Unique",seen.add(key));facts.putInt("Ordinal",seen.size());caps.merge(value.category(),value.cap(),Math::max);
        });return facts;
    }
    public void finishFacts(CompoundTag facts){if(facts.contains("Category")){int cap=caps.getOrDefault(facts.getString("Category"),facts.getInt("Cap"));facts.putInt("Cap",cap);facts.putBoolean("Counted",facts.getBoolean("Active")&&facts.getBoolean("Unique")&&facts.getInt("Ordinal")<=cap);}}
    public int points(int limit){int points=0;for(var entry:distinct.entrySet()){int n=Math.min(caps.getOrDefault(entry.getKey(),3),entry.getValue().size());if(n>0)points+=2+Math.max(0,n-1);}return Math.min(limit,points);}
    public java.util.Set<String> categories(){return java.util.Set.copyOf(distinct.keySet());}
}
