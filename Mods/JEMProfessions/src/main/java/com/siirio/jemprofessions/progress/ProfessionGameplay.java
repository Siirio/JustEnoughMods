package com.siirio.jemprofessions.progress;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.api.ProfessionProgressRequest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ProfessionGameplay {
    private static final Set<UUID> VEIN_OPERATIONS = ConcurrentHashMap.newKeySet();
    private ProfessionGameplay() {}

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || player.isCreative()) return;
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        if (event.getPlacedBlock().is(Tags.Blocks.ORES)) data.markPlayerPlaced(positionKey(player, event.getPos()));
        withRecord(player, (record, saved) -> {
            if (record.claimDiscovery("architect:material:" + block)) {
                saved.changed();
                credit(player, ProfessionId.ARCHITECT, SourceKind.BLOCK_PLACED, block, 5);
            }
            if ("create".equals(block.getNamespace()) && record.claimDiscovery("engineer:component:" + block)) {
                saved.changed();
                credit(player, ProfessionId.ENGINEER, SourceKind.BLOCK_PLACED, block, 10);
            }
        });
    }

    @SubscribeEvent
    public static void blockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer || player.isCreative() || player.isSpectator()) return;
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (event.getState().getBlock() instanceof CropBlock crop && crop.isMaxAge(event.getState())) {
            dailyCredit(player, ProfessionId.FARMER, SourceKind.CROP_HARVEST, block, 2, "farmer:crops", 500);
        } else if (event.getState().is(Tags.Blocks.ORES) && player.hasCorrectToolForDrops(event.getState())) {
            ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
            String key = positionKey(player, event.getPos());
            if (data.wasPlayerPlaced(key)) data.removePlaced(key);
            else {
                dailyCredit(player, ProfessionId.MINER, SourceKind.BLOCK_BREAK, block, 3, "miner:ores", 750);
                if (player.isShiftKeyDown() && !VEIN_OPERATIONS.contains(player.getUUID())
                        && ProfessionApi.level(player, ProfessionId.MINER) >= 15
                        && ProfessionApi.enabled(player, ProfessionId.MINER, "vein_miner")) {
                    mineVein(player, event.getPos(), event.getState().getBlock(), ProfessionApi.level(player, ProfessionId.MINER) >= 50 ? 32 : 8);
                }
            }
        }
    }

    @SubscribeEvent
    public static void oreSense(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || !player.isShiftKeyDown()) return;
        if (!event.getItemStack().is(ItemTags.PICKAXES) || ProfessionApi.level(player, ProfessionId.MINER) < 5
                || !ProfessionApi.enabled(player, ProfessionId.MINER, "ore_sense")) return;
        withRecord(player, (record, data) -> {
            long now = player.serverLevel().getGameTime();
            if (record.cooldown("miner:ore_sense") > now) {
                player.sendSystemMessage(Component.translatable("jem.professions.ore_sense.cooldown",
                        (record.cooldown("miner:ore_sense") - now + 19L) / 20L), true);
                return;
            }
            record.cooldown("miner:ore_sense", now + 900L);
            data.changed();
            boolean found = false;
            BlockPos origin = player.blockPosition();
            outer: for (int x = -6; x <= 6; x++) for (int y = -6; y <= 6; y++) for (int z = -6; z <= 6; z++) {
                BlockPos pos = origin.offset(x, y, z);
                if (!player.serverLevel().hasChunkAt(pos)) continue;
                if (player.serverLevel().getBlockState(pos).is(Tags.Blocks.ORES) && !data.wasPlayerPlaced(positionKey(player, pos))) {
                    found = true; break outer;
                }
            }
            player.sendSystemMessage(Component.translatable(found ? "jem.professions.ore_sense.yes" : "jem.professions.ore_sense.no"), true);
        });
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void fished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || event.getDrops().isEmpty()) return;
        ItemStack ordinary = event.getDrops().stream().filter(stack -> stack.is(ItemTags.FISHES)).findFirst().orElse(ItemStack.EMPTY);
        if (ordinary.isEmpty()) return;
        ResourceLocation fish = BuiltInRegistries.ITEM.getKey(ordinary.getItem());
        withRecord(player, (record, data) -> {
            int amount = record.claimDiscovery("fisher:species:" + fish) ? 12 : record.reserveDaily("fisher:repeat", utcDay(), 3, 300);
            data.changed();
            if (amount > 0) credit(player, ProfessionId.FISHER, SourceKind.FISH_CAUGHT, fish, amount);
            if (record.level(ProfessionId.FISHER) >= 50 && player.getRandom().nextFloat() < 0.10F) {
                ItemStack extra = ordinary.copy();
                extra.setCount(1);
                event.getDrops().add(extra);
            }
        });
    }

    @SubscribeEvent
    public static void defeated(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer || event.getEntity() == player) return;
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        int base = Math.max(1, Math.min(20, (int) Math.ceil(event.getEntity().getMaxHealth() / 10.0F)));
        dailyCredit(player, ProfessionId.WARRIOR, SourceKind.MOB_DEFEATED, type, base, "warrior:" + type, 120);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0
                || player instanceof FakePlayer || player.isSpectator()) return;
        String chunk = "explorer:chunk:" + player.level().dimension().location() + ":" + player.chunkPosition().x + ":" + player.chunkPosition().z;
        withRecord(player, (record, data) -> {
            if (!record.claimDiscovery(chunk)) return;
            int amount = record.reserveDaily("explorer:chunks", utcDay(), 2, 200);
            data.changed();
            if (amount > 0) credit(player, ProfessionId.EXPLORER, SourceKind.AREA_DISCOVERED,
                    player.level().dimension().location(), amount);
        });
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = ProfessionApi.level(player, ProfessionId.MINER);
        if (level >= 10 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                && player.level().getBlockState(player.blockPosition().below()).is(Tags.Blocks.STONE)) {
            event.setAmount(event.getAmount() * 0.90F);
        }
        if (level >= 30 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            withRecord(player, (record, data) -> {
                long now = player.serverLevel().getGameTime();
                if (record.cooldown("miner:lava_shield") > now) return;
                record.cooldown("miner:lava_shield", now + 12_000L);
                data.changed();
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, false, true));
            });
        }
    }

    @SubscribeEvent
    public static void breathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.canBreathe()
                || ProfessionApi.level(player, ProfessionId.MINER) < 40) return;
        if (player.getY() >= player.level().getSeaLevel() - 16 || !player.isUnderWater()) return;
        if (player.getRandom().nextFloat() < 0.15F) event.setConsumeAirAmount(0);
    }

    private static void dailyCredit(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source,
                                    int requested, String counter, int cap) {
        withRecord(player, (record, data) -> {
            int amount = record.reserveDaily(counter, utcDay(), requested, cap);
            if (amount <= 0) return;
            data.changed();
            credit(player, profession, kind, source, amount);
        });
    }

    private static void credit(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source, int amount) {
        ProfessionApi.credit(player, new ProfessionProgressRequest(profession, kind, source, amount, UUID.randomUUID()));
    }

    private static void withRecord(ServerPlayer player, RecordConsumer consumer) {
        var principal = IdentityPolicy.resolve(player);
        if (principal.isEmpty()) return;
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        consumer.accept(data.record(principal.get().accountId()), data);
    }

    private static long utcDay() { return LocalDate.now(ZoneOffset.UTC).toEpochDay(); }
    private static String positionKey(ServerPlayer player, BlockPos pos) {
        return player.level().dimension().location() + ":" + pos.asLong();
    }
    private static void mineVein(ServerPlayer player, BlockPos start, net.minecraft.world.level.block.Block target, int limit) {
        ProfessionSavedData data = ProfessionSavedData.get(player.getServer());
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(start); visited.add(start);
        VEIN_OPERATIONS.add(player.getUUID());
        try {
            int destroyed = 1;
            while (!pending.isEmpty() && destroyed < limit && !player.getMainHandItem().isEmpty()) {
                BlockPos current = pending.removeFirst();
                for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!visited.add(next) || !player.serverLevel().hasChunkAt(next)) continue;
                    if (!player.serverLevel().getBlockState(next).is(target) || data.wasPlayerPlaced(positionKey(player, next))) continue;
                    pending.addLast(next);
                    if (next.equals(start)) continue;
                    if (player.gameMode.destroyBlock(next)) destroyed++;
                    if (destroyed >= limit) break;
                }
            }
        } finally {
            VEIN_OPERATIONS.remove(player.getUUID());
        }
    }
    @FunctionalInterface private interface RecordConsumer { void accept(ProfessionRecord record, ProfessionSavedData data); }
}
