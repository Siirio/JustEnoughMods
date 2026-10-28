package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityTransmutationTable;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.inventory.MenuTransmutationTable", remap = false)
public abstract class WildlifeQuestTransmutationMixin {
    @Shadow @Final private Slot transmuteSlot;
    @Shadow private TileEntityTransmutationTable table;
    @Unique private int jem$xp;
    @Unique private ItemStack jem$input;
    @Unique private ItemStack jem$expected;
    @Inject(method = "transmute", at = @At("HEAD"))
    private void jem$before(Player player, int button, CallbackInfo callback) {
        jem$xp = player.experienceLevel; jem$input = transmuteSlot.getItem().copy(); jem$expected = table == null ? ItemStack.EMPTY : table.getPossibility(button).copy();
    }
    @Inject(method = "transmute", at = @At("RETURN"))
    private void jem$after(Player player, int button, CallbackInfo callback) {
        var actor = WildlifeQuestProof.real(player); if (actor == null || jem$input.isEmpty() || jem$expected.isEmpty() || table == null || AMConfig.transmutingExperienceCost <= 0) return;
        ItemStack actual = transmuteSlot.getItem();
        int count = Math.max(1, (int)Math.floor((double)jem$input.getCount() / ((double)jem$input.getMaxStackSize() / jem$expected.getMaxStackSize())));
        if (actor.experienceLevel == jem$xp - AMConfig.transmutingExperienceCost && actual.getCount() == count && ItemStack.isSameItemSameTags(actual, jem$expected))
            WildlifeQuestProof.emit(actor, "v3_fauna_far_exchange");
    }
}
