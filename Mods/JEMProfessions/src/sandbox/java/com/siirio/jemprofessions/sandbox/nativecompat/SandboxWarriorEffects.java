package com.siirio.jemprofessions.sandbox.nativecompat;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/** Native Epic Fight resources and successes; no vanilla dodge/guard simulation. */
public final class SandboxWarriorEffects {
    private static final UUID LISTENER = UUID.fromString("7fc274b7-c633-4891-ab37-a438688ab0f2");
    private static final UUID FOCUS = UUID.fromString("0f896c4f-4de3-41f8-8c7b-7f0d577f8933");
    private static final Map<ServerPlayer, Combat> COMBAT = new WeakHashMap<>();
    private SandboxWarriorEffects() {}
    static void register() { MinecraftForge.EVENT_BUS.register(SandboxWarriorEffects.class); }
    private static boolean has(ServerPlayer p, String node) { return SandboxProfessionSupport.enabled(p, ProfessionId.WARRIOR, node); }

    static void tick(ServerPlayer player) {
        if (!SandboxProfessionSupport.allowed(player) || !player.isAlive()) { COMBAT.remove(player); focus(player, false); return; }
        Combat combat = COMBAT.computeIfAbsent(player, p -> new Combat());
        ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch != null && combat.patch != patch) {
            combat.patch = patch;
            patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, LISTENER, event -> {
                if (has(player, "endurance") && event.getSkill() instanceof DodgeSkill
                        && event.getResourceType() == Skill.Resource.STAMINA) event.setAmount(event.getAmount() * .95F);
            });
        }
        if (!has(player, "counter_step")) combat.counterUntil = 0;
        if (!has(player, "duelist_focus")) { combat.parries.clear(); combat.focusUntil = 0; }
        focus(player, has(player, "duelist_focus") && combat.focusUntil > player.serverLevel().getGameTime());
        if (has(player, "second_wind") && player.getHealth() < player.getMaxHealth() * .30F
                && SandboxProfessionSupport.cooldown(player, "warrior:second_wind", 1200)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0));
            SandboxProfessionSupport.message(player, "Второе дыхание: Скорость I на 4 секунды.", "Second Wind: Speed I for 4 seconds.");
        }
    }

    private static void focus(ServerPlayer player, boolean enabled) {
        // Epic Fight getAttackSpeed reads these native attributes; synchronization also updates its client animator.
        for (AttributeInstance attribute : new AttributeInstance[]{player.getAttribute(Attributes.ATTACK_SPEED),
                player.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get())}) {
            if (attribute == null) continue;
            if (enabled && attribute.getModifier(FOCUS) == null)
                attribute.addTransientModifier(new AttributeModifier(FOCUS, "JEM duelist focus", .10, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (!enabled) attribute.removeModifier(FOCUS);
        }
    }

    static void logout(ServerPlayer player) { COMBAT.remove(player); focus(player, false); }

    /** Called only after native DodgeSkill.executeOnServer has actually executed. */
    public static void dodged(ServerPlayerPatch patch) {
        ServerPlayer player = patch.getOriginal();
        if (!SandboxProfessionSupport.allowed(player) || !has(player, "battle_recovery")
                || !SandboxProfessionSupport.cooldown(player, "warrior:battle_recovery", 600)) return;
        patch.setStamina(Math.min(patch.getMaxStamina(), patch.getStamina() + 2));
        SandboxProfessionSupport.message(player, "Боевое восстановление: +2 выносливости.", "Battle Recovery: +2 stamina.");
    }

    /** The ParryingSkill native event's isParried flag is set before this debit. */
    public static float parryCost(ServerPlayerPatch patch, boolean perfect, float original) {
        return perfect && has(patch.getOriginal(), "parry_master") ? original * .85F : original;
    }

    public static void parried(ServerPlayerPatch patch) {
        ServerPlayer player = patch.getOriginal();
        if (!SandboxProfessionSupport.allowed(player) || !player.isAlive()) return;
        Combat state = COMBAT.computeIfAbsent(player, p -> new Combat());
        long now = player.serverLevel().getGameTime();
        if (has(player, "counter_step") && SandboxProfessionSupport.cooldown(player, "warrior:counter_step", 400)) state.counterUntil = now + 60;
        if (has(player, "duelist_focus")) {
            while (!state.parries.isEmpty() && now - state.parries.peekFirst() > 200) state.parries.removeFirst();
            state.parries.addLast(now);
            if (state.parries.size() >= 3 && SandboxProfessionSupport.cooldown(player, "warrior:duelist_focus", 600)) {
                state.parries.clear();
                state.focusUntil = now + 120;
                SandboxProfessionSupport.message(player, "Сосредоточенность дуэлянта: +10% скорости атаки на 6 секунд.", "Duelist Focus: +10% attack speed for 6 seconds.");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void counter(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity() != player
                || !has(player, "counter_step") || event.getAmount() <= 0) return;
        Combat state = COMBAT.get(player);
        if (state != null && state.counterUntil > player.serverLevel().getGameTime()) {
            state.counterUntil = 0;
            event.setAmount(event.getAmount() + 1);
        }
    }

    private static final class Combat {
        private ServerPlayerPatch patch;
        private long counterUntil;
        private long focusUntil;
        private final ArrayDeque<Long> parries = new ArrayDeque<>();
    }
}
