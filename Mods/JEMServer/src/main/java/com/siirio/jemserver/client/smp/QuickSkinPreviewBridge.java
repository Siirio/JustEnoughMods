package com.siirio.jemserver.client.smp;

import com.quickskin.mod.client.compat.CPMCompatIntegration;
import com.quickskin.mod.client.services.PlayerAppearanceService;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** Version-gated by PreviewPlayerEntity before any Quick Skin class is resolved. */
final class QuickSkinPreviewBridge {
    private QuickSkinPreviewBridge() { }

    static ResourceLocation skin(UUID playerId) {
        if (CPMCompatIntegration.shouldDeferToCPM()) return null;
        var service = PlayerAppearanceService.getInstance();
        return service.hasActiveSkin(playerId) ? service.getSkinLocation(playerId) : null;
    }

    static String model(UUID playerId) {
        if (CPMCompatIntegration.shouldDeferToCPM()) return null;
        var service = PlayerAppearanceService.getInstance();
        if (!service.hasActiveSkin(playerId) && !service.hasModelOverride(playerId)) return null;
        return service.getModelName(playerId);
    }

    static boolean hasCapeChoice(UUID playerId) {
        return !CPMCompatIntegration.shouldDeferToCPM()
                && PlayerAppearanceService.getInstance().hasActiveCape(playerId);
    }

    static ResourceLocation cape(UUID playerId) {
        var service = PlayerAppearanceService.getInstance();
        var appearance = service.getAppearance(playerId);
        String capeId = appearance == null ? null : appearance.getCapeId();
        if (capeId != null && ("__NONE__".equals(capeId) || capeId.isEmpty())) return null;
        return service.getCapeLocation(playerId);
    }
}
