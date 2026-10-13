package com.siirio.jemprofessions.api;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.SourceKind;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record ProfessionProgressRequest(ProfessionId profession, SourceKind sourceKind,
                                        ResourceLocation sourceId, int baseDelta, UUID auditId) {
    public ProfessionProgressRequest {
        if (profession == null || sourceKind == null || sourceId == null || auditId == null || baseDelta <= 0) {
            throw new IllegalArgumentException("A profession progress request must be complete and positive");
        }
    }
}
