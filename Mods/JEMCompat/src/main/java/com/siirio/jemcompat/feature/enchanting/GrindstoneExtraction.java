package com.siirio.jemcompat.feature.enchanting;

import com.siirio.jemcompat.JEMCompat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** One sneaking main-hand click extracts the first displayed enchantment. */
@Mod.EventBusSubscriber(modid = JEMCompat.MOD_ID)
public final class GrindstoneExtraction {
    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(Blocks.GRINDSTONE)) return;
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.is(Items.ENCHANTED_BOOK) || held.is(Items.BOOK)) return;
        CompoundTag tag = held.getTag();
        if (tag == null || !tag.contains("Enchantments", 9)) return;
        ListTag enchantments = tag.getList("Enchantments", 10);
        if (enchantments.isEmpty()) return;

        List<List<EnchantmentCostHistory.Credit>> allCredits = new ArrayList<>();
        for (int index = 0; index < enchantments.size(); index++) {
            CompoundTag entry = enchantments.getCompound(index);
            List<EnchantmentCostHistory.Credit> credits = EnchantmentCostHistory.credits(held,
                    entry.getString("id"), entry.getInt("lvl"));
            if (credits.isEmpty()) return; // Existing untracked equipment retains its normal behavior.
            allCredits.add(credits);
        }

        CompoundTag first = enchantments.getCompound(0);
        var id = net.minecraft.resources.ResourceLocation.tryParse(first.getString("id"));
        var enchantment = id == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(id);
        int levelValue = first.getInt("lvl");
        if (enchantment == null || levelValue <= 0) return;
        EnchantmentRefundData refunds = EnchantmentRefundData.get(player.server);
        for (var credits : allCredits) if (!refunds.canRedeem(credits)) return;

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, levelValue));
        ItemEntity dropped = new ItemEntity(level, event.getPos().getX() + 0.5,
                event.getPos().getY() + 1.0, event.getPos().getZ() + 0.5, book);
        if (!level.addFreshEntity(dropped)) return;

        List<EnchantmentCostHistory.Credit> credits = allCredits.get(0);
        refunds.redeem(credits);
        enchantments.remove(0);
        if (enchantments.isEmpty()) tag.remove("Enchantments");
        else tag.put("Enchantments", enchantments);
        EnchantmentCostHistory.remove(held, first.getString("id"));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        long paid = credits.stream().mapToLong(EnchantmentCostHistory.Credit::paidXp).sum();
        int returned = (int) Math.min(Integer.MAX_VALUE, paid * 70L / 100L);
        if (returned > 0) {
            if (net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers"))
                com.siirio.jemworldbosstiers.api.ExperienceRewardApi.transfer(player, returned);
            else player.giveExperiencePoints(returned);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private GrindstoneExtraction() {}
}
