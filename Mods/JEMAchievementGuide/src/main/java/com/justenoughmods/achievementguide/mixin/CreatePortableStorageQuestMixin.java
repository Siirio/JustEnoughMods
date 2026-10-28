package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCargoConnection;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity", remap = false)
public abstract class CreatePortableStorageQuestMixin implements CreateCargoConnection {
    @Shadow protected Entity connectedEntity;

    @Override public CarriageContraptionEntity jem$connectedCarriage() {
        return connectedEntity instanceof CarriageContraptionEntity carriage ? carriage : null;
    }
}
