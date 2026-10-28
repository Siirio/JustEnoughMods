package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import com.github.alexthe666.alexsmobs.item.ItemEcholocator;
import com.github.alexthe666.alexsmobs.world.AMWorldData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.item.ItemEcholocator", remap = false)
public abstract class WildlifeQuestPupfishLocatorMixin {
    @Inject(method = "m_7203_", at = @At("RETURN"))
    private void jem$located(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        var actor = WildlifeQuestProof.real(player); if (actor == null || ((ItemEcholocator)(Object)this).type != ItemEcholocator.EchoType.PUPFISH) return;
        var data = AMWorldData.get(level);
        // The installed locator broadcasts the actual qualifying chunk even when already inside it.
        if (callback.getReturnValue().getResult().consumesAction() && data != null && data.getPupfishChunk() != null) WildlifeQuestFinders.located(actor);
    }
}
