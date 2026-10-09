package com.siirio.jemcompat.feature.kaleidoscope;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class VoidWalkerEvents {
    private static final ResourceLocation VOID_WALKER=new ResourceLocation("kaleidoscope_end","void_walker");
    private static final double NATIVE_SUPPORT_OFFSET=-0.3D;

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.onGround()||player.isShiftKeyDown()) return;
        var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(VOID_WALKER);
        if(enchantment==null||EnchantmentHelper.getEnchantmentLevel(enchantment,player)<=0) return;
        BlockPos support=BlockPos.containing(player.position().add(0,NATIVE_SUPPORT_OFFSET,0));
        if(player.level().getBlockState(support).isAir()) player.fallDistance=0;
    }

    private VoidWalkerEvents() {}
}
