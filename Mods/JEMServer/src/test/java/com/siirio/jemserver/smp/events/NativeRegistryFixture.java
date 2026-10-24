package com.siirio.jemserver.smp.events;

import org.junit.jupiter.api.BeforeAll;
import static org.mockito.Mockito.*;

/** Real vanilla registries without Forge's launch-only network transformation. */
public abstract class NativeRegistryFixture {
    @BeforeAll static void registries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try (var network = mockStatic(net.minecraftforge.network.NetworkHooks.class, CALLS_REAL_METHODS)) {
            network.when(net.minecraftforge.network.NetworkHooks::init).thenAnswer(call -> null);
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }
}
