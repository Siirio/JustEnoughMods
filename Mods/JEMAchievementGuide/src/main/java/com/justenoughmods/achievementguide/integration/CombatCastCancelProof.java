package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** One native accepted combat request and its nested native cancellation, never a key-press signal. */
public final class CombatCastCancelProof {
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    public static final class Run {
        final SkillContainer container;
        final ServerPlayerPatch executor;
        final ServerPlayer actor;
        final Skill skill;
        final MagicData magic;
        final String spell;
        final float stamina;
        boolean cancellationStarted;
        boolean canceled;
        boolean dodged;
        Run(SkillContainer container, ServerPlayerPatch executor, MagicData magic) {
            this.container = container;
            this.executor = executor;
            actor = executor.getOriginal();
            skill = container.getSkill();
            this.magic = magic;
            spell = magic.getCastingSpellId();
            stamina = executor.getStamina();
        }
    }
    private CombatCastCancelProof() {}

    public static Run begin(SkillContainer container, ServerPlayerPatch executor) {
        CURRENT.remove();
        var actor = executor.getOriginal();
        var magic = MagicData.getPlayerMagicData(actor);
        if (actor.isSpectator() || actor.getAbilities().instabuild || actor instanceof net.minecraftforge.common.util.FakePlayer
                || container.getExecutor() != executor || !(container.getSkill() instanceof DodgeSkill)
                || container.getSkill().getCategory() != SkillCategories.DODGE || container.isActivated()
                || !magic.isCasting() || magic.getCastType() != CastType.LONG
                || magic.getCastDuration() < 1 || magic.getCastDurationRemaining() < 1
                || magic.getCastingSpellId().isBlank() || !Float.isFinite(executor.getStamina())) return null;
        var run = new Run(container, executor, magic);
        CURRENT.set(run);
        return run;
    }

    public static void beforeCancellation(ServerPlayer actor) {
        Run run = CURRENT.get();
        if (run == null || run.actor != actor || MagicData.getPlayerMagicData(actor) != run.magic) return;
        run.cancellationStarted = run.magic.isCasting() && run.magic.getCastType() == CastType.LONG
                && run.magic.getCastDurationRemaining() > 0 && run.spell.equals(run.magic.getCastingSpellId());
    }

    public static void afterCancellation(ServerPlayer actor) {
        Run run = CURRENT.get();
        if (run != null && run.actor == actor && run.cancellationStarted) run.canceled = cleared(run.magic);
    }

    public static void dodged(SkillContainer container) {
        Run run = CURRENT.get();
        if (run != null && run.container == container && run.canceled && container.getSkill() == run.skill
                && container.isActivated() && run.executor.getStamina() < run.stamina && cleared(run.magic)) run.dodged = true;
    }

    public static void finish(Run run, boolean accepted) {
        CURRENT.remove();
        if (run != null && accepted && run.canceled && run.dodged && cleared(run.magic)
                && run.container.getExecutor() == run.executor && run.container.getSkill() == run.skill
                && Float.isFinite(run.executor.getStamina()) && run.executor.getStamina() < run.stamina) {
            QuestActionTrigger.INSTANCE.emit(run.actor, "v3_magic_cancel_cast");
        }
    }

    private static boolean cleared(MagicData magic) {
        return !magic.isCasting() && magic.getCastType() == CastType.NONE && magic.getCastDuration() == 0
                && magic.getCastDurationRemaining() == 0 && magic.getCastingSpellId().isBlank();
    }
}
