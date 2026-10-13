package com.siirio.jemsettlements.compat.workers;

import java.util.UUID;

public record WorkerWorkEvidence(UUID workerId, UUID workAreaId, long observedGameTime) {
}
