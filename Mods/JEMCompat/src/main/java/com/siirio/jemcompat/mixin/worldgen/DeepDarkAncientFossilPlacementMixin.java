package com.siirio.jemcompat.mixin.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

/** Keeps complete native fossils, including edge shape updates, inside the FEATURES write region. */
@Pseudo
@Mixin(targets = "net.mcreator.deepdarkregrowth.world.features.AncientFossilFeature", remap = false)
public abstract class DeepDarkAncientFossilPlacementMixin {
    @Shadow(remap = false)
    private StructureTemplate template;

    // Exact DDR 1.2.6.1 slice: only the X and Z draws precede OCEAN_FLOOR_WG.
    // Probability, attempt count, height, mirror and rotation draws stay native.
    @Redirect(
            method = "m_142674_(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "intValue=16", ordinal = 0),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/level/WorldGenLevel;m_6924_(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I",
                            remap = false)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;m_188503_(I)I", remap = false),
            remap = false, require = 2, expect = 2, allow = 2)
    private int jemcompat$boundedFossilCoordinate(RandomSource random, int nativeBound) {
        if (nativeBound != 16) {
            throw new IllegalStateException("Deep Dark Regrowth fossil coordinate contract changed: " + nativeBound);
        }
        var size = template.getSize();
        // For size 18: rotations reach +/-17 and shape updates reach one further block.
        // Choosing [2,13] keeps the full write envelope inside [-16,31].
        int margin = Math.max(0, Math.max(size.getX(), size.getZ()) - 16);
        int coordinateCount = 16 - 2 * margin;
        if (coordinateCount <= 0) {
            throw new IllegalStateException("Deep Dark Regrowth fossil template exceeds its native placement contract: " + size);
        }
        return margin + random.nextInt(coordinateCount);
    }
}
