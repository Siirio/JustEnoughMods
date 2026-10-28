package com.justenoughmods.achievementguide.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlot;
@Mixin(value=yesman.epicfight.network.client.CPChangeSkill.class,remap=false)
public interface BattleQuestBookAccess {
    @Accessor("skillSlot") SkillSlot jem$slot();
    @Accessor("skillBookSlotIndex") int jem$index();
    @Accessor("skill") Skill jem$skill();
}
