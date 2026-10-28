package com.justenoughmods.achievementguide.integration.combattools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Same native corpse/treasure lineage, never a location-based or arbitrary chest attribution. */
public final class CombatCorpseProof {
    public static final String CHEST = "JemQuestDeadBeardTreasureV4", VAULT = "JemQuestVaultDefeatV4", WATER = "JemQuestBurnedWaterV4";
    private static final Map<Entity, List<ItemEntity>> CHESTS = new LinkedHashMap<>();
    private static final ThreadLocal<Scopes> SCOPES = ThreadLocal.withInitial(Scopes::new);
    private static final class Scopes { final ArrayDeque<Entity> corpses = new ArrayDeque<>(); int overflow; }
    private CombatCorpseProof() {}
    public static ServerPlayer killer(LivingEntity target, DamageSource source) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) return null;
        Entity actor = source.getEntity(); if (!CombatToolProof.real(actor)) actor = target.getKillCredit();
        return CombatToolProof.real(actor) ? (ServerPlayer)actor : null;
    }
    public static void chestDropped(Entity boss, ItemEntity drop) {
        if (drop == null || drop.isRemoved() || !CombatToolProof.item(drop.getItem()).equals("rottencreatures:treasure_chest")) return;
        CompoundTag contents = drop.getItem().getTagElement("BlockEntityTag");
        if (contents == null || !contents.hasUUID("Owner") || !boss.getUUID().equals(contents.getUUID("Owner")) || contents.getList("Items", Tag.TAG_COMPOUND).isEmpty()) return;
        if (CHESTS.size() >= 64 && !CHESTS.containsKey(boss)) CHESTS.remove(CHESTS.keySet().iterator().next());
        List<ItemEntity> drops = CHESTS.computeIfAbsent(boss, ignored -> new ArrayList<>()); if (drops.size() < CombatToolProof.LIMIT) drops.add(drop);
    }
    public static void defeated(LivingEntity target, DamageSource source) {
        ServerPlayer actor = killer(target, source); List<ItemEntity> chests = CHESTS.remove(target);
        if (actor == null) return;
        String type = CombatToolProof.type(target);
        if (type.equals("deep_dark_regrowth:vault_chest")) {
            CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putUUID("corpse", target.getUUID()); target.getPersistentData().put(VAULT, proof);
        }
        if (type.equals("rottencreatures:burned")) {
            CompoundTag proof = target.getPersistentData().getCompound(WATER);
            if (proof.hasUUID("actor") && proof.getUUID("actor").equals(actor.getUUID())) CombatToolProof.emit(actor, "burned_weather");
        }
        if (!type.equals("rottencreatures:dead_beard") || chests == null) return;
        for (ItemEntity chest : chests) {
            if (chest.isRemoved()) continue;
            CompoundTag contents = chest.getItem().getTagElement("BlockEntityTag"); if (contents == null) continue;
            ListTag loot = contents.getList("Items", Tag.TAG_COMPOUND); if (loot.isEmpty() || loot.size() > 64) continue;
            CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID()); proof.putUUID("corpse", target.getUUID()); proof.put("loot", loot.copy());
            CompoundTag forge = contents.getCompound("ForgeData").copy(); forge.put(CHEST, proof); contents.put("ForgeData", forge);
            CombatToolProof.emit(actor, "deadbeard_defeated");
        }
    }
    public static void enter(Entity corpse) {
        Scopes scopes = SCOPES.get(); if (scopes.overflow > 0 || scopes.corpses.size() >= CombatToolProof.LIMIT) { scopes.overflow++; return; }
        if (corpse == null) { scopes.overflow++; return; } scopes.corpses.push(corpse);
    }
    public static void leave() {
        Scopes scopes = SCOPES.get(); if (scopes.overflow > 0) scopes.overflow--; else if (!scopes.corpses.isEmpty()) scopes.corpses.pop();
        if (scopes.corpses.isEmpty()) SCOPES.remove();
    }
    public static CombatDropProof.Context vault(Entity corpse) {
        if (corpse == null || corpse.isAlive() || !CombatToolProof.type(corpse).equals("deep_dark_regrowth:vault_chest")) return null;
        CompoundTag proof = corpse.getPersistentData().getCompound(VAULT);
        return proof.hasUUID("actor") && proof.hasUUID("corpse") && proof.getUUID("corpse").equals(corpse.getUUID())
                ? new CombatDropProof.Context(List.of(proof.getUUID("actor")), "sculk_vault") : null;
    }
    public static Runnable queued(Runnable original) {
        Scopes scopes = SCOPES.get(); Entity corpse = scopes.overflow == 0 && !scopes.corpses.isEmpty() ? scopes.corpses.peek() : null;
        return () -> { enter(corpse); CombatDropProof.enter(vault(corpse)); try { original.run(); } finally { CombatDropProof.leave(); leave(); } };
    }
    public static void clear() { CHESTS.clear(); SCOPES.remove(); }
}
