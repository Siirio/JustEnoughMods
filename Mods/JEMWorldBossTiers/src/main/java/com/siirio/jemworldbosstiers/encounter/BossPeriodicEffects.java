package com.siirio.jemworldbosstiers.encounter;

import com.siirio.jemworldbosstiers.JemWorldBossTiers;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BossPeriodicEffects {
    private static final TagKey<EntityType<?>> ZEUS_FAVORITES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation(JemWorldBossTiers.MOD_ID,"zeus_favorites"));
    private static final String ROOT="JEMBossPeriodicEffects";
    private static final String NEXT="ZeusNext";
    private static final String IMPACT="ZeusImpact";
    private static final String X="ZeusX";
    private static final String Y="ZeusY";
    private static final String Z="ZeusZ";
    private static final int INTERVAL_TICKS=140;
    private static final int TELEGRAPH_TICKS=10;
    private static final int TELEGRAPH_PARTICLES=10;
    private static final double TELEGRAPH_RADIUS=.7D;
    private static final double STRIKE_RADIUS=1.75D;
    private static final float STRIKE_DAMAGE=8F;

    public static void begin(LivingEntity boss) {
        if(!boss.getType().is(ZEUS_FAVORITES)) return;
        CompoundTag data=new CompoundTag();
        data.putLong(NEXT,now(boss)+INTERVAL_TICKS);
        boss.getPersistentData().put(ROOT,data);
    }

    public static void tick(LivingEntity boss,List<ServerPlayer> participants) {
        if(!(boss.level() instanceof ServerLevel level)||!boss.isAlive()||!boss.getType().is(ZEUS_FAVORITES)) return;
        CompoundTag data=data(boss);
        long now=now(boss);
        if(data.contains(IMPACT)) {
            telegraph(level,data);
            if(now>=data.getLong(IMPACT)) impact(level,boss,data,participants);
            return;
        }
        if(now<data.getLong(NEXT)) return;
        List<ServerPlayer> targets=participants.stream()
                .filter(player->player.isAlive()&&!player.isSpectator()&&player.level()==boss.level())
                .toList();
        if(targets.isEmpty()) return;
        ServerPlayer target=targets.get(boss.getRandom().nextInt(targets.size()));
        Vec3 position=target.position();
        data.putDouble(X,position.x);
        data.putDouble(Y,position.y);
        data.putDouble(Z,position.z);
        data.putLong(IMPACT,now+TELEGRAPH_TICKS);
        target.displayClientMessage(Component.translatable("jem.boss_effect.zeus_warning"),true);
        level.playSound(null,target.blockPosition(),SoundEvents.TRIDENT_RIPTIDE_1,SoundSource.HOSTILE,.8F,1.35F);
        telegraph(level,data);
    }

    public static void clear(LivingEntity boss) {
        boss.getPersistentData().remove(ROOT);
    }

    private static void telegraph(ServerLevel level,CompoundTag data) {
        double x=data.getDouble(X),y=data.getDouble(Y)+.08D,z=data.getDouble(Z);
        for(int index=0;index<TELEGRAPH_PARTICLES;index++) {
            double angle=Math.PI*2D*index/TELEGRAPH_PARTICLES;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,x+Math.cos(angle)*TELEGRAPH_RADIUS,y,z+Math.sin(angle)*TELEGRAPH_RADIUS,1,0,.02D,0,.01D);
        }
    }

    private static void impact(ServerLevel level,LivingEntity boss,CompoundTag data,List<ServerPlayer> participants) {
        double x=data.getDouble(X),y=data.getDouble(Y),z=data.getDouble(Z);
        LightningBolt lightning=EntityType.LIGHTNING_BOLT.create(level);
        if(lightning!=null) {
            lightning.moveTo(x,y,z);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
        double radiusSquared=STRIKE_RADIUS*STRIKE_RADIUS;
        participants.stream()
                .filter(player->player.isAlive()&&!player.isSpectator()&&player.level()==level&&player.distanceToSqr(x,y,z)<=radiusSquared)
                .forEach(player->player.hurt(level.damageSources().mobAttack(boss),STRIKE_DAMAGE));
        level.playSound(null,x,y,z,SoundEvents.LIGHTNING_BOLT_IMPACT,SoundSource.HOSTILE,1F,1F);
        data.remove(IMPACT);
        data.remove(X);
        data.remove(Y);
        data.remove(Z);
        data.putLong(NEXT,now(boss)+INTERVAL_TICKS);
    }

    private static CompoundTag data(LivingEntity boss) {
        CompoundTag persistent=boss.getPersistentData();
        if(!persistent.contains(ROOT)) begin(boss);
        return persistent.getCompound(ROOT);
    }

    private static long now(LivingEntity boss) {
        return boss.getServer().overworld().getGameTime();
    }

    private BossPeriodicEffects() {}
}
