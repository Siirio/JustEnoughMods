package com.justenoughmods.achievementguide.mixin;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.GumWormDestroyGobthumperGoal.class,remap=false)
public abstract class CaveQuestWormAttractionMixin {
    @Shadow @Final private GumWormEntity entity;
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;m_26519_(DDDD)Z"))
    private boolean jem$nativeLurePath(PathNavigation navigation,double x,double y,double z,double speed){
        boolean result=navigation.moveTo(x,y,z,speed);var proof=entity.getPersistentData();
        if(result && proof.hasUUID("jemCaveWormLureV4") && entity.getGobthumperPos()!=null
                && entity.getGobthumperPos().asLong()==proof.getLong("jemCaveWormLurePosV4"))proof.putBoolean("jemCaveWormPathV4",true);
        return result;
    }
}
