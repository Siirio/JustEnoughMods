package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.InteractionHand;import net.minecraft.world.InteractionResult;
@Mixin(value=net.mcreator.luminousnether.entity.PottergeistEntity.class,remap=false)
public abstract class WorldRitualQuestPotterTameMixin {
    @Unique private boolean jem$wasTame;
    @Unique private boolean jem$wasCanceled;
    @Inject(method="m_6071_",at=@At("HEAD"))private void jem$before(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){jem$wasCanceled=false;jem$wasTame=((net.minecraft.world.entity.TamableAnimal)(Object)this).isTame();}
    @Inject(method="m_6071_",at=@At("RETURN"))private void jem$after(Player actor,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(!jem$wasCanceled)WorldRitualQuestPotter.tamed((net.mcreator.luminousnether.entity.PottergeistEntity)(Object)this,actor,jem$wasTame);}
    @Redirect(method="m_6071_",at=@At(value="INVOKE",target="Lnet/minecraftforge/event/ForgeEventFactory;onAnimalTame(Lnet/minecraft/world/entity/animal/Animal;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean jem$tameResult(net.minecraft.world.entity.animal.Animal animal,Player player){boolean canceled=net.minecraftforge.event.ForgeEventFactory.onAnimalTame(animal,player);jem$wasCanceled=canceled;return canceled;}
}
