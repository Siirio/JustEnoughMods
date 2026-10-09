package com.siirio.jemclaims;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod("jem_claims")
public final class JemClaims {
    private static final Map<UUID, Selection> selections = new HashMap<>();

    public JemClaims() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ClaimsConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClaimSelectionGuard());
        if (net.minecraftforge.fml.ModList.get().isLoaded("flan")) FlanBridge.registerEvents();
    }

    public record Selection(ResourceKey<Level> dimension, BlockPos first, BlockPos second) {}

    public static Selection selection(ServerPlayer player) {
        Selection selection = selections.get(player.getUUID());
        return selection != null && selection.dimension().equals(player.level().dimension()) ? selection : null;
    }

    private static boolean tool(ItemStack stack) {
        if (!stack.is(Items.STICK) || !stack.hasCustomHoverName() || !stack.getHoverName().getString().equals(ClaimsConfig.TOOL_NAME.get())) return false;
        stack.getOrCreateTag().putBoolean("jem_claim_tool", true);
        if (!stack.isEnchanted()) {
            stack.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
            stack.hideTooltipPart(ItemStack.TooltipPart.ENCHANTMENTS);
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void anvil(net.minecraftforge.event.AnvilUpdateEvent event) {
        if (!event.getLeft().is(Items.STICK) || !event.getRight().isEmpty()
                || !ClaimsConfig.TOOL_NAME.get().equals(event.getName())) return;
        ItemStack output = event.getLeft().copy();
        output.setHoverName(Component.literal(event.getName()));
        tool(output);
        event.setOutput(output);
        event.setCost(Math.max(1, event.getLeft().getBaseRepairCost() + 1));
    }

    @SubscribeEvent
    public void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        tool(player.getMainHandItem());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void left(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !tool(event.getItemStack())) return;
        event.setCanceled(true);
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!ClaimSelectionGuard.click(player, event.getPos())) return;
        selections.put(player.getUUID(), new Selection(player.level().dimension(), event.getPos().immutable(), null));
        player.sendSystemMessage(Component.literal("Первая точка: " + event.getPos().getX() + ", " + event.getPos().getZ()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void breaking(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && tool(player.getMainHandItem())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND || !tool(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player.isShiftKeyDown()) { open(player); return; }
        if (!ClaimSelectionGuard.click(player, event.getPos())) return;
        Selection selection = selections.get(player.getUUID());
        if (selection == null || !selection.dimension().equals(player.level().dimension())) {
            player.sendSystemMessage(Component.literal("Сначала выберите первую точку ЛКМ в этом измерении."));
            return;
        }
        BlockPos second = event.getPos().immutable();
        if (!ClaimSelectionGuard.validate(player, selection.first(), second)) return;
        Selection pending = new Selection(selection.dimension(), selection.first(), second);
        selections.put(player.getUUID(), pending);
        long width = Math.abs((long) selection.first().getX() - second.getX()) + 1;
        long length = Math.abs((long) selection.first().getZ() - second.getZ()) + 1;
        VanillaMenus.chest(player, "confirm", "Создать территорию?", menu -> {
            menu.button(4, ClaimsMenus.location(VanillaMenus.icon(Items.OAK_FENCE, width + " × " + length,
                    "Площадь: " + width * length, "Новых блоков: " + ClaimUnion.additional(player.serverLevel(), player.getUUID(), pending.first(), pending.second(), null), "Доступно: " + (FlanBridge.budget(player) - FlanBridge.used(player))), pending.dimension().location(), pending.first(), pending.second()), null);
            menu.button(22, VanillaMenus.icon(Items.LIME_DYE, "Подтвердить"), () -> {
                if (selections.get(player.getUUID()) != pending || !player.level().dimension().equals(pending.dimension())) return;
                if (FlanBridge.create(player, pending.first(), pending.second()) != null) selections.remove(player.getUUID());
                player.closeContainer();
            });
            menu.button(49, VanillaMenus.icon(Items.ARROW, "Отмена"), () -> {
                selections.remove(player.getUUID());
                player.closeContainer();
            });
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void rightAir(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !tool(event.getItemStack())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        open(player);
    }

    private static void open(ServerPlayer player) {
        ClaimsMenus.overview(player, 0);
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("claims").executes(context -> {
            open(context.getSource().getPlayerOrException());
            return 1;
        }));
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        selections.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void save(LevelEvent.Save event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && level.dimension() == Level.OVERWORLD) ClaimsData.get(level.getServer()).prune(level.getServer());
    }
}
