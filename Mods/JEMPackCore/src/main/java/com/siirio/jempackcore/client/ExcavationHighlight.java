package com.siirio.jempackcore.client;

import com.siirio.jempackcore.JEMPackCore;
import com.siirio.jempackcore.enchantment.CoreEnchantments;
import com.siirio.jempackcore.enchantment.ExcavationPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JEMPackCore.MOD_ID, value = Dist.CLIENT)
public final class ExcavationHighlight {
    private static final double OUTLINE_EXPANSION = 0.003D;

    @SubscribeEvent
    public static void render(RenderHighlightEvent.Block event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null
                || EnchantmentHelper.getItemEnchantmentLevel(CoreEnchantments.EXCAVATION.get(), player.getMainHandItem()) == 0) return;
        var origin = event.getTarget().getBlockPos();
        var camera = event.getCamera().getPosition();
        var consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        for (var pos : ExcavationPattern.positions(origin, event.getTarget().getDirection().getAxis())) {
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !player.getMainHandItem().isCorrectToolForDrops(state)) continue;
            var shape = state.getShape(level, pos);
            if (shape.isEmpty()) continue;
            AABB bounds = shape.bounds().move(pos).inflate(OUTLINE_EXPANSION).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(event.getPoseStack(), consumer, bounds, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private ExcavationHighlight() {
    }
}
