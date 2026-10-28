package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.RelicheirusEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.github.alexmodguy.alexscaves.server.item.PrehistoricMixtureItem.class,remap=false)
public abstract class CaveQuestSoupFeedMixin {
    @Unique private static final ThreadLocal<int[]> jem$caveSoup=new ThreadLocal<>();
    @Inject(method="m_6880_",at=@At("HEAD"))private void jem$before(ItemStack soup,Player user,LivingEntity target,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$caveSoup.set(new int[]{CaveQuestProof.item(soup,"primordial_soup")?soup.getCount():0,target instanceof RelicheirusEntity relic?relic.getPushingTreesFor():-1});}
    @Inject(method="m_6880_",at=@At("RETURN"))private void jem$after(ItemStack soup,Player user,LivingEntity target,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
        int[] before=jem$caveSoup.get();jem$caveSoup.remove();
        if(before!=null && before[0]>0 && before[0]-soup.getCount()==1 && before[1]==0 && target instanceof RelicheirusEntity relic && relic.getPushingTreesFor()>0){
            target.getPersistentData().remove("jemCaveSoupFeederV4");if(CaveQuestProof.actor(user)!=null)target.getPersistentData().putUUID("jemCaveSoupFeederV4",user.getUUID());}
    }
}
