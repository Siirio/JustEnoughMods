package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.justenoughmods.achievementguide.integration.food.RemainingFoodActors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read native accepted support geometry after its actual final server movement settlement. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RemainingVoidWalkerQuestMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Inject(method = "handleMovePlayer", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;lastGoodZ:D", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void jem$nativeSupport(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (RemainingFoodActors.real(player) == null || awaitingPositionFromClient != null || !player.onGround()
                || player.isShiftKeyDown() || player.isPassenger() || player.isInWater() || player.isFallFlying()
                || player.isChangingDimension() || player.noPhysics || player.getAbilities().flying
                || player.mainSupportingBlockPos.isEmpty()) return;
        var boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != EquipmentSlot.FEET) return;
        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("kaleidoscope_end", "void_walker"));
        if (enchantment == null || EnchantmentHelper.getItemEnchantmentLevel(enchantment, boots) < 1) return;
        BlockPos supported = player.mainSupportingBlockPos.get();
        // The installed native provider creates a full collision cube only at this exact air position.
        BlockPos nativePosition = BlockPos.containing(player.position().add(0, -0.3, 0));
        if (supported.equals(nativePosition) && player.serverLevel().hasChunkAt(supported)
                && player.serverLevel().getBlockState(supported).isAir()) {
            QuestActionTrigger.INSTANCE.emit(player, "v3_food_void_steps");
        }
    }
}
