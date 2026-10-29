package com.siirio.jemcompat.feature.kaleidoscope;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class VoidWalkerEvents {
    private static final ResourceLocation VOID_WALKER=new ResourceLocation("kaleidoscope_end","void_walker");
    private static final String DISABLED="JemCompatVoidWalkerDisabled";
    private static final double NATIVE_SUPPORT_OFFSET=-0.3D;

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||!player.onGround()||player.isShiftKeyDown()) return;
        var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(VOID_WALKER);
        if(enchantment==null||!enabled(player)||EnchantmentHelper.getEnchantmentLevel(enchantment,player)<=0) return;
        BlockPos support=BlockPos.containing(player.position().add(0,NATIVE_SUPPORT_OFFSET,0));
        if(player.level().getBlockState(support).isAir()) player.fallDistance=0;
    }

    public static int effectiveLevel(Enchantment enchantment, net.minecraft.world.entity.LivingEntity entity) {
        int level=EnchantmentHelper.getEnchantmentLevel(enchantment,entity);
        ResourceLocation id=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        return VOID_WALKER.equals(id)&&!enabled(entity)?0:level;
    }

    public static boolean enabled(net.minecraft.world.entity.LivingEntity entity) {
        return !(entity instanceof net.minecraft.world.entity.player.Player player) || !player.getPersistentData().getBoolean(DISABLED);
    }

    static ToggleResult toggle(ServerPlayer player) {
        var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(VOID_WALKER);
        boolean equipped=enchantment!=null&&EnchantmentHelper.getEnchantmentLevel(enchantment,player)>0;
        if(equipped) {
            boolean enabled=!enabled(player);
            if(enabled) player.getPersistentData().remove(DISABLED);
            else player.getPersistentData().putBoolean(DISABLED,true);
        }
        return new ToggleResult(enabled(player),equipped);
    }

    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if(event.getEntity() instanceof ServerPlayer player&&event.getOriginal().getPersistentData().getBoolean(DISABLED))
            player.getPersistentData().putBoolean(DISABLED,true);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event); }
    private static void sync(PlayerEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) VoidWalkerNetwork.status(player,enabled(player),false,false);
    }

    record ToggleResult(boolean enabled,boolean equipped) { }

    private VoidWalkerEvents() {}
}
