package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import com.yungnickyoung.minecraft.ribbits.module.ItemModule;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="com.yungnickyoung.minecraft.ribbits.entity.goal.RibbitPlayMusicGoal",remap=false)
public abstract class PetUtilityQuestRibbitBandMixin {
 @Shadow @Final private RibbitEntity ribbit;
 @Inject(method="m_8037_",at=@At("RETURN"))private void jem$nativeBand(CallbackInfo ci){if(ribbit.level().isClientSide || !ribbit.isAlive() || !ribbit.isMasterRibbit() || !ribbit.getPlayingInstrument() || ribbit.getRibbitsPlayingMusic().stream().noneMatch(r->r.isAlive() && r.getMasterRibbit()==ribbit && r.getPlayingInstrument()))return;var performers=PetUtilityQuestInstrumentAccess.jem$petPerformers();for(var listener:ribbit.getPlayersHearingMusic())if(listener instanceof ServerPlayer p && PetUtilityQuestProof.actor(p)!=null && p.isUsingItem() && p.getUseItem().is(ItemModule.MARACA.get()) && performers.get(p)!=null && performers.get(p).contains(p))PetUtilityQuestProof.emit(p,"v3_pet_swamp_band");}
}
