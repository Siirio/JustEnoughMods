package com.siirio.jemrewards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class RewardClaimsRegressionTest {
    @TempDir Path directory;

    @BeforeAll static void registries() {
        SharedConstants.tryDetectVersion();
        // Unit JVMs do not run Forge's network-event constructor transformation.
        // Keep the actual vanilla registry/bootstrap and item/inventory behavior.
        try (MockedStatic<net.minecraftforge.network.NetworkHooks> network = mockStatic(net.minecraftforge.network.NetworkHooks.class, CALLS_REAL_METHODS)) {
            network.when(net.minecraftforge.network.NetworkHooks::init).thenAnswer(call -> null);
            Bootstrap.bootStrap();
        }
    }

    @Test void fullInventoryKeepsRewardPendingAndDoesNotStartAClaim() throws Exception {
        Fixture fixture = fixture(false);
        fixture.inventory.items.replaceAll(stack -> new ItemStack(Items.STONE, 64));
        try (MockedStatic<RewardsData> storage = mockStatic(RewardsData.class, CALLS_REAL_METHODS)) {
            storage.when(() -> RewardsData.get(fixture.server)).thenReturn(fixture.data);
            assertFalse(PendingRewardContainer.claim(fixture.player, fixture.bundle));
            assertFalse(fixture.data.pending().contains("Claim:" + fixture.owner));
            assertEquals(3, fixture.data.pending().getList("RewardContainers:" + fixture.owner, 10)
                    .getCompound(0).getList("Items", 10).getCompound(0).getByte("Count"));
        }
    }

    @Test void creativeClaimAndDuplicatePacketTransferOnlyOnceAndSendNativeContents() throws Exception {
        Fixture fixture = fixture(true);
        try (MockedStatic<RewardsData> storage = mockStatic(RewardsData.class, CALLS_REAL_METHODS)) {
            storage.when(() -> RewardsData.get(fixture.server)).thenReturn(fixture.data);
            assertTrue(PendingRewardContainer.claim(fixture.player, fixture.bundle));
            assertFalse(PendingRewardContainer.claim(fixture.player, fixture.bundle));
            assertEquals(3, fixture.inventory.items.stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum());
            verify(fixture.menu).sendAllDataToRemote();
            assertFalse(fixture.data.pending().contains("Claim:" + fixture.owner));
        }
    }

    @Test void playerWriteFailureLeavesRecoverableClaimAndCannotDuplicateOnRetry() throws Exception {
        Fixture fixture = fixture(false);
        PlayerList players = fixture.server.getPlayerList();
        doThrow(new IllegalStateException("player disk failure")).doAnswer(call -> { savePlayer(fixture); return null; })
                .when(players).saveAll();
        try (MockedStatic<RewardsData> storage = mockStatic(RewardsData.class, CALLS_REAL_METHODS)) {
            storage.when(() -> RewardsData.get(fixture.server)).thenReturn(fixture.data);
            assertThrows(IllegalStateException.class, () -> PendingRewardContainer.claim(fixture.player, fixture.bundle));
            assertTrue(fixture.data.pending().contains("Claim:" + fixture.owner));
            assertFalse(PendingRewardContainer.claim(fixture.player, fixture.bundle));
            assertEquals(3, fixture.inventory.items.stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum());
            assertFalse(fixture.data.pending().contains("Claim:" + fixture.owner));
        }
    }

    private Fixture fixture(boolean creative) throws Exception {
        UUID owner = UUID.randomUUID(), bundle = UUID.randomUUID();
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList players = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(players);
        when(server.getWorldPath(LevelResource.ROOT)).thenReturn(directory);
        when(server.getWorldPath(LevelResource.PLAYER_DATA_DIR)).thenReturn(directory.resolve("playerdata"));
        ServerPlayer player = mock(ServerPlayer.class);
        java.lang.reflect.Field serverField = ServerPlayer.class.getField("server");
        serverField.setAccessible(true);
        serverField.set(player, server);
        InventoryMenu menu = mock(InventoryMenu.class);
        java.lang.reflect.Field inventoryMenu = net.minecraft.world.entity.player.Player.class.getField("inventoryMenu");
        inventoryMenu.setAccessible(true);
        inventoryMenu.set(player, menu);
        player.containerMenu = menu;
        Inventory inventory = new Inventory(player);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUUID()).thenReturn(owner);
        when(player.isAlive()).thenReturn(true);
        when(player.isCreative()).thenReturn(creative);
        when(menu.getCarried()).thenReturn(ItemStack.EMPTY);
        CompoundTag persistent = new CompoundTag();
        when(player.getPersistentData()).thenReturn(persistent);
        when(players.getPlayer(owner)).thenReturn(player);
        when(player.saveWithoutId(any())).thenAnswer(call -> {
            CompoundTag tag = call.getArgument(0);
            tag.put("Inventory", inventory.save(new ListTag()));
            tag.put("ForgeData", persistent.copy());
            return tag;
        });
        CompoundTag container = new CompoundTag();
        container.putUUID("Id", bundle);
        ListTag items = new ListTag();
        items.add(new ItemStack(Items.DIAMOND, 3).save(new CompoundTag()));
        container.put("Items", items);
        ListTag rows = new ListTag();
        rows.add(container);
        CompoundTag root = new CompoundTag();
        root.put("RewardContainers:" + owner, rows);
        RewardsData data = new RewardsData(root);
        Fixture fixture = new Fixture(server, player, inventory, menu, data, owner, bundle);
        doAnswer(call -> { savePlayer(fixture); return null; }).when(players).saveAll();
        return fixture;
    }

    private void savePlayer(Fixture fixture) throws Exception {
        Files.createDirectories(directory.resolve("playerdata"));
        NbtIo.writeCompressed(fixture.player.saveWithoutId(new CompoundTag()), directory.resolve("playerdata/" + fixture.owner + ".dat").toFile());
    }

    private record Fixture(MinecraftServer server, ServerPlayer player, Inventory inventory,
                           InventoryMenu menu, RewardsData data, UUID owner, UUID bundle) {}
}
