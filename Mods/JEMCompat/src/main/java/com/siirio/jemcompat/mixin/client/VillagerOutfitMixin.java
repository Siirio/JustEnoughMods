package com.siirio.jemcompat.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.siirio.jemcompat.config.JEMClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Native/resource-pack clothing remains the renderer; only its visual type selector varies. */
@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerOutfitMixin {
    @Unique private static final VillagerType[] JEM_OUTFITS = {
            VillagerType.DESERT, VillagerType.JUNGLE, VillagerType.PLAINS, VillagerType.SAVANNA,
            VillagerType.SNOW, VillagerType.SWAMP, VillagerType.TAIGA
    };

    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/VillagerData;getType()Lnet/minecraft/world/entity/npc/VillagerType;"))
    private VillagerType jem$visualOutfit(VillagerData data, PoseStack poses, MultiBufferSource buffers,
                                        int light, LivingEntity entity, float limbSwing, float limbAmount,
                                        float partialTick, float age, float headYaw, float headPitch) {
        if (!(entity instanceof Villager) || !JEMClientConfig.VARIED_VILLAGER_OUTFITS.get()) return data.getType();
        return JEM_OUTFITS[Math.floorMod(entity.getUUID().hashCode(), JEM_OUTFITS.length)];
    }
}
