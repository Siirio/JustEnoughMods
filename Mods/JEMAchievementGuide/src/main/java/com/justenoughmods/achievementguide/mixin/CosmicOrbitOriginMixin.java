package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.CosmicQuestOrigin;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.sweenus.simplybows.world.CosmicOrbitManager$ActiveOrbit", remap = false)
public abstract class CosmicOrbitOriginMixin implements CosmicQuestOrigin {
    @Unique private UUID jem$origin;
    @Override public UUID jem$questCosmicActor() { return jem$origin; }
    @Override public void jem$questCosmicActor(UUID actor) { jem$origin = actor; }
}
