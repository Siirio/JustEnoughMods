package com.siirio.jemserver.smp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.siirio.jemidentity.api.DedicatedTestProfile;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Registered only on the physical dedicated server. Native auth remains mandatory, including for OP. */
public final class TestProfileRuntime {
    private final Map<UUID, ServerOpListEntry> original = new HashMap<>();
    private final Set<UUID> granted = new HashSet<>();
    private boolean active;

    @SubscribeEvent public void starting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer()) return;
        Path journal = server.getServerDirectory().toPath().resolve("local/test-profile/operators-before.json");
        try {
            var binding = DedicatedTestProfile.binding(server);
            active = binding.isPresent();
            if (Files.exists(journal)) {
                // Recover an interrupted session before accepting any player or OP command.
                JsonObject saved = JsonParser.parseString(Files.readString(journal)).getAsJsonObject();
                for (var row : saved.getAsJsonArray("operators")) {
                    ServerOpListEntry entry = new ServerOpListEntry(row.getAsJsonObject());
                    original.put(UUID.fromString(row.getAsJsonObject().get("uuid").getAsString()), entry);
                }
                restore(server);
                if (!active) {
                    Files.move(journal, journal.resolveSibling("operators-restored-"+System.currentTimeMillis()+".json"));
                    original.clear(); return;
                }
                if (!saved.get("saveIdentity").getAsString().equals(binding.get().saveIdentity().toString())
                        || !saved.get("epoch").getAsString().equals(binding.get().epoch()))
                    throw new IOException("Explicit operator epoch migration required");
            } else if (active) {
                var ops = server.getPlayerList().getOps();
                ops.save();
                JsonArray operators = JsonParser.parseString(Files.readString(ops.getFile().toPath())).getAsJsonArray();
                for (var row : operators) {
                    original.put(UUID.fromString(row.getAsJsonObject().get("uuid").getAsString()), new ServerOpListEntry(row.getAsJsonObject()));
                }
                JsonObject receipt = new JsonObject(); receipt.add("operators", operators);
                receipt.addProperty("saveIdentity", binding.get().saveIdentity().toString()); receipt.addProperty("epoch", binding.get().epoch());
                Files.createDirectories(journal.getParent());
                try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    ByteBuffer bytes = StandardCharsets.UTF_8.encode(receipt.toString());
                    while (bytes.hasRemaining()) channel.write(bytes);
                    channel.force(true);
                }
            }
        } catch (IOException | RuntimeException failure) { throw new IllegalStateException("Test operator profile cannot start safely", failure); }
        var binding = DedicatedTestProfile.binding(server);
        if (binding.isPresent()) com.mojang.logging.LogUtils.getLogger().info(
                "JEM dedicated TEST profile: save=jem-test-2026-10-20/world, identity={}, epoch={}, ordinaryQuotaBypass={}",
                binding.get().saveIdentity(), binding.get().epoch(), binding.get().bypassQuotas());
        else com.mojang.logging.LogUtils.getLogger().info("JEM dedicated production profile: test privileges disabled");
    }

    @SubscribeEvent public void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        if (active && com.siirio.jemeconomy.ledger.EconomySavedData.get(event.getServer()).frozen())
            throw new IllegalStateException("Bound test ledger is frozen; reconcile its existing owner state before admitting testers");
    }

    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.END || event.getServer().getTickCount()%20 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            var principal = IdentityPolicy.resolve(player);
            if (principal.isEmpty()) { revoke(player); continue; }
            com.siirio.jemroles.race.RaceService.prepareTestChoice(player);
            if (!com.siirio.jemeconomy.ledger.EconomySavedData.get(player.server).ensureTestGrant(player)) continue;
            var binding = DedicatedTestProfile.binding(player.server).orElseThrow();
            String modeEpoch = binding.saveIdentity()+"/"+binding.epoch()+"/"+principal.get().accountId();
            // Forge preserves PlayerPersisted during native death/respawn cloning.
            // Store this receipt with the native game mode in the same player save.
            var persisted = player.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            if (!modeEpoch.equals(persisted.getString("jem_test_initial_mode"))) {
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                persisted.putString("jem_test_initial_mode", modeEpoch);
                player.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);
            }
            UUID id = player.getUUID();
            if (original.containsKey(id) || !granted.add(id)) continue;
            player.server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
            player.server.getPlayerList().sendPlayerPermissionLevel(player);
            player.server.getCommands().sendCommands(player);
        }
    }

    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (active && event.getEntity() instanceof ServerPlayer player) revoke(player);
    }
    private void revoke(ServerPlayer player) {
        if (!granted.remove(player.getUUID())) return;
        player.server.getPlayerList().getOps().remove(player.getGameProfile());
        player.server.getPlayerList().sendPlayerPermissionLevel(player);
        player.server.getCommands().sendCommands(player);
    }
    private void restore(MinecraftServer server) {
        var ops = server.getPlayerList().getOps();
        for (var entry : new ArrayList<>(ops.getEntries())) ops.remove(entry);
        original.values().forEach(ops::add); granted.clear();
    }
    @SubscribeEvent public void stopping(ServerStoppingEvent event) {
        if (active) restore(event.getServer());
        active = false;
    }
}
