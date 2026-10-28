package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestBrews;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BrewingStandBlockEntity.class)
public abstract class WildlifeQuestBrewingMixin {
    @Unique private static WildlifeQuestBrews.Tick jem$tick;
    @Unique private static WildlifeQuestBrews.Brew jem$brew;
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void jem$beforeTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand, CallbackInfo callback) { jem$tick = WildlifeQuestBrews.tick(stand); }
    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void jem$afterTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand, CallbackInfo callback) { WildlifeQuestBrews.ticked(jem$tick); jem$tick = null; }
    @Inject(method = "doBrew", at = @At("HEAD"))
    private static void jem$before(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo callback) { jem$brew = level instanceof ServerLevel server ? WildlifeQuestBrews.before(server, pos, items) : null; }
    @Inject(method = "doBrew", at = @At("RETURN"))
    private static void jem$after(Level level, BlockPos pos, NonNullList<ItemStack> items, CallbackInfo callback) { WildlifeQuestBrews.brewed(jem$brew, items); jem$brew = null; }
}
