package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.companions.CompanionQuestPuppetMenuAccess;
import dev.xylonity.companions.common.entity.companion.PuppetEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=dev.xylonity.companions.common.container.PuppetContainerMenu.class,remap=false)
public interface CompanionQuestPuppetMenuMixin extends CompanionQuestPuppetMenuAccess {@Accessor("puppet") PuppetEntity jem$companionPuppet();}
