package dev.taste.engine.render.embeddium;

import dev.taste.engine.render.FrameSnapshot;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Shadow-only draw lists and renderer for the installed Embeddium 0.3.31 ABI. */
public final class EmbeddiumCaptureRenderer implements AutoCloseable {
    private static final String ROOT = "me.jellysquid.mods.sodium.client.";
    private static final String CHUNK = "render.chunk.";
    private final Method worldInstance;
    private final Field managerField;
    private final Field sectionsField;
    private final Method vertexType;
    private final Method isBuilt;
    private final Method isDisposed;
    private final Method sectionFlags;
    private final Method lastBuiltFrame;
    private final Method originX;
    private final Method originY;
    private final Method originZ;
    private final Method region;
    private final Method resetList;
    private final Method addSection;
    private final Method createCommands;
    private final Method enterDevice;
    private final Method exitDevice;
    private final Method drawPass;
    private final Method deleteRenderer;
    private final Field faceCulling;
    private final Object performanceOptions;
    private final Object device;
    private final Object solid;
    private final Object cutout;
    private final Class<?> listInterface;
    private final Constructor<?> newRenderer;
    private final Constructor<?> newList;
    private final Constructor<?> newMatrices;
    private final Constructor<?> newTransform;
    private Object renderer;
    private Object activeVertexType;
    private int drawFrame;

    public EmbeddiumCaptureRenderer() {
        try {
            Class<?> world = type("render.SodiumWorldRenderer");
            Class<?> manager = type(CHUNK + "RenderSectionManager");
            Class<?> section = type(CHUNK + "RenderSection");
            Class<?> list = type(CHUNK + "lists.ChunkRenderList");
            Class<?> matrices = type(CHUNK + "ChunkRenderMatrices");
            Class<?> pass = type(CHUNK + "terrain.TerrainRenderPass");
            Class<?> passes = type(CHUNK + "terrain.DefaultTerrainRenderPasses");
            Class<?> render = type(CHUNK + "DefaultChunkRenderer");
            Class<?> transform = type("render.viewport.CameraTransform");
            Class<?> renderDevice = type("gl.device.RenderDevice");
            Class<?> commands = type("gl.device.CommandList");
            listInterface = type(CHUNK + "lists.ChunkRenderListIterable");
            worldInstance = world.getMethod("instanceNullable");
            managerField = field(world, "renderSectionManager");
            sectionsField = field(manager, "sectionByPosition");
            vertexType = manager.getMethod("getVertexType");
            isBuilt = section.getMethod("isBuilt");
            isDisposed = section.getMethod("isDisposed");
            sectionFlags = section.getMethod("getFlags");
            lastBuiltFrame = section.getMethod("getLastBuiltFrame");
            originX = section.getMethod("getOriginX");
            originY = section.getMethod("getOriginY");
            originZ = section.getMethod("getOriginZ");
            region = section.getMethod("getRegion");
            resetList = list.getMethod("reset", int.class);
            addSection = list.getMethod("add", section);
            newList = list.getConstructor(region.getReturnType());
            newRenderer = render.getConstructor(renderDevice, vertexType.getReturnType());
            newMatrices = matrices.getConstructor(Matrix4fc.class, Matrix4fc.class);
            newTransform = transform.getConstructor(double.class, double.class, double.class);
            device = renderDevice.getField("INSTANCE").get(null);
            createCommands = renderDevice.getMethod("createCommandList");
            enterDevice = renderDevice.getMethod("enterManagedCode");
            exitDevice = renderDevice.getMethod("exitManagedCode");
            drawPass = render.getMethod("render", matrices, commands, listInterface, pass, transform);
            deleteRenderer = render.getMethod("delete", commands);
            solid = passes.getField("SOLID").get(null);
            cutout = passes.getField("CUTOUT").get(null);
            Object options = type("SodiumClientMod").getMethod("options").invoke(null);
            performanceOptions = options.getClass().getField("performance").get(options);
            faceCulling = performanceOptions.getClass().getField("useBlockFaceCulling");
        } catch (ReflectiveOperationException failure) {
            throw abiFailure(failure);
        }
    }

    public int draw(Matrix4f lightProjection, FrameSnapshot.Vec3d origin) {
        try {
            Object world = worldInstance.invoke(null);
            Object manager = world == null ? null : managerField.get(world);
            if (manager == null) return 0;
            Map<?, ?> sections = (Map<?, ?>) sectionsField.get(manager);
            FrustumIntersection frustum = new FrustumIntersection(lightProjection);
            Map<Object, Object> byRegion = new LinkedHashMap<>();
            int count = 0;
            drawFrame++;
            for (Object section : sections.values()) {
                if (!(boolean) isBuilt.invoke(section) || (boolean) isDisposed.invoke(section)
                        || ((int) sectionFlags.invoke(section) & 1) == 0) continue;
                float x = (float) ((int) originX.invoke(section) - origin.x());
                float y = (float) ((int) originY.invoke(section) - origin.y());
                float z = (float) ((int) originZ.invoke(section) - origin.z());
                if (!frustum.testAab(x - 0.18F, y, z - 0.18F, x + 16.18F, y + 16, z + 16.18F)) continue;
                Object sectionRegion = region.invoke(section);
                Object list = byRegion.get(sectionRegion);
                if (list == null) {
                    list = newList.newInstance(sectionRegion);
                    resetList.invoke(list, drawFrame);
                    byRegion.put(sectionRegion, list);
                }
                addSection.invoke(list, section);
                count++;
            }
            if (count == 0) return 0;
            drawLists(lightProjection, origin, vertexType.invoke(manager), new ArrayList<>(byRegion.values()));
            return count;
        } catch (ReflectiveOperationException failure) {
            throw abiFailure(failure);
        }
    }

    public long casterRevision() {
        try {
            Object world = worldInstance.invoke(null);
            Object manager = world == null ? null : managerField.get(world);
            if (manager == null) return 0;
            long revision = 0;
            Map<?, ?> sections = (Map<?, ?>) sectionsField.get(manager);
            for (Map.Entry<?, ?> entry : sections.entrySet()) {
                Object section = entry.getValue();
                if (!(boolean) isBuilt.invoke(section) || (boolean) isDisposed.invoke(section)) continue;
                long key = ((Number) entry.getKey()).longValue();
                int built = (int) lastBuiltFrame.invoke(section);
                revision += Long.rotateLeft(key * 0x9E3779B97F4A7C15L + built, built & 31);
            }
            return revision;
        } catch (ReflectiveOperationException failure) {
            throw abiFailure(failure);
        }
    }

    private void drawLists(Matrix4f projection, FrameSnapshot.Vec3d origin, Object format, List<Object> lists)
            throws ReflectiveOperationException {
        List<Object> reverse = new ArrayList<>(lists);
        Collections.reverse(reverse);
        Object iterable = Proxy.newProxyInstance(listInterface.getClassLoader(), new Class<?>[]{listInterface},
                (proxy, method, args) -> switch (method.getName()) {
                    case "iterator" -> args != null && args.length == 1 && Boolean.TRUE.equals(args[0])
                            ? reverse.iterator() : lists.iterator();
                    case "toString" -> "Taste light-frustum lists";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
        Object matrices = newMatrices.newInstance(projection, new Matrix4f());
        Object transform = newTransform.newInstance(origin.x(), origin.y(), origin.z());
        boolean priorCulling = faceCulling.getBoolean(performanceOptions);
        enterDevice.invoke(null);
        try (AutoCloseable commands = (AutoCloseable) createCommands.invoke(device)) {
            if (renderer == null || activeVertexType != format) {
                if (renderer != null) deleteRenderer.invoke(renderer, commands);
                renderer = null;
                renderer = newRenderer.newInstance(device, format);
                activeVertexType = format;
            }
            faceCulling.setBoolean(performanceOptions, false);
            drawPass.invoke(renderer, matrices, commands, iterable, solid, transform);
            drawPass.invoke(renderer, matrices, commands, iterable, cutout, transform);
        } catch (ReflectiveOperationException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("Could not close Embeddium shadow commands", failure);
        } finally {
            try {
                faceCulling.setBoolean(performanceOptions, priorCulling);
            } finally {
                exitDevice.invoke(null);
            }
        }
    }

    @Override
    public void close() {
        if (renderer == null) return;
        try {
            enterDevice.invoke(null);
            try (AutoCloseable commands = (AutoCloseable) createCommands.invoke(device)) {
                deleteRenderer.invoke(renderer, commands);
            } finally {
                renderer = null;
                activeVertexType = null;
                exitDevice.invoke(null);
            }
        } catch (ReflectiveOperationException failure) {
            throw abiFailure(failure);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not release Embeddium shadow commands", failure);
        }
    }

    private static Class<?> type(String suffix) throws ClassNotFoundException {
        return Class.forName(ROOT + suffix, false, EmbeddiumCaptureRenderer.class.getClassLoader());
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static IllegalStateException abiFailure(ReflectiveOperationException failure) {
        Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
        return new IllegalStateException("Embeddium 0.3.31 shadow draw failed", cause);
    }
}
