package com.justenoughmods.achievementguide.integration.worldritual;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.ArrayDeque;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class WorldRitualQuestPickup {
    private record Pickup(ItemEntity drop,ServerPlayer actor,ItemStack snapshot,int held){}
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Pickup> frames=new ArrayDeque<>();int overflow;}
    private WorldRitualQuestPickup(){}
    public static void begin(ItemEntity drop,Player player){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}ItemStack snapshot=drop.getItem().copy();ServerPlayer actor=WorldRitualQuestProof.real(player)?(ServerPlayer)player:null;calls.frames.push(new Pickup(drop,actor,snapshot,actor==null?0:WorldRitualQuestProof.held(actor,snapshot)));}
    public static void end(ItemEntity drop,Player player){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Pickup receipt=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(receipt==null||receipt.drop!=drop||receipt.actor!=player||!WorldRitualQuestProof.real(player))return;
        int received=WorldRitualQuestProof.held(receipt.actor,receipt.snapshot)-receipt.held;
        if(received>0&&receipt.snapshot.getCount()-drop.getItem().getCount()==received&&NativeFoodSources.get(receipt.actor.serverLevel()).consume(receipt.actor.getUUID(),receipt.snapshot,WorldRitualQuestRitual.CHANCE,received))WorldRitualQuestProof.emit(receipt.actor,"v3_magic_nether_ritual");
    }
    public static void clear(){CALLS.remove();}
}
