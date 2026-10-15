package com.siirio.jemroles.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.siirio.jemroles.api.RaceApi;
import com.siirio.jemroles.race.RaceId;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public final class PiglinFortuneLoot extends LootModifier {
    public static final Codec<PiglinFortuneLoot> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, PiglinFortuneLoot::new));
    public PiglinFortuneLoot(LootItemCondition[] conditions) { super(conditions); }

    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof AbstractPiglin piglin)) return loot;
        ServerPlayer ancestor = piglin.level().getEntitiesOfClass(ServerPlayer.class, piglin.getBoundingBox().inflate(16),
                        player -> RaceApi.hasRace(player, RaceId.ANCESTORS))
                .stream().min(java.util.Comparator.comparingDouble(piglin::distanceToSqr)).orElse(null);
        if (ancestor == null) return loot;
        for (ItemStack stack : List.copyOf(loot)) if (context.getRandom().nextFloat() < 0.30F) loot.add(stack.copy());
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
