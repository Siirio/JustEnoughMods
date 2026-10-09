package com.siirio.jempackcore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.entity.MobCategory;
import java.util.List;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public record CreatureSpawnProbability(HolderSet<Biome> biomes, float probability, List<MobSpawnSettings.SpawnerData> spawners) implements BiomeModifier {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, JEMPackCore.MOD_ID);
    private static final RegistryObject<Codec<CreatureSpawnProbability>> CODEC = SERIALIZERS.register(
            "creature_spawn_probability", () -> RecordCodecBuilder.create(instance -> instance.group(
                    Biome.LIST_CODEC.fieldOf("biomes").forGetter(CreatureSpawnProbability::biomes),
                    Codec.floatRange(0, 1).fieldOf("probability").forGetter(CreatureSpawnProbability::probability),
                    MobSpawnSettings.SpawnerData.CODEC.listOf().fieldOf("spawners").forGetter(CreatureSpawnProbability::spawners)
            ).apply(instance, CreatureSpawnProbability::new)));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.MODIFY && biomes.contains(biome)) {
            builder.getMobSpawnSettings().creatureGenerationProbability(probability);
            builder.getMobSpawnSettings().getSpawner(MobCategory.CREATURE)
                    .removeIf(existing -> spawners.stream().anyMatch(replacement -> replacement.type == existing.type));
            spawners.forEach(spawn -> builder.getMobSpawnSettings().addSpawn(MobCategory.CREATURE, spawn));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
