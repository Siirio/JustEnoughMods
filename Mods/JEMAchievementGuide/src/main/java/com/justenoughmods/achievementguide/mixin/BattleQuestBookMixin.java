package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import yesman.epicfight.skill.SkillSlot; import yesman.epicfight.skill.Skill; import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
@Mixin(value = yesman.epicfight.network.client.CPChangeSkill.class, remap = false)
public abstract class BattleQuestBookMixin {
    @Inject(method="lambda$handle$0",at=@At("HEAD"))
    private static void jem$before(yesman.epicfight.network.client.CPChangeSkill msg, ServerPlayerPatch patch, CallbackInfo ci) {
        BattleQuestBookAccess access = (BattleQuestBookAccess)(Object)msg;
        BattleQuestEpic.bookCallBegin(patch,access.jem$slot(),access.jem$skill(),access.jem$index());
    }
    @Inject(method="lambda$handle$0",at=@At("RETURN"))
    private static void jem$after(yesman.epicfight.network.client.CPChangeSkill msg, ServerPlayerPatch patch, CallbackInfo ci) {
        BattleQuestEpic.bookCallEnd();
    }
}
