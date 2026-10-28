package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ServerPlayerGameMode.class)
public abstract class CaveQuestSulfurHarvestMixin implements com.justenoughmods.achievementguide.integration.NativeHarvestWitness.Caves {
    @Shadow @Final protected ServerPlayer player;
    @Unique private net.minecraft.nbt.CompoundTag jem$caveGrowth;
    @Inject(method="destroyBlock",at=@At("HEAD")) private void jem$before(BlockPos pos,CallbackInfoReturnable<Boolean> ci){jem$caveGrowth=CaveQuestBlockOrigins.get(player.serverLevel(),pos,"Grown");}
    @Inject(method="destroyBlock",at=@At("RETURN")) private void jem$after(BlockPos pos,CallbackInfoReturnable<Boolean> ci){jem$caveGrowth=null;}
    @Override public void jem$caveNativeHarvest(Block block,Level world,Player user,BlockPos pos,BlockState state,BlockEntity owner,ItemStack tool,Runnable original){
        var origin=jem$caveGrowth==null?new net.minecraft.nbt.CompoundTag():jem$caveGrowth;
        boolean eligible=user==player && world==player.level() && CaveQuestProof.hazmat(player) && origin.getCompound("state").equals(net.minecraft.nbt.NbtUtils.writeBlockState(state)) && state.is(com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry.SULFUR_CLUSTER.get()) && world.getBlockState(pos).isAir();
        CaveQuestDrops.observe(player,eligible?player:null,"sulfur",()->{original.run();return true;});
    }
}
