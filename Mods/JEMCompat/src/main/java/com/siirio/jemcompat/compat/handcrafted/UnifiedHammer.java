package com.siirio.jemcompat.compat.handcrafted;

import earth.terrarium.handcrafted.common.blocks.base.Hammerable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** The existing BBB hammer also invokes Handcrafted's native hammerable block contract. */
public final class UnifiedHammer {
    private static final ResourceLocation HAMMER = new ResourceLocation("bbb", "hammer");

    private UnifiedHammer() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !HAMMER.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof Hammerable hammerable)
                || !event.getEntity().mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())) return;
        hammerable.onHammer(event.getLevel(), event.getPos(), event.getLevel().getBlockState(event.getPos()),
                event.getFace(), event.getEntity(), event.getHitVec().getLocation());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
