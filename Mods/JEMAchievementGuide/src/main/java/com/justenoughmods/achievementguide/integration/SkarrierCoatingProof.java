package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Quest-only application provenance on the real coated item, no coating mechanics or second inventory. */
public final class SkarrierCoatingProof {
    private static final String KEY = "JemQuestCoatingV4";
    private static final Set<String> KINDS = Set.of("VenomSack", "SerratedShred", "BowlderDust", "WretchedFang");
    private static final ThreadLocal<ArrayDeque<Effect>> EFFECTS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final class Effect {
        final Entity owner;
        final Entity target;
        final ItemStack weapon;
        final String kind;
        final double uses;
        final boolean eligible;
        boolean committed;
        Effect(Entity owner, Entity target, ItemStack weapon, String kind, double uses, boolean eligible) {
            this.owner = owner; this.target = target; this.weapon = weapon; this.kind = kind; this.uses = uses; this.eligible = eligible;
        }
    }
    private SkarrierCoatingProof() {}

    public static void applied(ServerPlayer actor, ItemStack actual, String kind) {
        if (!real(actor)) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID());
        proof.putString("kind", kind);
        actual.getOrCreateTag().put(KEY, proof);
    }

    public static void beginWeapon(Entity target, Entity owner) {
        ItemStack weapon = owner instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
        CompoundTag nativeTag = weapon.getTag();
        CompoundTag proof = weapon.getTagElement(KEY);
        String kind = nativeTag == null ? "" : nativeTag.getString("CoatingApplied");
        double uses = nativeTag == null || !KINDS.contains(kind) ? 0 : nativeTag.getDouble(kind + "Coating");
        boolean eligible = owner instanceof ServerPlayer actor && real(actor) && proof != null && proof.hasUUID("actor")
                && actor.getUUID().equals(proof.getUUID("actor")) && KINDS.contains(kind) && kind.equals(proof.getString("kind"))
                && Double.isFinite(uses) && uses > 0;
        EFFECTS.get().push(new Effect(owner, target, weapon, kind, uses, eligible));
    }

    public static void installed(LivingEntity target, MobEffectInstance effect, boolean accepted) {
        var effects = EFFECTS.get();
        if (effects.isEmpty()) return;
        Effect run = effects.peek();
        MobEffectInstance installed = target.getEffect(effect.getEffect());
        if (run.eligible && run.target == target && accepted && effect.getDuration() > 0
                && installed != null && installed.getDuration() > 0) run.committed = true;
    }

    public static void endWeapon(Entity target, Entity owner) {
        var effects = EFFECTS.get();
        Effect run = effects.isEmpty() ? null : effects.pop();
        if (effects.isEmpty()) EFFECTS.remove();
        if (run == null || !run.committed || run.owner != owner || run.target != target
                || !(owner instanceof ServerPlayer actor) || actor.getMainHandItem() != run.weapon) return;
        CompoundTag tag = run.weapon.getTag();
        if (tag != null && run.uses - tag.getDouble(run.kind + "Coating") == 1) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_coat_weapon");
        }
    }

    private static boolean real(ServerPlayer actor) {
        return !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer);
    }
}
