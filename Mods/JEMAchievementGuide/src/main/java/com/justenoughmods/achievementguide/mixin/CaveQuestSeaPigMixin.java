package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.living.SeaPigEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=SeaPigEntity.class,remap=false)
public abstract class CaveQuestSeaPigMixin {
    @Shadow private void digestItem(){throw new AssertionError();}
    @Unique private int jem$caveFoodCount;
    @Unique private boolean jem$caveDigesting;
    @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$caveFoodCount=actor.getItemInHand(hand).getCount();jem$caveDigesting=((SeaPigEntity)(Object)this).isDigesting();}
    @Inject(method="m_6071_",at=@At("RETURN"))private void jem$fed(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
        var pig=(SeaPigEntity)(Object)this;
        if(!jem$caveDigesting && pig.isDigesting() && jem$caveFoodCount-actor.getItemInHand(hand).getCount()==1 ){pig.getPersistentData().remove("jemCaveSeaPigFeederV4");if(CaveQuestProof.actor(actor)!=null)pig.getPersistentData().putUUID("jemCaveSeaPigFeederV4",actor.getUUID());}
    }
    @Redirect(method="m_8119_",at=@At(value="INVOKE",target="Lcom/github/alexmodguy/alexscaves/server/entity/living/SeaPigEntity;digestItem()V"))
    private void jem$digestion(SeaPigEntity pig){
        var actor=CaveQuestProof.owner(pig,"jemCaveSeaPigFeederV4");
        CaveQuestDrops.observe(pig,actor,"marine",()->{digestItem();return !pig.isDigesting();});pig.getPersistentData().remove("jemCaveSeaPigFeederV4");
    }
}
