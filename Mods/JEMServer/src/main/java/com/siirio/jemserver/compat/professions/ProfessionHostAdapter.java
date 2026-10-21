package com.siirio.jemserver.compat.professions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/** Optional development module contract; production compiles without Professions or Pufferfish classes. */
public final class ProfessionHostAdapter {
    private static final class NativeApi {
        static final Class<?> PROFESSION = type("com.siirio.jemprofessions.progress.ProfessionId");
        static final Class<?> API = type("com.siirio.jemprofessions.api.ProfessionApi");
        static final Method XP = method("xp", long.class, ServerPlayer.class, PROFESSION);
        static final Method LEVEL = method("level", int.class, ServerPlayer.class, PROFESSION);
        static final Method OPEN = method("openScreen", void.class, ServerPlayer.class);
        static final Object[] PROFESSIONS = professions();
        private static Class<?> type(String name) {
            try { return Class.forName(name); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed Professions API unavailable", failure); }
        }
        private static Method method(String name, Class<?> result, Class<?>... arguments) {
            try {
                Method method = API.getMethod(name, arguments);
                if (method.getReturnType() != result) throw new IllegalStateException("Installed Professions API return type changed: " + name);
                return method;
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed Professions API method unavailable: " + name, failure); }
        }
        private static Object[] professions() {
            Object[] values = PROFESSION.getEnumConstants();
            if (values == null) throw new IllegalStateException("Installed ProfessionId must remain an enum");
            return values;
        }
    }
    private ProfessionHostAdapter() { }
    private static Object invoke(Method method, Object... arguments) {
        try { return method.invoke(null, arguments); }
        catch (InvocationTargetException failure) { throw new IllegalStateException("Installed Professions API failed: " + method.getName(), failure.getCause()); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot invoke installed Professions API: " + method.getName(), failure); }
    }
    public static void open(ServerPlayer player) { invoke(NativeApi.OPEN, player); }
    public static ListTag snapshot(ServerPlayer player) {
        ListTag result = new ListTag();
        for (Object profession : NativeApi.PROFESSIONS) {
            long xp = (long) invoke(NativeApi.XP, player, profession);
            if (xp <= 0L) continue;
            CompoundTag row = new CompoundTag(); row.putString("id", ((Enum<?>) profession).name());
            row.putLong("xp", xp); row.putInt("level", (int) invoke(NativeApi.LEVEL, player, profession)); result.add(row);
        }
        return result;
    }
}
