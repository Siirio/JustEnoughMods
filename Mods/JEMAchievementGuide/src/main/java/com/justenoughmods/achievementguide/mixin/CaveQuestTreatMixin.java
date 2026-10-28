package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.item.BiomeTreatItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BiomeTreatItem.class,remap=false)
public abstract class CaveQuestTreatMixin {
    @Unique private static final ThreadLocal<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>> jem$caveBind=new ThreadLocal<>();
    @Inject(method="m_5922_",at=@At("HEAD"))private void jem$before(ItemStack stack,Level world,LivingEntity user,CallbackInfoReturnable<ItemStack> ci){
        jem$caveBind.remove();var actor=CaveQuestProof.actor(user);
        if(actor!=null && actor.getFoodData().getFoodLevel()==0 && BiomeTreatItem.getCaveBiome(stack)==null && stack.getCount()==1)world.getBiome(actor.blockPosition()).unwrapKey().ifPresent(jem$caveBind::set);
    }
    @Inject(method="m_5922_",at=@At("RETURN"))private void jem$bound(ItemStack stack,Level world,LivingEntity user,CallbackInfoReturnable<ItemStack> ci){
        var biome=jem$caveBind.get();jem$caveBind.remove();var actor=CaveQuestProof.actor(user);var result=ci.getReturnValue();
        if(actor!=null && biome!=null && result!=stack && result.getCount()==1 && biome.equals(BiomeTreatItem.getCaveBiome(result)))CaveQuestStackProof.put(result,actor.getUUID(),java.util.UUID.randomUUID(),"treat");
    }
}
