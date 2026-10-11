package com.siirio.jemworldbosstiers.encounter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.LivingEntity;

final class BossPresentation {
    private static final Map<Class<?>, List<Field>> BOSS_EVENTS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> BAR_VISIBILITY = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> MUSIC = new ConcurrentHashMap<>();

    private BossPresentation() {}

    static void suppress(LivingEntity boss) {
        visibility(boss, false);
    }

    static void restore(LivingEntity boss) {
        visibility(boss, true);
    }

    private static void visibility(LivingEntity boss, boolean visible) {
        for (Field field : BOSS_EVENTS.computeIfAbsent(boss.getClass(), BossPresentation::bossEvents)) {
            try {
                if (field.get(boss) instanceof ServerBossEvent event) event.setVisible(visible);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
        invoke(BAR_VISIBILITY, boss, "setBarVisible", visible);
        invoke(MUSIC, boss, "setPlayingMusic", visible);
    }

    private static List<Field> bossEvents(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && LivingEntity.class.isAssignableFrom(current); current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!ServerBossEvent.class.isAssignableFrom(field.getType()) || !field.trySetAccessible()) continue;
                fields.add(field);
            }
        }
        return List.copyOf(fields);
    }

    private static void invoke(Map<Class<?>, Optional<Method>> methods, LivingEntity boss, String name, boolean value) {
        Optional<Method> method = methods.computeIfAbsent(boss.getClass(), type -> {
            try {
                return Optional.of(type.getMethod(name, boolean.class));
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        });
        if (method.isEmpty()) return;
        try {
            method.get().invoke(boss, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
