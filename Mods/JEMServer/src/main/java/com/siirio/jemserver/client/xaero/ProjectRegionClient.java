package com.siirio.jemserver.client.xaero;

import com.siirio.jemserver.MapNetwork;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public final class ProjectRegionClient {
    private static UUID project;
    private static ResourceLocation dimension;
    private static boolean planning;
    private static boolean dragging;
    private static int startX, startZ, currentX, currentZ;
    private ProjectRegionClient() {}

    public static void begin(UUID projectId, ResourceLocation targetDimension) {
        project = projectId; dimension = targetDimension; planning = true; dragging = false;
    }
    public static boolean planning(ResourceLocation targetDimension) { return planning && targetDimension != null && targetDimension.equals(dimension); }
    public static boolean press(ResourceLocation targetDimension, int x, int z) {
        if (!planning(targetDimension)) return false;
        dragging = true; startX = currentX = x; startZ = currentZ = z; return true;
    }
    public static void move(ResourceLocation targetDimension, int x, int z) { if (dragging && planning(targetDimension)) { currentX = x; currentZ = z; } }
    public static boolean release(Screen parent, ResourceLocation targetDimension, int x, int z) {
        if (!dragging || !planning(targetDimension)) return false;
        currentX = x; currentZ = z; dragging = false; planning = false;
        if (project == null) Minecraft.getInstance().setScreen(new ProjectNameScreen(parent, dimension, startX, startZ, currentX, currentZ));
        else MapNetwork.extendProject(project, dimension, startX, startZ, currentX, currentZ);
        project = null; return true;
    }
    public static void render(GuiGraphics graphics, ResourceLocation targetDimension, double cameraX, double cameraZ, double scale, int width, int height) {
        if (!dragging || !planning(targetDimension)) return;
        int minX = Math.min(startX, currentX), maxX = Math.max(startX, currentX) + 1;
        int minZ = Math.min(startZ, currentZ), maxZ = Math.max(startZ, currentZ) + 1;
        int left = (int) Math.floor(width / 2.0 + (minX - cameraX) * scale), right = (int) Math.floor(width / 2.0 + (maxX - cameraX) * scale);
        int top = (int) Math.floor(height / 2.0 + (minZ - cameraZ) * scale), bottom = (int) Math.floor(height / 2.0 + (maxZ - cameraZ) * scale);
        int color = 0xFFDA8A45;
        graphics.fill(left, top, right, Math.min(bottom, top + 2), color); graphics.fill(left, Math.max(top, bottom - 2), right, bottom, color);
        graphics.fill(left, top, Math.min(right, left + 2), bottom, color); graphics.fill(Math.max(left, right - 2), top, right, bottom, color);
        graphics.drawString(Minecraft.getInstance().font, (maxX - minX) + " × " + (maxZ - minZ), left + 4, top + 4, 0xFFFFE4BE, true);
    }
}
