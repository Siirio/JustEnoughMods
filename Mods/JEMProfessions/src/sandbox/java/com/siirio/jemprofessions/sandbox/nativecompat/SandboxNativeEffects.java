package com.siirio.jemprofessions.sandbox.nativecompat;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.ProfessionSavedData;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraftforge.network.simple.SimpleChannel;

/** Native owners stay optional: this entry point has no optional-mod class references in its API. */
public final class SandboxNativeEffects {
    private static final Map<ServerPlayer, ArrayDeque<Placement>> PLACEMENTS = new WeakHashMap<>();
    private SandboxNativeEffects() {}

    public static void registerPackets(SimpleChannel channel) { SandboxSchematicProjection.register(channel); }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(SandboxNativeEffects.class);
        if (ModList.get().isLoaded("irons_spellbooks")) SandboxMagicEffects.register();
        if (ModList.get().isLoaded("epicfight")) SandboxWarriorEffects.register();
        if (ModList.get().isLoaded("playerrevive")) SandboxReviveEffects.register();
    }

    public static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        if (!SandboxProfessionSupport.allowed(player)) return false;
        if (ModList.get().isLoaded("create") && SandboxEngineerEffects.activate(player, node, input)) return true;
        return ModList.get().isLoaded("irons_spellbooks") && SandboxMagicEffects.activate(player, node, input);
    }

    static boolean eligible(ServerPlayer player) {
        return SandboxProfessionSupport.allowed(player) && !player.isCreative() && !player.isSpectator() && player.isAlive();
    }

    static CompoundTag state(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains("JemSandboxNative")) persisted.put("JemSandboxNative", new CompoundTag());
        return persisted.getCompound("JemSandboxNative");
    }

    static boolean discovered(ServerPlayer player, String key) {
        boolean first = SandboxProfessionSupport.record(player).claimDiscovery(key);
        if (first) ProfessionSavedData.get(player.server).changed();
        return first;
    }

    static void engineerPlacement(ServerPlayer player, BlockPos pos, ResourceLocation block) {
        if (!eligible(player) || !"create".equals(block.getNamespace())) return;
        String location = player.level().dimension().location() + ":" + pos.asLong();
        if (!discovered(player, "engineer:position:" + location)) return;
        if (discovered(player, "engineer:component:" + block)) {
            SandboxProfessionSupport.credit(player, ProfessionId.ENGINEER, SourceKind.BLOCK_PLACED, block,
                    10, "engineer:first", Integer.MAX_VALUE, "engineer:first:" + block);
        }
        SandboxProfessionSupport.credit(player, ProfessionId.ENGINEER, SourceKind.BLOCK_PLACED, block,
                1, "engineer:work", 400, "engineer:position:" + location);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        ResourceLocation block = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (!"create".equals(block.getNamespace())) return;
        ArrayDeque<Placement> pending = PLACEMENTS.computeIfAbsent(player, p -> new ArrayDeque<>());
        if (pending.size() < 512) pending.addLast(new Placement(player.level().dimension().location(), event.getPos().immutable(), block));
    }

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !eligible(player) || event.getCrafting().isEmpty()) return;
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        if (!"create".equals(item.getNamespace())) return;
        // ItemCraftedEvent is raised by the native result-slot take, never by Create machine outputs.
        SandboxProfessionSupport.credit(player, ProfessionId.ENGINEER, SourceKind.ITEM_CRAFTED, item, 2,
                "engineer:work", 400, "engineer:craft:" + UUID.randomUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void defeated(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Enemy)
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || !eligible(player)) return;
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        int amount = Math.max(1, Math.min(20, (int) Math.ceil(event.getEntity().getMaxHealth() / 10.0F)));
        SandboxProfessionSupport.credit(player, ProfessionId.WARRIOR, SourceKind.MOB_DEFEATED, type, amount,
                "warrior:" + type, 120, "warrior:death:" + event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        ArrayDeque<Placement> pending = PLACEMENTS.remove(player);
        if (pending != null && eligible(player)) for (Placement placement : pending) {
            if (placement.dimension.equals(player.level().dimension().location()) && player.serverLevel().hasChunkAt(placement.pos)
                    && BuiltInRegistries.BLOCK.getKey(player.serverLevel().getBlockState(placement.pos).getBlock()).equals(placement.block))
                engineerPlacement(player, placement.pos, placement.block);
        }
        if (ModList.get().isLoaded("irons_spellbooks")) SandboxMagicEffects.tick(player);
        if (ModList.get().isLoaded("epicfight")) SandboxWarriorEffects.tick(player);
        SandboxSchematicProjection.tick(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PLACEMENTS.remove(player);
        SandboxSchematicProjection.logout(player);
        if (ModList.get().isLoaded("irons_spellbooks")) SandboxMagicEffects.logout(player);
        if (ModList.get().isLoaded("epicfight")) SandboxWarriorEffects.logout(player);
        if (ModList.get().isLoaded("playerrevive")) SandboxReviveEffects.logout(player);
        if (ModList.get().isLoaded("create")) SandboxEngineerEffects.logout(player);
    }

    private record Placement(ResourceLocation dimension, BlockPos pos, ResourceLocation block) {}
}
