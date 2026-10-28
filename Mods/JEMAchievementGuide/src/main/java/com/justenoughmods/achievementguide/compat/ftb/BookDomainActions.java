package com.justenoughmods.achievementguide.compat.ftb;

import net.minecraftforge.fml.ModList;

/** Optional domain classes are linked only after checking their owning module. */
public final class BookDomainActions {
    private BookDomainActions() {}

    public static void register() {
        if (ModList.get().isLoaded("create")) com.justenoughmods.achievementguide.integration.create.CreateDeployerProofs.register();
        if (ModList.get().isLoaded("toms_storage")) BookTerminalActions.register();
        if (ModList.get().isLoaded("jemfood")) BookFoodActions.register();
        if (ModList.get().isLoaded("mobhome")) BookHomeActions.register();
        if (ModList.get().isLoaded("jem_settlements")) BookSettlementActions.register();
        if (ModList.get().isLoaded("curios")) BookCurioActions.register();
        if (ModList.get().isLoaded("sophisticatedcore") && ModList.get().isLoaded("sophisticatedbackpacks")) BookBackpackActions.register();
        if (ModList.get().isLoaded("sophisticatedcore") && ModList.get().isLoaded("sophisticatedstorage") && ModList.get().isLoaded("toms_storage")) BookControllerActions.register();
    }
}
