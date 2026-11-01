package com.siirio.jembuilding.compat.jade;

import com.siirio.jembuilding.construction.ConstructionPlaceholderBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class ConstructionExpiryJadePlugin implements IWailaPlugin {
    private static final ConstructionExpiryProvider PROVIDER = new ConstructionExpiryProvider();

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, ConstructionPlaceholderBlock.class);
    }
}
