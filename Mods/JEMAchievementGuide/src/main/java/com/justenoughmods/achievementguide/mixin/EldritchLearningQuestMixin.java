package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.network.spells.LearnSpellPacket;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LearnSpellPacket.class, remap = false)
public abstract class EldritchLearningQuestMixin {
    @Shadow @Final private byte hand;
    @Shadow @Final private String spell;
    @Unique private boolean jem$unlearned;
    @Unique private int jem$manuscripts;

    @Inject(method = "lambda$handle$0", at = @At("HEAD"))
    private void jem$beforeLearning(NetworkEvent.Context context, CallbackInfo callback) {
        ServerPlayer actor = context.getSender();
        jem$unlearned = false;
        if (actor == null) return;
        ItemStack source = actor.getItemInHand(LearnSpellPacket.byteToHand(hand));
        var chosen = SpellRegistry.getSpell(spell);
        jem$unlearned = source.is(ItemRegistry.ELDRITCH_PAGE.get()) && source.getCount() > 0
                && chosen != SpellRegistry.none()
                && !MagicData.getPlayerMagicData(actor).getSyncedData().isSpellLearned(chosen);
        jem$manuscripts = source.getCount();
    }

    @Inject(method = "lambda$handle$0", at = @At("TAIL"))
    private void jem$afterLearning(NetworkEvent.Context context, CallbackInfo callback) {
        ServerPlayer actor = context.getSender();
        if (actor != null && jem$unlearned
                && actor.getItemInHand(LearnSpellPacket.byteToHand(hand)).getCount() == jem$manuscripts - 1
                && MagicData.getPlayerMagicData(actor).getSyncedData().isSpellLearned(SpellRegistry.getSpell(spell))) {
            QuestActionTrigger.INSTANCE.emit(actor, "spell_eldritch_knowledge");
        }
        jem$unlearned = false;
    }
}
