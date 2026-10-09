package com.siirio.jemserver.smp.events;

import com.siirio.jemserver.smp.SmpData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="jem_server",value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class BossDebuffController {
    private static final String PROFILE_CURSOR="setPieceDebuffProfileCursor";
    private static final String PROFILE="jem:debuff_profile";
    private static final String WINDOW_END="jem:debuff_window_end";
    private static final String NEXT_CAST="setPieceDebuffNextCast";
    private static final String LAST_DEBUFF="jem:last_debuff";
    private static final String PENETRATION_END="jem:penetration_end";
    private static final String EPIC_LOCK_END="jem:epic_lock_end";
    private static final String NIGHTFALL_LOCK_END="jem:nightfall_lock_end";
    private static final String NEXT_BLOCK_NOTICE="jem:debuff_block_notice";
    private static final int CAST_WINDOW_TICKS=800;
    private static final int MIN_CAST_COOLDOWN_TICKS=240;
    private static final int CAST_COOLDOWN_VARIANCE_TICKS=161;
    private static final int BLOCK_NOTICE_COOLDOWN_TICKS=20;
    private static final float PENETRATION_DAMAGE_MULTIPLIER=1.3F;
    private static final List<Profile> PROFILES=List.of(
            new Profile(List.of(
                    vanilla("weakness",()->MobEffects.WEAKNESS,240,1),
                    vanilla("slowness",()->MobEffects.MOVEMENT_SLOWDOWN,200,1),
                    vanilla("mining_fatigue",()->MobEffects.DIG_SLOWDOWN,240,1),
                    custom("penetration",200,PENETRATION_END),
                    custom("no_epic_moveset",160,EPIC_LOCK_END))),
            new Profile(List.of(
                    vanilla("darkness",()->MobEffects.DARKNESS,160,0),
                    vanilla("blindness",()->MobEffects.BLINDNESS,100,0),
                    vanilla("nausea",()->MobEffects.CONFUSION,200,0),
                    vanilla("poison",()->MobEffects.POISON,160,0),
                    custom("no_nightfall",160,NIGHTFALL_LOCK_END))),
            new Profile(List.of(
                    vanilla("wither",()->MobEffects.WITHER,120,0),
                    vanilla("hunger",()->MobEffects.HUNGER,240,1),
                    vanilla("levitation",()->MobEffects.LEVITATION,60,0),
                    vanilla("unluck",()->MobEffects.UNLUCK,300,0),
                    vanilla("glowing",()->MobEffects.GLOWING,200,0))));

    public static void initializeBoss(ServerLevel level,CompoundTag row,Mob boss,List<ServerPlayer> players) {
        long now=level.getGameTime();
        int profile=Math.floorMod(row.getInt(PROFILE_CURSOR),PROFILES.size());
        row.putInt(PROFILE_CURSOR,profile+1);
        CompoundTag data=boss.getPersistentData();
        data.putInt(PROFILE,profile);
        data.putLong(WINDOW_END,now+CAST_WINDOW_TICKS);
        if(!row.contains(NEXT_CAST)) {
            cast(level,boss,PROFILES.get(profile),players);
            row.putLong(NEXT_CAST,nextCast(now,level.random));
        }
        SmpData.get(level.getServer()).changed(row);
    }

    public static void tick(ServerLevel level,CompoundTag row,List<Mob> bosses) {
        long now=level.getGameTime();
        if(now<row.getLong(NEXT_CAST)) return;
        List<ServerPlayer> players=new EventSession(row).active(level.getServer());
        if(players.isEmpty()) return;
        List<Mob> casters=bosses.stream().filter(boss->boss.getPersistentData().contains(WINDOW_END)&&now<=boss.getPersistentData().getLong(WINDOW_END)).toList();
        if(casters.isEmpty()) return;
        Mob boss=casters.get(level.random.nextInt(casters.size()));
        int profile=Math.floorMod(boss.getPersistentData().getInt(PROFILE),PROFILES.size());
        cast(level,boss,PROFILES.get(profile),players);
        row.putLong(NEXT_CAST,nextCast(now,level.random));
        SmpData.get(level.getServer()).changed(row);
    }

    public static void reset(CompoundTag row) {
        row.remove(NEXT_CAST);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof ServerPlayer player)||!ModList.get().isLoaded("epicfight")) return;
        level.getServer().execute(()->com.siirio.jemserver.smp.compat.EpicFightDebuffBridge.register(player));
    }

    @SubscribeEvent
    public static void cloned(PlayerEvent.Clone event) {
        copy(event.getOriginal().getPersistentData(),event.getEntity().getPersistentData(),PENETRATION_END);
        copy(event.getOriginal().getPersistentData(),event.getEntity().getPersistentData(),EPIC_LOCK_END);
        copy(event.getOriginal().getPersistentData(),event.getEntity().getPersistentData(),NIGHTFALL_LOCK_END);
    }

    public static float penetrationMultiplier(ServerPlayer player) {
        return active(player,PENETRATION_END)?PENETRATION_DAMAGE_MULTIPLIER:1F;
    }

    public static boolean epicMovesetBlocked(ServerPlayer player) {
        return active(player,EPIC_LOCK_END);
    }

    public static boolean nightfallBlocked(ServerPlayer player) {
        return active(player,NIGHTFALL_LOCK_END);
    }

    public static void explainBlocked(ServerPlayer player,String effect) {
        long now=player.server.overworld().getGameTime();
        CompoundTag data=player.getPersistentData();
        if(now<data.getLong(NEXT_BLOCK_NOTICE)) return;
        data.putLong(NEXT_BLOCK_NOTICE,now+BLOCK_NOTICE_COOLDOWN_TICKS);
        player.displayClientMessage(Component.translatable("jem.event.blood_debuff.blocked_"+effect),true);
    }

    private static void cast(ServerLevel level,Mob boss,Profile profile,List<ServerPlayer> players) {
        if(players.isEmpty()) return;
        CompoundTag data=boss.getPersistentData();
        List<Debuff> available=profile.debuffs().stream()
                .filter(debuff->!debuff.id().equals(data.getString(LAST_DEBUFF)))
                .filter(BossDebuffController::available)
                .toList();
        if(available.isEmpty()) available=profile.debuffs().stream().filter(BossDebuffController::available).toList();
        if(available.isEmpty()) return;
        Debuff debuff=available.get(level.random.nextInt(available.size()));
        ServerPlayer target=players.get(level.random.nextInt(players.size()));
        debuff.apply(target);
        data.putString(LAST_DEBUFF,debuff.id());
        Component name=debuff.name();
        target.sendSystemMessage(Component.translatable("jem.event.blood_debuff.applied",boss.getDisplayName(),name,debuff.durationTicks()/20));
        level.sendParticles(ParticleTypes.WITCH,target.getX(),target.getY()+target.getBbHeight()*.6,target.getZ(),24,.45,.7,.45,.08);
        level.playSound(null,target.blockPosition(),SoundEvents.ILLUSIONER_CAST_SPELL,SoundSource.HOSTILE,1F,.8F);
    }

    private static boolean available(Debuff debuff) {
        return (!debuff.id().equals("no_epic_moveset")||ModList.get().isLoaded("epicfight"))
                &&(!debuff.id().equals("no_nightfall")||ModList.get().isLoaded("efn"));
    }

    private static Debuff vanilla(String id,Supplier<MobEffect> effect,int durationTicks,int amplifier) {
        return new Debuff(id,durationTicks,()->effect.get().getDisplayName(),player->player.addEffect(new MobEffectInstance(effect.get(),durationTicks,amplifier,false,true,true)));
    }

    private static Debuff custom(String id,int durationTicks,String state) {
        return new Debuff(id,durationTicks,()->Component.translatable("jem.event.blood_debuff."+id),player->player.getPersistentData().putLong(state,player.server.overworld().getGameTime()+durationTicks));
    }

    private static long nextCast(long now,RandomSource random) {
        return now+MIN_CAST_COOLDOWN_TICKS+random.nextInt(CAST_COOLDOWN_VARIANCE_TICKS);
    }

    private static boolean active(ServerPlayer player,String key) {
        return player.getPersistentData().getLong(key)>player.server.overworld().getGameTime();
    }

    private static void copy(CompoundTag source,CompoundTag target,String key) {
        if(source.contains(key)) target.putLong(key,source.getLong(key));
    }

    private record Profile(List<Debuff> debuffs) {}
    private record Debuff(String id,int durationTicks,Supplier<Component> displayName,Effect action) {
        Component name() {return displayName.get();}
        void apply(ServerPlayer player) {action.apply(player);}
    }
    @FunctionalInterface private interface Effect {void apply(ServerPlayer player);}
    private BossDebuffController() {}
}
