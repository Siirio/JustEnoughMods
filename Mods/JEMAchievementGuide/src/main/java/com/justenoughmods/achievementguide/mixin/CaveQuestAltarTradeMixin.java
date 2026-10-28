package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.DeepOneBaseEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.AbyssalAltarBlockEntity;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbyssalAltarBlockEntity.class,remap=false)
public abstract class CaveQuestAltarTradeMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/entity/living/DeepOneBaseEntity;addReputation(Ljava/util/UUID;I)V"))
    private static void jem$actualExchange(DeepOneBaseEntity trader,UUID player,int amount,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,AbyssalAltarBlockEntity altar){
        int before=trader.getReputationOf(player);trader.addReputation(player,amount);var proof=altar.getPersistentData().getCompound("jemCaveOfferingV4");
        if(proof.hasUUID("actor") && proof.getUUID("actor").equals(player) && trader.getReputationOf(player)>before
                && net.minecraft.world.item.ItemStack.isSameItemSameTags(trader.getMainHandItem(),net.minecraft.world.item.ItemStack.of(proof.getCompound("item")))){
            trader.getPersistentData().putUUID("jemCaveTradeActorV4",player);trader.getPersistentData().putLong("jemCaveTradeAltarV4",pos.asLong());altar.getPersistentData().remove("jemCaveOfferingV4");altar.setChanged();
        }
    }
    @Inject(method="onEntityInteract",at=@At("RETURN"))private void jem$actualLoot(LivingEntity entity,boolean takeOffering,CallbackInfo ci){
        var altar=(AbyssalAltarBlockEntity)(Object)this;
        if(!takeOffering && entity instanceof DeepOneBaseEntity trader && trader.getPersistentData().getLong("jemCaveTradeAltarV4")==altar.getBlockPos().asLong()
                && trader.getPersistentData().hasUUID("jemCaveTradeActorV4") && !altar.getItem(0).isEmpty()){
            var actor=CaveQuestProof.owner(trader,"jemCaveTradeActorV4");if(actor!=null)CaveQuestProof.emit(actor,"v3_cave_deep_diplomacy");trader.getPersistentData().remove("jemCaveTradeActorV4");}
    }
}
