package com.siirio.jemserver;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.PathPackResources;

final class EventDefaultPack {
    private static final String PACK_ID = "jem_server:event_defaults";

    private EventDefaultPack() {
    }

    static void register() {
        if (!ModList.get().isLoaded("jem_pack_core")) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(EventDefaultPack::find);
        }
    }

    private static void find(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) event.addRepositorySource(EventDefaultPack::load);
    }

    private static void load(Consumer<Pack> consumer) {
        var root = ModList.get().getModFileById(JemServer.MOD_ID).getFile().findResource("builtin", "event_defaults");
        Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Blood Moon defaults"), true,
                id -> new PathPackResources(id, true, root), PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
        if (pack == null) throw new IllegalStateException("Missing Blood Moon defaults pack metadata");
        consumer.accept(pack);
    }
}
