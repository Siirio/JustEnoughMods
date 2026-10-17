package com.siirio.jemvisualsandbox.workbench;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Canonical PiglinFortuneLoot algorithm, restricted to the selected integrated owner. */
public final class SandboxRaceLoot {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "jem_visual_sandbox");
    static { MODIFIERS.register("piglin_fortune", () -> Fortune.CODEC); }
    private SandboxRaceLoot() {}
    public static void register(IEventBus bus) { MODIFIERS.register(bus); }

    public static final class Fortune extends LootModifier {
        public static final Codec<Fortune> CODEC = RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, Fortune::new));
        public Fortune(LootItemCondition[] conditions) { super(conditions); }
        @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof AbstractPiglin piglin)
                    || piglin.getServer() == null || !RaceExperience.serverActive(piglin.getServer())) return loot;
            ServerPlayer ancestor = piglin.level().getEntitiesOfClass(ServerPlayer.class, piglin.getBoundingBox().inflate(16),
                            p -> SandboxRaceTraits.hasRace(p, 0))
                    .stream().min(java.util.Comparator.comparingDouble(piglin::distanceToSqr)).orElse(null);
            if (ancestor != null) for (ItemStack stack : List.copyOf(loot))
                if (context.getRandom().nextFloat() < .30F) loot.add(stack.copy());
            return loot;
        }
        @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC; }
    }
}
