package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.compat.ftb.FtbQuestProjection;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The audited API has no server quest-folder setter or pre-load event. */
@Mixin(targets="dev.ftb.mods.ftbquests.quest.ServerQuestFile", remap=false)
public abstract class FtbQuestFileMixin {
    @Shadow @Final public MinecraftServer server;
    @Shadow private Path folder;

    @Inject(method="load", at=@At(value="FIELD",
            target="Ldev/ftb/mods/ftbquests/quest/ServerQuestFile;folder:Ljava/nio/file/Path;",
            opcode=Opcodes.PUTFIELD, ordinal=0, shift=At.Shift.AFTER), remap=false)
    private void jem$authoredProjection(CallbackInfo callback) {
        folder = FtbQuestProjection.prepare(server, folder);
    }
}
