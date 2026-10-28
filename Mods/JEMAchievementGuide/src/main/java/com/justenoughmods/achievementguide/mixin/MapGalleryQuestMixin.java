package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class MapGalleryQuestMixin {
    @Unique private boolean jem$emptyBefore;
    @Unique private ItemStack jem$mapBefore = ItemStack.EMPTY;
    @Unique private int jem$countBefore;

    @Inject(method = "interact", at = @At("HEAD"))
    private void jem$beforeInsert(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        jem$emptyBefore = ((ItemFrame) (Object) this).getItem().isEmpty();
        jem$mapBefore = player.getItemInHand(hand).copy();
        jem$countBefore = jem$mapBefore.getCount();
    }

    @Inject(method = "interact", at = @At("RETURN"))
    private void jem$afterInsert(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        ItemFrame frame = (ItemFrame) (Object) this;
        if (player instanceof ServerPlayer actor && jem$emptyBefore && jem$mapBefore.is(Items.FILLED_MAP)
                && callback.getReturnValue().consumesAction() && frame.getItem().is(Items.FILLED_MAP)
                && java.util.Objects.equals(MapItem.getMapId(jem$mapBefore), MapItem.getMapId(frame.getItem()))
                && player.getItemInHand(hand).getCount() == jem$countBefore - 1) {
            MapItemSavedData map = MapItem.getSavedData(frame.getItem(), actor.serverLevel());
            if (map != null) {
                for (byte color : map.colors) {
                    if (color != 0) {
                        QuestActionTrigger.INSTANCE.emit(actor, "v3_qol_map_gallery");
                        break;
                    }
                }
            }
        }
        jem$mapBefore = ItemStack.EMPTY;
    }
}
