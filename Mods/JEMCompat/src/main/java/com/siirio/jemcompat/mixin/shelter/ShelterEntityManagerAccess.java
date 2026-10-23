package com.siirio.jemcompat.mixin.shelter;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentEntitySectionManager.class)
public interface ShelterEntityManagerAccess {
    @Accessor("permanentStorage")
    EntityPersistentStorage<Entity> jemcompat$permanentStorage();

    @Invoker("storeChunkSections")
    boolean jemcompat$storeChunkSections(long chunk, Consumer<Entity> afterStore);
}
