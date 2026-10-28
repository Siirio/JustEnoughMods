package com.justenoughmods.achievementguide.integration.caves;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
public final class CaveQuestDrops {
    public static final String KEY="jemCaveDropV4";
    public static final class Run {public final List<ItemEntity> items=new ArrayList<>();public boolean overflow;}
    private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();
    private CaveQuestDrops(){}
    public static void spawned(Entity entity,boolean accepted){var run=CURRENT.get();if(run!=null && accepted && entity instanceof ItemEntity item && !item.getItem().isEmpty()){
        if(run.items.size()>=512)run.overflow=true;else run.items.add(item);
    }}
    public static boolean observe(Entity source,ServerPlayer actor,String kind,BooleanSupplier nativeCall){
        return observe(source,actor,kind,nativeCall,()->true);
    }
    public static boolean observe(Entity source,ServerPlayer actor,String kind,BooleanSupplier nativeCall,BooleanSupplier committed){
        if(actor==null)return nativeCall.getAsBoolean();
        Run previous=CURRENT.get(),run=new Run();CURRENT.set(run);boolean success;
        try{success=nativeCall.getAsBoolean();}finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
        if(success && committed.getAsBoolean() && !run.overflow)for(var item:run.items){mark(item,source,actor,kind);if(kind.equals("sulfur") && CaveQuestProof.hazmat(actor) && CaveQuestProof.item(item.getItem(),"sulfur_dust"))CaveQuestProof.emit(actor,"v3_cave_hazmat_processing_sulfur");}
        return success;
    }
    public static void mark(ItemEntity item,Entity source,ServerPlayer actor,String kind){
        if(actor==null || !(item.level() instanceof ServerLevel level) || level.getEntity(item.getUUID())!=item || item.getItem().isEmpty())return;
        CompoundTag proof=new CompoundTag();proof.putUUID("actor",actor.getUUID());proof.putUUID("source",source.getUUID());proof.putString("kind",kind);
        proof.put("item",item.getItem().save(new CompoundTag()));item.getPersistentData().put(KEY,proof);
    }
    public static CompoundTag proof(ItemEntity item){var tag=item.getPersistentData().getCompound(KEY);
        return tag.hasUUID("actor") && tag.hasUUID("source") && item.getItem().save(new CompoundTag()).equals(tag.getCompound("item"))?tag.copy():new CompoundTag();}
}
