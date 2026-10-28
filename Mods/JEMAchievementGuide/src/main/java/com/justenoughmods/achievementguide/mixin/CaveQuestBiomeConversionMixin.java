package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestBiomeWrites;
import com.github.alexmodguy.alexscaves.server.block.blockentity.ConversionCrucibleBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ConversionCrucibleBlockEntity.class,remap=false)
public abstract class CaveQuestBiomeConversionMixin {
    @Shadow private int conversionTime;
    @Inject(method="convertBiome",at=@At("HEAD"))private void jem$before(CallbackInfo ci){CaveQuestBiomeWrites.begin((ConversionCrucibleBlockEntity)(Object)this,conversionTime>=ConversionCrucibleBlockEntity.MAX_CONVERSION_TIME);}
    @Redirect(method="convertBiome",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;m_183442_(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"))
    private void jem$nativeBiomeCommit(ChunkAccess chunk,BiomeResolver resolver,Climate.Sampler sampler){CaveQuestBiomeWrites.fill((ConversionCrucibleBlockEntity)(Object)this,chunk,resolver,sampler);}
    @Inject(method="convertBiome",at=@At("RETURN"))private void jem$after(CallbackInfo ci){CaveQuestBiomeWrites.end((ConversionCrucibleBlockEntity)(Object)this);}
}
