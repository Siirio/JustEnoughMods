package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRides;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import com.github.alexthe666.alexsmobs.entity.EntityEndergrade;
import com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {"com.github.alexthe666.alexsmobs.entity.EntityTusklin", "com.github.alexthe666.alexsmobs.entity.EntityEndergrade", "com.github.alexthe666.alexsmobs.entity.EntityKomodoDragon"}, remap = false)
public abstract class WildlifeQuestNativeRideMixin implements WildlifeQuestRides.NativeRide {
    @Override public String jem$wildlifeRideAction(ServerPlayer actor) {
        if ((Object)this instanceof EntityTusklin tusklin && tusklin.isSaddled()) return "v3_fauna_exotic_travel";
        if ((Object)this instanceof EntityEndergrade endergrade && endergrade.isSaddled()
                && (WildlifeQuestProof.item(actor.getMainHandItem()).equals("alexsmobs:chorus_on_a_stick")
                || WildlifeQuestProof.item(actor.getOffhandItem()).equals("alexsmobs:chorus_on_a_stick"))) return "v3_fauna_exotic_travel";
        if ((Object)this instanceof EntityKomodoDragon komodo && komodo.isTame() && actor.getUUID().equals(komodo.getOwnerUUID()) && komodo.isSaddled()
                && WildlifeQuestProof.matches(komodo, "komodo_native_owner", actor.getUUID())) return "v3_fauna_cargo_and_equipment";
        return "";
    }
}
