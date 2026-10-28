package com.justenoughmods.achievementguide.integration.caves;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Physical native slot provenance, separate from native advancement progress. */
public final class CaveQuestStation {
    private static final String ROOT="jemCaveStationV4";
    public record Before(ItemStack item,CompoundTag input,CompoundTag output){}
    public record Row(BlockEntity owner,int slot,Before before,int held){}
    public record Click(AbstractContainerMenu menu,ServerPlayer actor,List<Row> rows){}
    private CaveQuestStation(){}
    public static Before before(BlockEntity owner,int slot){var root=owner.getPersistentData().getCompound(ROOT);return new Before(((Container)owner).getItem(slot).copy(),root.getCompound("in"+slot).copy(),root.getCompound("out"+slot).copy());}
    private static boolean matches(CompoundTag proof,ItemStack item){return proof.hasUUID("actor") && !item.isEmpty() && item.save(new CompoundTag()).equals(proof.getCompound("item"));}
    public static ServerPlayer actor(BlockEntity owner,Before before){return owner.getLevel() instanceof net.minecraft.server.level.ServerLevel level && matches(before.input(),before.item())?CaveQuestProof.actor(level,before.input().getUUID("actor")):null;}
    public static void consumed(BlockEntity owner,int slot,Before before){
        var now=((Container)owner).getItem(slot);var root=owner.getPersistentData().getCompound(ROOT);
        if(!now.isEmpty() && matches(before.input(),before.item()) && ItemStack.isSameItemSameTags(now,before.item()) && now.getCount()<before.item().getCount()){
            var proof=before.input().copy();proof.put("item",now.save(new CompoundTag()));root.put("in"+slot,proof);
        }else root.remove("in"+slot);owner.getPersistentData().put(ROOT,root);owner.setChanged();
    }
    public static void output(BlockEntity owner,int slot,Before before,ServerPlayer actor,String action){
        var now=((Container)owner).getItem(slot);
        if(actor==null || now.isEmpty() || !before.item().isEmpty() && (!matches(before.output(),before.item()) || !before.output().getUUID("actor").equals(actor.getUUID()) || !before.output().getString("action").equals(action))
                || !before.item().isEmpty() && !ItemStack.isSameItemSameTags(before.item(),now) || now.getCount()<=before.item().getCount())return;
        var root=owner.getPersistentData().getCompound(ROOT);var proof=new CompoundTag();proof.putUUID("actor",actor.getUUID());proof.putString("action",action);proof.put("item",now.save(new CompoundTag()));
        long generation=root.getLong("generation"+slot)+1;proof.putLong("generation",generation);root.putLong("generation"+slot,generation);root.put("out"+slot,proof);owner.getPersistentData().put(ROOT,root);owner.setChanged();
    }
    public static void transformed(BlockEntity owner,int slot,Before before,ServerPlayer actor,String action){
        if(actor==null || before.item().isEmpty())return;var now=((Container)owner).getItem(slot);
        if(now.isEmpty() || now.getCount()!=before.item().getCount() || ItemStack.isSameItemSameTags(now,before.item()))return;
        output(owner,slot,new Before(ItemStack.EMPTY,new CompoundTag(),new CompoundTag()),actor,action);
    }
    public static void invalidated(BlockEntity owner,int slot){var data=owner.getPersistentData();if(!data.contains(ROOT))return;var root=data.getCompound(ROOT);if(root.contains("in"+slot) || root.contains("out"+slot)){root.remove("in"+slot);root.remove("out"+slot);owner.setChanged();}}
    private static int held(ServerPlayer actor,AbstractContainerMenu menu,ItemStack item){return CaveQuestProof.count(actor.getInventory(),item)+(ItemStack.isSameItemSameTags(menu.getCarried(),item)?menu.getCarried().getCount():0);}
    public static Click capture(AbstractContainerMenu menu,ServerPlayer actor){
        if(CaveQuestProof.actor(actor)==null || actor.containerMenu!=menu || !menu.stillValid(actor) || menu.slots.size()>256)return null;
        List<Row> rows=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(var slot:menu.slots){if(!(slot.container instanceof BlockEntity owner) || !(owner instanceof Container) || slot.getContainerSlot()>4 || slot.getContainerSlot()<0)continue;
            if(!(owner instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity || owner instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity || owner instanceof com.github.alexmodguy.alexscaves.server.block.blockentity.NuclearFurnaceBlockEntity))continue;
            int index=slot.getContainerSlot();String key=owner.getBlockPos().asLong()+"/"+index;if(!seen.add(key))continue;
            var before=before(owner,index);rows.add(new Row(owner,index,before,held(actor,menu,before.item())));
        }return new Click(menu,actor,List.copyOf(rows));
    }
    public static void finish(Click click){
        if(click==null || CaveQuestProof.actor(click.actor())==null || click.actor().containerMenu!=click.menu() || !click.menu().stillValid(click.actor()))return;
        for(var row:click.rows()){
            var now=((Container)row.owner()).getItem(row.slot());var before=row.before();var root=row.owner().getPersistentData().getCompound(ROOT);int taken=before.item().getCount()-now.getCount();
            if(matches(before.output(),before.item()) && before.output().getUUID("actor").equals(click.actor().getUUID()) && taken>0
                    && (now.isEmpty() || ItemStack.isSameItemSameTags(now,before.item())) && held(click.actor(),click.menu(),before.item())-row.held()==taken
                    && root.getLong("generation"+row.slot())==before.output().getLong("generation")){
                CaveQuestProof.emit(click.actor(),before.output().getString("action"));
                if(!now.isEmpty()){var output=before.output().copy();output.put("item",now.save(new CompoundTag()));root.put("out"+row.slot(),output);}else root.remove("out"+row.slot());row.owner().setChanged();
            }
            if(now.isEmpty() || !before.item().isEmpty() && !ItemStack.isSameItemSameTags(now,before.item()))continue;
            int added=now.getCount()-before.item().getCount();if(added<=0)continue;
            // Compare actual player debit for this exact new item, using the before snapshot of all player slots captured by the click owner.
            if(!before.item().isEmpty() && (!matches(before.input(),before.item()) || !before.input().getUUID("actor").equals(click.actor().getUUID())))continue;
            var debit=CaveQuestMenuDebit.before(click.actor(),now);
            if(debit<0 || debit-held(click.actor(),click.menu(),now)!=added)continue;
            var input=new CompoundTag();input.putUUID("actor",click.actor().getUUID());input.put("item",now.save(new CompoundTag()));root.put("in"+row.slot(),input);row.owner().getPersistentData().put(ROOT,root);row.owner().setChanged();
        }
    }
}
