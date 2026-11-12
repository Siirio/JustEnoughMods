package dev.taste.engine.render.embeddium;

import dev.taste.engine.effects.VegetationMotion;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;

/** Bridges the pinned optional mesher without linking Embeddium types on Vanilla. */
public final class EmbeddiumVegetation {
    private record Access(Method state, Method origin) { }

    private static final ClassValue<Access> CONTEXTS = new ClassValue<>() {
        @Override protected Access computeValue(Class<?> type) {
            try {
                return new Access(type.getMethod("state"), type.getMethod("origin"));
            } catch (NoSuchMethodException error) {
                throw new IllegalStateException("Embeddium block context ABI changed", error);
            }
        }
    };
    private static final ClassValue<Field> Y_COORDINATES = new ClassValue<>() {
        @Override protected Field computeValue(Class<?> type) {
            try {
                return type.getField("y");
            } catch (NoSuchFieldException error) {
                throw new IllegalStateException("Embeddium terrain vertex ABI changed", error);
            }
        }
    };

    private EmbeddiumVegetation() { }

    public static void begin(Object context) {
        try {
            Access access = CONTEXTS.get(context.getClass());
            var state = (BlockState) access.state().invoke(context);
            var origin = (Vector3fc) access.origin().invoke(context);
            VegetationMotion.beginMesh(state, origin.y());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not read the Embeddium block context", error);
        }
    }

    public static int flags(Object vertex) {
        if (!VegetationMotion.hasMesh()) return 0;
        try {
            return VegetationMotion.vertex(Y_COORDINATES.get(vertex.getClass()).getFloat(vertex)) << 3;
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not read the Embeddium terrain vertex", error);
        }
    }
}
