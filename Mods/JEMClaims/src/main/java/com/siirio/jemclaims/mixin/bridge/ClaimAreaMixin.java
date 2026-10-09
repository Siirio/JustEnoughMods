package com.siirio.jemclaims.mixin.bridge;

import com.siirio.jemclaims.ClaimSelectionGuard;
import com.siirio.jemclaims.FlanBridge;
import com.siirio.jemclaims.ClaimUnion;
import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.player.PlayerClaimData;
import io.github.flemmli97.flan.api.data.IPlayerData;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.flemmli97.flan.claim.ClaimStorage", remap = false)
public abstract class ClaimAreaMixin {
    @Inject(method = "createClaim", at = @At("HEAD"), cancellable = true)
    private void jemClaims$create(BlockPos first, BlockPos second, ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (!ClaimSelectionGuard.validate(player, first, second)) callback.setReturnValue(false);
    }

    @Inject(method = "resizeClaim", at = @At("HEAD"), cancellable = true)
    private void jemClaims$resize(@Coerce Object claim, BlockPos previous, BlockPos target, ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (!FlanBridge.validateResize(claim, previous, target, player)) callback.setReturnValue(false);
    }

    @Redirect(method = "conflicts", at = @At(value = "INVOKE", target = "Lio/github/flemmli97/flan/claim/Claim;intersects(Lio/github/flemmli97/flan/claim/Claim;)Z"))
    private boolean jemClaims$foreignOverlap(Claim proposed, Claim existing, Claim requested, Claim replaced) {
        Claim ownerClaim = replaced == null ? proposed : replaced;
        return (ownerClaim.isAdminClaim() || existing.isAdminClaim() || !java.util.Objects.equals(ownerClaim.getOwner(), existing.getOwner()))
                && proposed.intersects(existing);
    }

    @Redirect(method = "createClaim", at = @At(value = "INVOKE", target = "Lio/github/flemmli97/flan/player/PlayerClaimData;canUseClaimBlocks(I)Z"))
    private boolean jemClaims$createBudget(PlayerClaimData data, int area, BlockPos first, BlockPos second, ServerPlayer player) {
        return data.canUseClaimBlocks(ClaimUnion.additional(player.serverLevel(), player.getUUID(), first, second, null));
    }

    @Redirect(method = "resizeClaim", at = @At(value = "INVOKE", target = "Lio/github/flemmli97/flan/api/data/IPlayerData;canUseClaimBlocks(I)Z"))
    private boolean jemClaims$resizeBudget(IPlayerData data, int area, Claim claim, BlockPos previous, BlockPos target, ServerPlayer player) {
        var box = claim.getDimensions();
        BlockPos opposite = new BlockPos(previous.getX() == box.minX() ? box.maxX() : box.minX(), previous.getY(),
                previous.getZ() == box.minZ() ? box.maxZ() : box.minZ());
        return data.canUseClaimBlocks(ClaimUnion.additional(claim.getLevel(), claim.getOwner(), opposite, target, claim));
    }
    @Inject(method = "transferOwner(Lio/github/flemmli97/flan/claim/Claim;Ljava/util/UUID;)Z", at = @At("HEAD"), cancellable = true)
    private void jemClaims$transferOverlap(Claim claim, java.util.UUID owner, CallbackInfoReturnable<Boolean> callback) {
        boolean conflict = io.github.flemmli97.flan.claim.ClaimStorage.get(claim.getLevel()).getClaims().values().stream()
                .flatMap(java.util.Collection::stream).anyMatch(other -> !other.equals(claim) && !other.isRemoved()
                        && !java.util.Objects.equals(owner, other.getOwner()) && claim.intersects(other));
        if (conflict) callback.setReturnValue(false);
    }
}
