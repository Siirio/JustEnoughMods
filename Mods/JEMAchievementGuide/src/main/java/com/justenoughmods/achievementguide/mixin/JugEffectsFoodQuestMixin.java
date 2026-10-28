package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.JugFoodRun;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.satisfy.herbalbrews.core.blocks.entity.JugBlockEntity;
import net.satisfy.herbalbrews.core.items.DrinkBlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = JugBlockEntity.class, remap = false)
public abstract class JugEffectsFoodQuestMixin implements JugFoodRun {
    @Unique private ServerPlayer jem$drinker;
    @Unique private Set<MobEffect> jem$accepted;

    @Override public void jem$applyForQuest(LivingEntity actor, int duration) {
        jem$drinker = null;
        jem$accepted = null;
        JugBlockEntity owner = (JugBlockEntity)(Object)this;
        Set<ResourceLocation> teas = new HashSet<>();
        Set<MobEffect> expected = new HashSet<>();
        if (actor instanceof ServerPlayer && owner.getDrinks().size() >= 2 && owner.getDrinks().size() <= 3) {
            for (ItemStack tea : owner.getDrinks()) {
                CompoundTag tag = tea.getTag();
                if (tea.isEmpty() || !(tea.getItem() instanceof DrinkBlockItem) || tag == null
                        || !tag.contains("Effect") || !tag.contains("EffectDuration")) continue;
                ResourceLocation effectId = ResourceLocation.tryParse(tag.getString("Effect"));
                MobEffect effect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect != null) {
                    teas.add(BuiltInRegistries.ITEM.getKey(tea.getItem()));
                    expected.add(effect);
                }
            }
            if (teas.size() >= 2) jem$accepted = new HashSet<>();
        }
        try {
            owner.applyEffects(actor, duration);
            if (actor instanceof ServerPlayer player && jem$accepted != null
                    && !expected.isEmpty() && jem$accepted.containsAll(expected)) jem$drinker = player;
        } finally {
            jem$accepted = null;
        }
    }

    @Redirect(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean jem$acceptedEffect(LivingEntity actor, MobEffectInstance effect) {
        boolean accepted = actor.addEffect(effect);
        if (accepted && jem$accepted != null) jem$accepted.add(effect.getEffect());
        return accepted;
    }

    @Override public ServerPlayer jem$consumeDrinker() {
        ServerPlayer player = jem$drinker;
        jem$drinker = null;
        return player;
    }
}
