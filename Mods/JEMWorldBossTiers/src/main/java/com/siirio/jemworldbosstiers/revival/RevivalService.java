package com.siirio.jemworldbosstiers.revival;

import com.siirio.jemworldbosstiers.api.WorldTierApi;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.progression.WorldTierData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RevivalService {
    private static final String ARENA_OFFERING = "arena_offering";

    @SubscribeEvent
    public void useArenaOffering(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        WorldTierData data = WorldTierData.get(level.getServer());
        String dimension = level.dimension().location().toString();
        ArenaRecord arena = data.arenas().stream().filter(value -> value.contains(dimension, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ())).findFirst().orElse(null);
        if (arena == null || !arena.unlocked() || arena.activeEncounterId() != null) {
            return;
        }
        BossProfile profile = BalanceRegistry.bossByKey(arena.profileKey()).orElse(null);
        if (profile == null || !ARENA_OFFERING.equals(profile.revivalStrategy()) || profile.revivalOffering() == null || !event.getItemStack().is(BuiltInRegistries.ITEM.get(profile.revivalOffering())) || player.experienceLevel < profile.revivalXpLevels()) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(profile.entityIds().get(0));
        Entity entity = type.create(level);
        if (!(entity instanceof net.minecraft.world.entity.LivingEntity boss)) {
            return;
        }
        boss.moveTo(event.getPos().getX() + 0.5D, event.getPos().getY() + 1.0D, event.getPos().getZ() + 0.5D, player.getYRot(), 0.0F);
        ArenaRecord active = arena.activate(boss.getUUID(), level.getGameTime()).orElse(null);
        if (active == null) {
            return;
        }
        data.putArena(active);
        WorldTierApi.markRevivalEncounter(boss, arena.id());
        if (!level.addFreshEntity(boss)) {
            data.putArena(active.release(boss.getUUID()));
            return;
        }
        data.cancelRespawn(arena.id());
        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
            player.giveExperienceLevels(-profile.revivalXpLevels());
        }
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new com.siirio.jemworldbosstiers.event.HostedEncounterAvailableEvent(player, boss));
    }
}
