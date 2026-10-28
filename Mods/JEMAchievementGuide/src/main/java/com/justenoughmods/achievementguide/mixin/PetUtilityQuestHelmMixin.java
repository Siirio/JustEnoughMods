package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.fruityspikes.whaleborne.server.entities.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=HelmEntity.class,remap=false)
public abstract class PetUtilityQuestHelmMixin {
 @Unique private Map<AnchorEntity,Boolean> jem$anchors;
 @Unique private ServerPlayer jem$actor;
 @Inject(method="m_7199_",at=@At("HEAD"))private void jem$before(int power,CallbackInfo ci){var helm=(HelmEntity)(Object)this;jem$actor=helm.getControllingPassenger() instanceof ServerPlayer actor?PetUtilityQuestProof.actor(actor):null;jem$anchors=new IdentityHashMap<>();if(jem$actor!=null && helm.getVehicle() instanceof HullbackEntity whale && jem$actor.getVehicle()==helm)for(var passenger:whale.getPassengers())if(passenger instanceof AnchorEntity anchor)jem$anchors.put(anchor,anchor.isDown());}
 @Inject(method="m_7199_",at=@At("RETURN"))private void jem$changed(int power,CallbackInfo ci){var helm=(HelmEntity)(Object)this;var actor=jem$actor;var anchors=jem$anchors;jem$actor=null;jem$anchors=null;if(actor==null || anchors==null || actor.getVehicle()!=helm || !(helm.getVehicle() instanceof HullbackEntity whale))return;for(var row:anchors.entrySet())if(row.getKey().isDown()!=row.getValue() && PetUtilityQuestWhale.widget(row.getKey(),actor,whale))PetUtilityQuestProof.emit(actor,"v3_pet_whale_widgets");}
}
