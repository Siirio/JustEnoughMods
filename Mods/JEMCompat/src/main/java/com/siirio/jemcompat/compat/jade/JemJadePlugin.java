package com.siirio.jemcompat.compat.jade;

import com.siirio.jemcompat.config.JEMClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class JemJadePlugin implements IWailaPlugin {
    private static final TamingProvider TAMING = new TamingProvider();
    private static final BreedingProvider BREEDING = new BreedingProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(TAMING, LivingEntity.class);
        registration.registerEntityDataProvider(BREEDING, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback(Integer.MAX_VALUE, (tooltip, accessor) -> {
            if (!JEMClientConfig.SHOW_MOD_NAMES.get()) {
                tooltip.remove(Identifiers.CORE_MOD_NAME);
            }
            if (accessor instanceof EntityAccessor entityAccessor && !entityAccessor.getEntity().hasCustomName()) {
                tooltip.remove(Identifiers.CORE_OBJECT_NAME);
                tooltip.add(0, Component.translatable(entityAccessor.getEntity().getType().getDescriptionId()), Identifiers.CORE_OBJECT_NAME);
            }
        });
        registration.registerEntityComponent(TAMING, LivingEntity.class);
        registration.registerEntityComponent(BREEDING, LivingEntity.class);
    }
}
