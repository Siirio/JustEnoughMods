package com.siirio.jemcompat.mixin.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla serializes only currently registered mappings; retain removed mods' native entries. */
@Mixin(Options.class)
public abstract class OptionsKeyRetentionMixin {
    @Shadow @Final private File optionsFile;
    @Unique private final Map<String,String> jemcompat$rememberedKeys=new LinkedHashMap<>();

    @Inject(method="save",at=@At("HEAD"))
    private void jemcompat$captureKeys(CallbackInfo ci) {
        try {
            var path=optionsFile.toPath();
            if(!Files.isRegularFile(path))return;
            if(Files.size(path)>2_097_152)throw new IOException("options.txt exceeds native preference budget");
            for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)) {
                int separator=line.indexOf(':');
                if(separator>4&&line.startsWith("key_"))jemcompat$rememberedKeys.put(line.substring(0,separator),line);
            }
        } catch(IOException error) { com.mojang.logging.LogUtils.getLogger().error("Cannot preserve native key mappings in {}",optionsFile,error); }
    }

    @Inject(method="save",at=@At("TAIL"))
    private void jemcompat$restoreMissingKeys(CallbackInfo ci) {
        if(jemcompat$rememberedKeys.isEmpty())return;
        try {
            var path=optionsFile.toPath();
            var written=new java.util.HashSet<String>();
            for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)) {
                int separator=line.indexOf(':');if(separator>0)written.add(line.substring(0,separator));
            }
            var missing=jemcompat$rememberedKeys.entrySet().stream().filter(entry->!written.contains(entry.getKey())).map(Map.Entry::getValue).toList();
            if(!missing.isEmpty())Files.write(path,missing,StandardCharsets.UTF_8,StandardOpenOption.APPEND);
        } catch(IOException error) { com.mojang.logging.LogUtils.getLogger().error("Cannot restore removed mods' native key mappings in {}",optionsFile,error); }
    }
}
