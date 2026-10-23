package com.siirio.jemcompat.feature.shelter.compat;

import com.siirio.jemcompat.feature.shelter.CompanionAdapters;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

/** Exact installed-class admission. Native serializers remain the inventory/capability owner. */
public final class PackCompanionAdapter {
    public interface NativeOwner { UUID jemShelterNativeOwner(); }
    public interface NativeSummon extends NativeOwner { void jemShelterCaptureSummon(CompoundTag tag); void jemShelterResumeSummon(); void jemShelterAbortSummon(); void jemShelterCommitSummon(); }
    public interface NativeSummonManager { void jemShelterCapture(Entity entity, CompoundTag tag); Object jemShelterBegin(Entity entity); void jemShelterAbort(Entity entity, Object token); }
    public interface NativeExpiration { UUID jemShelterId(); int jemShelterExpiry(); }
    public interface NativeTransient { void jemShelterWriteTransient(CompoundTag tag); void jemShelterReadTransient(CompoundTag tag); }
    private static final Set<String> CLASSES = Set.of(
            "dev.xylonity.companions.common.entity.summon.BubbleFrogEntity",
            "dev.xylonity.companions.common.entity.summon.EmberPoleEntity",
            "dev.xylonity.companions.common.entity.summon.EnderFrogEntity",
            "dev.xylonity.companions.common.entity.summon.FireworkToadEntity",
            "dev.xylonity.companions.common.entity.summon.LivingCandleEntity",
            "dev.xylonity.companions.common.entity.summon.NetherBullfrogEntity",
            "io.redspace.ironsspellbooks.entity.mobs.SummonedHorse",
            "io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear",
            "io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton",
            "io.redspace.ironsspellbooks.entity.mobs.SummonedVex",
            "io.redspace.ironsspellbooks.entity.mobs.SummonedZombie",
            "com.hm.efn.entity.doppelganger.DoppelgangerEntity",
            "com.hm.efn.entity.falchion.GuardianEntity",

            "net.jadenxgamer.netherexp.registry.entity.custom.Stampede",
            "tech.alexnijjar.golemoverhaul.common.entities.golems.NetheriteGolem",
            "com.blackgear.vanillabackport.common.level.entities.happyghast.HappyGhast",
            "com.fruityspikes.whaleborne.server.entities.HullbackEntity",
            "com.github.L_Ender.cataclysm.entity.Pet.Modern_Remnant_Entity",
            "com.github.L_Ender.cataclysm.entity.Pet.Netherite_Ministrosity_Entity",
            "com.github.L_Ender.cataclysm.entity.Pet.The_Baby_Leviathan_Entity",
            "com.github.teamfusion.rottencreatures.common.level.entities.living.glacialhunter.HunterWolf",
            "com.ninni.spawn.server.entity.mob.Ant",
            "com.ninni.spawn.server.entity.mob.Hamster",
            "com.ninni.spawn.server.entity.mob.Seal",
            "com.talhanation.recruits.entities.BowmanEntity",
            "com.talhanation.recruits.entities.CaptainEntity",
            "com.talhanation.recruits.entities.CommanderEntity",
            "com.talhanation.recruits.entities.CrossBowmanEntity",
            "com.talhanation.recruits.entities.HorsemanEntity",
            "com.talhanation.recruits.entities.MessengerEntity",
            "com.talhanation.recruits.entities.NomadEntity",
            "com.talhanation.recruits.entities.RecruitEntity",
            "com.talhanation.recruits.entities.RecruitShieldmanEntity",
            "com.talhanation.recruits.entities.ScoutEntity",
            "com.talhanation.recruits.entities.SiegeEngineerEntity",
            "com.talhanation.recruits.entities.VillagerNobleEntity",
            "dev.obscuria.aquamirae.common.entity.Shellback",
            "dev.xylonity.companions.common.entity.companion.AntlionEntity",
            "dev.xylonity.companions.common.entity.companion.CloakEntity",
            "dev.xylonity.companions.common.entity.companion.CorneliusEntity",
            "dev.xylonity.companions.common.entity.companion.CroissantDragonEntity",
            "dev.xylonity.companions.common.entity.companion.DinamoEntity",
            "dev.xylonity.companions.common.entity.companion.GoldenAllayEntity",
            "dev.xylonity.companions.common.entity.companion.MankhEntity",
            "dev.xylonity.companions.common.entity.companion.MinionEntity",
            "dev.xylonity.companions.common.entity.companion.PuppetEntity",
            "dev.xylonity.companions.common.entity.companion.PuppetGloveEntity",
            "dev.xylonity.companions.common.entity.companion.ShadeBatEntity",
            "dev.xylonity.companions.common.entity.companion.ShadeMawEntity",
            "dev.xylonity.companions.common.entity.companion.ShadeSwordEntity",
            "dev.xylonity.companions.common.entity.companion.SoulMageEntity",
            "dev.xylonity.companions.common.entity.companion.TeddyEntity",
            "net.imasillylittleguy.cnc.entity.BlackbearEntity",
            "net.imasillylittleguy.cnc.entity.CaribouEntity",
            "net.imasillylittleguy.cnc.entity.CougarEntity",
            "net.imasillylittleguy.cnc.entity.RingtailEntity",
            "net.mcreator.barnyardbuddies.entity.ClydesdaleHorseEntity",
            "net.mcreator.barnyardbuddies.entity.GreatPyreneesDogEntity",
            "net.mcreator.barnyardbuddies.entity.ManxCatEntity",
            "net.mcreator.barnyardbuddies.entity.TraderClydesdaleEntity",
            "net.mcreator.luminousbeasts.entity.TamedShadowEntity",
            "net.mcreator.luminousnether.entity.PottergeistEntity",
            "net.mcreator.skarriermobs.entity.GlowinEntity",
            "net.mcreator.skarriermobs.entity.QuakeEntity",
            "net.mcreator.skarriermobs.entity.StoneGolemEntity",
            "net.mcreator.unearthedjourney.entity.CarbonemysEntity",
            "net.mcreator.unearthedjourney.entity.GnatusuchusEntity",
            "net.mcreator.unearthedjourney.entity.LeptictidiumEntity",
            "net.mcreator.unearthedjourney.entity.ThylacineEntity",
            "net.mehvahdjukaar.snowyspirit.common.entity.GingyEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.MossyGolemEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.SkeloraptorEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.knights.FHauntedGuardEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.knights.FLivingArmorEntity",
            "net.satisfy.vinery.core.entity.TraderMuleEntity",
            "org.primal.entity.animal.BearEntity",
            "org.primal.entity.animal.EagleEntity",
            "org.primal.entity.animal.LionEntity",
            "org.primal.entity.animal.SnakeEntity",
            "org.primal.entity.animal.WalrusEntity",
            "team.recrafted.blastfromthepast.entity.BurrelEntity",
            "team.recrafted.blastfromthepast.entity.FrostomperEntity",
            "team.recrafted.blastfromthepast.entity.speartooth.SpeartoothEntity",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.AxolotlPetEnderman",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.HammerheadPetEnderman",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.PetEnderman");
    private static final Set<String> TELEPORT = Set.of(
            "dev.xylonity.companions.common.entity.summon.EmberPoleEntity",
            "dev.xylonity.companions.common.entity.summon.LivingCandleEntity",
            "com.github.L_Ender.cataclysm.entity.Pet.Modern_Remnant_Entity",
            "com.github.L_Ender.cataclysm.entity.Pet.Netherite_Ministrosity_Entity",
            "com.github.L_Ender.cataclysm.entity.Pet.The_Baby_Leviathan_Entity",
            "com.ninni.spawn.server.entity.mob.Ant",
            "com.ninni.spawn.server.entity.mob.Hamster",
            "com.talhanation.recruits.entities.MessengerEntity",
            "dev.xylonity.companions.common.entity.companion.AntlionEntity",
            "dev.xylonity.companions.common.entity.companion.CloakEntity",
            "dev.xylonity.companions.common.entity.companion.CroissantDragonEntity",
            "dev.xylonity.companions.common.entity.companion.DinamoEntity",
            "dev.xylonity.companions.common.entity.companion.MankhEntity",
            "dev.xylonity.companions.common.entity.companion.MinionEntity",
            "dev.xylonity.companions.common.entity.companion.PuppetEntity",
            "dev.xylonity.companions.common.entity.companion.PuppetGloveEntity",
            "dev.xylonity.companions.common.entity.companion.ShadeBatEntity",
            "dev.xylonity.companions.common.entity.companion.ShadeSwordEntity",
            "dev.xylonity.companions.common.entity.companion.SoulMageEntity",
            "dev.xylonity.companions.common.entity.companion.TeddyEntity",
            "net.imasillylittleguy.cnc.entity.CougarEntity",
            "net.imasillylittleguy.cnc.entity.RingtailEntity",
            "net.mcreator.barnyardbuddies.entity.GreatPyreneesDogEntity",
            "net.mcreator.barnyardbuddies.entity.ManxCatEntity",
            "net.mcreator.luminousbeasts.entity.TamedShadowEntity",
            "net.mcreator.luminousnether.entity.PottergeistEntity",
            "net.mcreator.skarriermobs.entity.GlowinEntity",
            "net.mcreator.skarriermobs.entity.StoneGolemEntity",
            "net.mcreator.unearthedjourney.entity.CarbonemysEntity",
            "net.mcreator.unearthedjourney.entity.GnatusuchusEntity",
            "net.mcreator.unearthedjourney.entity.LeptictidiumEntity",
            "net.mcreator.unearthedjourney.entity.ThylacineEntity",
            "net.mehvahdjukaar.snowyspirit.common.entity.GingyEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.MossyGolemEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.SkeloraptorEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.knights.FHauntedGuardEntity",
            "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.knights.FLivingArmorEntity",
            "org.primal.entity.animal.BearEntity",
            "org.primal.entity.animal.EagleEntity",
            "org.primal.entity.animal.LionEntity",
            "org.primal.entity.animal.SnakeEntity",
            "team.recrafted.blastfromthepast.entity.speartooth.SpeartoothEntity",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.AxolotlPetEnderman",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.HammerheadPetEnderman",
            "tech.alexnijjar.endermanoverhaul.common.entities.pets.PetEnderman");
    private PackCompanionAdapter() {}
    public static boolean handles(Entity entity) { return CLASSES.contains(entity.getClass().getName()); }
    public static UUID owner(Entity entity) {
        if (!handles(entity)) return null;
        if (entity instanceof NativeOwner nativeOwner) return nativeOwner.jemShelterNativeOwner();
        if (entity instanceof TamableAnimal animal) return animal.isTame() ? animal.getOwnerUUID() : null;
        if (entity instanceof AbstractHorse horse) return horse.isTamed() ? horse.getOwnerUUID() : null;
        return entity instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
    }
    public static CompanionAdapters.Habitat habitat(Entity entity) {
        String name = entity.getClass().getName();
        if (name.equals("com.fruityspikes.whaleborne.server.entities.HullbackEntity")) return CompanionAdapters.Habitat.AQUATIC;
        if (name.equals("com.github.L_Ender.cataclysm.entity.Pet.The_Baby_Leviathan_Entity")
                || name.equals("org.primal.entity.animal.WalrusEntity")
                || name.equals("com.ninni.spawn.server.entity.mob.Seal")
                || name.equals("net.mcreator.unearthedjourney.entity.CarbonemysEntity")
                || name.equals("net.mcreator.unearthedjourney.entity.GnatusuchusEntity")
                || name.equals("dev.obscuria.aquamirae.common.entity.Shellback")) return CompanionAdapters.Habitat.AMPHIBIOUS;
        if (name.equals("io.redspace.ironsspellbooks.entity.mobs.SummonedVex")
                || name.equals("dev.xylonity.companions.common.entity.summon.EnderFrogEntity")
                || name.equals("org.primal.entity.animal.EagleEntity")
                || name.equals("dev.xylonity.companions.common.entity.companion.GoldenAllayEntity")
                || name.equals("dev.xylonity.companions.common.entity.companion.ShadeBatEntity")
                || name.equals("net.mcreator.luminousbeasts.entity.TamedShadowEntity")
                || name.equals("net.mcreator.luminousnether.entity.PottergeistEntity")
                || name.equals("com.blackgear.vanillabackport.common.level.entities.happyghast.HappyGhast")) return CompanionAdapters.Habitat.FLYING;
        return CompanionAdapters.Habitat.GROUND;
    }
    public static boolean nativeOwnerTeleport(Entity entity) { return TELEPORT.contains(entity.getClass().getName()); }
    public static void snapshot(Entity entity, CompoundTag tag) {
        if (entity instanceof NativeSummon summon) summon.jemShelterCaptureSummon(tag);
        if (entity.getClass().getName().equals("com.ninni.spawn.server.entity.mob.Hamster")) {
            // Native defaults restore hydration (20), no active TNT fuse and no vanished wheel membership.
            for (String key : new String[]{"Hydration", "HasEatenTnt", "TntTimer", "InWheel", "WheelCooldown"}) tag.remove(key);
        }
    }
    public static void recovered(Entity entity) {
        if (entity instanceof NativeSummon summon) summon.jemShelterResumeSummon();
    }
    public static boolean recoveryAllowed(CompoundTag tag, net.minecraft.server.level.ServerLevel level) {
        CompoundTag data = tag.getCompound("ForgeData");
        long deadline = data.getLong("jemShelterNativeSummonDeadline");
        if (deadline > 0 && level.getServer().overworld().getGameTime() >= deadline) return false;
        if (tag.getString("id").startsWith("irons_spellbooks:"))
            return data.hasUUID("jemShelterNativeSummoner") && deadline > 0;
        return !tag.getString("id").equals("efn:guardian") || !tag.contains("jemShelterNativeAge")
                || tag.getInt("jemShelterNativeAge") < 600;
    }
    public static void recoveryAborted(Entity entity) {
        if (entity instanceof NativeSummon summon) summon.jemShelterAbortSummon();
    }
    public static void recoveryCommitted(Entity entity) {
        if (entity instanceof NativeSummon summon) summon.jemShelterCommitSummon();
    }
    public static boolean ready(Entity entity) {
        if (!(entity instanceof NativeSummon summon)) return true;
        summon.jemShelterCaptureSummon(new CompoundTag());
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID("jemShelterNativeSummoner") && entity.getServer() != null
                && data.getLong("jemShelterNativeSummonDeadline") > entity.getServer().overworld().getGameTime();
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "jemcompat")
    public static final class NativeSummonLifecycle {
        private static final java.util.Queue<Entity> JOINS = new java.util.concurrent.ConcurrentLinkedQueue<>();
        private NativeSummonLifecycle() {}
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
            Entity entity = event.getEntity();
            if (!event.getLevel().isClientSide && entity instanceof NativeSummon
                    && entity.getPersistentData().hasUUID("jemShelterNativeSummoner")
                    && entity.getPersistentData().contains("jemShelterNativeSummonDeadline")) {
                if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                    com.siirio.jemcompat.feature.shelter.CompanionData.Pet pet =
                            com.siirio.jemcompat.feature.shelter.CompanionData.get(level).pet(entity.getUUID());
                    if (pet != null && (pet.pending != null || pet.restoring)) return;
                }
                JOINS.add(entity);
            }
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            Entity entity;
            for (int remaining = JOINS.size(); remaining > 0 && (entity = JOINS.poll()) != null; remaining--) {
                if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)
                        || !entity.isAlive() || entity.isRemoved() || level.getEntity(entity.getUUID()) != entity) continue;
                com.siirio.jemcompat.feature.shelter.CompanionData.Pet pet =
                        com.siirio.jemcompat.feature.shelter.CompanionData.get(level).pet(entity.getUUID());
                if (pet != null && (pet.pending != null || pet.restoring || pet.epoch != entity.getPersistentData()
                        .getLong(com.siirio.jemcompat.feature.shelter.CompanionData.ENTITY_EPOCH))) continue;
                if (entity.getPersistentData().getLong("jemShelterNativeSummonDeadline") <= level.getServer().overworld().getGameTime()) {
                    entity.discard(); continue;
                }
                java.util.UUID owner = entity.getPersistentData().getUUID("jemShelterNativeSummoner");
                if (level.getServer().getPlayerList().getPlayer(owner) == null) { JOINS.add(entity); continue; }
                ((NativeSummon) entity).jemShelterResumeSummon();
                ((NativeSummon) entity).jemShelterCommitSummon();
            }
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { JOINS.clear(); }
    }
    public static boolean preserveNativeAge(Entity entity) {
        String name = entity.getClass().getName();
        return name.startsWith("dev.xylonity.companions.common.entity.summon.")
                || name.equals("com.hm.efn.entity.falchion.GuardianEntity")
                || name.equals("com.hm.efn.entity.doppelganger.DoppelgangerEntity");
    }
}
