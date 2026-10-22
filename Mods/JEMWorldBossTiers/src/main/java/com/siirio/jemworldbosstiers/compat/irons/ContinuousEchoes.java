package com.siirio.jemworldbosstiers.compat.irons;

import io.redspace.ironsspellbooks.entity.spells.EchoingStrikeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Merge already-computed native pre-mitigation contributions, without running the proc again. */
public final class ContinuousEchoes {
    private static final Map<ServerLevel, Map<Key, Window>> WINDOWS = new HashMap<>();
    private ContinuousEchoes() { }

    public static boolean spawn(Level world, Entity entity, LivingHurtEvent event) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof EchoingStrikeEntity echo)
                || !event.getSource().is(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                        new net.minecraft.resources.ResourceLocation("cataclysm", "shredder")))) {
            return world.addFreshEntity(entity);
        }
        Entity owner = event.getSource().getEntity();
        if (owner == null) return world.addFreshEntity(entity);
        Map<Key, Window> windows = WINDOWS.computeIfAbsent(level, ignored -> new HashMap<>());
        Key key = new Key(owner.getUUID(), event.getEntity().getUUID());
        long now = level.getGameTime();
        Window window = windows.get(key);
        if (window != null && now < window.until && !window.echo.isRemoved()) {
            window.echo.setDamage(window.echo.getDamage() + echo.getDamage());
            return true;
        }
        boolean added = world.addFreshEntity(entity);
        if (added) windows.put(key, new Window(echo, now + 5));
        return added;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        WINDOWS.entrySet().removeIf(entry -> {
            long now = entry.getKey().getGameTime();
            entry.getValue().values().removeIf(window -> now >= window.until || window.echo.isRemoved());
            return entry.getValue().isEmpty();
        });
    }

    @SubscribeEvent public static void stop(ServerStoppedEvent event) { WINDOWS.clear(); }
    private record Key(UUID owner, UUID target) { }
    private record Window(EchoingStrikeEntity echo, long until) { }
}
