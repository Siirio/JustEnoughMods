package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.criterion.JemCriteria;
import com.ninni.spawn.server.item.CastingNetItem;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CastingNetItem.class, remap = false)
public abstract class CastingNetItemMixin {
    private static final ResourceLocation NET_RESULT = new ResourceLocation("jem_guide", "fishing/6_2");
    private static final String BARRACUDA = "spawn:barracuda";

    @Inject(method = "m_7203_", at = @At("RETURN"))
    private void afterNetUsed(Level level, Player player, InteractionHand hand,
                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || !cir.getReturnValue().getResult().consumesAction()) {
            return;
        }
        ItemStack net = cir.getReturnValue().getObject();
        if (net.getTag() == null) {
            return;
        }
        ListTag captured = net.getTag().getList("CapturedMobs", Tag.TAG_COMPOUND);
        for (Tag entry : captured) {
            if (BARRACUDA.equals(((net.minecraft.nbt.CompoundTag) entry).getString("id"))) {
                JemCriteria.fire(serverPlayer, NET_RESULT);
                return;
            }
        }
    }
}
