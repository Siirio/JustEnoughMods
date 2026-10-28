package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.justenoughmods.achievementguide.integration.food.FoodTransferSnapshots.SeashellReceipt;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the native loot consumer followed by the native shell debit proves opening. */
@Pseudo
@Mixin(targets = "net.satisfy.beachparty.core.item.SeashellItem", remap = false)
public abstract class SeashellFoodQuestMixin {
    @Unique private final ThreadLocal<SeashellReceipt> jem$opening = new ThreadLocal<>();

    @Inject(method = "m_7203_", at = @At("HEAD"), remap = false)
    private void jem$begin(Level level, Player player, InteractionHand hand,
                           CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        jem$opening.remove();
        if (player instanceof ServerPlayer actor && !(actor instanceof FakePlayer)
                && !actor.isSpectator() && !actor.getAbilities().instabuild
                && !actor.getItemInHand(hand).isEmpty() && actor.getItemInHand(hand).getItem() == (Object)this) {
            jem$opening.set(new SeashellReceipt(actor.getUUID(), actor.getItemInHand(hand)));
        }
    }

    @Redirect(method = "m_7203_", at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;forEach(Ljava/util/function/Consumer;)V"), remap = false)
    private void jem$nativeLoot(ObjectArrayList<ItemStack> loot, Consumer<? super ItemStack> nativeConsumer,
                                Level level, Player player, InteractionHand hand) {
        SeashellReceipt receipt = jem$opening.get();
        if (!(player instanceof ServerPlayer actor) || receipt == null || !receipt.actor().equals(actor.getUUID())) {
            loot.forEach(nativeConsumer);
            return;
        }
        loot.forEach(stack -> {
            ItemStack output = stack.copy();
            long before = jem$held(actor, output);
            nativeConsumer.accept(stack);
            long after = jem$held(actor, output);
            if (!output.isEmpty() && before >= 0 && after > before && after - before <= output.getCount()) {
                receipt.receipt();
            }
        });
    }

    @Inject(method = "m_7203_", at = @At("RETURN"), remap = false)
    private void jem$finished(Level level, Player player, InteractionHand hand,
                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        SeashellReceipt receipt = jem$opening.get();
        jem$opening.remove();
        if (player instanceof ServerPlayer actor && receipt != null && receipt.actor().equals(actor.getUUID())
                && receipt.received() && callback.getReturnValue().getObject() == receipt.shell()
                && receipt.shell().getCount() == receipt.count() - 1) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_beach_treasure");
        }
    }

    @Unique
    private static long jem$held(ServerPlayer actor, ItemStack output) {
        int slots = actor.getInventory().getContainerSize();
        if (output.isEmpty() || slots > 256) return -1;
        long count = 0;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = actor.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, output)) count += stack.getCount();
        }
        return count;
    }

}
