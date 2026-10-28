package com.justenoughmods.achievementguide.integration.battle;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.hm.efn.gameasset.animations.*;
import com.hm.efn.registries.EFNMobEffectRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.skill.SkillSlots;
public final class BattleQuestNightfall {
    private static final Map<UUID, Attempt> ATTEMPTS = new HashMap<>();
    private static final class Attempt {
        final ServerPlayerPatch patch; final DynamicAnimation animation; final String item; final String action; final long started; final net.minecraft.world.item.ItemStack weapon;
        boolean paid; boolean outcome;
        Attempt(ServerPlayerPatch patch, DynamicAnimation animation, String item, String action) { this.patch=patch;this.animation=animation;this.item=item;this.action=action;started=patch.getOriginal().level().getGameTime();weapon=patch.getOriginal().getMainHandItem(); }
    }
    private BattleQuestNightfall() {}
    private static Attempt attempt(ServerPlayerPatch patch,DynamicAnimation animation) {
        if(!BattleQuestEpic.valid(patch)) return null;
        String item=BattleQuestProof.item(patch.getOriginal().getMainHandItem());
        String action = item.equals("efn:ruinsgreatsword") && animation == EFNGreatSwordAnimations.NG_GREATSWORD_SKILL_CLASH.get() ? "v3_combat_heavy_school"
            : item.equals("efn:nf_dual_sword") && (animation == EFNDualSwordAnimations.NF_DUAL_SKILL.get() || animation == EFNDualSwordAnimations.NF_DUAL_SKILL_EXTEND.get()) ? "v3_combat_agile_school"
            : item.equals("efn:nf_claw") && animation == EFNClawAnimations.NF_CLAW_BEASTROAR.get() ? "v3_combat_risky_school" : "";
        if(action.isEmpty()) return null;
        Attempt old=ATTEMPTS.get(patch.getOriginal().getUUID());
        if(old!=null && old.patch==patch && old.animation==animation && old.item.equals(item) && old.weapon==patch.getOriginal().getMainHandItem() && patch.getOriginal().level().getGameTime()-old.started<200) return old;
        if(ATTEMPTS.size()>=256) ATTEMPTS.clear();
        Attempt result=new Attempt(patch,animation,item,action); ATTEMPTS.put(patch.getOriginal().getUUID(),result); return result;
    }
    public static int stack(PlayerPatch<?> patch) { return patch instanceof ServerPlayerPatch server ? server.getSkill(SkillSlots.WEAPON_INNATE).getStack() : 0; }
    public static void paid(PlayerPatch<?> patch,int before) {
        if(!(patch instanceof ServerPlayerPatch server) || stack(patch)>=before) return;
        Attempt attempt=attempt(server,server.getAnimator().getPlayerFor(null).getAnimation().get());
        if(attempt!=null) { attempt.paid=true; finish(attempt); }
    }
    public static void damage(LivingEntity target,DamageSource source) {
        if(!BattleQuestProof.hostile(target) || !(source instanceof EpicFightDamageSource epic) || epic.getAnimation()==null || !BattleQuestProof.real(source.getEntity())) return;
        EpicFightCapabilities.getUnparameterizedEntityPatch(source.getEntity(),ServerPlayerPatch.class).ifPresent(patch->{
            if(!BattleQuestEpic.valid(patch)) return;
            String item=BattleQuestProof.item(patch.getOriginal().getMainHandItem());
            DynamicAnimation animation=epic.getAnimation().get();
            boolean form=item.equals("efn:kusabimaru") && animation==EFNSekiroAnimations.DRAGON_FLASH.get()
                || item.equals("efn:thornwheel") && (animation==EFNThornWheelAnimations.THORNWHEEL_SKILL_START.get() || animation==EFNThornWheelAnimations.THORNWHEEL_SKILL_LOOP.get());
            if(form) BattleQuestProof.emit(patch.getOriginal(),"v3_combat_nightfall_forms");
            Attempt attempt=attempt(patch,animation); if(attempt!=null && !attempt.action.equals("v3_combat_risky_school")) { attempt.outcome=true;finish(attempt); }
        });
    }
    public static void effect(LivingEntity target,MobEffectInstance effect) {
        if(effect.getEffect()!=EFNMobEffectRegistry.CLAW.get() || !BattleQuestProof.real(target)) return;
        EpicFightCapabilities.getUnparameterizedEntityPatch(target,ServerPlayerPatch.class).ifPresent(patch->{
            Attempt attempt=attempt(patch,patch.getAnimator().getPlayerFor(null).getAnimation().get());
            if(attempt!=null && attempt.action.equals("v3_combat_risky_school")) { attempt.outcome=true;finish(attempt); }
        });
    }
    private static void finish(Attempt attempt) {
        if(attempt.paid && attempt.outcome && BattleQuestEpic.valid(attempt.patch) && attempt.patch.getOriginal().getMainHandItem()==attempt.weapon && BattleQuestProof.item(attempt.patch.getOriginal().getMainHandItem()).equals(attempt.item)) {
            ATTEMPTS.remove(attempt.patch.getOriginal().getUUID()); BattleQuestProof.emit(attempt.patch.getOriginal(),attempt.action);
        }
    }
    public static void animationEnd(ServerPlayerPatch patch,DynamicAnimation animation) { Attempt attempt=ATTEMPTS.get(patch.getOriginal().getUUID()); if(attempt!=null && attempt.animation==animation) ATTEMPTS.remove(patch.getOriginal().getUUID()); }
    public static void forget(UUID actor) { ATTEMPTS.remove(actor); }
    public static void clear() { ATTEMPTS.clear(); }
}
