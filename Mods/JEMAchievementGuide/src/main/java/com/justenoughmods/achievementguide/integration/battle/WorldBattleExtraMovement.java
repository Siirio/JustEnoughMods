package com.justenoughmods.achievementguide.integration.battle;

import com.github.L_Ender.cataclysm.capabilities.ChargeCapability.IChargeCapability;
import com.github.L_Ender.cataclysm.capabilities.RenderRushCapability.IRenderRushCapability;
import com.github.L_Ender.cataclysm.entity.projectile.Tidal_Hook_Entity;
import com.github.L_Ender.cataclysm.init.ModCapabilities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Real native ability state plus accepted movement, no copied timer or motion owner. */
public final class WorldBattleExtraMovement {
    private static final class Ride {
        final ServerPlayer actor; final ItemStack stack; final String kind; final Tidal_Hook_Entity hook;
        Vec3 force; boolean pulling;
        Ride(ServerPlayer actor, ItemStack stack, String kind, Tidal_Hook_Entity hook) {
            this.actor = actor; this.stack = stack; this.kind = kind; this.hook = hook; force = actor.getDeltaMovement();
        }
    }
    private static final String RELEASE="jemBattleNativeReleaseV4";
    private static final Map<UUID, Ride> RIDES = new HashMap<>();
    private WorldBattleExtraMovement() {}
    private static boolean active(Ride ride) {
        if (!BattleQuestProof.real(ride.actor) || ride.actor.serverLevel().getServer().getPlayerList().getPlayer(ride.actor.getUUID()) != ride.actor || ride.actor.getMainHandItem() != ride.stack && ride.actor.getOffhandItem() != ride.stack) return false;
        if (ride.kind.equals("soul")) {
            IRenderRushCapability state = (IRenderRushCapability)ModCapabilities.getCapability(ride.actor, ModCapabilities.RENDER_RUSH_CAPABILITY);
            return state != null && state.isRush() && state.getTimer() > 0;
        }
        if (ride.kind.equals("bulwark")) {
            IChargeCapability state = (IChargeCapability)ModCapabilities.getCapability(ride.actor, ModCapabilities.CHARGE_CAPABILITY);
            return state != null && state.isCharge() && state.getTimer() > 0;
        }
        return ride.hook != null && !ride.hook.isRemoved() && ride.hook.level() == ride.actor.level()
                && ride.hook.getOwner() == ride.actor;
    }
    public static net.minecraft.nbt.CompoundTag stackReceipt(ItemStack stack) {
        if(stack.isEmpty()||stack.getCount()!=1||!((Object)stack instanceof WorldBattleExtraStackCarrier carrier))return new net.minecraft.nbt.CompoundTag();
        var value=carrier.jem$battleRelease();return value.hasUUID("source")&&value.hasUUID("actor")&&value.getString("item").equals(BattleQuestProof.item(stack))?value:new net.minecraft.nbt.CompoundTag();
    }
    private static void persist(Ride ride) {
        var value=new net.minecraft.nbt.CompoundTag();value.putUUID("source",UUID.randomUUID());value.putUUID("actor",ride.actor.getUUID());value.putString("kind",ride.kind);value.putString("item",BattleQuestProof.item(ride.stack));
        ((WorldBattleExtraStackCarrier)(Object)ride.stack).jem$battleRelease(value);
        value.putString("dimension",ride.actor.level().dimension().location().toString());value.putDouble("x",ride.force.x);value.putDouble("y",ride.force.y);value.putDouble("z",ride.force.z);ride.actor.getPersistentData().put(RELEASE,value);
    }
    private static Ride ride(ServerPlayer actor) {
        Ride live=RIDES.get(actor.getUUID());if(live!=null)return live;
        var value=actor.getPersistentData().getCompound(RELEASE);String kind=value.getString("kind");
        if(!value.hasUUID("source")||!value.hasUUID("actor")||!value.getUUID("actor").equals(actor.getUUID())||!java.util.Set.of("soul","bulwark").contains(kind))return null;
        if(!value.getString("dimension").equals(actor.level().dimension().location().toString())){clear(actor);return null;}
        for(ItemStack stack:java.util.List.of(actor.getMainHandItem(),actor.getOffhandItem())){
            var physical=stackReceipt(stack);if(physical.isEmpty()||!physical.getUUID("source").equals(value.getUUID("source"))||!physical.getUUID("actor").equals(actor.getUUID())||!physical.getString("kind").equals(kind)||!physical.getString("item").equals(value.getString("item")))continue;
            Ride restored=new Ride(actor,stack,kind,null);restored.force=new Vec3(value.getDouble("x"),value.getDouble("y"),value.getDouble("z"));
            if(!Double.isFinite(restored.force.lengthSqr())||restored.force.lengthSqr()<=1.0E-10||!active(restored)){clear(actor);return null;}
            if(RIDES.size()>=256)return null;RIDES.put(actor.getUUID(),restored);return restored;
        }
        clear(actor);return null;
    }
    public static void nativeTick(net.minecraft.world.entity.LivingEntity entity,Object capability){if(!(entity instanceof ServerPlayer actor)||!(entity.level() instanceof net.minecraft.server.level.ServerLevel))return;
        String kind=actor.getPersistentData().getCompound(RELEASE).getString("kind");
        if(kind.equals("soul")&&capability instanceof IRenderRushCapability rush&&!rush.isRush()||kind.equals("bulwark")&&capability instanceof IChargeCapability charge&&!charge.isCharge())clear(actor);
    }
    public static void suspend(ServerPlayer actor){RIDES.remove(actor.getUUID());}
    public static void interrupted(ServerPlayer actor){if(RIDES.containsKey(actor.getUUID()))clear(actor);else suspend(actor);}
    public static void begin(WorldBattleExtraProof.Use use, String kind) {
        Ride ride = new Ride(use.actor, use.stack, kind, null);
        if (!active(ride) || RIDES.size() >= 256 && !RIDES.containsKey(use.actor.getUUID())) return;
        RIDES.put(use.actor.getUUID(), ride);persist(ride);
        if (use.actor.position().distanceToSqr(use.position) > 1.0E-8) accepted(use.actor, use.position);
    }
    public static void hook(ServerPlayer actor, ItemStack stack, Tidal_Hook_Entity hook) {
        if (!BattleQuestProof.real(actor) || RIDES.size() >= 256 && !RIDES.containsKey(actor.getUUID())) return;
        RIDES.put(actor.getUUID(), new Ride(actor, stack, "tidal", hook));
    }
    public static void pulled(Tidal_Hook_Entity hook, Vec3 before, boolean nativePulling) {
        if (!(hook.getOwner() instanceof ServerPlayer actor)) return;
        Ride ride = RIDES.get(actor.getUUID());
        if (ride == null || ride.hook != hook || !nativePulling || !active(ride)) return;
        if (actor.getDeltaMovement().distanceToSqr(before) > 1.0E-10) {
            ride.force = actor.getDeltaMovement(); ride.pulling = true;
        }
    }
    public static void accepted(ServerPlayer actor, Vec3 before) {
        Ride ride = ride(actor);
        if (ride == null) return;
        if (ride.actor != actor || !active(ride)) { clear(actor); return; }
        Vec3 displacement = actor.position().subtract(before);
        if (displacement.lengthSqr() <= 1.0E-8 || displacement.dot(ride.force) <= 0
                || !Double.isFinite(displacement.lengthSqr()) || ride.kind.equals("tidal") && !ride.pulling) return;
        clear(actor);
        if (ride.kind.equals("bulwark")) BattleQuestProof.emit(actor, "v3_combat_artifact_charge");
        else if (WorldBattleEquipmentOrigins.mode(actor, ride.stack, ride.kind.equals("soul") ? "soul_render" : "tidal_claws", 1))
            BattleQuestProof.emit(actor, "v3_combat_artifact_stances");
    }
    public static void impact(net.minecraft.world.entity.LivingEntity actor, net.minecraft.world.entity.LivingEntity target,
                              net.minecraft.world.damagesource.DamageSource source, float healthBefore, String kind) {
        if (!(actor instanceof ServerPlayer player) || !BattleQuestProof.real(player) || source.getEntity() != player
                || !BattleQuestProof.hostile(target) || target.isAlliedTo(player) || !Float.isFinite(healthBefore)
                || !Float.isFinite(target.getHealth()) || target.getHealth() >= healthBefore) return;
        Ride ride = ride(player);
        if (ride == null || !ride.kind.equals(kind) || !active(ride)) return;
        clear(player);
        if (kind.equals("bulwark")) BattleQuestProof.emit(player, "v3_combat_artifact_charge");
        else if (WorldBattleEquipmentOrigins.mode(player, ride.stack, "soul_render", 1)) BattleQuestProof.emit(player, "v3_combat_artifact_stances");
    }
    public static void clear(ServerPlayer actor) { RIDES.remove(actor.getUUID());actor.getPersistentData().remove(RELEASE); }
    public static void clear() { RIDES.clear(); }
}
