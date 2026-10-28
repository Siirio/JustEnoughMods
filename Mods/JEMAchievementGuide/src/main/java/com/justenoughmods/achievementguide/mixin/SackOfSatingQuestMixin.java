package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.SackOfSatingItem", remap = false)
public abstract class SackOfSatingQuestMixin {
    @Unique private static final String jem$supplier = "JemQuestSackSupplierV4";
    @Unique private static final ThreadLocal<ArrayDeque<int[]>> jem$fillCalls = ThreadLocal.withInitial(ArrayDeque::new);
    @Unique private static final ThreadLocal<ArrayDeque<int[]>> jem$feedCalls = ThreadLocal.withInitial(ArrayDeque::new);
    @Shadow public static int getHunger(ItemStack sack) { throw new AssertionError(); }
    @Shadow public static boolean isExploding(ItemStack sack) { throw new AssertionError(); }

    @Inject(method = "m_142305_", at = @At("HEAD"))
    private void jem$beforeNativeFill(ItemStack sack, ItemStack food, Slot slot, ClickAction action, Player player,
                                      SlotAccess cursor, CallbackInfoReturnable<Boolean> callback) {
        if (player instanceof ServerPlayer) jem$fillCalls.get().push(new int[]{getHunger(sack), food.getCount()});
    }

    @Inject(method = "m_142305_", at = @At("RETURN"))
    private void jem$afterNativeFill(ItemStack sack, ItemStack food, Slot slot, ClickAction action, Player player,
                                     SlotAccess cursor, CallbackInfoReturnable<Boolean> callback) {
        if (!(player instanceof ServerPlayer actor)) return;
        var calls = jem$fillCalls.get();
        int[] before = calls.isEmpty() ? null : calls.pop();
        if (calls.isEmpty()) jem$fillCalls.remove();
        if (before != null && jem$real(actor) && callback.getReturnValue() && before[1] > 0 && food.isEmpty()
                && getHunger(sack) > before[0] && !isExploding(sack)) {
            sack.getOrCreateTag().putUUID(jem$supplier, actor.getUUID());
        }
    }

    @Inject(method = "m_6883_", at = @At("HEAD"))
    private void jem$beforeNativeFeed(ItemStack sack, Level level, Entity carrier, int slot, boolean selected, CallbackInfo callback) {
        if (carrier instanceof ServerPlayer actor) {
            jem$feedCalls.get().push(new int[]{getHunger(sack), actor.getFoodData().getFoodLevel()});
        }
    }

    @Inject(method = "m_6883_", at = @At("RETURN"))
    private void jem$afterNativeFeed(ItemStack sack, Level level, Entity carrier, int slot, boolean selected, CallbackInfo callback) {
        if (!(carrier instanceof ServerPlayer actor)) return;
        var calls = jem$feedCalls.get();
        int[] before = calls.isEmpty() ? null : calls.pop();
        if (calls.isEmpty()) jem$feedCalls.remove();
        var tag = sack.getTag();
        if (before != null && jem$real(actor) && !sack.isEmpty() && !isExploding(sack)
                && tag != null && tag.hasUUID(jem$supplier) && actor.getUUID().equals(tag.getUUID(jem$supplier))
                && before[0] - getHunger(sack) == 1 && actor.getFoodData().getFoodLevel() > before[1]) {
            tag.remove(jem$supplier);
            QuestActionTrigger.INSTANCE.emit(actor, "v3_cave_sack_food");
        }
    }

    @Unique private static boolean jem$real(ServerPlayer actor) {
        return !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof net.minecraftforge.common.util.FakePlayer);
    }
}
