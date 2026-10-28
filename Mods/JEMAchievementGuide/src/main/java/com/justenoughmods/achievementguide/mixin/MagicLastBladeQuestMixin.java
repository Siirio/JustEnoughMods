package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastSpellProof;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.merlin204.efsiss.util.EFSISSUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = "org.merlin204.efsiss.skill.passive.MagicSwordSkill", remap = false)
public abstract class MagicLastBladeQuestMixin {
    @Redirect(method = "lambda$onInitiate$0", at = @At(value = "INVOKE", target = "Lorg/merlin204/efsiss/util/EFSISSUtils;attemptInitiateCast(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lio/redspace/ironsspellbooks/api/spells/CastSource;Ljava/lang/String;Z)V"))
    private static void jem$nativeSkill(AbstractSpell spell, ItemStack stack, int spellLevel, Level level, Player user, CastSource source, String slot, boolean warning) { MagicLastSpellProof.attemptBegin(spell, spellLevel, user instanceof ServerPlayer actor ? actor : null, source); try { EFSISSUtils.attemptInitiateCast(spell, stack, spellLevel, level, user, source, slot, warning); } finally { MagicLastSpellProof.attemptEnd(); } }
}
