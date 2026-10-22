package com.siirio.jemeconomy.mixin;

import com.siirio.jemeconomy.merchant.NativeMerchantOffersAccess;
import com.siirio.jemeconomy.merchant.ProfessionTradePools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native offer serialization and stock objects intact while projecting optional Coin extensions. */
@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerOfferProjectionMixin implements NativeMerchantOffersAccess {
    @Unique private static final String JEM_FORMAT = "JemEconomySeparatedOffersV2";
    @Unique private static final String JEM_EXTENSIONS = "JemEconomyExtensionOffersV2";
    @Unique private static final String JEM_LEGACY = "JemEconomyLegacyMixedOffers";
    @Shadow protected MerchantOffers offers;
    @Shadow protected abstract void updateTrades();
    @Unique private MerchantOffers jem$extensions = new MerchantOffers();
    @Unique private boolean jem$separated;
    @Unique private boolean jem$legacyMixed;
    @Unique private int jem$nativeGeneration;

    @Override public MerchantOffers jem$extensions() { return jem$extensions; }
    @Override public void jem$generatingOffers(boolean generating) { jem$nativeGeneration += generating ? 1 : -1; }
    @Override public boolean jem$legacyMixedOffers() {
        if (!jem$separated && offers != null) {
            jem$legacyMixed |= ProfessionTradePools.legacyMixed((AbstractVillager) (Object) this, offers);
            jem$separated = true;
        }
        return jem$legacyMixed;
    }

    @Inject(method = "getOffers", at = @At("RETURN"), cancellable = true)
    private void jem$project(CallbackInfoReturnable<MerchantOffers> callback) {
        var merchant = (AbstractVillager) (Object) this;
        if (!merchant.level().isClientSide && jem$nativeGeneration == 0)
            callback.setReturnValue(ProfessionTradePools.project(merchant, callback.getReturnValue()));
    }

    @Redirect(method = "getOffers", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/AbstractVillager;updateTrades()V"))
    private void jem$initialGeneration(AbstractVillager merchant) {
        jem$nativeGeneration++;
        try { updateTrades(); }
        finally { jem$nativeGeneration--; }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void jem$save(CompoundTag tag, CallbackInfo callback) {
        // Vanilla calls getOffers while saving; persist its original field, never the projected view.
        if (offers != null && !offers.isEmpty()) tag.put("Offers", offers.createTag());
        else tag.remove("Offers");
        tag.putBoolean(JEM_LEGACY, jem$legacyMixedOffers());
        tag.putBoolean(JEM_FORMAT, jem$separated);
        tag.put(JEM_EXTENSIONS, jem$extensions.createTag());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) {
        jem$separated = tag.getBoolean(JEM_FORMAT);
        jem$legacyMixed = tag.getBoolean(JEM_LEGACY);
        jem$extensions = new MerchantOffers(tag.getCompound(JEM_EXTENSIONS));
    }
}
