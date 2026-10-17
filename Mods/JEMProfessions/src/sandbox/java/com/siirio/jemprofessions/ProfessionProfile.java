package com.siirio.jemprofessions;

import com.siirio.jemprofessions.sandbox.SandboxProfessionRuntime;
import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import com.siirio.jemprofessions.sandbox.nativecompat.SandboxNativeEffects;
import com.siirio.jemprofessions.sandbox.civic.SandboxCivicEffects;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ProfessionProfile {
    private ProfessionProfile() {}
    public static String networkVersion() { return "sandbox-5"; }
    public static void registerPackets(SimpleChannel channel) { SandboxProfessionRuntime.registerPackets(channel); }
    public static void initialize() {
        SandboxProfessionRuntime.register();
        SandboxCoreEffects.register();
        SandboxNativeEffects.register();
        SandboxCivicEffects.register();
    }
}
