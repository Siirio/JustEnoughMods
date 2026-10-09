package com.siirio.jempackcore.postend;

import com.siirio.jemcompat.gate.CampaignSavedData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretEndingLifecycleTest {
    @Test
    void vanillaPortalReturnsBeforeChangedDimensionEvent() throws IOException {
        MethodNode changeDimension = read("net/minecraft/server/level/ServerPlayer").methods.stream()
                .filter(method -> method.name.equals("changeDimension")
                        && method.desc.contains("ITeleporter"))
                .findFirst().orElseThrow();
        int creditsConstructors = 0;
        boolean returnedAfterCredits = false;
        boolean dimensionEventAfterReturn = false;
        for (var instruction : changeDimension.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("net/minecraft/network/protocol/game/ClientboundGameEventPacket")
                        && call.name.equals("<init>")) creditsConstructors++;
                if (call.name.equals("firePlayerChangedDimensionEvent")) {
                    dimensionEventAfterReturn = returnedAfterCredits;
                }
            }
            if (creditsConstructors > 0 && instruction.getOpcode() == Opcodes.ARETURN) {
                returnedAfterCredits = true;
            }
        }
        assertEquals(1, creditsConstructors);
        assertTrue(dimensionEventAfterReturn);
    }

    @Test
    void lockedCreditsCannotInvokeVanillaRespawnOnClose() throws IOException {
        MethodNode close = read("com/siirio/jempackcore/client/SecretEndingClient$LockedWinScreen")
                .methods.stream().filter(method -> method.name.equals("onClose")).findFirst().orElseThrow();
        for (var instruction : close.instructions) {
            assertFalse(instruction instanceof MethodInsnNode);
        }
    }

    @Test
    void eachPlayerReceivesOneSequencePerSavedWorld() throws ReflectiveOperationException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        CampaignSavedData data = new CampaignSavedData();
        assertTrue(data.startSecretEnding(first));
        assertFalse(data.startSecretEnding(first));
        var load = CampaignSavedData.class.getDeclaredMethod("load", CompoundTag.class);
        load.setAccessible(true);
        CampaignSavedData restored = (CampaignSavedData) load.invoke(null, data.save(new CompoundTag()));
        assertFalse(restored.startSecretEnding(first));
        assertTrue(restored.startSecretEnding(second));
    }

    private static ClassNode read(String name) throws IOException {
        try (var input = SecretEndingLifecycleTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input);
            ClassNode type = new ClassNode();
            new ClassReader(input).accept(type, 0);
            return type;
        }
    }
}
