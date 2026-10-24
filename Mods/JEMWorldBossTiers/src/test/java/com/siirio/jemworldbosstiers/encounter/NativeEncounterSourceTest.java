package com.siirio.jemworldbosstiers.encounter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.siirio.jemworldbosstiers.api.HostedEncounterApi;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.MockedStatic;

class NativeEncounterSourceTest {
    @BeforeAll static void registries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try (MockedStatic<net.minecraftforge.network.NetworkHooks> network = mockStatic(net.minecraftforge.network.NetworkHooks.class, CALLS_REAL_METHODS)) {
            network.when(net.minecraftforge.network.NetworkHooks::init).thenAnswer(call -> null);
            net.minecraft.server.Bootstrap.bootStrap();
        }
        HostedConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
    }

    private static LivingEntity boss(ServerLevel level) {
        when(level.dimension()).thenReturn(net.minecraft.world.level.Level.OVERWORLD);
        LivingEntity boss = mock(LivingEntity.class);
        when(boss.getUUID()).thenReturn(UUID.randomUUID());
        when(boss.level()).thenReturn(level);
        when(boss.getPersistentData()).thenReturn(new CompoundTag());
        var server = level.getServer();
        if (server == null) { server = mock(net.minecraft.server.MinecraftServer.class); when(level.getServer()).thenReturn(server); }
        when(server.isDedicatedServer()).thenReturn(true);
        when(boss.getServer()).thenReturn(server);
        active(boss);
        return boss;
    }

    private static UUID active(LivingEntity boss) {
        UUID attempt = UUID.randomUUID();
        CompoundTag state = new CompoundTag();
        state.putUUID("Attempt", attempt);
        boss.getPersistentData().put("JEMHostedEncounter", state);
        return attempt;
    }

    @Test void nestedNativeScopesRestoreOuterOwnerAndCleanUpExceptions() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity first = boss(level), second = boss(level);
        try (MockedStatic<HostedEncounterApi> api = mockStatic(HostedEncounterApi.class)) {
            api.when(() -> HostedEncounterApi.enabled(any())).thenReturn(true);
            api.when(() -> HostedEncounterApi.damageOwner(any(Entity.class))).thenAnswer(call -> call.getArgument(0));
            NativeEncounterSource.run(first, () -> {
                assertSame(first, NativeEncounterSource.current());
                assertThrows(IllegalStateException.class, () -> NativeEncounterSource.run(second, () -> {
                    assertSame(second, NativeEncounterSource.current());
                    throw new IllegalStateException("native tick failure");
                }));
                assertSame(first, NativeEncounterSource.current());
            });
            assertNull(NativeEncounterSource.current());
            assertThrows(IllegalArgumentException.class, () -> NativeEncounterSource.run(first, () -> { throw new IllegalArgumentException("outer failure"); }));
            assertNull(NativeEncounterSource.current());
        }
    }

    @Test void playerOwnedNestedTickMasksBossScopeAndCannotBeCapturedAsHostile() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        ServerPlayer player = mock(ServerPlayer.class);
        Projectile projectile = mock(Projectile.class);
        when(projectile.level()).thenReturn(level);
        when(projectile.getPersistentData()).thenReturn(new CompoundTag());
        try (MockedStatic<HostedEncounterApi> api = mockStatic(HostedEncounterApi.class)) {
            api.when(() -> HostedEncounterApi.enabled(any())).thenReturn(true);
            api.when(() -> HostedEncounterApi.damageOwner(boss)).thenReturn(boss);
            api.when(() -> HostedEncounterApi.damageOwner(projectile)).thenReturn(player);
            active(boss);
            NativeEncounterSource.run(boss, () -> {
                assertFalse(NativeEncounterSource.capture(projectile));
                NativeEncounterSource.run(projectile, () -> assertNull(NativeEncounterSource.current()));
                assertSame(boss, NativeEncounterSource.current());
            });
            assertFalse(projectile.getPersistentData().hasUUID("JEMHostedNativeOwner"));
        }
    }

    @Test void onlyFreshUnownedNativeSpawnIsCapturedAndConflictingMarkerIsPreserved() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        Entity effect = mock(Entity.class);
        when(effect.level()).thenReturn(level);
        CompoundTag marker = new CompoundTag();
        when(effect.getPersistentData()).thenReturn(marker);
        try (MockedStatic<HostedEncounterApi> api = mockStatic(HostedEncounterApi.class)) {
            api.when(() -> HostedEncounterApi.enabled(any())).thenReturn(true);
            api.when(() -> HostedEncounterApi.damageOwner(boss)).thenReturn(boss);
            api.when(() -> HostedEncounterApi.damageOwner(effect)).thenReturn(effect);
            active(boss);
            NativeEncounterSource.run(boss, () -> {
                NativeEncounterSource.joined(new EntityJoinLevelEvent(effect, level, true));
                assertFalse(marker.hasUUID("JEMHostedNativeOwner"));
                NativeEncounterSource.joined(new EntityJoinLevelEvent(effect, level, false));
                assertEquals(boss.getUUID(), marker.getUUID("JEMHostedNativeOwner"));
                UUID conflicting = UUID.randomUUID();
                marker.putUUID("JEMHostedNativeOwner", conflicting);
                assertFalse(NativeEncounterSource.capture(effect));
                assertEquals(conflicting, marker.getUUID("JEMHostedNativeOwner"));
            });
        }
    }

    @Test void unownedScriptedDamageUsesExactScopeWhileExplicitPlayerProjectileAlwaysWins() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        ServerPlayer player = mock(ServerPlayer.class);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        Projectile projectile = mock(Projectile.class);
        when(projectile.getUUID()).thenReturn(UUID.randomUUID());
        when(projectile.getOwner()).thenReturn(player);
        when(projectile.level()).thenReturn(level);
        when(projectile.getPersistentData()).thenReturn(new CompoundTag());
        var scripted = mock(net.minecraft.world.damagesource.DamageSource.class);
        var attributed = mock(net.minecraft.world.damagesource.DamageSource.class);
        when(attributed.getDirectEntity()).thenReturn(projectile);
        try (MockedStatic<HostedAllies> allies = mockStatic(HostedAllies.class)) {
            active(boss);
            NativeEncounterSource.run(boss, () -> {
                assertSame(boss, HostedEncounterApi.damageOwner(scripted));
                assertSame(player, HostedEncounterApi.damageOwner(attributed));
                assertFalse(NativeEncounterSource.capture(projectile));
                assertFalse(projectile.getPersistentData().hasUUID("JEMHostedNativeOwner"));
                projectile.getPersistentData().putUUID("JEMHostedNativeOwner", UUID.randomUUID());
                assertSame(player, HostedEncounterApi.damageOwner(attributed));
                assertFalse(NativeEncounterSource.unresolved(projectile));
            });
            assertNull(HostedEncounterApi.damageOwner(scripted));
        }
    }

    @Test void unresolvedMarkedEffectKeepsNativeLifetimeButCannotProvideDamagePermissionUntilOwnerLoads() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        Entity effect = mock(Entity.class);
        when(effect.getUUID()).thenReturn(UUID.randomUUID());
        when(effect.level()).thenReturn(level);
        CompoundTag marker = new CompoundTag();
        marker.putUUID("JEMHostedNativeOwner", boss.getUUID());
        when(effect.getPersistentData()).thenReturn(marker);
        var scripted = mock(net.minecraft.world.damagesource.DamageSource.class);
        try (MockedStatic<HostedAllies> allies = mockStatic(HostedAllies.class)) {
            marker.putUUID("JEMHostedNativeAttempt", active(boss));
            assertTrue(NativeEncounterSource.unresolved(effect));
            boolean[] ticked = {false};
            NativeEncounterSource.run(effect, () -> {
                ticked[0] = true;
                assertSame(effect, HostedEncounterApi.damageOwner(scripted));
                assertTrue(NativeEncounterSource.unresolved(HostedEncounterApi.damageOwner(scripted)));
            });
            assertTrue(ticked[0]);
            when(level.getEntity(boss.getUUID())).thenReturn(boss);
            assertFalse(NativeEncounterSource.unresolved(effect));
            NativeEncounterSource.run(effect, () -> assertSame(boss, HostedEncounterApi.damageOwner(scripted)));
            assertNull(NativeEncounterSource.current());
        }
    }

    @Test void exactNativeCrossDimensionSpawnResolvesOnlyItsPersistedSourceDimensionWithoutLoadingChunks() {
        var server = mock(net.minecraft.server.MinecraftServer.class);
        ServerLevel sourceLevel = mock(ServerLevel.class), otherLevel = mock(ServerLevel.class);
        LivingEntity boss = boss(sourceLevel);
        when(sourceLevel.getServer()).thenReturn(server);
        when(otherLevel.getServer()).thenReturn(server);
        when(otherLevel.dimension()).thenReturn(net.minecraft.world.level.Level.NETHER);
        when(server.getLevel(net.minecraft.world.level.Level.OVERWORLD)).thenReturn(sourceLevel);
        when(sourceLevel.getEntity(boss.getUUID())).thenReturn(boss);
        Entity effect = mock(Entity.class);
        when(effect.getUUID()).thenReturn(UUID.randomUUID());
        when(effect.level()).thenReturn(otherLevel);
        CompoundTag marker = new CompoundTag();
        when(effect.getPersistentData()).thenReturn(marker);
        var scripted = mock(net.minecraft.world.damagesource.DamageSource.class);
        try (MockedStatic<HostedAllies> allies = mockStatic(HostedAllies.class)) {
            active(boss);
            NativeEncounterSource.run(boss, () -> assertTrue(NativeEncounterSource.capture(effect)));
            assertEquals("minecraft:overworld", marker.getString("JEMHostedNativeDimension"));
            assertSame(boss, NativeEncounterSource.owner(effect));
            NativeEncounterSource.run(effect, () -> assertSame(boss, HostedEncounterApi.damageOwner(scripted)));
            verify(otherLevel, never()).getEntity(any(UUID.class));
            verify(sourceLevel, never()).getChunkSource();
        }
    }

    @Test void previousAttemptEffectCannotInheritNewTokenForSameBossOrRetagItsChildren() {
        var server = mock(net.minecraft.server.MinecraftServer.class);
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        when(level.getServer()).thenReturn(server);
        when(server.getLevel(net.minecraft.world.level.Level.OVERWORLD)).thenReturn(level);
        when(level.getEntity(boss.getUUID())).thenReturn(boss);
        Entity effect = mock(Entity.class), child = mock(Entity.class);
        when(effect.getUUID()).thenReturn(UUID.randomUUID());
        when(effect.level()).thenReturn(level);
        when(effect.getPersistentData()).thenReturn(new CompoundTag());
        when(child.level()).thenReturn(level);
        when(child.getPersistentData()).thenReturn(new CompoundTag());
        var scripted = mock(net.minecraft.world.damagesource.DamageSource.class);
        try (MockedStatic<HostedAllies> allies = mockStatic(HostedAllies.class)) {
            UUID previous = active(boss);
            NativeEncounterSource.run(boss, () -> assertTrue(NativeEncounterSource.capture(effect)));
            assertFalse(NativeEncounterSource.denied(effect));
            active(boss);
            assertTrue(NativeEncounterSource.denied(effect));
            NativeEncounterSource.run(effect, () -> {
                assertSame(effect, HostedEncounterApi.damageOwner(scripted));
                assertTrue(NativeEncounterSource.denied(HostedEncounterApi.damageOwner(scripted)));
                assertFalse(NativeEncounterSource.capture(child));
            });
            NativeEncounterSource.run(boss, () -> assertFalse(NativeEncounterSource.capture(effect)));
            assertEquals(previous, effect.getPersistentData().getUUID("JEMHostedNativeAttempt"));
            assertFalse(child.getPersistentData().hasUUID("JEMHostedNativeOwner"));
            assertNull(NativeEncounterSource.current());
        }
    }

    @Test void heldBossAndLivingSupportRemainDormantButTransientNativeEffectsKeepTheirLifetime() throws Exception {
        LivingEntity boss = mock(LivingEntity.class), support = mock(LivingEntity.class);
        Entity effect = mock(Entity.class);
        when(boss.getPersistentData()).thenReturn(new CompoundTag());
        when(support.getPersistentData()).thenReturn(new CompoundTag());
        when(effect.getPersistentData()).thenReturn(new CompoundTag());
        var mixin = mock(com.siirio.jemworldbosstiers.mixin.HostedBossTickMixin.class, CALLS_REAL_METHODS);
        var held = com.siirio.jemworldbosstiers.mixin.HostedBossTickMixin.class.getDeclaredMethod("jem$waitForHostedStart", Entity.class,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);
        held.setAccessible(true);
        try (MockedStatic<HostedEncounterApi> api = mockStatic(HostedEncounterApi.class)) {
            api.when(() -> HostedEncounterApi.damageOwner(any(Entity.class))).thenReturn(boss);
            api.when(() -> HostedEncounterApi.held(boss)).thenReturn(true);
            var effectTick = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("tick", true);
            held.invoke(mixin, effect, effectTick);
            assertFalse(effectTick.isCancelled());
            var supportTick = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("tick", true);
            held.invoke(mixin, support, supportTick);
            assertTrue(supportTick.isCancelled());
            var bossTick = new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("tick", true);
            held.invoke(mixin, boss, bossTick);
            assertTrue(bossTick.isCancelled());
        }
    }

    @Test void duplicateNativeDeathCannotMoveTheFrozenVictoryTimeAndLivingBossCannotSettle() {
        ServerLevel level = mock(ServerLevel.class);
        LivingEntity boss = boss(level);
        var server = boss.getServer();
        when(server.overworld()).thenReturn(level);
        when(level.getGameTime()).thenReturn(100L, 200L);
        when(boss.blockPosition()).thenReturn(net.minecraft.core.BlockPos.ZERO);
        doReturn(net.minecraft.world.entity.EntityType.WITHER).when(boss).getType();
        var source = mock(net.minecraft.world.damagesource.DamageSource.class);
        var death = new net.minecraftforge.event.entity.living.LivingDeathEvent(boss, source);
        HostedEncounters.death(death);
        assertFalse(boss.getPersistentData().getCompound("JEMHostedEncounter").contains("VictoryAt"));
        when(boss.isDeadOrDying()).thenReturn(true);
        HostedEncounters.death(death);
        HostedEncounters.death(death);
        var state = boss.getPersistentData().getCompound("JEMHostedEncounter");
        assertEquals(100L, state.getLong("VictoryAt"));
        assertEquals(boss.getUUID(), state.getUUID("Boss"));
        verify(level, times(1)).getGameTime();
    }
}
