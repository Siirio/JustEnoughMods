package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.GazingPearlItem.class,remap=false)
public abstract class CaveQuestPearlMixin {
    @Unique private static final ThreadLocal<Long> jem$caveResolved=new ThreadLocal<>();
    @Inject(method="m_6883_",at=@At("HEAD"))private void jem$before(ItemStack pearl,Level level,Entity holder,int slot,boolean held,CallbackInfo ci){jem$caveResolved.set(pearl.hasTag()?pearl.getTag().getLong("LastReputationTimestamp"):0);}
    @Inject(method="m_6883_",at=@At("RETURN"))private void jem$after(ItemStack pearl,Level level,Entity holder,int slot,boolean held,CallbackInfo ci){
        Long before=jem$caveResolved.get();jem$caveResolved.remove();
        if(before!=null && pearl.hasTag() && pearl.getTag().getBoolean("HasReputation") && pearl.getTag().getLong("LastReputationTimestamp")>before)CaveQuestProof.emit(holder,"v3_cave_deep_diplomacy_pearl");
    }
}
