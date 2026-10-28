package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import com.justenoughmods.achievementguide.integration.home.HomeItemUseProof;
import java.util.Deque;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class HomeChimePlaceQuestMixin {
    @Unique private final ThreadLocal<Deque<HomeInteractionProof.Call>> jem$homeCalls = ThreadLocal.withInitial(HomeInteractionProof::calls);

    @Inject(method = "place", at = @At("HEAD"))
    private void jem$before(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.begin(jem$homeCalls.get(), context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getHand());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void jem$after(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        HomeInteractionProof.Call call = HomeInteractionProof.finish(jem$homeCalls.get(), context.getLevel(), context.getClickedPos(), context.getPlayer(), callback.getReturnValue());
        if (call != null && HomeInteractionProof.item(call.material).equals("chimes:carved_bamboo_chimes")
                && HomeInteractionProof.block(call.level.getBlockState(call.pos)).equals("chimes:carved_bamboo_chimes")
                && call.debitedOne()) HomeItemUseProof.defer(HomeInteractionProof.actor(call.player),
                        () -> HomeInteractionProof.block(call.level.getBlockState(call.pos)).equals("chimes:carved_bamboo_chimes"),
                        () -> HomeInteractionProof.emit(call.player, "v3_home_chime_hang"));
    }
}
