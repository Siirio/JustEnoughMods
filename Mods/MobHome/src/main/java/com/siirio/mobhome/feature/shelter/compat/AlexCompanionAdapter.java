package com.siirio.mobhome.feature.shelter.compat;

import com.siirio.mobhome.feature.shelter.CompanionAdapters.Habitat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.siirio.mobhome.feature.shelter.CompanionData;
import com.siirio.mobhome.feature.shelter.CompanionAdapters;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

/** Exact installed Alex's Mobs1.22.9 / Alex's Caves2.0.2 entity contracts, not a TamableAnimal-wide grant. */
@Mod.EventBusSubscriber(modid = "mobhome")
public final class AlexCompanionAdapter {
    private static final String BODY = "JemShelterWormBody", CLEARANCE = "JemShelterAlexClearance", ORIGIN = "JemShelterWormOrigin";
    private static final java.util.Set<Entity> ADMITTING_PARTS = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private record Profile(String implementation, Habitat habitat, boolean ownerTeleport, boolean riderCustodian) { }
    private static final Map<String, Profile> PROFILES = profiles();
    private static Map<String, Profile> profiles() {
        Map<String, Profile> rows = new LinkedHashMap<>();
        am(rows, "bald_eagle", "BaldEagle", Habitat.FLYING, true, false);
        am(rows, "caiman", "Caiman", Habitat.AMPHIBIOUS, true, false);
        am(rows, "capuchin_monkey", "CapuchinMonkey", Habitat.GROUND, true, false);
        am(rows, "cosmaw", "Cosmaw", Habitat.FLYING, true, false);
        am(rows, "crocodile", "Crocodile", Habitat.AMPHIBIOUS, false, false);
        am(rows, "crow", "Crow", Habitat.FLYING, true, false);
        am(rows, "elephant", "Elephant", Habitat.GROUND, false, false);
        am(rows, "endergrade", "Endergrade", Habitat.FLYING, false, true);
        am(rows, "flutter", "Flutter", Habitat.FLYING, true, false);
        am(rows, "gorilla", "Gorilla", Habitat.GROUND, false, false);
        am(rows, "grizzly_bear", "GrizzlyBear", Habitat.GROUND, true, false);
        am(rows, "kangaroo", "Kangaroo", Habitat.GROUND, true, false);
        am(rows, "komodo_dragon", "KomodoDragon", Habitat.GROUND, true, false);
        // Laviathan's verified navigation and fluid predicates accept water/lava; ground is also native-safe.
        am(rows, "laviathan", "Laviathan", Habitat.AMPHIBIOUS, false, true);
        // These two maintain a native moisture reserve and eventually take damage on dry ground.
        am(rows, "mantis_shrimp", "MantisShrimp", Habitat.AQUATIC, true, false);
        am(rows, "mimic_octopus", "MimicOctopus", Habitat.AQUATIC, true, false);
        am(rows, "mudskipper", "Mudskipper", Habitat.AMPHIBIOUS, true, false);
        am(rows, "raccoon", "Raccoon", Habitat.GROUND, true, false);
        am(rows, "sugar_glider", "SugarGlider", Habitat.FLYING, true, false);
        am(rows, "tarantula_hawk", "TarantulaHawk", Habitat.FLYING, true, false);
        am(rows, "tusklin", "Tusklin", Habitat.GROUND, false, true);
        am(rows, "warped_toad", "WarpedToad", Habitat.AMPHIBIOUS, true, false);
        ac(rows, "atlatitan", "Atlatitan", Habitat.GROUND, false, true);
        ac(rows, "candicorn", "Candicorn", Habitat.GROUND, true, false);
        ac(rows, "gum_worm", "GumWorm", Habitat.GROUND, false, true);
        ac(rows, "raycat", "Raycat", Habitat.GROUND, true, false);
        ac(rows, "subterranodon", "Subterranodon", Habitat.FLYING, true, false);
        ac(rows, "tremorsaurus", "Tremorsaurus", Habitat.GROUND, true, false);
        ac(rows, "tremorzilla", "Tremorzilla", Habitat.AMPHIBIOUS, true, false);
        ac(rows, "vallumraptor", "Vallumraptor", Habitat.GROUND, false, false);
        return Map.copyOf(rows);
    }
    private static void am(Map<String, Profile> rows, String id, String type, Habitat habitat, boolean teleport, boolean rider) {
        rows.put("alexsmobs:" + id, new Profile("com.github.alexthe666.alexsmobs.entity.Entity" + type, habitat, teleport, rider));
    }
    private static void ac(Map<String, Profile> rows, String id, String type, Habitat habitat, boolean teleport, boolean rider) {
        rows.put("alexscaves:" + id, new Profile("com.github.alexmodguy.alexscaves.server.entity.living." + type + "Entity", habitat, teleport, rider));
    }
    private AlexCompanionAdapter() { }
    private static Profile profile(Entity entity) {
        Profile profile = PROFILES.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        return profile != null && profile.implementation.equals(entity.getClass().getName()) ? profile : null;
    }
    public static boolean handles(Entity entity) { return profile(entity) != null; }
    public static UUID owner(Entity entity) {
        return handles(entity) && entity instanceof TamableAnimal animal && animal.isTame() ? animal.getOwnerUUID() : null;
    }
    public static Habitat habitat(Entity entity) { Profile profile = profile(entity); return profile == null ? Habitat.GROUND : profile.habitat; }
    public static boolean nativeOwnerTeleport(Entity entity) { Profile profile = profile(entity); return profile != null && profile.ownerTeleport; }
    public static boolean permitsRiderCustodian(Entity entity) { Profile profile = profile(entity); return profile != null && profile.riderCustodian; }
    /** Resolve the native worm's ride proxy without inventing a second companion identity. */
    public static Entity subject(Entity entity) {
        if (entity.getClass().getName().equals("com.github.alexmodguy.alexscaves.server.entity.living.GumWormSegmentEntity")) {
            Object head = invoke(entity, "getHeadEntity"); return head instanceof Entity parent ? parent : entity;
        }
        return entity;
    }
    public static Entity canonical(Entity entity) { return subject(entity); }
    public static UUID rider(Entity entity) {
        LivingEntity rider = controllingRider(subject(entity));
        return rider instanceof net.minecraft.server.level.ServerPlayer player ? player.getUUID() : null;
    }
    public static LivingEntity controllingRider(Entity entity) {
        if (!handles(entity)) return null;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("alexscaves:gum_worm")) {
            Object player = invoke(entity, "getRidingPlayer"); return player instanceof LivingEntity living ? living : null;
        }
        return entity.getControllingPassenger();
    }
    public static void snapshot(Entity entity, CompoundTag nativeData) {
        if (!handles(entity)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (id.equals("alexsmobs:bald_eagle")) { nativeData.putBoolean("Launched", false); nativeData.putInt("LaunchTime", 0); }
        CompoundTag extra = nativeData.getCompound("ForgeData");
        AABB clearance = nativeBounds(entity).move(entity.position().scale(-1)); ListTag coordinates = new ListTag();
        for (double coordinate : new double[]{clearance.minX, clearance.minY, clearance.minZ, clearance.maxX, clearance.maxY, clearance.maxZ}) coordinates.add(net.minecraft.nbt.DoubleTag.valueOf(coordinate));
        extra.put(CLEARANCE, coordinates);
        if (id.equals("alexscaves:gum_worm")) {
            ListTag body = collectBody(entity); extra.put(BODY, body); extra.put(ORIGIN, position(entity.position()));
        }
        nativeData.put("ForgeData", extra);
        // Commands, taming, variant, equipment, native Items and persistent traits remain native-serialized.
    }
    public static void recovered(Entity entity) {
        if (!handles(entity)) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        // Reset only native environmental reserves to the exact value used by native water/bucket restoration.
        if (id.equals("alexsmobs:mantis_shrimp") || id.equals("alexsmobs:mimic_octopus")) invoke(entity, "setMoistness", int.class, 60000);
        if (id.equals("alexsmobs:orca")) invoke(entity, "setMoistness", int.class, 2400);
        if (id.equals("alexscaves:gum_worm")) restoreBody(entity);
    }
    public static boolean ready(Entity entity) {
        if (!handles(entity) || !id(entity).equals("alexscaves:gum_worm")) return true;
        ListTag body = entity.getPersistentData().contains(BODY) ? entity.getPersistentData().getList(BODY, Tag.TAG_COMPOUND) : collectBody(entity);
        return complete(body, entity.getUUID());
    }
    public static AABB bounds(Entity entity) {
        ListTag saved = entity.getPersistentData().getList(CLEARANCE, Tag.TAG_DOUBLE);
        AABB box = saved.size() == 6 ? new AABB(saved.getDouble(0), saved.getDouble(1), saved.getDouble(2), saved.getDouble(3), saved.getDouble(4), saved.getDouble(5)).move(entity.position()) : nativeBounds(entity);
        if (id(entity).equals("alexscaves:gum_worm") && entity.getPersistentData().contains(BODY)) {
            Vec3 translation = entity.position().subtract(readPosition(entity.getPersistentData().getList(ORIGIN, Tag.TAG_DOUBLE)));
            for (Tag row : entity.getPersistentData().getList(BODY, Tag.TAG_COMPOUND)) {
                Vec3 pos = readPosition(((CompoundTag) row).getList("Pos", Tag.TAG_DOUBLE)).add(translation);
                box = box.minmax(new AABB(pos.x - 1, pos.y, pos.z - 1, pos.x + 1, pos.y + 2, pos.z + 1));
            }
        }
        return box;
    }
    private static AABB nativeBounds(Entity entity) {
        AABB result = entity.getBoundingBox();
        var parts = entity.getParts();
        if (parts != null) for (Entity part : parts) if (part != null) result = result.minmax(part.getBoundingBox());
        if (id(entity).equals("alexscaves:gum_worm")) for (Entity part : loadedWormParts(entity)) result = result.minmax(part.getBoundingBox());
        return result;
    }
    private static String id(Entity entity) { return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(); }
    private static ListTag position(Vec3 point) {
        ListTag tag = new ListTag(); tag.add(net.minecraft.nbt.DoubleTag.valueOf(point.x)); tag.add(net.minecraft.nbt.DoubleTag.valueOf(point.y)); tag.add(net.minecraft.nbt.DoubleTag.valueOf(point.z)); return tag;
    }
    private static Vec3 readPosition(ListTag tag) { return new Vec3(tag.getDouble(0), tag.getDouble(1), tag.getDouble(2)); }
    private static java.util.List<Entity> loadedWormParts(Entity head) {
        if (!(head.level() instanceof ServerLevel level)) return java.util.List.of();
        return level.getEntitiesOfClass(Entity.class, head.getBoundingBox().inflate(64), entity ->
                id(entity).equals("alexscaves:gum_worm_segment") && head.getUUID().equals(invoke(entity, "getHeadUUID")));
    }
    private static ListTag collectBody(Entity head) {
        ListTag rows = new ListTag();
        for (Entity part : loadedWormParts(head)) { CompoundTag tag = part.saveWithoutId(new CompoundTag()); tag.putString("id", id(part)); rows.add(tag); }
        return rows;
    }
    private static boolean complete(ListTag rows, UUID head) {
        if (rows.size() < 15 || rows.size() > 19) return false; // Exact native finalizeSpawn count is15 + nextInt(5).
        Map<UUID, CompoundTag> parts = new LinkedHashMap<>(); CompoundTag current = null;
        for (Tag row : rows) {
            CompoundTag tag = (CompoundTag) row;
            if (!tag.hasUUID("UUID") || !tag.hasUUID("HeadUUID") || !head.equals(tag.getUUID("HeadUUID")) || parts.put(tag.getUUID("UUID"), tag) != null) return false;
            if (tag.getInt("Index") == 0) { if (current != null) return false; current = tag; }
        }
        UUID previous = head;
        for (int index = 0; index < rows.size(); index++) {
            if (current == null || current.getInt("Index") != index || !current.hasUUID("FrontUUID") || !previous.equals(current.getUUID("FrontUUID"))) return false;
            previous = current.getUUID("UUID");
            if (index == rows.size() - 1) return !current.hasUUID("BackUUID");
            if (!current.hasUUID("BackUUID")) return false; current = parts.get(current.getUUID("BackUUID"));
        }
        return false;
    }
    private static void restoreBody(Entity head) {
        if (!(head.level() instanceof ServerLevel level)) return;
        ListTag rows = head.getPersistentData().getList(BODY, Tag.TAG_COMPOUND);
        if (!complete(rows, head.getUUID())) throw new IllegalStateException("Native Gum Worm body awaits missing saved segments");
        Vec3 delta = head.position().subtract(readPosition(head.getPersistentData().getList(ORIGIN, Tag.TAG_DOUBLE)));
        long epoch = head.getPersistentData().getLong(CompanionData.ENTITY_EPOCH);
        for (Tag row : rows) {
            CompoundTag nativePart = ((CompoundTag) row).copy(); Vec3 point = readPosition(nativePart.getList("Pos", Tag.TAG_DOUBLE)).add(delta);
            Entity previous = level.getEntity(nativePart.getUUID("UUID"));
            if (previous != null) {
                if (!id(previous).equals("alexscaves:gum_worm_segment") || !head.getUUID().equals(invoke(previous, "getHeadUUID"))) throw new IllegalStateException("Native worm segment identity conflict");
                previous.discard();
            }
            Entity part = EntityType.loadEntityRecursive(nativePart, level, entity -> entity);
            if (part == null || !id(part).equals("alexscaves:gum_worm_segment")) throw new IllegalStateException("Native worm segment cannot be restored");
            part.moveTo(point.x, point.y, point.z, part.getYRot(), part.getXRot());
            part.getPersistentData().putLong(CompanionData.ENTITY_EPOCH, epoch);
            ADMITTING_PARTS.add(part);
            try { if (!level.hasChunkAt(part.blockPosition()) || !level.addFreshEntity(part)) throw new IllegalStateException("Native worm segment restoration denied"); }
            finally { ADMITTING_PARTS.remove(part); }
        }
    }
    public static java.util.List<Entity> persistenceEntities(Entity head) {
        java.util.List<Entity> entities = new java.util.ArrayList<>(); entities.add(head);
        if (id(head).equals("alexscaves:gum_worm") && head.level() instanceof ServerLevel level) {
            for (Tag row : head.getPersistentData().getList(BODY, Tag.TAG_COMPOUND)) {
                CompoundTag tag = (CompoundTag) row; Entity part = level.getEntity(tag.getUUID("UUID"));
                if (part == null || !head.getUUID().equals(invoke(part, "getHeadUUID"))) throw new IllegalStateException("Restored native worm segment is missing");
                entities.add(part);
            }
        }
        return java.util.List.copyOf(entities);
    }
    public static void rollback(Entity head) {
        if (!id(head).equals("alexscaves:gum_worm") || !(head.level() instanceof ServerLevel level)) return;
        for (Tag row : head.getPersistentData().getList(BODY, Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) row; Entity part = level.getEntity(tag.getUUID("UUID"));
            if (part != null && id(part).equals("alexscaves:gum_worm_segment") && head.getUUID().equals(invoke(part, "getHeadUUID"))) part.discard();
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void segmentJoined(EntityJoinLevelEvent event) {
        Entity segment = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !id(segment).equals("alexscaves:gum_worm_segment")) return;
        Object nativeHead = invoke(segment, "getHeadUUID"); if (!(nativeHead instanceof UUID head)) return;
        CompanionData data = CompanionData.get(level); CompanionData.Pet pet = data.pet(head);
        if (pet != null && pet.restoring && pet.pending != null && !ADMITTING_PARTS.contains(segment)) { event.setCanceled(true); return; }
        long epoch = CompanionAdapters.epoch(head, level);
        if (epoch <= 0 || segment.getPersistentData().getLong(CompanionData.ENTITY_EPOCH) == epoch) return;
        if (pet != null && pet.pending != null && !pet.restoring) {
            CompoundTag extra = pet.pending.getCompound("ForgeData"); ListTag body = extra.getList(BODY, Tag.TAG_COMPOUND);
            boolean known = body.stream().map(CompoundTag.class::cast).anyMatch(tag -> tag.hasUUID("UUID") && tag.getUUID("UUID").equals(segment.getUUID()));
            if (!known) { CompoundTag row = segment.saveWithoutId(new CompoundTag()); row.putString("id", id(segment)); body.add(row); extra.put(BODY, body); pet.pending.put("ForgeData", extra); data.flush(level); }
        }
        // Native head identity is stable, so a pre-death child must not silently reattach to a recovered head.
        event.setCanceled(true);
    }
    private static Object invoke(Object instance, String name) {
        try { return instance.getClass().getMethod(name).invoke(instance); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed Alex companion API changed: " + name, failure); }
    }
    private static void invoke(Object instance, String name, Class<?> argumentType, Object argument) {
        try { instance.getClass().getMethod(name, argumentType).invoke(instance, argument); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Installed Alex companion API changed: " + name, failure); }
    }
    public static Map<String, String> implementations() {
        Map<String, String> result = new LinkedHashMap<>(); PROFILES.forEach((id, profile) -> result.put(id, profile.implementation)); return Map.copyOf(result);
    }
}
