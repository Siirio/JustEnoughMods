package com.siirio.jemcompat.compat.handcrafted;

import earth.terrarium.handcrafted.common.blocks.base.Hammerable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** The player-facing JEM hammer invokes Handcrafted's native hammerable block contract. */
public final class UnifiedHammer {
    private static final ResourceLocation JEM_HAMMER = new ResourceLocation("jem_building", "builder_hammer");
    private static final ResourceLocation LEGACY_HAMMER = new ResourceLocation("bbb", "hammer");

    private UnifiedHammer() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        ResourceLocation held = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        ResourceLocation unified = ModList.get().isLoaded("jem_building") ? JEM_HAMMER : LEGACY_HAMMER;
        if (event.isCanceled() || !unified.equals(held)
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof Hammerable hammerable)
                || !event.getEntity().mayUseItemAt(event.getPos(), event.getFace(), event.getItemStack())) return;
        hammerable.onHammer(event.getLevel(), event.getPos(), event.getLevel().getBlockState(event.getPos()),
                event.getFace(), event.getEntity(), event.getHitVec().getLocation());
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
