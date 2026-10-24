package com.siirio.jemworldbosstiers.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentEntitySectionManager.class)
public interface PersistentEntityStorageAccessor {
    @Accessor("permanentStorage") EntityPersistentStorage<Entity> jemWorldBossTiers$permanentStorage();
    @Invoker("storeChunkSections") boolean jemWorldBossTiers$storeChunkSections(long chunk, Consumer<Entity> afterStore);
}
