package com.siirio.jemvisualsandbox.workbench;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Optional encounter integration for race abilities; no race state is owned here. */
public final class ProductionRaceBridge {
    private ProductionRaceBridge() {}

    /** Boss membership remains owned by its optional profile registry. */
    public static boolean registeredBoss(net.minecraft.world.entity.LivingEntity entity) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return false;
        try {
            return ((Optional<?>)Class.forName("com.siirio.jemworldbosstiers.api.WorldTierApi")
                    .getMethod("profile", net.minecraft.world.entity.LivingEntity.class).invoke(null, entity)).isPresent();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Registered boss profile API", failure);
        }
    }

    /** Optional encounter authority is shared by controls, healing and damage. */
    public static boolean canInteract(net.minecraft.world.entity.LivingEntity actor, net.minecraft.world.entity.LivingEntity target) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return true;
        try {
            return (boolean)Class.forName("com.siirio.jemworldbosstiers.api.HostedEncounterApi")
                    .getMethod("canInteract", net.minecraft.world.entity.LivingEntity.class, net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, actor, target);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Hosted encounter interaction API", failure);
        }
    }

    public static ServerPlayer damageOwner(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof ServerPlayer player) return player;
        ServerPlayer guardian = SandboxUtilities.guardianOwner(entity);
        if (guardian != null) return guardian;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal pet) return pet.getOwner() instanceof ServerPlayer player ? player : null;
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jem_world_boss_tiers")) return null;
        try {
            Object owner=Class.forName("com.siirio.jemworldbosstiers.api.HostedEncounterApi")
                    .getMethod("damageOwner", net.minecraft.world.entity.Entity.class).invoke(null, entity);
            return owner instanceof ServerPlayer player ? player : null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Hosted encounter owner API", failure);
        }
    }

}
