package com.siirio.mobhome.feature.shelter.compat;

import com.siirio.mobhome.feature.shelter.CompanionAdapters;
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
    public interface NativeTransient { void jemShelterWriteTransient(CompoundTag tag); void jemShelterReadTransient(CompoundTag tag); }
    private static final Set<String> CLASSES = Set.of(
            "icy.betterhorses.net.entity.AmericanPaintHorse",
            "icy.betterhorses.net.entity.AndalusianHorse",
            "icy.betterhorses.net.entity.AppaloosaHorse",
            "icy.betterhorses.net.entity.ArabianHorse",
            "icy.betterhorses.net.entity.BelgianHorse",
            "icy.betterhorses.net.entity.ClydesdaleHorse",
            "icy.betterhorses.net.entity.FriesianHorse",
            "icy.betterhorses.net.entity.HaflingerHorse",
            "icy.betterhorses.net.entity.IcelandicHorse",
            "icy.betterhorses.net.entity.MorganHorse",
            "icy.betterhorses.net.entity.MustangHorse",
            "icy.betterhorses.net.entity.PercheronHorse",
            "icy.betterhorses.net.entity.QuarterHorse",
            "icy.betterhorses.net.entity.ShireHorse",
            "icy.betterhorses.net.entity.ThoroughbredHorse",
            "dev.xylonity.companions.common.entity.summon.BubbleFrogEntity",
            "dev.xylonity.companions.common.entity.summon.EmberPoleEntity",
            "dev.xylonity.companions.common.entity.summon.EnderFrogEntity",
            "dev.xylonity.companions.common.entity.summon.FireworkToadEntity",
            "dev.xylonity.companions.common.entity.summon.LivingCandleEntity",
            "dev.xylonity.companions.common.entity.summon.NetherBullfrogEntity",
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
        if (name.equals("dev.xylonity.companions.common.entity.summon.EnderFrogEntity")
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
        if (entity.getClass().getName().equals("com.ninni.spawn.server.entity.mob.Hamster")) {
            // Native defaults restore hydration (20), no active TNT fuse and no vanished wheel membership.
            for (String key : new String[]{"Hydration", "HasEatenTnt", "TntTimer", "InWheel", "WheelCooldown"}) tag.remove(key);
        }
    }
    public static void recovered(Entity entity) {}
    public static boolean recoveryAllowed(CompoundTag tag, net.minecraft.server.level.ServerLevel level) {
        return !tag.getString("id").equals("efn:guardian") || !tag.contains("jemShelterNativeAge")
                || tag.getInt("jemShelterNativeAge") < 600;
    }
    public static void recoveryAborted(Entity entity) {}
    public static void recoveryCommitted(Entity entity) {}
    public static boolean ready(Entity entity) { return true; }
    public static boolean preserveNativeAge(Entity entity) {
        String name = entity.getClass().getName();
        return name.startsWith("dev.xylonity.companions.common.entity.summon.")
                || name.equals("com.hm.efn.entity.falchion.GuardianEntity")
                || name.equals("com.hm.efn.entity.doppelganger.DoppelgangerEntity");
    }
}
