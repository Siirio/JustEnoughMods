package com.siirio.jemserver.smp.events;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class DimensionProgress extends SavedData {
    private boolean netherUnlocked;
    private boolean endUnlocked;

    public static DimensionProgress get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DimensionProgress::load, DimensionProgress::new, "jem_dimension_progress");
    }

    static DimensionProgress load(CompoundTag tag) {
        var data = new DimensionProgress();
        data.netherUnlocked = tag.getBoolean("netherUnlocked");
        data.endUnlocked = tag.getBoolean("endUnlocked");
        return data;
    }

    public boolean netherUnlocked() { return netherUnlocked; }
    public boolean endUnlocked() { return endUnlocked; }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("netherUnlocked", netherUnlocked);
        tag.putBoolean("endUnlocked", endUnlocked);
        return tag;
    }

    @SubscribeEvent
    public static void entered(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) visit(player);
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) visit(player);
    }

    private static void visit(ServerPlayer player) {
        var data = get(player.server);
        if ((player.level().dimension() == Level.NETHER || completed(player, "story/enter_the_nether")) && !data.netherUnlocked) {
            data.netherUnlocked = true;
            data.setDirty();
        }
        if ((player.level().dimension() == Level.END || completed(player, "end/root")) && !data.endUnlocked) {
            data.endUnlocked = true;
            data.setDirty();
        }
    }

    private static boolean completed(ServerPlayer player, String path) {
        var advancement = player.server.getAdvancements().getAdvancement(new net.minecraft.resources.ResourceLocation("minecraft", path));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
