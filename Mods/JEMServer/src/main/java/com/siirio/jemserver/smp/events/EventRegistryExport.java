package com.siirio.jemserver.smp.events;

import com.google.gson.*;
import com.siirio.jemworldbosstiers.balance.BalanceRegistry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class EventRegistryExport {
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var bosses=new JsonArray();
        for(var entity:BuiltInRegistries.ENTITY_TYPE) {
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(entity);
            BalanceRegistry.boss(id).ifPresent(profile->{var row=new JsonObject();row.addProperty("entity",id.toString());row.addProperty("profile",profile.key().toString());row.addProperty("arenaStrategy",profile.arenaStrategy());row.addProperty("nativeRadius",profile.arenaRadius());bosses.add(row);});
        }
        var mobs=new JsonObject();
        for(var category:BloodMoonWaves.Category.values()) {
            var ids=new JsonArray();
            var tag=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("jem_server","blood_moon/"+category.name().toLowerCase(Locale.ROOT)));
            BuiltInRegistries.ENTITY_TYPE.getTag(tag).ifPresent(values->values.forEach(holder->ids.add(BuiltInRegistries.ENTITY_TYPE.getKey(holder.value()).toString())));
            if(ids.isEmpty()) org.slf4j.LoggerFactory.getLogger(EventRegistryExport.class).warn("Blood Moon category {} has no installed entity entries",category);
            mobs.add(category.name(),ids);
        }
        var structures=new JsonArray();
        var registry=event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);
        registry.getTag(TagKey.create(Registries.STRUCTURE,new ResourceLocation("jem_claims","boss_territories"))).ifPresent(values->values.forEach(holder->structures.add(registry.getKey(holder.value()).toString())));
        var json=new GsonBuilder().setPrettyPrinting().create();
        var snapshots=Map.of("boss_profiles.json",json.toJson(bosses),"blood_moon_entities.json",json.toJson(mobs),"event_structures.json",json.toJson(structures));
        var output=FMLPaths.CONFIGDIR.get().resolve("jem/generated");
        java.util.concurrent.CompletableFuture.runAsync(()->{
            try { Files.createDirectories(output);for(var entry:snapshots.entrySet()) Files.writeString(output.resolve(entry.getKey()),entry.getValue()); }
            catch(java.io.IOException error) { org.slf4j.LoggerFactory.getLogger(EventRegistryExport.class).error("Cannot write generated event registry data",error); }
        });
    }
    private EventRegistryExport() {}
}
