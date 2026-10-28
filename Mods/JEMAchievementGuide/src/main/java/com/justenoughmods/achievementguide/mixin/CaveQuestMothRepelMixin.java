package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.living.GloomothEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.GloomothFleeMothBallsGoal.class,remap=false)
public abstract class CaveQuestMothRepelMixin {
    @Shadow private GloomothEntity gloomoth;
    @Shadow private BlockPos blockPos;
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;m_26519_(DDDD)Z"))
    private boolean jem$nativeRepel(PathNavigation navigation,double x,double y,double z,double speed){
        var old=navigation.getPath();boolean result=navigation.moveTo(x,y,z,speed);
        if(result && navigation.getPath()!=old && gloomoth.level() instanceof ServerLevel level && blockPos!=null && gloomoth.lightPos!=null
                && CaveQuestProof.loaded(level,blockPos) && CaveQuestProof.loaded(level,gloomoth.lightPos)
                && level.getBlockState(gloomoth.lightPos).getLightEmission(level,gloomoth.lightPos)>0){
            var origin=CaveQuestBlockOrigins.get(level,blockPos,"Placed");var actor=origin.hasUUID("actor")?CaveQuestProof.actor(level,origin.getUUID("actor")):null;
            if(actor!=null && origin.getString("block").equals("alexscaves:moth_ball") && new Vec3(x,y,z).distanceToSqr(gloomoth.lightPos.getCenter())>gloomoth.position().distanceToSqr(gloomoth.lightPos.getCenter()))CaveQuestProof.emit(actor,"v3_cave_moth_lighting_repel");
        }return result;
    }
}
