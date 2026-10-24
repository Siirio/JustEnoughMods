package com.siirio.jemserver.smp.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class BossTeleportScopeRegressionTest extends NativeRegistryFixture {
    @Test void internalEjectionBypassesOnlyItsOwnCallAndFailureNeverLeaksPermission() {
        var player=mock(ServerPlayer.class); var level=mock(ServerLevel.class);
        var destination=new Vec3(50,64,50);
        try(var environment=mockStatic(com.siirio.jemserver.smp.SmpEnvironment.class);
            var boundary=mockStatic(BloodMoonSolidBoundary.class)) {
            boundary.when(()->BloodMoonSolidBoundary.validateTeleport(player,level,destination)).thenReturn(BossTeleportSafety.Decision.redirect(null));
            assertNull(BossTeleportSafety.validate(player,level,destination).destination());
            doAnswer(call->{
                assertEquals(destination,BossTeleportSafety.validate(player,level,destination).destination());
                throw new IllegalStateException("transfer failed");
            }).when(player).teleportTo(eq(level),anyDouble(),anyDouble(),anyDouble(),anyFloat(),anyFloat());
            assertThrows(IllegalStateException.class,()->BossTeleportSafety.apply(player,level,destination,0,0));
            assertNull(BossTeleportSafety.validate(player,level,destination).destination());
        }
    }
}
