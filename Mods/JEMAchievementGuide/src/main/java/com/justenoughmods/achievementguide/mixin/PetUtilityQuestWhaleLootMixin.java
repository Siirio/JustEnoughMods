package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.petutilities.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class PetUtilityQuestWhaleLootMixin {
 @Shadow protected ResourceLocation lootTable;
 @Unique private ResourceLocation jem$table;
 @Inject(method="unpackLootTable",at=@At("HEAD"))private void jem$before(Player p,CallbackInfo ci){jem$table=lootTable;}
 @Inject(method="unpackLootTable",at=@At("RETURN"))private void jem$filled(Player p,CallbackInfo ci){var table=jem$table;jem$table=null;var chest=(RandomizableContainerBlockEntity)(Object)this;if(table==null || lootTable!=null || chest.getLevel()==null || chest.getLevel().isClientSide)return;var source=java.util.UUID.randomUUID();for(int i=0;i<chest.getContainerSize();i++)PetUtilityQuestWhaleLoot.loot(chest.getItem(i),table.toString(),source);}
}
