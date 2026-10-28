package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"com.github.alexmodguy.alexscaves.server.entity.item.DesolateDaggerEntity",
 "com.github.alexmodguy.alexscaves.server.entity.item.DarkArrowEntity",
 "com.github.alexmodguy.alexscaves.server.entity.item.FrostmintSpearEntity",
 "com.github.alexmodguy.alexscaves.server.entity.item.GumballEntity"},remap=false)
public abstract class CaveQuestProjectileDamageMixin {
    @Redirect(method={"m_8119_","m_5790_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$caveNativeHit(Entity target,DamageSource damage,float amount){
        Entity self=(Entity)(Object)this, actor=damage.getEntity();
        String action="";
        if(self instanceof com.github.alexmodguy.alexscaves.server.entity.item.DesolateDaggerEntity) action="v3_cave_shadow_arsenal";
        else if(self instanceof com.github.alexmodguy.alexscaves.server.entity.item.FrostmintSpearEntity) action="v3_cave_candy_arsenal_spear";
        else if(self.getPersistentData().hasUUID("jemCaveVolleyV4") && actor!=null
                && self.getPersistentData().getUUID("jemCaveVolleyV4").equals(actor.getUUID())) action="v3_cave_shadow_arsenal_volley";
        else if(self.getPersistentData().getBoolean("jemCaveWallBounceV4") && self.getPersistentData().hasUUID("jemCaveShotGumV4")
                && actor!=null && self.getPersistentData().getUUID("jemCaveShotGumV4").equals(actor.getUUID())) action="v3_cave_candy_arsenal";
        return action.isEmpty()?target.hurt(damage,amount):CaveQuestProof.damage(target,damage,amount,actor,action);
    }
}
