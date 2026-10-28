package com.siirio.jemanimalhints.compat.jade;

import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class JEMAnimalHintsJadePlugin implements IWailaPlugin {
    private static final TamingProvider TAMING = new TamingProvider();
    private static final BreedingProvider BREEDING = new BreedingProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(TAMING, LivingEntity.class);
        registration.registerEntityDataProvider(BREEDING, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(TAMING, LivingEntity.class);
        registration.registerEntityComponent(BREEDING, LivingEntity.class);
    }
}
