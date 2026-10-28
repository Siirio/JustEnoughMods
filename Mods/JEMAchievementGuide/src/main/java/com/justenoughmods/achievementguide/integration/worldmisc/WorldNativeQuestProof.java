package com.justenoughmods.achievementguide.integration.worldmisc;

import com.justenoughmods.achievementguide.integration.travel.TravelProof;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Provenance of the actual supplied native item entity, no gameplay state. */
@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class WorldNativeQuestProof {
    private static final String SUPPLY = "jemWorldNativeSupplyV4";
    private WorldNativeQuestProof() {}
    public static boolean real(ServerPlayer actor) { return TravelProof.eligible(actor); }
    public static void emit(ServerPlayer actor, String action) { TravelProof.emit(actor, action); }
    public static boolean entity(Entity entity, String id) { return id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()); }
    public static boolean item(ItemStack stack, String id) { return !stack.isEmpty() && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()); }
    public static boolean nativeMob(Entity entity) { return entity instanceof Mob mob && mob.getSpawnType() != MobSpawnType.COMMAND; }
    public static ServerPlayer actor(ServerLevel level, UUID uuid) {
        ServerPlayer actor = uuid == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
        return real(actor) && actor.level() == level ? actor : null;
    }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void tossed(ItemTossEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer actor) || !real(actor)) return;
        ItemEntity drop = event.getEntity();
        if (!item(drop.getItem(), "minecraft:beef") && !item(drop.getItem(), "aquamirae:jellyfish_jelly")) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID());
        proof.putUUID("entity", drop.getUUID());
        proof.put("stack", drop.getItem().save(new CompoundTag()));
        drop.getPersistentData().put(SUPPLY, proof);
    }
    public static ServerPlayer supplier(ItemEntity drop) {
        CompoundTag proof = drop.getPersistentData().getCompound(SUPPLY);
        return drop.level() instanceof ServerLevel level && proof.hasUUID("actor") && proof.hasUUID("entity")
                && proof.getUUID("entity").equals(drop.getUUID()) && proof.getCompound("stack").equals(drop.getItem().save(new CompoundTag()))
                ? actor(level, proof.getUUID("actor")) : null;
    }
    public static void sharkConsumed(Entity shark, ItemEntity drop) {
        ServerPlayer actor = supplier(drop);
        boolean eligible = actor != null && nativeMob(shark) && entity(shark, "oceanicrealms:great_white_shark")
                && shark.level() == drop.level() && item(drop.getItem(), "minecraft:beef") && drop.getItem().getCount() == 1;
        drop.discard();
        if (eligible && drop.isRemoved()) emit(actor, "v3_fauna_shark_supper");
    }
}
