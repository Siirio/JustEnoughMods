package com.siirio.jemserver.client.smp;

import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;

final class EventShaderState {
    private static final Object API = api();
    private static final Method SHADOW = method(API, "isRenderingShadowPass");

    static boolean shadowPass() {
        return invokeBoolean(API, SHADOW);
    }

    private static Object api() {
        if (!ModList.get().isLoaded("oculus")) {
            return null;
        }
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return type.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method method(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getClass().getMethod(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean invokeBoolean(Object owner, Method method) {
        if (owner == null || method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(owner));
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}

