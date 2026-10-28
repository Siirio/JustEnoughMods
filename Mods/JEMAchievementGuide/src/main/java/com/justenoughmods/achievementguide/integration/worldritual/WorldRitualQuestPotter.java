package com.justenoughmods.achievementguide.integration.worldritual;
import java.util.ArrayDeque;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.mcreator.luminousnether.entity.PottergeistEntity;
import net.mcreator.luminousnether.world.inventory.PottergeistGUIMenu;
public final class WorldRitualQuestPotter {
    public interface MenuAccess { Entity jem$ritualBoundEntity(); }
    private record Attachment(AbstractContainerMenu menu,ServerPlayer actor,PottergeistEntity pet,ItemStack old){}
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Attachment> frames=new ArrayDeque<>();int overflow;}
    private WorldRitualQuestPotter(){}
    public static void tamed(PottergeistEntity pet,Player actor,boolean before){if(!before&&WorldRitualQuestProof.real(actor)&&pet.isTame()&&actor.getUUID().equals(pet.getOwnerUUID())&&WorldRitualQuestProof.natural(pet))pet.getPersistentData().putUUID("jem_quest_potter_tamer",actor.getUUID());}
    public static void begin(AbstractContainerMenu menu,Player actor){
        Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}
        PottergeistEntity pet=menu instanceof PottergeistGUIMenu&&menu instanceof MenuAccess access&&access.jem$ritualBoundEntity() instanceof PottergeistEntity actual?actual:null;
        ItemStack old=pet!=null&&menu.slots.size()>15?menu.getSlot(15).getItem().copy():ItemStack.EMPTY;
        calls.frames.push(new Attachment(menu,actor instanceof ServerPlayer player?player:null,pet,old));
    }
    public static void end(AbstractContainerMenu menu,Player actor){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Attachment receipt=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(receipt==null||receipt.menu!=menu||receipt.actor!=actor||receipt.pet==null||!WorldRitualQuestProof.real(actor)||actor.containerMenu!=menu||!menu.stillValid(actor))return;
        PottergeistEntity pet=receipt.pet;
        if(!pet.isAlive()||!pet.isTame()||!actor.getUUID().equals(pet.getOwnerUUID())||!WorldRitualQuestProof.natural(pet)||!pet.getPersistentData().hasUUID("jem_quest_potter_tamer")||!pet.getPersistentData().getUUID("jem_quest_potter_tamer").equals(actor.getUUID()))return;
        ItemStack vase=menu.getSlot(15).getItem();
        if(vase.isEmpty()||!vase.is(ItemTags.create(new ResourceLocation("luminous_nether:decorative_vases")))||ItemStack.isSameItemSameTags(vase,receipt.old))return;
        pet.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(inventory->{if(inventory.getSlots()>15&&ItemStack.isSameItemSameTags(vase,inventory.getStackInSlot(15))&&inventory.getStackInSlot(15).getCount()==vase.getCount())WorldRitualQuestProof.emit(actor,"v3_combat_nether_houseguest");});
    }
    public static void clear(){CALLS.remove();}
}
