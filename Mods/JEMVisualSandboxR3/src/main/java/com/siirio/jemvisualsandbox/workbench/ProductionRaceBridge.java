package com.siirio.jemvisualsandbox.workbench;

import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** Optional native adapter: dedicated JEMRoles remains the sole gameplay and persistence owner. */
public final class ProductionRaceBridge {
    private static final String[] RACES = {"ANCESTORS", "VOID_WALKERS", "BORN_IN_BLOOM", "INFECTED_BY_SCULK", "NETHER_HEARTEDS"};
    private ProductionRaceBridge() {}

    public static boolean available(ServerPlayer player) {
        return player != null && player.server.isDedicatedServer() && ModList.get().isLoaded("jem_roles");
    }
    public static boolean authenticated(ServerPlayer player) {
        return available(player) && optional(call(api("com.siirio.jemidentity.api.IdentityPolicy"), "resolve",
                new Class<?>[]{ServerPlayer.class}, player)).isPresent();
    }
    public static RaceExperience.Status status(ServerPlayer player) {
        if (!authenticated(player)) return null;
        Optional<?> record = optional(call(api("com.siirio.jemroles.race.RaceService"), "record", new Class<?>[]{ServerPlayer.class}, player));
        int[] tiers = {1, 1, 1, 1, 1}; int[] progress = new int[5];
        if (record.isEmpty()) return new RaceExperience.Status(-1, false, tiers, progress, 0L, 0L, false, 0);
        Object value = record.get(); Object nativeRace = call(value, "race", new Class<?>[0]);
        if (!(nativeRace instanceof Enum<?> race) || race.ordinal() >= RACES.length || !RACES[race.ordinal()].equals(race.name()))
            throw incompatible("Unexpected JEMRoles race identity", null);
        int index = race.ordinal();
        tiers[index] = number(call(value, "spellTier", new Class<?>[0])).intValue();
        progress[index] = number(call(value, "spellProgress", new Class<?>[0])).intValue();
        long now = player.level().getGameTime();
        long signature = remaining(value, "signature", now);
        String utilityKey = index == 1 ? player.isShiftKeyDown() ? "gravity" : "void_projectile" : index == 4 ? "lava_drink" : null;
        long utility = utilityKey == null ? 0 : remaining(value, utilityKey, now);
        return new RaceExperience.Status(index, true, tiers, progress, signature, utility, false, 0);
    }
    public static void choose(ServerPlayer player, int race) {
        if (!authenticated(player) || race < 0 || race >= RACES.length) return;
        Class<?> raceType = api("com.siirio.jemroles.race.RaceId");
        Object next = constant(raceType, RACES[race]);
        Object result = call(api("com.siirio.jemroles.race.RaceService"), "choose", new Class<?>[]{ServerPlayer.class, raceType}, player, next);
        if (!(result instanceof Enum<?> outcome)) throw incompatible("Unexpected JEMRoles selection result", null);
        if (outcome.name().equals("CHANGE_LOCKED")) player.sendSystemMessage(Component.translatable("jem.roles.change_locked"));
        else if (outcome.name().equals("UNAUTHENTICATED")) player.sendSystemMessage(Component.translatable("jem.roles.identity_required"));
        call(api("com.siirio.jemroles.network.RolesNetwork"), "sendStatus", new Class<?>[]{ServerPlayer.class, boolean.class}, player, false);
    }
    public static boolean cast(ServerPlayer player) {
        if (!authenticated(player)) return false;
        Object result = call(api("com.siirio.jemroles.trait.RaceAbilities"), "castSignature", new Class<?>[]{ServerPlayer.class}, player);
        if (!(result instanceof Boolean success)) throw incompatible("Unexpected JEMRoles cast result", null);
        return success;
    }
    public static void utility(ServerPlayer player, boolean pressed) {
        if (!authenticated(player)) return;
        Class<?> actionType = api("com.siirio.jemroles.network.RaceActionPacket$Action");
        call(api("com.siirio.jemroles.trait.RaceAbilities"), "handle", new Class<?>[]{ServerPlayer.class, actionType},
                player, constant(actionType, pressed ? "UTILITY_PRESS" : "UTILITY_RELEASE"));
    }
    private static long remaining(Object record, String key, long now) {
        return number(call(record, "remaining", new Class<?>[]{String.class, long.class}, key, now)).longValue();
    }
    private static Class<?> api(String name) {
        try { return Class.forName(name, false, ProductionRaceBridge.class.getClassLoader()); }
        catch (ClassNotFoundException | LinkageError failure) { throw incompatible("Missing native race API " + name, failure); }
    }
    private static Object constant(Class<?> type, String name) {
        Object[] values = type.getEnumConstants();
        if (values != null) for (Object value : values) if (value instanceof Enum<?> entry && entry.name().equals(name)) return value;
        throw incompatible("Missing native race enum value " + type.getName() + "." + name, null);
    }
    private static Optional<?> optional(Object value) {
        if (value instanceof Optional<?> result) return result;
        throw incompatible("Native race API did not return Optional", null);
    }
    private static Number number(Object value) {
        if (value instanceof Number result) return result;
        throw incompatible("Native race API did not return a number", null);
    }
    private static Object call(Object target, String name, Class<?>[] parameters, Object... arguments) {
        Class<?> type = target instanceof Class<?> staticType ? staticType : target.getClass();
        try {
            Method method = type.getMethod(name, parameters);
            return method.invoke(target instanceof Class<?> ? null : target, arguments);
        } catch (ReflectiveOperationException | LinkageError failure) {
            Throwable cause = failure instanceof InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : failure;
            throw incompatible("Native race API failed: " + type.getName() + "." + name, cause);
        }
    }
    private static IllegalStateException incompatible(String message, Throwable cause) {
        LogUtils.getLogger().error("JEM Races native integration is incompatible: " + message, cause);
        return new IllegalStateException("JEM Races native integration is incompatible: " + message, cause);
    }
}
