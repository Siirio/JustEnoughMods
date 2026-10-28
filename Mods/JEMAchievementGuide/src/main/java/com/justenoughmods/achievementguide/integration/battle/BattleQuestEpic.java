package com.justenoughmods.achievementguide.integration.battle;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.skill.passive.TechnicianSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.item.SkillBookItem;
public final class BattleQuestEpic {
    private static final ThreadLocal<Calls> CALLS = ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Basic> frames = new ArrayDeque<>(); int overflow; }
    private static final class Basic { final ServerPlayerPatch patch; int context; Basic(ServerPlayerPatch patch) { this.patch = patch; } }
    private record Pending(ServerPlayerPatch patch, DynamicAnimation animation, int context, long time) {}
    private record Leap(ServerPlayerPatch patch, Skill skill, long time, Vec3 origin, boolean released) {}
    private static final Map<UUID, Pending> BASIC = new HashMap<>();
    private static final Map<UUID, Leap> LEAPS = new HashMap<>();
    private static final ThreadLocal<BookCalls> BOOKS = ThreadLocal.withInitial(BookCalls::new);
    private static final class BookCalls { final ArrayDeque<Book> frames = new ArrayDeque<>(); int overflow; }
    public record Book(ServerPlayerPatch patch, SkillSlot slot, Skill skill, int index, ItemStack original, boolean valid) {}
    private BattleQuestEpic() {}
    public static boolean valid(ServerPlayerPatch patch) { return patch != null && BattleQuestProof.real(patch.getOriginal()) && patch.isEpicFightMode(); }
    public static void basicBegin(SkillContainer container) {
        Calls calls = CALLS.get(); if (calls.overflow > 0 || calls.frames.size() >= BattleQuestProof.LIMIT) { calls.overflow++; return; }
        calls.frames.push(new Basic(container.getServerExecutor()));
    }
    public static void context(SkillContainer container, boolean dash, boolean air, boolean accepted) {
        Calls calls = CALLS.get(); if (calls.overflow == 0 && !calls.frames.isEmpty() && accepted && calls.frames.peek().patch == container.getServerExecutor()) calls.frames.peek().context = air ? 4 : dash ? 2 : 1;
    }
    public static void played(AssetAccessor<? extends DynamicAnimation> animation) {
        Calls calls = CALLS.get(); if (calls.overflow != 0 || calls.frames.isEmpty()) return;
        Basic basic = calls.frames.peek();
        if (basic.context == 0 || !valid(basic.patch) || basic.patch.getOriginal().getMainHandItem().isEmpty() || basic.patch.getHoldingItemCapability(net.minecraft.world.InteractionHand.MAIN_HAND).getWeaponCategory() == yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories.NOT_WEAPON || !(animation.get() instanceof AttackAnimation)) return;
        if (BASIC.size() >= 256) BASIC.clear();
        BASIC.put(basic.patch.getOriginal().getUUID(), new Pending(basic.patch, animation.get(), basic.context, basic.patch.getOriginal().level().getGameTime()));
    }
    public static void basicEnd() { Calls calls = CALLS.get(); if (calls.overflow > 0) { calls.overflow--; return; } if (!calls.frames.isEmpty()) calls.frames.pop(); if (calls.frames.isEmpty()) CALLS.remove(); }
    public static void animationEnd(ServerPlayerPatch patch, DynamicAnimation animation, boolean naturalEnd) {
        Pending pending = BASIC.get(patch.getOriginal().getUUID());
        if (pending == null || pending.animation != animation) return;
        BASIC.remove(patch.getOriginal().getUUID());
        if (!naturalEnd || pending.patch != patch || !valid(patch) || patch.getOriginal().level().getGameTime() - pending.time > 1200) return;
        ServerPlayer actor = patch.getOriginal();
        BattleQuestProof.emit(actor, pending.context == 4 ? "epic_context_air" : pending.context == 2 ? "epic_context_sprint" : "epic_context");
    }
    public static void bookCallBegin(ServerPlayerPatch patch, SkillSlot slot, Skill skill, int index) {
        BookCalls calls=BOOKS.get(); if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;} calls.frames.push(bookBegin(patch,slot,skill,index));
    }
    public static void bookCallEnd() {
        BookCalls calls=BOOKS.get();if(calls.overflow>0){calls.overflow--;return;}bookEnd(calls.frames.isEmpty()?null:calls.frames.pop());if(calls.frames.isEmpty())BOOKS.remove();
    }
    public static Book bookBegin(ServerPlayerPatch patch, SkillSlot slot, Skill skill, int index) {
        boolean valid = valid(patch) && slot != null && index >= 0 && index < patch.getOriginal().getInventory().getContainerSize() && skill != null;
        ItemStack book = valid ? patch.getOriginal().getInventory().getItem(index).copy() : ItemStack.EMPTY;
        valid = valid && book.getCount() == 1 && book.getItem() instanceof SkillBookItem && SkillBookItem.getContainSkill(book) == skill
                && !patch.getSkillCapability().hasLearned(skill) && SkillManager.getSkill(skill.toString()) == skill && slot.category() == skill.getCategory() && skill.getCategory().learnable();
        return new Book(patch, slot, skill, index, book, valid);
    }
    public static void bookEnd(Book receipt) {
        if (receipt == null || !receipt.valid || !valid(receipt.patch)) return;
        ServerPlayer actor = receipt.patch.getOriginal();
        if (actor.getInventory().getItem(receipt.index).isEmpty() && receipt.patch.getSkill(receipt.slot).getSkill() == receipt.skill
                && receipt.patch.getSkillCapability().hasLearned(receipt.skill)) BattleQuestProof.emit(actor, "v3_combat_skill_book");
    }
    private static boolean physical(DamageSource source) { return source.getEntity() instanceof LivingEntity && source.getDirectEntity() != null && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_EXPLOSION); }
    public static void dodge(ServerPlayerPatch patch, DamageSource source) {
        SkillContainer container = patch.getSkill(SkillSlots.DODGE);
        if (valid(patch) && physical(source) && container.getSkill() instanceof DodgeSkill && (container.getSkill().toString().equals("epicfight:roll") || container.getSkill().toString().equals("epicfight:step")) && patch.getSkillCapability().hasLearned(container.getSkill())) BattleQuestProof.emit(patch.getOriginal(), "v3_combat_dodge_guard_dodge");
    }
    public static void guard(ServerPlayerPatch patch, DamageSource source, AttackResult result) {
        SkillContainer container = patch.getSkill(SkillSlots.GUARD);
        if (valid(patch) && physical(source) && result.resultType == AttackResult.ResultType.BLOCKED
                && container.getSkill() instanceof GuardSkill && patch.getSkillCapability().hasLearned(container.getSkill())) BattleQuestProof.emit(patch.getOriginal(), "v3_combat_dodge_guard");
    }
    public static void technician(SkillContainer container, float before) {
        ServerPlayerPatch patch = container.getServerExecutor();
        if (valid(patch) && container.getSkill() instanceof TechnicianSkill && patch.getSkillCapability().hasLearned(container.getSkill())
                && patch.getSkillCapability().listSkillContainers().anyMatch(equipped -> equipped == container) && patch.getStamina() > before)
            BattleQuestProof.emit(patch.getOriginal(), "v3_combat_skill_synergy");
    }
    public static void leapStarted(SkillContainer container) { if (container.getExecutor() instanceof ServerPlayerPatch patch) LEAPS.remove(patch.getOriginal().getUUID()); }
    public static void leapPaid(SkillContainer container, float staminaBefore) {
        if (!(container.getExecutor() instanceof ServerPlayerPatch patch) || !valid(patch) || patch.getStamina() >= staminaBefore) return;
        if (LEAPS.size() >= 256) LEAPS.clear();
        LEAPS.put(patch.getOriginal().getUUID(), new Leap(patch, container.getSkill(), patch.getOriginal().level().getGameTime(), patch.getOriginal().position(), false));
    }
    public static void leapReleased(SkillContainer container) {
        if (!(container.getExecutor() instanceof ServerPlayerPatch patch)) return; Leap paid = LEAPS.get(patch.getOriginal().getUUID());
        if (paid != null && paid.patch == patch && paid.skill == container.getSkill() && patch.getOriginal().level().getGameTime() - paid.time < 100)
            LEAPS.put(patch.getOriginal().getUUID(), new Leap(patch, paid.skill, patch.getOriginal().level().getGameTime(), patch.getOriginal().position(), true));
    }
    public static void moved(ServerPlayer actor, Vec3 before) {
        Leap leap = LEAPS.get(actor.getUUID()); if (leap == null) return;
        if (!valid(leap.patch) || actor != leap.patch.getOriginal() || actor.level().getGameTime() - leap.time > 10 || leap.patch.getSkill(SkillSlots.MOVER).getSkill() != leap.skill) { LEAPS.remove(actor.getUUID()); return; }
        if (leap.released && leap.patch.getAnimator().getPlayerFor(null).getAnimation().get() == yesman.epicfight.gameasset.Animations.BIPED_DEMOLITION_LEAP.get() && actor.position().distanceToSqr(before) > 0.0001 && actor.getY() > leap.origin.y && !actor.onGround()) {
            LEAPS.remove(actor.getUUID()); BattleQuestProof.emit(actor, "v3_combat_skill_mobility");
        }
    }
    public static void forget(UUID actor) { BASIC.remove(actor); LEAPS.remove(actor); }
    public static void clear() { CALLS.remove(); BOOKS.remove(); BASIC.clear(); LEAPS.clear(); }
}
