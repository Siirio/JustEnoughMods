package com.justenoughmods.achievementguide.integration.battlefinal;
import java.util.ArrayDeque;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.*;
public final class BattleFinalQuestRelics {
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Receipt> frames=new ArrayDeque<>();int overflow;}
    private static final class Receipt {final Event event;final Entity actor;final Entity target;final String kind;final ItemStack item;final String id;boolean effect;boolean cost;Receipt(Event event,Entity actor,Entity target,String kind){this.event=event;this.actor=actor;this.target=target;this.kind=kind;item=actor instanceof LivingEntity living?living.getOffhandItem():ItemStack.EMPTY;id=BattleFinalQuestProof.item(item);}}
    private BattleFinalQuestRelics(){}
    public static void begin(Event event,Entity actor,Entity target,String kind){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Receipt(event,actor,target,kind));}
    private static Receipt current(){Calls calls=CALLS.get();return calls.overflow==0&&!calls.frames.isEmpty()?calls.frames.peek():null;}
    public static void effect(LivingEntity target,MobEffectInstance effect,boolean accepted){Receipt receipt=current();if(receipt!=null&&accepted&&target.hasEffect(effect.getEffect())&&target==(receipt.kind.equals("jungle")?receipt.actor:receipt.target)
        &&(receipt.kind.equals("root")&&effect.getEffect()==net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN
        ||receipt.kind.equals("gator")&&effect.getEffect()==net.mcreator.luminousbeasts.init.LuminousBeastsModMobEffects.UNIVERSAL_POISON.get()
        ||receipt.kind.equals("jungle")&&effect.getEffect()==net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE))receipt.effect=true;}
    public static void cost(ItemStack actual){Receipt receipt=current();if(receipt!=null&&actual==receipt.item)receipt.cost=true;}
    public static void end(){Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Receipt receipt=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(receipt==null||!receipt.effect||!receipt.cost||!BattleFinalQuestProof.real(receipt.actor)||!(receipt.target instanceof LivingEntity target)||!BattleFinalQuestProof.target(target))return;
        String expected=receipt.kind.equals("root")?"magic_root":receipt.kind.equals("gator")?"gator_tooth":"jungle_pendant";
        if(!receipt.id.equals("luminous_beasts:"+expected))return;
        BattleFinalQuestLife.event(receipt.event,()->BattleFinalQuestProof.emit(receipt.actor,"v3_combat_beast_relics"));
    }
    public static void clear(){CALLS.remove();}
}
