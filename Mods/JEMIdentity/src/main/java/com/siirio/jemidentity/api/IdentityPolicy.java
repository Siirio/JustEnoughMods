package com.siirio.jemidentity.api;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class IdentityPolicy {
    private static volatile Method serverLoginIsLocked;
    private static volatile boolean serverLoginLookupAttempted;

    private IdentityPolicy() {
    }

    public static Optional<VerifiedPrincipal> resolve(ServerPlayer player) {
        if (player == null) return Optional.empty();
        MinecraftServer server = player.getServer();
        if (server == null) return Optional.empty();
        if (!server.isDedicatedServer()) {
            return Optional.of(new VerifiedPrincipal(player.getUUID(), PrincipalKind.LOCAL_OWNER));
        }
        if (server.usesAuthentication()) {
            return Optional.of(new VerifiedPrincipal(player.getUUID(), PrincipalKind.ONLINE_SESSION));
        }
        if (!ModList.get().isLoaded("serverlogin") || isServerLoginLocked(player)) return Optional.empty();
        return Optional.of(new VerifiedPrincipal(player.getUUID(), PrincipalKind.SERVER_LOGIN));
    }

    public static boolean permitsAccountMutation(ServerPlayer player) {
        return resolve(player).isPresent();
    }

    private static boolean isServerLoginLocked(ServerPlayer player) {
        try {
            Method method = serverLoginMethod();
            return method == null || (boolean) method.invoke(null, player);
        } catch (ReflectiveOperationException | LinkageError failure) {
            return true;
        }
    }

    private static Method serverLoginMethod() {
        if (!serverLoginLookupAttempted) {
            synchronized (IdentityPolicy.class) {
                if (!serverLoginLookupAttempted) {
                    try {
                        Class<?> gate = Class.forName("ru.iwoss.serverlogin.gate.AuthGate", false, IdentityPolicy.class.getClassLoader());
                        serverLoginIsLocked = gate.getMethod("isLocked", ServerPlayer.class);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        serverLoginIsLocked = null;
                    }
                    serverLoginLookupAttempted = true;
                }
            }
        }
        return serverLoginIsLocked;
    }
}
