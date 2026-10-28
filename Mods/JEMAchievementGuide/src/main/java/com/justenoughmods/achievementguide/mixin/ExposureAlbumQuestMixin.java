package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.SignedAlbumItem;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Signing commits on the native server packet handler, not AlbumMenu's client-only signAlbum method. */
@Pseudo
@Mixin(targets = "io.github.mortuusars.exposure.network.packet.serverbound.AlbumSignC2SP", remap = false)
public abstract class ExposureAlbumQuestMixin {
    @Redirect(method = "handle", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Inventory;m_6836_(ILnet/minecraft/world/item/ItemStack;)V"))
    private void jem$nativeAlbumSigned(Inventory inventory, int slot, ItemStack result, PacketFlow flow, Player user) {
        boolean unsigned = slot >= 0 && slot < inventory.getContainerSize()
                && inventory.getItem(slot).getItem() instanceof AlbumItem;
        inventory.setItem(slot, result);
        if (!unsigned || flow != PacketFlow.SERVERBOUND || !(user instanceof ServerPlayer actor)
                || inventory != actor.getInventory() || inventory.getItem(slot) != result
                || !(result.getItem() instanceof SignedAlbumItem album)) return;
        var content = album.getContent(result);
        if (content.title().isBlank() || !content.author().equals(actor.getScoreboardName())) return;
        boolean ownPhotograph = false, writtenText = false;
        for (var page : content.pages()) {
            writtenText |= !page.note().getString().isBlank();
            ItemStack photo = page.photograph();
            if (photo.getItem() instanceof PhotographItem photograph) {
                var frame = photograph.getFrame(photo);
                ownPhotograph |= frame.isTakenBy(actor)
                        && frame.identifier().getId().filter(id -> !id.isBlank()).isPresent();
            }
        }
        if (ownPhotograph && writtenText) QuestActionTrigger.INSTANCE.emit(actor, "v3_home_album");
    }
}
