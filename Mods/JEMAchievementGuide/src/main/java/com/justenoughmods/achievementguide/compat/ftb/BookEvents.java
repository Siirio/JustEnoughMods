package com.justenoughmods.achievementguide.compat.ftb;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Small event adapter; indexed predicates and native FTB own all quest progress. */
public final class BookEvents {
    private static long placementTick = Long.MIN_VALUE;
    private static final Set<String> PLACEMENTS = new HashSet<>();
    private static long interactionTick = Long.MIN_VALUE;
    private static final Set<String> INTERACTIONS = new HashSet<>();
    private BookEvents() {}

    private static void item(ServerPlayer player, String event, ItemStack stack, boolean offHand) {
        BookRules.event(player, event, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack, null, null, offHand);
    }

    private static void block(ServerPlayer player, String event, BlockState state, boolean offHand) {
        BookRules.event(player, event, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), null, null, state, offHand);
    }

    private static void entity(ServerPlayer player, String event, net.minecraft.world.entity.Entity entity) {
        BookRules.event(player, event, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), null, entity, null, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long tick = player.server.getTickCount();
        if (placementTick != tick) { placementTick = tick; PLACEMENTS.clear(); }
        // A bed/door is one placement action, even when Forge reports multiple blocks.
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiple) {
            Set<net.minecraft.world.level.block.Block> kinds = new HashSet<>();
            for (var snapshot : multiple.getReplacedBlockSnapshots()) {
                BlockState state = player.level().getBlockState(snapshot.getPos());
                if (kinds.add(state.getBlock())) placed(player, snapshot.getPos(), state);
            }
        } else placed(player, event.getPos(), event.getPlacedBlock());
    }

    private static void placed(ServerPlayer player, net.minecraft.core.BlockPos position, BlockState state) {
        if (PLACEMENTS.add(player.getUUID() + ":" + player.level().dimension().location() + ":" + position.asLong())) block(player, "place", state, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) block(player, "break", event.getState(), false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void use(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) item(player, "use_item", event.getItemStack(), event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == Event.Result.DENY || event.getUseItem() == Event.Result.DENY) return;
        if (event.getEntity() instanceof ServerPlayer player) block(player, "use_block", event.getLevel().getBlockState(event.getPos()), event.getHand() == net.minecraft.world.InteractionHand.OFF_HAND);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player) interacted(player, event.getTarget(), event.getHand());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player) interacted(player, event.getTarget(), event.getHand());
    }

    private static void interacted(ServerPlayer player, net.minecraft.world.entity.Entity target, net.minecraft.world.InteractionHand hand) {
        long tick = player.server.getTickCount();
        if (interactionTick != tick) { interactionTick = tick; INTERACTIONS.clear(); }
        if (INTERACTIONS.add(player.getUUID() + ":" + target.getUUID() + ":" + hand)) {
            BookRules.event(player, "interact_entity", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(), null, target, null,
                    hand == net.minecraft.world.InteractionHand.OFF_HAND);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player) entity(player, "mount", event.getEntityBeingMounted());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (!(event.getAmount() > 0) || !Float.isFinite(event.getAmount())) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            BookRules.event(player, "hit", BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString(),
                    null, event.getEntity(), null, false, event.getSource().getDirectEntity() == player
                            && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK));
        }
        if (event.getEntity() instanceof Mob victim && victim.getTarget() instanceof ServerPlayer player
                && event.getSource().getEntity() != null
                && BuiltInRegistries.ENTITY_TYPE.getKey(event.getSource().getEntity().getType()).toString().equals("guardvillagers:guard")) {
            entity(player, "guard_hit", event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) entity(player, "kill", event.getEntity());
    }

    @SubscribeEvent
    public static void consumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            item(player, "eat", event.getItem(), false);
            item(player, "drink", event.getItem(), false);
        }
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) item(player, "craft", event.getCrafting(), false);
    }

    @SubscribeEvent
    public static void smelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) item(player, "smelt", event.getSmelting(), false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) for (ItemStack stack : event.getDrops()) item(player, "fished", stack, false);
    }

    @SubscribeEvent
    public static void cloned(PlayerEvent.Clone event) {
        var receipts = event.getOriginal().getPersistentData().get(BookXpReward.RECEIPTS);
        if (receipts != null) event.getEntity().getPersistentData().put(BookXpReward.RECEIPTS, receipts.copy());
    }

    static void clear() {
        PLACEMENTS.clear(); placementTick = Long.MIN_VALUE;
        INTERACTIONS.clear(); interactionTick = Long.MIN_VALUE;
    }
}
