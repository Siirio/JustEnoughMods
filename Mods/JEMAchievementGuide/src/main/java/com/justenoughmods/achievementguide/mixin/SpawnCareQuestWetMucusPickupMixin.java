package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.spawncare.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class SpawnCareQuestWetMucusPickupMixin {
    @Unique private net.minecraft.world.item.ItemStack jem$variant;
    @Unique private int jem$count;@Unique private boolean jem$wet;@Unique private int jem$known;
    @Unique private int jem$inventory;
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void jem$before(Player player, CallbackInfo callback) { ItemEntity self = (ItemEntity)(Object)this; jem$wet="v3_pet_wet_work".equals(self.getPersistentData().getCompound(com.justenoughmods.achievementguide.integration.food.FoodDroppedProof.KEY).getString("action")) && player.getUUID().equals(com.justenoughmods.achievementguide.integration.food.FoodDroppedProof.actor(self));jem$known=com.justenoughmods.achievementguide.integration.food.FoodDroppedProof.known(self);jem$variant = self.getItem().copy(); jem$count = jem$variant.getCount(); jem$inventory = player instanceof ServerPlayer actor ? SpawnCareQuestProof.count(SpawnCareQuestProof.held(actor), self.getItem()) : 0; }
    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void jem$after(Player player, CallbackInfo callback) {
        var actor = SpawnCareQuestProof.real(player); ItemEntity self = (ItemEntity)(Object)this; if (actor == null) return; var proof = SpawnCareQuestProof.data(self);
        int removed = jem$count - (self.isRemoved() ? 0 : self.getItem().getCount());
        if (removed > 0 && jem$wet && jem$known>0
                && SpawnCareQuestProof.count(SpawnCareQuestProof.held(actor), jem$variant) - jem$inventory == removed) SpawnCareQuestProof.data(actor).putUUID("wet_snail_collected", proof.hasUUID("wet_snail")?proof.getUUID("wet_snail"):self.getUUID());
    }
}
