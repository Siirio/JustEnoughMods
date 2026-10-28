package com.justenoughmods.achievementguide.integration.worldritual;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.mcreator.luminousnether.world.inventory.RitualGUIMenu;
import net.mcreator.luminousnether.network.LuminousNetherModVariables.MapVariables;
import net.mcreator.luminousnether.configuration.LuminousNetherServerConfigConfiguration;
public final class WorldRitualQuestRitual {
    public interface MenuAccess { BlockEntity jem$ritualBlock(); }
    public static final String CHANCE="world_ritual_useful_chance";
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls {final ArrayDeque<Receipt> frames=new ArrayDeque<>();int overflow;}
    private static final class Receipt {
        final ServerPlayer actor;final String kind;final RitualGUIMenu menu;final boolean valid;final float health;final int food;final float saturation;final int levels;final boolean day;final Set<MobEffect> harmful;
        final List<ItemEntity> drops=new ArrayList<>();boolean paid;boolean granted;
        Receipt(Entity entity,BlockPos pos,String kind,String active){
            actor=entity instanceof ServerPlayer player?player:null;this.kind=kind;menu=actor!=null&&actor.containerMenu instanceof RitualGUIMenu ritual?ritual:null;
            valid=WorldRitualQuestProof.real(actor)&&menu!=null&&menu.entity==actor&&menu.x==pos.getX()&&menu.y==pos.getY()&&menu.z==pos.getZ()
                &&actor.serverLevel().hasChunkAt(pos)&&actor.serverLevel().hasChunkAt(pos.below())&&menu.stillValid(actor)
                &&menu instanceof MenuAccess access&&access.jem$ritualBlock()!=null&&actor.serverLevel().getBlockEntity(pos)==access.jem$ritualBlock()
                &&BuiltInRegistries.BLOCK.getKey(actor.level().getBlockState(pos).getBlock()).toString().equals("luminous_nether:ritual_table")
                &&BuiltInRegistries.BLOCK.getKey(actor.level().getBlockState(pos.below()).getBlock()).toString().equals("luminous_nether:pedestal")
                &&MapVariables.get(actor.level()).ActiveRitual.equals(active)&&enabled(kind);
            health=actor==null?0:actor.getHealth();food=actor==null?0:actor.getFoodData().getFoodLevel();saturation=actor==null?0:actor.getFoodData().getSaturationLevel();levels=actor==null?0:actor.experienceLevel;day=actor!=null&&actor.level().isDay();
            harmful=new HashSet<>();if(actor!=null)for(MobEffectInstance effect:actor.getActiveEffects())if(effect.getEffect().getCategory()==MobEffectCategory.HARMFUL)harmful.add(effect.getEffect());
        }
    }
    private record Home(ServerPlayer actor,long time){}
    private record Teleport(Entity actor,Vec3 before,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension){}
    private static final Map<UUID,Home> HOMES=new HashMap<>();
    private static final ThreadLocal<Teleports> TELEPORTS=ThreadLocal.withInitial(Teleports::new);
    private static final class Teleports {final ArrayDeque<Teleport> frames=new ArrayDeque<>();int overflow;}
    private WorldRitualQuestRitual(){}
    private static boolean enabled(String kind){return switch(kind){
        case "health"->LuminousNetherServerConfigConfiguration.RITUAL_OF_HEALTH_TOGGLE.get();case "night"->LuminousNetherServerConfigConfiguration.RITUAL_OF_NIGHT_TOGGLE.get();
        case "cleansing"->LuminousNetherServerConfigConfiguration.RITUAL_OF_CLEANSING_TOGGLE.get();case "xp"->LuminousNetherServerConfigConfiguration.RITUAL_OF_EXPERIENCE_TOGGLE.get();
        case "chance"->LuminousNetherServerConfigConfiguration.RITUAL_OF_CHANCE_TOGGLE.get();case "perception"->LuminousNetherServerConfigConfiguration.RITUAL_OF_PERCEPTION_TOGGLE.get();
        case "saturation"->LuminousNetherServerConfigConfiguration.RITUAL_OF_SATURATION_TOGGLE.get();case "mending"->LuminousNetherServerConfigConfiguration.RITUAL_OF_MENDING_TOGGLE.get();
        case "luck"->LuminousNetherServerConfigConfiguration.RITUAL_OF_LUCK_TOGGLE.get();case "sanctuary"->LuminousNetherServerConfigConfiguration.RITUAL_OF_SANCTUARY_TOGGLE.get();default->false;};}
    public static void begin(Entity actor,double x,double y,double z,String kind,String active){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Receipt(actor,BlockPos.containing(x,y,z),kind,active));}
    private static Receipt current(){Calls calls=CALLS.get();return calls.overflow==0&&!calls.frames.isEmpty()&&calls.frames.peek().valid?calls.frames.peek():null;}
    public static ItemStack remove(Slot slot,int amount){
        ItemStack before=slot.getItem().copy();ItemStack removed=slot.remove(amount);
        Receipt r=current();if(r!=null&&slot==r.menu.getSlot(0)&&WorldRitualQuestProof.item(before).equals("luminous_nether:ghostly_essence")&&amount==16&&removed.getCount()==16
            &&ItemStack.isSameItemSameTags(before,removed)&&before.getCount()-slot.getItem().getCount()==16)r.paid=true;
        return removed;
    }
    public static void effect(LivingEntity target,MobEffectInstance effect,boolean accepted){Receipt r=current();if(r!=null&&r.actor==target&&accepted&&target.hasEffect(effect.getEffect())&&(effect.getEffect().getCategory()==MobEffectCategory.BENEFICIAL
        ||r.kind.equals("mending")&&effect.getEffect()==net.mcreator.luminousnether.init.LuminousNetherModMobEffects.AUTOMENDING.get()
        ||r.kind.equals("perception")&&effect.getEffect()==net.mcreator.luminousnether.init.LuminousNetherModMobEffects.PERCEPTION.get()))r.granted=true;}
    public static void spawned(Entity entity,boolean accepted){Receipt r=current();if(r!=null&&r.kind.equals("chance")&&accepted&&entity instanceof ItemEntity drop&&drop.level()==r.actor.level()&&!drop.isRemoved()&&r.actor.serverLevel().getEntity(drop.getUUID())==drop&&r.drops.size()<16)r.drops.add(drop);}
    public static void end(){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Receipt r=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();if(r==null||!r.valid||!r.paid||!WorldRitualQuestProof.real(r.actor))return;
        if(r.kind.equals("sanctuary")){if(r.actor.getPersistentData().getDouble("TpTimer")>0){if(HOMES.size()>=256)HOMES.clear();HOMES.put(r.actor.getUUID(),new Home(r.actor,r.actor.server.overworld().getGameTime()));}return;}
        for(ItemEntity drop:r.drops)if(!drop.isRemoved())NativeFoodSources.get(r.actor.serverLevel()).certify(r.actor.serverLevel(),r.actor.getUUID(),CHANCE,drop.getItem(),drop.getItem().getCount());
        boolean benefit=r.granted||r.actor.getHealth()>r.health||r.actor.experienceLevel>r.levels||r.actor.getFoodData().getFoodLevel()>r.food||r.actor.getFoodData().getSaturationLevel()>r.saturation
            ||r.harmful.stream().anyMatch(effect->!r.actor.hasEffect(effect))||r.kind.equals("night")&&r.day&&r.actor.level().isNight();
        if(benefit)WorldRitualQuestProof.emit(r.actor,"v3_magic_nether_ritual");
    }
    public static void homeBegin(Entity actor){Teleports calls=TELEPORTS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}calls.frames.push(new Teleport(actor,actor.position(),actor.level().dimension()));}
    public static void homeEnd(Entity actor){Teleports calls=TELEPORTS.get();if(calls.overflow>0){calls.overflow--;return;}Teleport teleport=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())TELEPORTS.remove();Home home=HOMES.get(actor.getUUID());
        if(home==null||home.actor!=actor||teleport==null||teleport.actor!=actor||!WorldRitualQuestProof.real(actor))return;
        if(home.actor.server.overworld().getGameTime()-home.time>200){HOMES.remove(actor.getUUID());return;}
        if(actor.level().dimension()!=teleport.dimension||actor.position().distanceToSqr(teleport.before)>0.0001){HOMES.remove(actor.getUUID());WorldRitualQuestProof.emit(actor,"v3_magic_nether_ritual");}
    }
    public static void forget(UUID actor){HOMES.remove(actor);}
    public static void clear(){CALLS.remove();TELEPORTS.remove();HOMES.clear();}
}
