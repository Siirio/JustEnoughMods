package com.siirio.jembuilding.client;

import java.lang.reflect.Method;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.fml.ModList;

/** Uses the inspected Punchy 2.7e public trigger without making Punchy a hard runtime dependency. */
final class PunchyBlueprintBridge {
    private static Method forceTypeOnce;
    private static boolean resolved;

    static boolean play(InteractionHand hand, String animation) {
        if (!ModList.get().isLoaded("punchy")) return false;
        try {
            if (!resolved) {
                Class<?> manager = Class.forName("punchy.client.animation.ItemAnimationManager");
                forceTypeOnce = manager.getMethod("forceTypeOnce", InteractionHand.class, String.class, boolean.class);
                resolved = true;
            }
            if (forceTypeOnce == null) return false;
            forceTypeOnce.invoke(null, hand, animation, true);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            resolved = true;
            forceTypeOnce = null;
            return false;
        }
    }

    private PunchyBlueprintBridge() { }
}
