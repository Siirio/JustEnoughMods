package com.siirio.jemsettlements.happiness;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.town.SettlementSavedData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Animal records change only on lifecycle and section transitions. */
public final class AnimalObservationService {
    private static final String TOWN = "JemAnimalObservationTown";
    private AnimalObservationService() { }

    public static boolean supported(Mob mob) {
        return mob instanceof Animal || mob instanceof net.minecraft.world.entity.animal.WaterAnimal;
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && supported(mob))
            observe(level, mob);
    }

    @SubscribeEvent
    public static void moved(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange() || !(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level) || !supported(mob)) return;
        observe(level, mob);
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level && supported(mob))
            remove(level, mob);
    }

    private static void observe(ServerLevel level, Mob animal) {
        if (!SettlementAuthority.active(level.getServer())) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        removePrior(data, animal);
        var town = data.townAt(level.dimension().location(), animal.blockPosition()).orElse(null);
        boolean peaceful = animal.isAlive() && animal.getTarget() == null
                && !(animal instanceof net.minecraft.world.entity.monster.Enemy)
                && !(animal instanceof net.minecraft.world.entity.NeutralMob neutral && neutral.isAngry());
        if (town == null || !peaceful) {
            data.changed();
            return;
        }
        String id = animal.getUUID().toString();
        if (town.animalObservations().size() < 512 || town.animalObservations().contains(id)) {
            CompoundTag row = new CompoundTag();
            row.putString("Type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString());
            row.putLong("Position", animal.blockPosition().asLong());
            town.animalObservations().put(id, row);
            animal.getPersistentData().putUUID(TOWN, town.id());
        }
        data.changed();
    }

    private static void remove(ServerLevel level, Mob animal) {
        if (!SettlementAuthority.active(level.getServer())) return;
        SettlementSavedData data = SettlementSavedData.get(level.getServer());
        removePrior(data, animal);
        data.changed();
    }

    private static void removePrior(SettlementSavedData data, Mob animal) {
        if (!animal.getPersistentData().hasUUID(TOWN)) return;
        UUID town = animal.getPersistentData().getUUID(TOWN);
        data.town(town).ifPresent(value -> value.animalObservations().remove(animal.getUUID().toString()));
        animal.getPersistentData().remove(TOWN);
    }
}
