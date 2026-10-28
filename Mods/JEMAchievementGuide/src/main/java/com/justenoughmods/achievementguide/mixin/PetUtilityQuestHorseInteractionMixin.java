package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Player.class)
public abstract class PetUtilityQuestHorseInteractionMixin {
 @Redirect(method="interactOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))private InteractionResult jem$nativeInteraction(Entity e,Player p,InteractionHand h){return PetUtilityQuestHorseman.observe(e,p,h,()->e.interact(p,h));}
}
