package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.magiclast.MagicLastCauldronProof;
import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = AlchemistCauldronTile.class, remap = false)
public abstract class MagicLastCauldronQuestMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void jem$registered(BlockPos pos, BlockState state, CallbackInfo callback) { MagicLastCauldronProof.register((AlchemistCauldronTile)(Object)this); }
    @Inject(method = "handleUse", at = @At("HEAD"))
    private void jem$useBegin(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { MagicLastCauldronProof.useBegin((AlchemistCauldronTile)(Object)this, actor, hand); }
    @Inject(method = "tryExecuteRecipeInteractions", at = @At("RETURN"))
    private void jem$output(Level level, ItemStack input, CallbackInfoReturnable<ItemStack> callback) { MagicLastCauldronProof.result(callback.getReturnValue()); }
    @Inject(method = "handleUse", at = @At("RETURN"))
    private void jem$useEnd(BlockState state, Level level, BlockPos pos, Player actor, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) { MagicLastCauldronProof.useEnd(callback.getReturnValue().consumesAction()); }
    @Inject(method = "tryMeltInput", at = @At("HEAD"))
    private void jem$meltBegin(ItemStack item, CallbackInfo callback) { MagicLastCauldronProof.meltBegin((AlchemistCauldronTile)(Object)this, item); }
    @Redirect(method = "tryMeltInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;m_44015_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"))
    private Optional<?> jem$recipe(RecipeManager manager, RecipeType type, Container input, Level level) { Optional<?> actual = manager.getRecipeFor(type, input, level); actual.ifPresent(MagicLastCauldronProof::selected); return actual; }
    @Inject(method = "tryMeltInput", at = @At("RETURN"))
    private void jem$meltEnd(ItemStack item, CallbackInfo callback) { MagicLastCauldronProof.meltEnd(); }
    @Inject(method = "m_6836_", at = @At("HEAD"))
    private void jem$input(int slot, ItemStack item, CallbackInfo callback) { MagicLastCauldronProof.inputChanged((AlchemistCauldronTile)(Object)this, slot); }
}
