package com.justenoughmods.achievementguide.integration.battle;
import java.util.ArrayDeque;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.registries.BuiltInRegistries;
public final class BattleQuestRelics {
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Receipt> frames=new ArrayDeque<>(); int overflow; }
    private static final class Receipt {
        final LivingEntity actor; final String kind; final ItemStack stack; final int count; final float health; final boolean valid; boolean effect;
        Receipt(Entity entity,String kind,ItemStack stack){actor=entity instanceof LivingEntity living?living:null;this.kind=kind;this.stack=stack;count=stack.getCount();health=actor==null?0:actor.getHealth();valid=BattleQuestProof.real(entity);}
    }
    private BattleQuestRelics() {}
    public static void begin(Entity actor,String kind,ItemStack stack){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Receipt(actor,kind,stack));}
    public static void effect(LivingEntity target,MobEffectInstance effect){
        Calls calls=CALLS.get();if(calls.overflow!=0||calls.frames.isEmpty())return;Receipt receipt=calls.frames.peek();
        if(!receipt.valid||receipt.actor!=target)return;
        String id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString();
        if(receipt.kind.equals("viper")&&BattleQuestProof.item(receipt.stack).equals("luminous_beasts:viper_tooth")
            ||receipt.kind.equals("blessed")&&BattleQuestProof.item(receipt.stack).equals("cataclysm:blessed_amethyst_crab_meat")&&id.equals("cataclysm:blessing_of_amethyst"))receipt.effect=true;
    }
    public static void end(){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Receipt receipt=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(receipt==null||!receipt.valid||!BattleQuestProof.real(receipt.actor))return;
        if(receipt.kind.equals("yeti")&&BattleQuestProof.item(receipt.stack).equals("luminous_beasts:yeti_horn")&&receipt.actor.getHealth()>receipt.health
            ||receipt.kind.equals("viper")&&receipt.effect)BattleQuestProof.emit(receipt.actor,"v3_combat_beast_relics");
        if(receipt.kind.equals("blessed")&&receipt.effect&&receipt.count-receipt.stack.getCount()==1)BattleQuestProof.emit(receipt.actor,"v3_combat_abyss_kit");
    }
    public static void clear(){CALLS.remove();}
}
