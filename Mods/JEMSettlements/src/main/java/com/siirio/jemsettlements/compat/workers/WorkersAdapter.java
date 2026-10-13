package com.siirio.jemsettlements.compat.workers;

import com.siirio.jemsettlements.workforce.WorkforceType;
import com.talhanation.workers.entities.AbstractWorkerEntity;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

public final class WorkersAdapter {
    private WorkersAdapter() {
    }

    public static Optional<WorkforceType> typeOwnedBy(Entity entity, UUID accountId) {
        if (!(entity instanceof AbstractWorkerEntity worker) || worker.getOwnerUUID() == null
                || !worker.getOwnerUUID().equals(accountId)) return Optional.empty();
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(worker.getType()).getPath().toLowerCase(Locale.ROOT);
        return switch (path) {
            case "farmer" -> Optional.of(WorkforceType.FARMER);
            case "miner" -> Optional.of(WorkforceType.MINER);
            case "fisherman" -> Optional.of(WorkforceType.FISHER);
            case "cook" -> Optional.of(WorkforceType.COOK);
            case "builder" -> Optional.of(WorkforceType.BUILDER);
            case "courier" -> Optional.of(WorkforceType.COURIER);
            case "merchant" -> Optional.of(WorkforceType.MERCHANT);
            case "lumberjack" -> Optional.of(WorkforceType.LUMBERJACK);
            case "animal_farmer" -> Optional.of(WorkforceType.ANIMAL_FARMER);
            default -> Optional.empty();
        };
    }

    public static Optional<WorkerWorkEvidence> currentEvidence(Entity entity) {
        if (!(entity instanceof AbstractWorkerEntity worker) || !worker.isWorking() || worker.getCurrentWorkArea() == null) {
            return Optional.empty();
        }
        return Optional.of(new WorkerWorkEvidence(worker.getUUID(), worker.getCurrentWorkArea().getUUID(), worker.level().getGameTime()));
    }
}
