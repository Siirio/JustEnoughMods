package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateSchematicRun;
import com.simibubi.create.content.schematics.cannon.ConfigureSchematicannonPacket;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.ConfigureSchematicannonPacket", remap = false)
public abstract class CreateSchematicCommandQuestMixin {
    @Shadow private ConfigureSchematicannonPacket.Option option;

    @Inject(method = "lambda$handle$0", at = @At("TAIL"), remap = false)
    private void jem$nativeJobCommand(NetworkEvent.Context context, CallbackInfo callback) {
        var actor = context.getSender();
        if (actor == null || !(actor.containerMenu instanceof SchematicannonMenu menu)
                || !(menu.contentHolder instanceof CreateSchematicRun cannon)) return;
        if (option == ConfigureSchematicannonPacket.Option.PLAY) cannon.jem$jobCommand(actor, true);
        else if (option == ConfigureSchematicannonPacket.Option.STOP) cannon.jem$jobCommand(actor, false);
    }
}
