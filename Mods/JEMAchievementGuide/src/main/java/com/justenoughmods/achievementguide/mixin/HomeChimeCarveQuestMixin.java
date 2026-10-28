package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import java.util.Deque;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.cicada.chimes.event.EventsForge", remap = false)
public abstract class HomeChimeCarveQuestMixin {
    @Unique private static final ThreadLocal<Deque<HomeInteractionProof.Call>> jem$homeCalls = ThreadLocal.withInitial(HomeInteractionProof::calls);

    @Inject(method = "rightClick", at = @At("HEAD"))
    private static void jem$before(PlayerInteractEvent.RightClickBlock event, CallbackInfo callback) {
        HomeInteractionProof.begin(jem$homeCalls.get(), event.getLevel(), event.getPos(), event.getEntity(), event.getHand());
    }

    @Inject(method = "rightClick", at = @At("RETURN"))
    private static void jem$after(PlayerInteractEvent.RightClickBlock event, CallbackInfo callback) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(jem$homeCalls.get(), event.getLevel(), event.getPos(), event.getEntity(), InteractionResult.SUCCESS);
        if (call != null && !event.isCanceled() && call.material.getItem() instanceof AxeItem
                && HomeInteractionProof.block(call.before).equals("chimes:bamboo_chimes")
                && HomeInteractionProof.block(call.level.getBlockState(call.pos)).equals("chimes:carved_bamboo_chimes")) {
            HomeInteractionProof.emit(call.player, "v3_home_chime");
        }
    }
}
