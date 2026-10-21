package com.siirio.jemserver;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;

/** Registered exclusively by the physical dedicated-server bootstrap. */
final class FishingBalancePack {
    static void find(AddPackFindersEvent event) {
        if (event.getPackType()!=PackType.SERVER_DATA) return;
        event.addRepositorySource(consumer->{
            var root=ModList.get().getModFileById(JemServer.MOD_ID).getFile().findResource("builtin","fishing_balance");
            var pack=Pack.readMetaAndCreate("jem_server:fishing_balance",Component.literal("JEM fishing balance"),true,
                    id->new PathPackResources(id,true,root),PackType.SERVER_DATA,Pack.Position.TOP,PackSource.BUILT_IN);
            if(pack==null)throw new IllegalStateException("Missing native Tide balance pack");
            consumer.accept(pack);
        });
    }
    private FishingBalancePack() {}
}
