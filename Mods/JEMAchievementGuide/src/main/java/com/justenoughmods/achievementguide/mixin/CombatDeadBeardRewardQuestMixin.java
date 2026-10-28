package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatCorpseProof;
import com.github.teamfusion.rottencreatures.common.level.entities.living.deadbeard.DeadBeard;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = "com.github.teamfusion.rottencreatures.common.level.entities.living.deadbeard.DeadBeard", remap = false)
public abstract class CombatDeadBeardRewardQuestMixin {
    @Redirect(method = "m_7472_", at = @At(value = "INVOKE", target = "Lcom/github/teamfusion/rottencreatures/common/level/entities/living/deadbeard/DeadBeard;m_19983_(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$nativeTreasure(DeadBeard boss, ItemStack output) {
        ItemEntity actual = boss.spawnAtLocation(output); CombatCorpseProof.chestDropped(boss, actual); return actual;
    }
}
