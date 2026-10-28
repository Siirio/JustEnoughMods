package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateWhistleProofs;
import com.railwayteam.railways.content.conductor.ConductorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.railwayteam.railways.content.conductor.ConductorCapItem", remap = false)
public abstract class ConductorCreationQuestMixin {
    @Unique private final ThreadLocal<ConductorEntity> jem$created = new ThreadLocal<>();
    @Unique private final ThreadLocal<Integer> jem$count = new ThreadLocal<>();
    @Unique private final ThreadLocal<Boolean> jem$casing = new ThreadLocal<>();
    @Inject(method = {"useOn", "m_6225_"}, at = @At("HEAD"), remap = false)
    private void jem$before(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        jem$created.remove(); jem$count.remove(); jem$casing.remove();
        if (context.getPlayer() instanceof ServerPlayer actor && CreateWhistleProofs.eligible(actor)) {
            jem$count.set(context.getItemInHand().getCount()); jem$casing.set("create:andesite_casing".equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(context.getLevel().getBlockState(context.getClickedPos()).getBlock()))));
        }
    }
    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE", target = "Lcom/railwayteam/railways/content/conductor/ConductorEntity;spawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lcom/railwayteam/railways/content/conductor/ConductorEntity;"), remap = false)
    private ConductorEntity jem$nativeCreated(Level world, BlockPos pos, ItemStack cap) {
        ConductorEntity result = ConductorEntity.spawn(world, pos, cap); jem$created.set(result); return result;
    }
    @Inject(method = {"useOn", "m_6225_"}, at = @At("RETURN"), remap = false)
    private void jem$after(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        ConductorEntity created = jem$created.get(); Integer count = jem$count.get();
        if (context.getPlayer() instanceof ServerPlayer actor && CreateWhistleProofs.eligible(actor) && Boolean.TRUE.equals(jem$casing.get()) && count != null
                && callback.getReturnValue() == InteractionResult.CONSUME && context.getItemInHand().getCount() == count - 1
                && context.getLevel() instanceof ServerLevel level && created != null && level.getEntity(created.getUUID()) == created
                && level.getBlockState(context.getClickedPos()).isAir()) created.getPersistentData().putUUID(CreateWhistleProofs.CREATOR, actor.getUUID());
        jem$created.remove(); jem$count.remove(); jem$casing.remove();
    }
}
