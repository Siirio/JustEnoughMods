package com.siirio.jemserver.smp;

import com.siirio.jemworldbosstiers.api.*;
import com.siirio.jemworldbosstiers.event.HostedEncounterClosedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.*;

public final class HostedParties {
    public static void start(ServerPlayer host, PartyState row) {
        var data = SmpData.get(host.server);
        data.edit(() -> {
            var current = data.parties().bind(row);
            UUID priorSession = current.partyTeleportSession();
            String priorState = current.state();
            UUID priorAttempt = current.hostedAttempt(), priorBoss = current.bossEntity();
            try {
                startOwned(host, current);
                data.flush(host.server);
            } catch (RuntimeException failure) {
                if (!Objects.equals(priorSession, current.partyTeleportSession())) try {
                    if (current.hostedAttempt() != null && current.hasBossEntity()) {
                        var dimension = net.minecraft.resources.ResourceLocation.tryParse(current.dimension());
                        var level = dimension == null ? null : host.server.getLevel(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION, dimension));
                        if (level != null && level.getEntity(current.bossEntity()) instanceof LivingEntity boss
                                && !HostedEncounterApi.cancel(boss) && HostedEncounterApi.status(boss).isEmpty()) {
                            current.hostedAttempt(priorAttempt);
                            if (priorBoss == null) current.clearBossEntity(); else current.bossEntity(priorBoss);
                            current.state(priorState);
                        }
                    } else current.state(priorState);
                    com.siirio.jemserver.smp.events.PartyTeleportFlow.rollback(host.server, current);
                } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        });
    }

    private static void startOwned(ServerPlayer host, PartyState row) {
        SmpRecords.require(SmpEnvironment.active(host), "unavailable");
        SmpRecords.owner(host, row);
        if (row.state().equals("ACTIVE")) return;
        String reason = startReason(host, row);
        SmpRecords.require(reason.isEmpty(), reason);
        if (row.hasEventId()) {
            var event = SmpData.get(host.server).events().current(row.eventId());
            row.dimension(event.dimension());
            row.position(event.position());
        }
        SmpRecords.require(row.activity().equals("BOSS"),"unavailable");
        var ids =
                SmpRecords.memberIds(row).stream()
                        .filter(id -> Parties.accepted(row, id))
                        .filter(
                                id -> {
                                    var player = host.server.getPlayerList().getPlayer(id);
                                    return player != null;
                                })
                        .toList();
        SmpRecords.require(ids.contains(host.getUUID()), "host_must_be_present");
        String arenaReason = com.siirio.jemserver.smp.events.StructureStaging.startReason(host, row, ids);
        SmpRecords.require(arenaReason.isEmpty(), arenaReason);
        com.siirio.jemserver.smp.events.PartyTeleportFlow.begin(host,row,ids);
        if(row.bloodMoon()) {
            EventParties.start(host,row,ids);
            return;
        }
        if (row.raid()) {
            com.siirio.jemserver.smp.events.RaidEvent.start(host, row, ids);
            row.state("ACTIVE");
            Profiles.count(host.server, host.getUUID(), com.siirio.jemserver.smp.ProfileCounter.BOSSES_HOSTED);
            
            return;
        }
        if(com.siirio.jemserver.smp.events.StructureStaging.start(host,row,ids)) return;
        var level = host.serverLevel();
        LivingEntity boss = null;
        if (row.hasBossEntity()
                && level.getEntity(row.bossEntity()) instanceof LivingEntity found)
            boss = found;
        if (boss == null) {
            var center = BlockPos.of(row.position());
            var candidates =
                    level.getEntitiesOfClass(
                            LivingEntity.class,
                            new AABB(center).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                            entity -> entity.isAlive() && HostedEncounterApi.canHost(entity));
            SmpRecords.require(candidates.size() == 1, "one_awakened_boss_required");
            boss = candidates.get(0);
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("jem_twelve_eyes"))
            for (UUID id : ids) SmpRecords.require(com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(
                    host.server.getPlayerList().getPlayer(id),
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType())), "unavailable");
        boss.getPersistentData().putBoolean("jem_solo", row.solo());
        var bounds=com.siirio.jemserver.smp.events.StructureStaging.prepareCombat(level,row,boss);
        LivingEntity startBoss = boss;
        SmpRecords.require(HostedEncounterApi.start(boss, ids, false, bounds, row.id(),
                attempt -> commitHostedStart(host.server, row, startBoss, attempt)), "unavailable");
        row.bossEntity(boss.getUUID());
        row.hostedAttempt(HostedEncounterApi.attemptId(boss));
        row.state("ACTIVE");
        Profiles.count(host.server, host.getUUID(), com.siirio.jemserver.smp.ProfileCounter.BOSSES_HOSTED);
        
    }

    /** WBT invokes this before releasing AI, including after an interrupted cross-owner Start. */
    public static boolean recoverHostedAttempt(net.minecraft.server.MinecraftServer server, UUID bossId, UUID attemptId, CompoundTag state) {
        var store = SmpData.get(server);
        return store.edit(() -> {
            if (!state.hasUUID("Party")) return false;
            var party = store.parties().current(state.getUUID("Party"));
            if (!validHostedLink(party, bossId, attemptId, state)) return false;
            String arena = state.getCompound("NativeEncounter").getString("ArenaId");
            if (party.hasStructureId() && !arena.isEmpty()) {
                var world = com.siirio.jemworldbosstiers.progression.WorldTierData.get(server);
                if (!world.canonicalArenaId(arena).equals(world.canonicalArenaId(party.structureId().toString()))) return false;
            }
            EventState event = party.hasEventId() ? store.events().current(party.eventId()) : null;
            if (party.hasEventId() && (event == null || event.closed() || !event.pendingOutcome().isEmpty()
                    || !event.hasParty() || !event.party().equals(party.id()))) return false;
            var ids = state.getCompound("Players").getAllKeys().stream().map(UUID::fromString).toList();
            if (event != null && event.hasEncounterParticipants()
                    && !new HashSet<>(event.encounterParticipants()).equals(new HashSet<>(ids))) return false;
            if (party.raid() && event != null && event.hasRaidArenaId()) {
                var world = com.siirio.jemworldbosstiers.progression.WorldTierData.get(server);
                world.recordRaidParticipation(event.raidArenaId(), ids, attemptId);
                if (!world.flush(server)) return false;
            }
            party.bossEntity(bossId); party.hostedAttempt(attemptId); party.nativePending(false); party.state("ACTIVE");
            if (event != null) {
                event.encounterHost(party.owner()); event.encounterParticipants(ids); event.attemptId(attemptId);
                event.bossEntity(bossId); event.solo(party.solo()); event.combatStarted(true); event.encounterState("ACTIVE");
            }
            for (UUID id : ids) {
                var frozen = state.getCompound("Players").getCompound(id.toString());
                var member = party.member(id);
                if ("ELIMINATED".equals(frozen.getString("State"))) member.eliminated(true);
                if (frozen.getBoolean("NativeDeath")) member.nativeDeath(true);
                if (event != null && event.member(id) != null) {
                    if (event.member(id).eliminated()) member.eliminated(true);
                    if (event.member(id).nativeDeath()) member.nativeDeath(true);
                }
                // Return only monotonic admission facts to WBT. Its owner persists the merge before AI release.
                if (member.eliminated()) frozen.putString("State", "ELIMINATED");
                if (member.nativeDeath()) frozen.putBoolean("NativeDeath", true);
                if (event != null) {
                    var eventMember = event.member(id);
                    if (eventMember == null) eventMember = new EventMember();
                    eventMember.accepted(true);
                    if (member.eliminated()) eventMember.eliminated(true);
                    if (member.nativeDeath()) eventMember.nativeDeath(true);
                    event.addMember(id, eventMember);
                }
            }
            store.flush(server);
            return true;
        });
    }

    /** Exact failed terminal intent lets the canonical owner finish an interrupted close. */
    public static boolean hostedAttemptTerminal(net.minecraft.server.MinecraftServer server, UUID bossId, UUID attemptId, CompoundTag state) {
        var store = SmpData.get(server);
        return store.edit(() -> {
            if (!state.hasUUID("Party")) return false;
            var party = store.parties().current(state.getUUID("Party"));
            if (party == null || !party.hasBossEntity() || !bossId.equals(party.bossEntity())
                    || !attemptId.equals(party.hostedAttempt())) return false;
            boolean failed = party.pendingOutcome().equals("FAILED") || party.state().equals("FAILED")
                    || party.state().equals("CANCELLED") || party.state().equals("CLOSED");
            if (!failed || party.closedAttempt() != null && !attemptId.equals(party.closedAttempt())) return false;
            store.flush(server);
            return true;
        });
    }

    static boolean validHostedLink(PartyState party, UUID bossId, UUID attemptId, CompoundTag state) {
        if (party == null || party.closed() || !party.pendingOutcome().isEmpty() || party.closedAttempt() != null
                || !state.hasUUID("Party") || !party.id().equals(state.getUUID("Party"))
                || !state.hasUUID("Boss") || !bossId.equals(state.getUUID("Boss"))
                || !state.hasUUID("Attempt") || !attemptId.equals(state.getUUID("Attempt"))
                || party.hostedAttempt() != null && !party.hostedAttempt().equals(attemptId)
                || party.hasBossEntity() && !party.bossEntity().equals(bossId)) return false;
        var frozen = state.getCompound("Players").getAllKeys().stream().map(UUID::fromString).collect(java.util.stream.Collectors.toSet());
        var accepted = party.members().entrySet().stream().filter(entry -> entry.getValue().accepted())
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        return !frozen.isEmpty() && frozen.contains(party.owner()) && frozen.equals(accepted);
    }

    public static void commitHostedStart(net.minecraft.server.MinecraftServer server, PartyState party,
                                         LivingEntity boss, UUID attemptId) {
        party.hostedAttempt(attemptId);
        if (party.raid() && party.hasEventId()) {
            var event = SmpData.get(server).events().current(party.eventId());
            if (event != null && event.hasRaidArenaId()) {
                var world = com.siirio.jemworldbosstiers.progression.WorldTierData.get(server);
                var ids = boss.getPersistentData().getCompound("JEMHostedEncounter").getCompound("Players")
                        .getAllKeys().stream().map(UUID::fromString).toList();
                world.recordRaidParticipation(event.raidArenaId(), ids, attemptId);
                SmpRecords.require(world.flush(server), "storage_unavailable");
            }
        }
        SmpRecords.require(recoverHostedAttempt(server, boss.getUUID(), attemptId,
                boss.getPersistentData().getCompound("JEMHostedEncounter")), "storage_unavailable");
    }

    public static void closed(HostedEncounterClosedEvent event) {
        var data = SmpData.get(event.server());
        data.edit(() -> closedOwned(event));
    }

    private static void closedOwned(HostedEncounterClosedEvent event) {
        var server = event.server();
        var data = SmpData.get(server);
        for (var row : data.parties().currentRows()) {
            if (!row.hasBossEntity() || !row.bossEntity().equals(event.bossId())) continue;
            if (row.hostedAttempt() != null && !row.hostedAttempt().equals(event.attemptId())) continue;
            if (event.attemptId().equals(row.closedAttempt())) { acknowledgeClose(server, row); continue; }
            if (SmpData.closed(row) && row.hostedAttempt() == null) continue;
            var frozenMembers = event.frozenState().getCompound("Players");
            for (UUID id : SmpRecords.memberIds(row)) {
                var frozen = frozenMembers.getCompound(id.toString());
                if ("ELIMINATED".equals(frozen.getString("State"))) row.member(id).eliminated(true);
                if (frozen.getBoolean("NativeDeath")) {
                    row.member(id).nativeDeath(true);
                    if (row.hasEventId()) {
                        var linkedEvent = data.events().current(row.eventId());
                        if (linkedEvent != null && linkedEvent.member(id) != null) linkedEvent.member(id).nativeDeath(true);
                    }
                }
            }
            row.pendingOutcome(event.success() ? "COMPLETED" : "FAILED");
            row.state("RECOVERY");
            data.flush(server);
            try {
                com.siirio.jemserver.smp.events.StructureStaging.finish(server, row);
                if (row.hasEventId()) {
                    var active = data.events().current(row.eventId());
                    if (active != null && (active.state().equals("ACTIVE") || !active.pendingOutcome().isEmpty())) {
                        if (event.success()) com.siirio.jemserver.smp.events.EventScheduler.finish(server, active, "COMPLETED");
                        else if (active.pendingOutcome().isEmpty()) {
                            // WBT is still inside its close callback. Drain this existing intent after it acknowledges closure.
                            active.pendingOutcome("FAILED_ATTEMPT");
                            data.flush(server);
                        }
                    }
                }
            } catch (RuntimeException failure) {
                row.state("RECOVERY");
                throw failure;
            }
            if (event.success()) for (UUID id : event.eligibleParticipants()) {
                Profiles.count(server, id, event.raid() ? ProfileCounter.RAIDS_COMPLETED : ProfileCounter.BOSS_ASSISTS);
                Profiles.countEvent(server, id, event.raid() ? "BOSS_RAID" : "BOSS");
            }
            row.closedAttempt(event.attemptId());
            // Retain the in-memory acknowledgment on failed storage, retry only the durable write.
            acknowledgeClose(server, row);
            if (!event.success()) notifyDefeat(server, row, event.raid());
            if (!event.success() && !event.raid() && row.hasStructureId() && event.boss() != null)
                server.execute(() -> {
                    if (event.boss().level() instanceof net.minecraft.server.level.ServerLevel level)
                        com.siirio.jemserver.smp.events.StructureStaging.stageBoss(level, row, event.boss());
                });
        }
    }

    private static void acknowledgeClose(net.minecraft.server.MinecraftServer server, PartyState row) {
        acknowledgeClose(row, () -> SmpData.get(server).flush(server));
    }

    static void acknowledgeClose(PartyState row, Runnable flush) {
        if (row.pendingOutcome().isEmpty()) { flush.run(); return; }
        String outcome = row.pendingOutcome();
        row.state(outcome); row.pendingOutcome("");
        try { flush.run(); }
        catch (RuntimeException failure) {
            row.pendingOutcome(outcome); row.state("RECOVERY"); throw failure;
        }
    }

    private static void notifyDefeat(net.minecraft.server.MinecraftServer server,PartyState party,boolean raid) {
        for(UUID id:SmpRecords.memberIds(party)) {
            if(!Parties.accepted(party,id)) continue;
            var player=server.getPlayerList().getPlayer(id);
            if(player==null) continue;
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("jem.event.defeat_title")
                    .withStyle(net.minecraft.ChatFormatting.RED,net.minecraft.ChatFormatting.BOLD));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(raid?"jem.event.defeat_retry.raid":"jem.event.defeat_retry.boss")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        var data = SmpData.get(server);
        data.edit(() -> tickOwned(server));
    }

    private static void tickOwned(net.minecraft.server.MinecraftServer server) {
        if (!SmpEnvironment.active(server)) return;
        if (server.getTickCount() % 20 != 0) return;
        var data = SmpData.get(server);
        long now = System.currentTimeMillis();
        for (var row : data.parties().currentRows()) {
            if (row.pendingOutcome().isEmpty()) continue;
            try {
                if (row.closedAttempt() == null) {
                    if (!row.pendingOutcome().equals("FAILED") || !row.hasBossEntity() || row.hostedAttempt() == null) continue;
                    var target = net.minecraft.resources.ResourceLocation.tryParse(row.dimension());
                    var level = target == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, target));
                    if (level == null || !(level.getEntity(row.bossEntity()) instanceof LivingEntity boss)
                            || !row.hostedAttempt().equals(HostedEncounterApi.attemptId(boss))) continue;
                    data.flush(server);
                    HostedEncounterApi.cancel(boss);
                    continue;
                }
                com.siirio.jemserver.smp.events.StructureStaging.finish(server, row);
                acknowledgeClose(server, row);
            } catch (RuntimeException recovery) {
                com.mojang.logging.LogUtils.getLogger().error("Party terminal cleanup remains pending for {}", row.id(), recovery);
            }
        }
        for (var row : data.parties().currentRows()) {
            if (!row.state().equals("ACTIVE") || row.hasEventId() || !row.hasBossEntity() || row.hostedAttempt() == null) continue;
            var dimension = net.minecraft.resources.ResourceLocation.tryParse(row.dimension());
            var level = dimension == null ? null : server.getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, dimension));
            if (level == null || !(level.getEntity(row.bossEntity()) instanceof LivingEntity boss) || !boss.isAlive()
                    || !row.hostedAttempt().equals(HostedEncounterApi.attemptId(boss))) continue;
            var status = HostedEncounterApi.status(boss).stream().filter(member -> member.playerId().equals(row.owner())).findFirst().orElse(null);
            var canonical = boss.getPersistentData().getCompound("JEMHostedEncounter").getCompound("Players").getCompound(row.owner().toString());
            if (status == null || !absentHostDefeat(true, server.getPlayerList().getPlayer(row.owner()) == null || canonical.getBoolean("AbsenceExpired"),
                    status.state(), canonical.getBoolean("NativeDeath"), canonical.getBoolean("AbsenceExpired")
                    || canonical.contains("DisconnectUntil") && server.overworld().getGameTime() >= canonical.getLong("DisconnectUntil"))) continue;
            row.pendingOutcome("FAILED"); row.state("RECOVERY");
            try {
                data.flush(server);
                HostedEncounterApi.cancel(boss);
            } catch (RuntimeException failure) {
                com.mojang.logging.LogUtils.getLogger().error("Absent host failure remains pending for {}", row.id(), failure);
            }
        }
        for (var row : data.parties().currentRows()) {
            if (SmpData.closed(row) || row.state().equals("ACTIVE") || !row.hasBossEntity()) continue;
            var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.dimension())));
            if (level != null && level.getEntity(row.bossEntity()) instanceof LivingEntity boss && boss.isAlive()) HostedEncounterApi.hold(boss);
        }

        for (var row : data.parties().currentRows())
            if (!SmpData.closed(row) && row.pendingOutcome().isEmpty() && !row.hasPartyTeleportSession()
                    && !row.state().equals("ACTIVE")
                    && now - row.created() > SmpConfig.PARTY_WAIT_MINUTES.get() * 60000L) {
                release(server, row);
                row.state("CLOSED");
                
            }
    }

    static boolean absentHostDefeat(boolean host, boolean offline, String canonicalState, boolean nativeDeath, boolean expired) {
        return host && offline && expired && !nativeDeath && canonicalState.equals("ELIMINATED");
    }

    public static void arrive(ServerPlayer player, PartyState row) {
        var data = SmpData.get(player.server);
        data.edit(() -> arriveOwned(player, data.parties().bind(row)));
    }

    private static void arriveOwned(ServerPlayer player, PartyState row) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        SmpRecords.require(!row.state().equals("ACTIVE") && !SmpData.closed(row), "combat_locked");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        if (row.activity().equals("BOSS")&&com.siirio.jemserver.smp.events.StructureStaging.arrive(player, row)) return;
        if(row.bloodMoon()&&row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null,"unavailable");
            var targetDimension=net.minecraft.resources.ResourceLocation.tryParse(event.dimension());
            var targetLevel=targetDimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,targetDimension));
            SmpRecords.require(targetLevel!=null,"unavailable");
            var outside=com.siirio.jemserver.smp.events.EventRegions.safeOutside(targetLevel,event,BlockPos.of(event.position())).orElseThrow(()->new IllegalArgumentException("no_safe_arrival"));
            player.teleportTo(targetLevel,outside.getX()+.5,outside.getY(),outside.getZ()+.5,player.getYRot(),player.getXRot());
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            player.fallDistance=0;
            return;
        }
        var host=player.server.getPlayerList().getPlayer(row.owner());
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.activity().equals("BOSS")?row.dimension()
                :host==null?"":host.serverLevel().dimension().location().toString());
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        SmpRecords.require(level!=null,"unavailable");
        var origin = row.activity().equals("BOSS")?BlockPos.of(row.position()):host.blockPosition();
        for (var pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 2, 2))) {
            double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
            var box = player.getDimensions(player.getPose()).makeBoundingBox(x, y, z);
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                    || !level.noCollision(player, box)) continue;
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
            player.fallDistance = 0;
            return;
        }
        throw new IllegalArgumentException("no_safe_arrival");
    }

    public static void release(net.minecraft.server.MinecraftServer server, PartyState row) {
        var data = SmpData.get(server);
        data.edit(() -> releaseOwned(server, data.parties().bind(row)));
    }

    private static void releaseOwned(net.minecraft.server.MinecraftServer server, PartyState row) {
        if (!row.hasBossEntity()) return;
        var level = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(row.dimension())));
        if (level != null && level.getEntity(row.bossEntity()) instanceof LivingEntity boss) HostedEncounterApi.release(boss);
    }

    public static void forfeit(ServerPlayer player,UUID partyId,UUID actionToken) {
        var data = SmpData.get(player.server);
        data.edit(() -> forfeitOwned(player, partyId, actionToken));
    }

    private static void forfeitOwned(ServerPlayer player,UUID partyId,UUID actionToken) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        PartyState row=SmpData.get(player.server).parties().current(partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.state().equals("ACTIVE")
                &&row.activity().equals("BOSS"),"unavailable");
        SmpRecords.owner(player,row);
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.matchesAction(player.server,row,actionToken),"unavailable");
        if(row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null&&com.siirio.jemserver.smp.events.EventScheduler.cancelAttempt(player.server,event,actionToken),"unavailable");
            return;
        }
        var dimension=net.minecraft.resources.ResourceLocation.tryParse(row.dimension());
        var level=dimension==null?null:player.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
        LivingEntity boss=level!=null&&row.hasBossEntity()&&level.getEntity(row.bossEntity()) instanceof LivingEntity living?living:null;
        if(boss!=null&&HostedEncounterApi.cancel(boss)) return;
        row.pendingOutcome("FAILED"); row.state("RECOVERY"); row.closedAttempt(actionToken);
        SmpData.get(player.server).flush(player.server);
        if (boss != null) HostedEncounterApi.hold(boss);
        com.siirio.jemserver.smp.events.HostedBoundary.clearParty(row);
        com.siirio.jemserver.smp.events.StructureStaging.finish(player.server, row);
        acknowledgeClose(player.server, row);
        notifyDefeat(player.server, row, row.raid());
    }

    public static void leaveEncounter(ServerPlayer player,UUID partyId) {
        var data = SmpData.get(player.server);
        data.edit(() -> leaveEncounterOwned(player, partyId));
    }

    private static void leaveEncounterOwned(ServerPlayer player,UUID partyId) {
        SmpRecords.require(SmpEnvironment.active(player), "unavailable");
        PartyState row=SmpData.get(player.server).parties().current(partyId);
        SmpRecords.require(row!=null&&!SmpData.closed(row)&&row.state().equals("ACTIVE")
                &&row.activity().equals("BOSS"),"unavailable");
        SmpRecords.require(Parties.accepted(row,player.getUUID()),"not_member");
        SmpRecords.require(!row.owner().equals(player.getUUID()),"host_only");
        SmpRecords.require(com.siirio.jemserver.smp.events.HostedBoundary.canForfeit(player,row),"unavailable");
        if(row.hasEventId()) {
            var event=SmpData.get(player.server).events().current(row.eventId());
            SmpRecords.require(event!=null&&event.state().equals("ACTIVE"),"unavailable");
            var member=event.member(player.getUUID());
            if (member != null) member.eliminated(true);
            row.member(player.getUUID()).eliminated(true);
            SmpData.get(player.server).flush(player.server);
            if (row.raid()) SmpRecords.require(HostedEncounterApi.leave(player), "unavailable");
            com.siirio.jemserver.smp.events.PartyTeleportFlow.returnNow(player,row);
            
            boolean remains=new com.siirio.jemserver.smp.events.EncounterContext(event).participantIds().stream()
                    .anyMatch(id->event.member(id) == null || !event.member(id).eliminated());
            if(!remains) com.siirio.jemserver.smp.events.EventScheduler.failAttempt(player.server,event);
            return;
        }
        row.member(player.getUUID()).eliminated(true);
        SmpData.get(player.server).flush(player.server);
        SmpRecords.require(HostedEncounterApi.leave(player), "unavailable");
        com.siirio.jemserver.smp.events.PartyTeleportFlow.returnNow(player, row);
    }

    public static String startReason(ServerPlayer viewer, PartyState row) {
        if (!SmpEnvironment.active(viewer)) return "unavailable";
        if (!row.owner().equals(viewer.getUUID())) return "host_only";
        if (SmpData.closed(row)) return "closed";
        if (row.state().equals("ACTIVE")) return "combat_locked";
        if (!row.pendingOutcome().isEmpty() || row.hasPartyTeleportSession()) return "return_pending";
        var host = viewer.server.getPlayerList().getPlayer(row.owner());
        if (host == null) return "host_must_be_present";
        if (row.hasEventId()) {
            var event = SmpData.get(viewer.server).events().snapshot(row.eventId());
            if (event == null || SmpData.closed(event)) return "unavailable";
            if (!event.state().equals("ACTIVE")) return "event_not_started";
            if (event.combatStarted() || event.hasBossEntity()) return "combat_locked";
        }
        for (UUID id : SmpRecords.memberIds(row)) {
            if (!Parties.accepted(row, id)) continue;
            var member = viewer.server.getPlayerList().getPlayer(id);
            if (member == null || !member.isAlive() || member.isSpectator() || member.isSleeping()
                    || !row.member(id).ready() || row.member(id).eliminated()
                    || member.isPassenger() || member.isVehicle()) return "party_not_ready";
        }
        if (row.activity().equals("BOSS") && !row.raid() && !row.bloodMoon() && !row.nativePending()) {
            LivingEntity campaignBoss = row.hasBossEntity() && host.serverLevel().getEntity(row.bossEntity()) instanceof LivingEntity found ? found : null;
            if (row.hasBossEntity()) {
                if (!(host.serverLevel().getEntity(row.bossEntity()) instanceof LivingEntity boss) || !boss.isAlive()) return "unavailable";
            } else {
                var candidates = host.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.of(row.position())).inflate(SmpConfig.ENCOUNTER_RADIUS.get()),
                        entity -> entity.isAlive() && WorldTierApi.profile(entity).isPresent());
                if (candidates.size() != 1) return "one_awakened_boss_required";
                campaignBoss = candidates.get(0);
            }
            if (campaignBoss != null && net.minecraftforge.fml.ModList.get().isLoaded("jem_twelve_eyes"))
                for (UUID id : SmpRecords.memberIds(row)) {
                    if (!Parties.accepted(row, id)) continue;
                    var member = viewer.server.getPlayerList().getPlayer(id);
                    if (!com.siirio.jemtwelveeyes.api.CampaignApi.unlocked(member,
                            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(campaignBoss.getType()))) return "unavailable";
                }
        }
        if (row.activity().equals("BOSS") && !row.raid() && row.nativePending()) {
            String reason=com.siirio.jemserver.smp.events.StructureStaging.nativeBossStartReason(host,row);
            if(!reason.isEmpty()) return reason;
        }
        return "";
    }

    private HostedParties() {}
}
