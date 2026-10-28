package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import com.fruityspikes.whaleborne.server.entities.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=CannonEntity.class,remap=false)
public abstract class PetUtilityQuestCannonMixin {
 @Unique private ServerPlayer jem$actor;
 @Unique private int jem$fuel;
 @Unique private int jem$ammo;
 @Unique private boolean jem$projectile;
 @Inject(method="fireCannon",at=@At("HEAD"))private void jem$before(int power,CallbackInfo ci){var cannon=(CannonEntity)(Object)this;jem$actor=cannon.getControllingPassenger() instanceof ServerPlayer actor?PetUtilityQuestProof.actor(actor):null;jem$fuel=cannon.inventory.getItem(1).is(Items.GUNPOWDER)?cannon.inventory.getItem(1).getCount():0;jem$ammo=cannon.inventory.getItem(0).getCount();jem$projectile=false;}
 @Redirect(method="fireCannon",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))private boolean jem$shot(Level l,Entity e){boolean accepted=l.addFreshEntity(e);if(accepted && e instanceof Projectile projectile && jem$actor!=null && projectile.getOwner()==jem$actor)jem$projectile=true;return accepted;}
 @Inject(method="fireCannon",at=@At("RETURN"))private void jem$fired(int power,CallbackInfo ci){var cannon=(CannonEntity)(Object)this;var actor=jem$actor;jem$actor=null;if(actor!=null && jem$projectile && actor.getVehicle()==cannon && cannon.getVehicle() instanceof HullbackEntity whale && jem$fuel-cannon.inventory.getItem(1).getCount()==1 && jem$ammo-cannon.inventory.getItem(0).getCount()==1 && PetUtilityQuestWhale.widget(cannon,actor,whale))PetUtilityQuestProof.emit(actor,"v3_pet_whale_widgets");jem$projectile=false;}
}
