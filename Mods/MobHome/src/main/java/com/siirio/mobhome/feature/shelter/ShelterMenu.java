package com.siirio.mobhome.feature.shelter;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** No inventory slots: the nonce, revision and stable pet UUID are the complete action contract. */
public final class ShelterMenu extends AbstractContainerMenu {
    public record Row(UUID id, String species, String name, int flags, String blocked) { }
    public static final int SUPPORTED = 1, HERE = 2, ELSEWHERE = 4, PENDING = 8, UNLOADED = 16, BIND_READY = 32;
    public final BlockPos anchor;
    public final UUID nonce;
    private final ServerPlayer viewer;
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    private long lastBroadcast;
    private int revision;
    private List<Row> lastRows = List.of();
    private boolean lastOccupied;

    public ShelterMenu(int id, Inventory inventory, FriendlyByteBuf opening) {
        super(ShelterContent.MENU.get(), id); anchor = opening.readBlockPos(); nonce = opening.readUUID(); viewer = null; dimension = inventory.player.level().dimension();
    }
    public ShelterMenu(int id, Inventory inventory, ShelterController controller, UUID nonce) {
        super(ShelterContent.MENU.get(), id); anchor = controller.getBlockPos(); this.nonce = nonce; viewer = (ServerPlayer) inventory.player; dimension = viewer.level().dimension();
    }
    public static void writeOpening(FriendlyByteBuf buffer, ShelterController controller, UUID nonce) {
        // Forge writes this payload before creating/assigning the server menu.
        buffer.writeBlockPos(controller.getBlockPos()); buffer.writeUUID(nonce);
    }
    @Override public boolean stillValid(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        ServerLevel level = serverPlayer.serverLevel();
        if (level.dimension() != dimension || !level.hasChunkAt(anchor) || anchor.distToCenterSqr(player.position()) > 64) return false;
        if (!(level.getBlockEntity(anchor) instanceof ShelterController home) || !player.getUUID().equals(home.owner())) return false;
        var state = level.getBlockState(anchor);
        return state.getBlock() instanceof ShelterBlock block && block.intact(level, anchor, state.getValue(ShelterBlock.FACING));
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer != null && viewer.serverLevel().getGameTime() - lastBroadcast >= 20) refresh(false);
    }
    public void refresh(boolean force) {
        if (viewer == null || !stillValid(viewer)) return;
        lastBroadcast = viewer.serverLevel().getGameTime();
        CompanionData data = CompanionData.get(viewer.serverLevel()); String dimension = viewer.serverLevel().dimension().location().toString();
        List<Row> rows = data.owned(viewer.getUUID()).stream().map(pet -> row(viewer, pet, dimension, anchor)).toList();
        boolean occupied = data.occupant(dimension, anchor) != null;
        if (!force && rows.equals(lastRows) && occupied == lastOccupied) return;
        lastRows = rows; lastOccupied = occupied; revision++;
        ShelterNetwork.snapshot(viewer, new ShelterNetwork.Snapshot(containerId, nonce, revision, occupied, rows));
    }
    private static Row row(ServerPlayer player, CompanionData.Pet pet, String dimension, BlockPos anchor) {
        int flags = pet.supported ? SUPPORTED : 0;
        if (pet.home != null) flags |= pet.assigned(dimension, anchor) ? HERE : ELSEWHERE;
        if (pet.pending != null) flags |= PENDING;
        Entity entity = loaded(player, pet); String blocked = pet.blocked;
        if (entity == null) flags |= UNLOADED;
        if (pet.provisional && (entity == null || !player.getUUID().equals(CompanionAdapters.rider(entity)))) blocked = "mount_to_bind";
        else if (entity != null && !entity.isAlive() && pet.pending == null) blocked = "native_death_pending";
        else if (entity != null && !CompanionAdapters.ready(entity)) blocked = CompanionAdapters.readinessReason(entity);
        else flags |= BIND_READY;
        return new Row(pet.id, pet.species, pet.name, flags, blocked);
    }
    private static Entity loaded(ServerPlayer player, CompanionData.Pet pet) {
        for (ServerLevel level : player.server.getAllLevels()) { Entity found = level.getEntity(pet.id); if (found != null) return found; }
        return null;
    }
    public void action(ShelterNetwork.Action request) {
        if (viewer == null || viewer.containerMenu != this || !nonce.equals(request.nonce()) || revision != request.revision() || !stillValid(viewer)) return;
        CompanionData data = CompanionData.get(viewer.serverLevel()); CompanionData.Pet pet = data.pet(request.pet());
        if (pet == null || CompanionAdapters.excluded(pet.species) || !viewer.getUUID().equals(pet.owner)) return;
        Entity nativePet = loaded(viewer, pet);
        if (nativePet != null && (!viewer.getUUID().equals(CompanionAdapters.owner(nativePet)) || !pet.species.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(nativePet.getType()).toString()))) return;
        String dimension = viewer.serverLevel().dimension().location().toString();
        if (request.bind()) {
            if (!pet.supported || (nativePet != null && !CompanionAdapters.supported(nativePet))) return;
            if (nativePet != null && !CompanionAdapters.ready(nativePet)) return;
            if (nativePet != null && !nativePet.isAlive() && pet.pending == null) return;
            if (pet.provisional && (nativePet == null || !viewer.getUUID().equals(CompanionAdapters.rider(nativePet)))) return;
            CompanionData.Pet occupant = data.occupant(dimension, anchor);
            if (occupant != null && occupant != pet) return;
            if (nativePet instanceof net.minecraft.world.entity.LivingEntity animal
                    && nativePet.level() == viewer.serverLevel()
                    && !ShelterHold.place(viewer.serverLevel(), animal, anchor, viewer.getUUID())) {
                pet.blocked = "no_safe_space";
                data.flush(viewer.serverLevel()); refresh(true);
                return;
            }
            if (nativePet != null && CompanionAdapters.provisional(nativePet)) {
                nativePet.getPersistentData().putUUID(CompanionAdapters.CUSTODIAN, viewer.getUUID()); pet.provisional = false; pet.custodian = true;
            }
            // Reassignment is a single server-thread mutation. Hitbox and house size never enter admission.
            pet.home = anchor; pet.homeDimension = dimension; pet.blocked = "";
        } else {
            if (!pet.assigned(dimension, anchor)) return;
            if (nativePet != null) ShelterHold.release(nativePet);
            pet.home = null; pet.homeDimension = ""; pet.blocked = "";
            // Pending is deliberately retained when unbinding a dead animal or removing its house.
        }
        data.flush(viewer.serverLevel()); refresh(true); ShelterRecovery.nearby(viewer);
    }
}
