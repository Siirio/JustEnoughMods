package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.resident.ResidentMood;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.BedBlock;

/** Exact bed-centred amenity region. No invented roof, door or furniture requirements. */
public final class HousingInspection {
    private HousingInspection() {}
    public static CompoundTag capture(MinecraftServer server,ResidentMood resident) {
        CompoundTag result=new CompoundTag();result.putUUID("Resident",resident.villagerId());result.putInt("Radius",AmenityScanService.WIDTH/2);result.putInt("VerticalRadius",AmenityScanService.HEIGHT/2);
        result.putInt("VerifiedPoints",resident.homeAmenities().points());result.putBoolean("Stale",resident.homeAmenities().stale());
        ListTag examples=new ListTag();
        for(var block:java.util.List.of(net.minecraft.world.level.block.Blocks.LANTERN,net.minecraft.world.level.block.Blocks.POTTED_POPPY,net.minecraft.world.level.block.Blocks.BOOKSHELF)) {
            CompoundTag row=new CompoundTag();row.putString("Block",net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).toString());
            AmenityRegistry.get(block).ifPresent(value->{row.putString("Category",value.category());row.putInt("Cap",value.cap());row.putBoolean("Active",!value.requiresLight()||block.defaultBlockState().getLightEmission()>0);row.putBoolean("Unique",true);row.putBoolean("Counted",row.getBoolean("Active"));});examples.add(row);
        }result.put("ExampleAmenities",examples);
        if(resident.home().isEmpty()){result.putBoolean("NoHome",true);return result;}
        var home=resident.home().get();BlockPos anchor=home.pos();result.putLong("Anchor",anchor.asLong());result.putString("Dimension",home.dimension().location().toString());
        var level=server.getLevel(home.dimension());if(level==null){result.putBoolean("Unloaded",true);return result;}
        ListTag palette=new ListTag(),blocks=new ListTag();java.util.Map<net.minecraft.world.level.block.state.BlockState,Integer> ids=new java.util.LinkedHashMap<>();
        AmenityEvidence evidence=new AmenityEvidence();
        for(int y=-AmenityScanService.HEIGHT/2;y<=AmenityScanService.HEIGHT/2;y++)for(int z=-AmenityScanService.WIDTH/2;z<=AmenityScanService.WIDTH/2;z++)for(int x=-AmenityScanService.WIDTH/2;x<=AmenityScanService.WIDTH/2;x++){
            BlockPos pos=anchor.offset(x,y,z);if(!level.hasChunkAt(pos)){result.putBoolean("Unloaded",true);continue;}
            var state=level.getBlockState(pos);if(state.isAir())continue;
            int id=ids.computeIfAbsent(state,value->{palette.add(NbtUtils.writeBlockState(value));return palette.size()-1;});
            CompoundTag row=new CompoundTag();row.putLong("Position",pos.asLong());row.putInt("State",id);row.putBoolean("Bed",state.getBlock() instanceof BedBlock && pos.distSqr(anchor)<=1);
            row.merge(evidence.observe(level,pos,state));blocks.add(row);
        }
        for(int i=0;i<blocks.size();i++)evidence.finishFacts(blocks.getCompound(i));
        result.putInt("CurrentPoints",evidence.points(12));result.putBoolean("BedPresent",level.hasChunkAt(anchor)&&level.getBlockState(anchor).getBlock() instanceof BedBlock);
        result.put("Palette",palette);result.put("Blocks",blocks);result.putLong("Captured",level.getGameTime());return result;
    }
}
