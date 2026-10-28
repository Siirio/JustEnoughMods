package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.mixin.TomsLevelEmitterQuestAccess;
import com.tom.storagemod.block.LevelEmitterBlock;
import com.tom.storagemod.tile.InventoryConnectorBlockEntity;
import com.tom.storagemod.tile.LevelEmitterBlockEntity;
import com.tom.storagemod.util.IProxy;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

/** Native observed stock crossing and a later real output commit on the same already configured emitter. */
public final class TomsStockSignalProof {
    public static final String KEY = "jemQuestStockSignalV1";
    private static final String MASTER_ID = KEY + "Master";
    private static final String PENDING = KEY + "Transfers";
    private TomsStockSignalProof() {}

    public static void extracted(NativeTerminalExtractionEvent event) {
        InventoryConnectorBlockEntity master = master(event.network());
        if (master == null || master.getLevel() != event.actor().level() || event.quantity() < 1
                || event.stockAfter() < 0 || event.stockBefore() > Integer.MAX_VALUE) return;
        identity(master);
        ListTag retained = new ListTag();
        for (var raw : master.getPersistentData().getList(PENDING, 10)) {
            CompoundTag row = (CompoundTag) raw;
            if (retained.size() >= 63) break;
            if (!ItemStack.isSameItemSameTags(event.item(), ItemStack.of(row.getCompound("item")))) retained.add(row.copy());
        }
        CompoundTag row = new CompoundTag();
        row.putUUID("actor", event.actor().getUUID()); row.put("item", event.item().save(new CompoundTag()));
        row.putLong("before", event.stockBefore()); row.putLong("after", event.stockAfter());
        row.putLong("tick", master.getLevel().getGameTime()); retained.add(row);
        master.getPersistentData().put(PENDING, retained); master.setChanged();
    }

    public static void committed(LevelEmitterBlockEntity emitter, BlockState oldState, BlockState installed, long nativeCount) {
        if (!(emitter.getLevel() instanceof ServerLevel level) || !validCount(nativeCount) || emitter.getFilter().isEmpty()
                || !oldState.hasProperty(LevelEmitterBlock.POWERED) || !installed.hasProperty(LevelEmitterBlock.POWERED)) return;
        InventoryConnectorBlockEntity master = master(network(emitter));
        if (master == null || master.getLevel() != level) return;
        CompoundTag emitterData = emitter.getPersistentData().getCompound(KEY);
        CompoundTag sample = emitterData.getCompound("sample");
        CompoundTag configuration = configuration(emitter);
        if (!sample.hasUUID("master") || !sample.getUUID("master").equals(identity(master))
                || !sample.getCompound("configuration").equals(configuration)
                || sample.getBoolean("powered") != oldState.getValue(LevelEmitterBlock.POWERED)) return;
        boolean oldPower = oldState.getValue(LevelEmitterBlock.POWERED), newPower = installed.getValue(LevelEmitterBlock.POWERED);
        if (oldPower == newPower || compare(emitter, nativeCount) != newPower) return;
        String emitterId = emitterIdentity(emitter).toString();
        ListTag rows = master.getPersistentData().getList(PENDING, 10);
        if (rows.size() > 64) return;
        for (int index = 0; index < rows.size(); index++) {
            CompoundTag row = rows.getCompound(index);
            if (!row.hasUUID("actor") || sample.getLong("since") > row.getLong("tick") || row.getLong("tick") > level.getGameTime()
                    || !ItemStack.isSameItemSameTags(emitter.getFilter(), ItemStack.of(row.getCompound("item")))
                    || row.getLong("after") != nativeCount || compare(emitter, row.getLong("before")) != oldPower
                    || seen(row.getList("seen", 8), emitterId)) continue;
            UUID actor = row.getUUID("actor");
            NativeQuestRecipients.accepted(level.getServer(), actor, "v3_store_level_signal",
                    new ResourceLocation("jem_advancements", "quest_v3/v3_store_level_signal"));
            rows.remove(index); master.setChanged();
            return;
        }
    }

    public static void sampled(LevelEmitterBlockEntity emitter, long nativeCount) {
        if (!(emitter.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10L != 2L || !validCount(nativeCount)
                || emitter.getFilter().isEmpty()) return;
        InventoryConnectorBlockEntity master = master(network(emitter));
        if (master == null || master.getLevel() != level) return;
        CompoundTag root = emitter.getPersistentData().getCompound(KEY);
        CompoundTag previous = root.getCompound("sample"), config = configuration(emitter);
        UUID masterId = identity(master);
        boolean same = previous.hasUUID("master") && previous.getUUID("master").equals(masterId)
                && previous.getCompound("configuration").equals(config);
        CompoundTag sample = new CompoundTag();
        sample.putUUID("master", masterId); sample.put("configuration", config);
        sample.putLong("since", same ? previous.getLong("since") : level.getGameTime());
        sample.putLong("count", nativeCount);
        BlockState actual = level.getBlockState(emitter.getBlockPos());
        if (!actual.hasProperty(LevelEmitterBlock.POWERED)) return;
        sample.putBoolean("powered", actual.getValue(LevelEmitterBlock.POWERED));
        if (!sample.equals(previous)) { root.put("sample", sample); emitter.getPersistentData().put(KEY, root); emitter.setChanged(); }
        // Each emitter may consider this real transfer only at its first native sample after it.
        String emitterId = emitterIdentity(emitter).toString();
        boolean changed = false;
        ListTag pending = master.getPersistentData().getList(PENDING, 10);
        if (pending.size() > 64) return;
        for (var raw : pending) {
            CompoundTag row = (CompoundTag) raw;
            if (row.getLong("tick") > level.getGameTime()
                    || !ItemStack.isSameItemSameTags(emitter.getFilter(), ItemStack.of(row.getCompound("item")))) continue;
            ListTag seen = row.getList("seen", 8);
            if (!seen(seen, emitterId) && seen.size() < 64) { seen.add(StringTag.valueOf(emitterId)); row.put("seen", seen); changed = true; }
        }
        if (changed) master.setChanged();
    }

    private static boolean compare(LevelEmitterBlockEntity emitter, long count) { return emitter.isLessThan() ? count < emitter.getCount() : count > emitter.getCount(); }
    private static boolean validCount(long count) { return count >= 0 && count <= Integer.MAX_VALUE; }
    private static boolean seen(ListTag seen, String id) { return seen.size() >= 64 || seen.stream().anyMatch(value -> value.getAsString().equals(id)); }
    private static CompoundTag configuration(LevelEmitterBlockEntity emitter) {
        CompoundTag tag = new CompoundTag();
        tag.put("item", emitter.getFilter().copyWithCount(1).save(new CompoundTag()));
        tag.putInt("threshold", emitter.getCount()); tag.putBoolean("lessThan", emitter.isLessThan()); return tag;
    }
    private static IItemHandler network(LevelEmitterBlockEntity emitter) {
        var top = ((TomsLevelEmitterQuestAccess) emitter).jem$nativeEmitterNetwork();
        return top == null ? null : top.orElse(null);
    }
    private static InventoryConnectorBlockEntity master(IItemHandler handler) {
        if (handler == null || handler.getSlots() > 4096) return null;
        IItemHandler actual = IProxy.resolve(handler);
        if (!(actual instanceof NativeTerminalNetworkOwner owner) || !(owner.jem$nativeNetworkOwner() instanceof InventoryConnectorBlockEntity master)
                || !(master.getLevel() instanceof ServerLevel level) || master.isRemoved() || !level.isLoaded(master.getBlockPos())
                || level.getBlockEntity(master.getBlockPos()) != master) return null;
        return master;
    }
    private static UUID identity(BlockEntity master) {
        if (!master.getPersistentData().hasUUID(MASTER_ID)) { master.getPersistentData().putUUID(MASTER_ID, UUID.randomUUID()); master.setChanged(); }
        return master.getPersistentData().getUUID(MASTER_ID);
    }
    private static UUID emitterIdentity(LevelEmitterBlockEntity emitter) {
        CompoundTag root = emitter.getPersistentData().getCompound(KEY);
        if (!root.hasUUID("identity")) { root.putUUID("identity", UUID.randomUUID()); emitter.getPersistentData().put(KEY, root); emitter.setChanged(); }
        return root.getUUID("identity");
    }
}
