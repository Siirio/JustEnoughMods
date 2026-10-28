package com.justenoughmods.achievementguide.integration.battle;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public final class BattleQuestArtifacts {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Receipt> frames = new ArrayDeque<>(); int overflow; }
    private static final class Receipt {
        final ServerPlayer actor;
        final ItemStack stack;
        final String kind;
        final Set<java.util.UUID> spawned = new HashSet<>();
        ItemStack ammo = ItemStack.EMPTY;
        int ammoCount;
        boolean nativeFreeAmmo;
        int expectedArrows;
        final boolean valid;
        int cooldowns;
        int effects;
        int mode;
        Receipt(Entity actor, ItemStack stack, String kind) {
            this.actor = actor instanceof ServerPlayer player ? player : null;
            this.stack = stack;
            this.kind = kind;
            this.valid = BattleQuestProof.real(actor) && !stack.isEmpty();
        }
    }
    private BattleQuestArtifacts() {}
    public static void begin(Entity actor, ItemStack stack, String kind) {
        Calls calls = CALLS.get();
        if (calls.overflow > 0 || calls.frames.size() >= BattleQuestProof.LIMIT) { calls.overflow++; return; }
        calls.frames.push(new Receipt(actor, stack, kind));
    }
    private static Receipt current() {
        Calls calls = CALLS.get();
        return calls.overflow == 0 && !calls.frames.isEmpty() && calls.frames.peek().valid ? calls.frames.peek() : null;
    }
    public static void spawned(Entity entity, boolean accepted) {
        Receipt receipt = current();
        if (receipt == null || !accepted || entity.level() != receipt.actor.level() || entity.isRemoved()
                || receipt.actor.serverLevel().getEntity(entity.getUUID()) != entity) return;
        if (entity instanceof Projectile projectile && projectile.getOwner() != receipt.actor) return;
        String name = entity.getClass().getSimpleName();
        boolean matches = switch (receipt.kind) {
            case "earth" -> name.equals("Void_Rune_Entity");
            case "charge" -> name.equals("Flame_Strike_Entity");
            case "stances" -> name.equals("Player_Ceraunus_Entity") || name.equals("Wave_Entity");
            case "ranged" -> entity instanceof net.minecraft.world.entity.projectile.AbstractArrow;
            case "sand" -> name.equals("Sandstorm_Entity");
            default -> false;
        };
        if (matches && receipt.spawned.size() < 16) {
            receipt.spawned.add(entity.getUUID());
            if (receipt.kind.equals("stances")) receipt.mode = name.equals("Wave_Entity") ? 2 : 1;
        }
    }
    public static void ammo(ItemStack actualAmmo, boolean nativeFree) {
        Receipt receipt = current();
        if (receipt != null && receipt.kind.equals("ranged")) {
            receipt.ammo = actualAmmo.copy();
            receipt.ammoCount = BattleQuestProof.held(receipt.actor, actualAmmo);
            receipt.nativeFreeAmmo = nativeFree;
            receipt.expectedArrows = actualAmmo.is(net.minecraft.world.item.Items.ARROW) ? 3 : 2;
        }
    }
    public static void cooldown(net.minecraft.world.item.ItemCooldowns cooldowns, net.minecraft.world.item.Item item, int duration) {
        Receipt receipt = current();
        if (receipt != null && receipt.actor.getCooldowns() == cooldowns && duration > 0 && receipt.stack.getItem() == item && receipt.actor.getCooldowns().isOnCooldown(item)) receipt.cooldowns++;
    }
    public static void effect(LivingEntity target, MobEffectInstance effect, boolean accepted) {
        Receipt receipt = current();
        if (receipt == null || !accepted || target.level() != receipt.actor.level() || !BattleQuestProof.natural(target)) return;
        String id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString();
        if (receipt.kind.equals("ignitium") && id.equals("cataclysm:blazing_brand") && target != receipt.actor
                || receipt.kind.equals("cursium") && id.equals("minecraft:glowing") && target != receipt.actor) receipt.effects++;
    }
    public static void end() {
        Calls calls = CALLS.get();
        if (calls.overflow > 0) { calls.overflow--; return; }
        Receipt receipt = calls.frames.isEmpty() ? null : calls.frames.pop();
        if (calls.frames.isEmpty()) CALLS.remove();
        if (receipt == null || !receipt.valid || !BattleQuestProof.real(receipt.actor)) return;
        boolean nativeOutcome = switch (receipt.kind) {
            case "earth", "charge" -> !receipt.spawned.isEmpty() && receipt.cooldowns > 0;
            case "sand" -> receipt.spawned.size() == 2 && receipt.cooldowns > 0;
            case "ranged" -> receipt.expectedArrows > 0 && receipt.spawned.size() == receipt.expectedArrows
                    && (receipt.ammoCount - BattleQuestProof.held(receipt.actor, receipt.ammo) == 1 || receipt.nativeFreeAmmo);
            case "stances" -> receipt.mode == 1 && receipt.spawned.size() == 1 || receipt.mode == 2 && receipt.spawned.size() == 4 && receipt.cooldowns > 0;
            case "ignitium", "cursium" -> receipt.effects > 0 && receipt.cooldowns > 0
                    && receipt.actor.getItemBySlot(EquipmentSlot.HEAD) == receipt.stack
                    && BattleQuestSmithing.certified(receipt.actor, receipt.stack, receipt.kind);
            default -> false;
        };
        if (!nativeOutcome) return;
        if (receipt.kind.equals("stances")) {
            if (!WorldBattleEquipmentOrigins.mode(receipt.actor, receipt.stack, "ceraunus", receipt.mode)) return;
        }
        String action = switch (receipt.kind) {
            case "ignitium" -> "v3_combat_forged_armor";
            case "cursium" -> "v3_combat_cursium";
            default -> "v3_combat_artifact_" + receipt.kind;
        };
        BattleQuestProof.emit(receipt.actor, action);
    }
    public static void clear() { CALLS.remove(); }
}
