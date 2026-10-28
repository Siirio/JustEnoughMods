package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestContext;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestWork;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCrow$AIDepositChests", remap = false)
public abstract class WildlifeQuestCrowDepositMixin {
    @Shadow private ItemFrame targetEntity;
    @Unique private WildlifeQuestWork.Deposit jem$deposit;
    @Inject(method = "m_8037_", at = @At("HEAD"))
    private void jem$before(CallbackInfo callback) {
        jem$deposit = null;
        if (targetEntity != null && WildlifeQuestContext.current() instanceof EntityCrow crow) {
            var actor = WildlifeQuestProof.owner(crow);
            if (actor != null && crow.getCommand() == 3) jem$deposit = new WildlifeQuestWork.Deposit(crow, actor, targetEntity);
        }
    }
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$inserted(IItemHandler handler, ItemStack input, boolean simulate) {
        ItemStack before = input.copy(); int old = simulate ? 0 : WildlifeQuestWork.count(handler, before);
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, input, simulate);
        if (!simulate && jem$deposit != null && ItemStack.isSameItemSameTags(before, jem$deposit.before)
                && before.getCount() - remainder.getCount() > 0 && WildlifeQuestWork.count(handler, before) - old == before.getCount() - remainder.getCount()) {
            jem$deposit.handler = handler; jem$deposit.inventoryBefore = old; jem$deposit.inserted = before.getCount() - remainder.getCount();
        }
        return remainder;
    }
    @Inject(method = "m_8037_", at = @At("RETURN"))
    private void jem$after(CallbackInfo callback) { var run = jem$deposit; jem$deposit = null; WildlifeQuestWork.deposited(run); }
}
