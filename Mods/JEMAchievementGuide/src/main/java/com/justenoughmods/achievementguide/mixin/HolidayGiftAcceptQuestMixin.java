package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.events.HolidayGiftProof;
import com.justenoughmods.achievementguide.integration.events.HolidayGiftView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.siirio.jemserver.smp.events.calendar.HolidayActivities", remap = false)
public abstract class HolidayGiftAcceptQuestMixin {
    @Inject(method = "acceptGift", at = @At("HEAD"))
    private static void jem$before(ServerPlayer target, UUID id, CallbackInfoReturnable<Boolean> callback) { HolidayGiftProof.begin(target, id); }
    @Redirect(method = "acceptGift", at = @At(value = "INVOKE", target =
            "Lcom/siirio/jemserver/smp/events/calendar/CalendarEventData;gift(Ljava/util/UUID;)Ljava/util/Optional;"))
    private static Optional<CompoundTag> jem$nativeGift(@Coerce Object owner, UUID id) {
        Optional<CompoundTag> original = ((HolidayGiftView)owner).jem$questGift(id);
        HolidayGiftProof.read(id, original);
        return original;
    }
    @Inject(method = "acceptGift", at = @At(value = "INVOKE", target =
            "Lcom/siirio/jemserver/smp/events/calendar/CalendarEventData;removeGift(Ljava/util/UUID;)V", shift = At.Shift.AFTER))
    private static void jem$removed(ServerPlayer target, UUID id, CallbackInfoReturnable<Boolean> callback) { HolidayGiftProof.removed(id); }
    @Inject(method = "acceptGift", at = @At("RETURN"))
    private static void jem$after(ServerPlayer target, UUID id, CallbackInfoReturnable<Boolean> callback) { HolidayGiftProof.end(target, id, callback.getReturnValue()); }
}
