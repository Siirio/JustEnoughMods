package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
import com.ninni.spawn.server.entity.mob.SeaCow;import net.minecraft.world.item.ItemStack;import net.minecraft.world.entity.player.Player;
@Pseudo @Mixin(targets="com.ninni.spawn.server.entity.mob.SeaCow",remap=false) public abstract class SpawnCareQuestSeaCowFeedMixin { 
 @Unique private ItemStack jem$food;@Unique private int jem$full;@Unique private net.minecraft.server.level.ServerPlayer jem$actor;
 @Inject(method="feed",at=@At("HEAD"),remap=false) private void jem$before(ItemStack food,Player player,CallbackInfo ci){jem$food=food.copy();jem$full=((SeaCow)(Object)this).getFullness();jem$actor=SpawnCareQuestProof.real(player);}
 @Inject(method="feed",at=@At("RETURN"),remap=false) private void jem$after(ItemStack food,Player player,CallbackInfo ci){var self=(SeaCow)(Object)this;if(jem$actor!=null&&self.isUnderWater()&&self.getFullness()>jem$full&&SpawnCareQuestProof.debited(jem$food,food,1))SpawnCareQuestProof.mark(self,"forage_actor",jem$actor.getUUID());jem$actor=null;}
 }
