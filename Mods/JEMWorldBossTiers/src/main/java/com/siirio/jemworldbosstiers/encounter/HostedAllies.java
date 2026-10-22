package com.siirio.jemworldbosstiers.encounter;

import net.minecraft.world.entity.Entity;

/** Optional native ownership contracts, never arbitrary entity NBT. */
public final class HostedAllies {
    private HostedAllies() {}

    public static Entity owner(Entity entity) {
        Entity summoner = invoke(entity, "io.redspace.ironsspellbooks.entity.mobs.IMagicSummon", "getSummoner");
        if (summoner != null) return summoner;
        Entity recruit = invoke(entity, "com.talhanation.recruits.entities.AbstractRecruitEntity", "getOwner");
        if (recruit != null) return recruit;
        try {
            Class<?> api = Class.forName("com.siirio.jemvisualsandbox.workbench.SandboxUtilities", false, HostedAllies.class.getClassLoader());
            Object owner = api.getMethod("guardianOwner", Entity.class).invoke(null, entity);
            return owner instanceof Entity value ? value : null;
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Installed race guardian ownership contract failed", failure);
        }
    }

    private static Entity invoke(Entity entity, String contract, String method) {
        try {
            Class<?> type = Class.forName(contract, false, HostedAllies.class.getClassLoader());
            if (!type.isInstance(entity)) return null;
            Object owner = type.getMethod(method).invoke(entity);
            return owner instanceof Entity value ? value : null;
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Installed ally ownership contract failed: " + contract, failure);
        }
    }
}
