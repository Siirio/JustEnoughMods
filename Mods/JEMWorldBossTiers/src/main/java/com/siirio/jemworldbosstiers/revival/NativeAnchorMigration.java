package com.siirio.jemworldbosstiers.revival;

import com.mojang.datafixers.util.Either;
import com.siirio.jemworldbosstiers.balance.BossProfile;
import com.siirio.jemworldbosstiers.mixin.SinglePoolElementAccessor;
import com.siirio.jemworldbosstiers.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class NativeAnchorMigration {
    private static final ResourceLocation ANCIENT_REMNANT = id("cataclysm:ancient_remnant");
    private static final ResourceLocation IGNIS = id("cataclysm:ignis");
    private static final ResourceLocation LEVIATHAN = id("cataclysm:the_leviathan");
    private static final ResourceLocation MALEDICTUS = id("cataclysm:maledictus");
    private static final ResourceLocation INFERNAL_DRAGON = id("block_factorys_bosses:infernal_dragon");
    private static final ResourceLocation KRAKEN = id("block_factorys_bosses:kraken");
    private static final ResourceLocation LUXTRUCTOSAURUS = id("alexscaves:luxtructosaurus");
    private static final ResourceLocation OBLITERATOR = id("legendary_monsters:the_obliterator");
    private static final ResourceLocation ALTAR_OF_FIRE = id("cataclysm:altar_of_fire");
    private static final ResourceLocation ALTAR_OF_ABYSS = id("cataclysm:altar_of_abyss");
    private static final ResourceLocation CURSED_TOMBSTONE = id("cataclysm:cursed_tombstone");
    private static final ResourceLocation CATACLYSM_RESPAWNER = id("cataclysm:boss_respawner");
    private static final ResourceLocation BOSS_SPAWNER = id("block_factorys_bosses:boss_spawner");
    private static final ResourceLocation KRAKEN_SPAWNER = id("block_factorys_bosses:kraken_spawner");
    private static final ResourceLocation VOLCANIC_CORE = id("alexscaves:volcanic_core");
    private static final ResourceLocation TELEPORT_MACHINE = id("legendary_monsters:teleport_machine");
    private static final TagKey<Block> VOLCANO_BLOCKS = TagKey.create(Registries.BLOCK, id("alexscaves:volcano_blocks"));
    private static final String ENTITY_TYPE_TAG = "EntityType";
    private static final String DATA_MARKER_TAG = "metadata";
    private static final String REMNANT_MARKER = "remnant";
    private static final String FACING_PROPERTY = "facing";
    private static final String CUSTOM_TEMPLATE_METHOD = "getTemplate";
    private static final int ALTAR_SPAWN_HEIGHT = 3;
    private static final int TOMBSTONE_SPAWN_HEIGHT = 2;
    private static final int VOLCANO_SEARCH_RADIUS = 5;
    private static final int VOLCANO_SPAWN_HEIGHT = 2;
    private static final int KRAKEN_LONGITUDINAL_OFFSET = 24;
    private static final int KRAKEN_LATERAL_OFFSET = 4;
    private static final int KRAKEN_VERTICAL_OFFSET = -4;

    public static Optional<BlockPos> fromStructure(MinecraftServer server, ArenaRecord arena, BossProfile profile, StructureStart start) {
        Set<BlockPos> entityAnchors = new LinkedHashSet<>();
        Set<BlockPos> signalAnchors = new LinkedHashSet<>();
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof TemplateStructurePiece templatePiece) {
                collect(templatePiece.template(), templatePiece.templatePosition(), templatePiece.placeSettings(), profile, entityAnchors, signalAnchors);
            } else if (piece instanceof PoolElementStructurePiece poolPiece) {
                StructureTemplate template = poolTemplate(server, poolPiece.getElement());
                if (template != null) {
                    StructurePlaceSettings settings = new StructurePlaceSettings().setMirror(Mirror.NONE).setRotation(poolPiece.getRotation());
                    collect(template, poolPiece.getPosition(), settings, profile, entityAnchors, signalAnchors);
                }
            }
        }
        Optional<BlockPos> entityAnchor = uniqueInside(entityAnchors, arena);
        return entityAnchor.isPresent() ? entityAnchor : uniqueInside(signalAnchors, arena);
    }

    public static Optional<BlockPos> fromPersistentSignal(ServerLevel level, BlockPos position, ArenaRecord arena, BossProfile profile) {
        if (!matches(profile, LUXTRUCTOSAURUS) || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(position).getBlock()).equals(VOLCANIC_CORE)) {
            return Optional.empty();
        }
        BlockPos highest = position;
        for (int x = -VOLCANO_SEARCH_RADIUS; x <= VOLCANO_SEARCH_RADIUS; x++) {
            for (int z = -VOLCANO_SEARCH_RADIUS; z <= VOLCANO_SEARCH_RADIUS; z++) {
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(position.getX() + x, position.getY(), position.getZ() + z);
                while (cursor.getY() < level.getMaxBuildHeight() && level.getBlockState(cursor).is(VOLCANO_BLOCKS)) cursor.move(Direction.UP);
                cursor.move(Direction.DOWN);
                if (cursor.getY() > highest.getY()) highest = cursor.immutable();
            }
        }
        BlockPos anchor = highest.above(VOLCANO_SPAWN_HEIGHT);
        return arena.bounds().isInside(anchor) ? Optional.of(anchor) : Optional.empty();
    }

    private static void collect(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings, BossProfile profile,
                                Set<BlockPos> entityAnchors, Set<BlockPos> signalAnchors) {
        for (StructureTemplate.StructureEntityInfo entity : ((StructureTemplateAccessor) template).jemWorldBossTiers$entityInfoList()) {
            if (!matches(profile, ResourceLocation.tryParse(entity.nbt.getString("id")))) continue;
            Vec3 transformed = StructureTemplate.transform(entity.pos,
                    settings.getMirror(), settings.getRotation(), settings.getRotationPivot()).add(Vec3.atLowerCornerOf(origin));
            entityAnchors.add(BlockPos.containing(transformed));
        }
        collectSignal(template, origin, settings, profile, ALTAR_OF_FIRE, signalAnchors);
        collectSignal(template, origin, settings, profile, ALTAR_OF_ABYSS, signalAnchors);
        collectSignal(template, origin, settings, profile, CURSED_TOMBSTONE, signalAnchors);
        collectSignal(template, origin, settings, profile, CATACLYSM_RESPAWNER, signalAnchors);
        collectSignal(template, origin, settings, profile, BOSS_SPAWNER, signalAnchors);
        collectSignal(template, origin, settings, profile, KRAKEN_SPAWNER, signalAnchors);
        collectSignal(template, origin, settings, profile, TELEPORT_MACHINE, signalAnchors);
        Block structureBlock = Blocks.STRUCTURE_BLOCK;
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, settings, structureBlock, true)) {
            if (matches(profile, ANCIENT_REMNANT) && info.nbt() != null && REMNANT_MARKER.equals(info.nbt().getString(DATA_MARKER_TAG))) {
                signalAnchors.add(info.pos().immutable());
            }
        }
    }

    private static void collectSignal(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings, BossProfile profile,
                                      ResourceLocation blockId, Set<BlockPos> anchors) {
        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) return;
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, settings, block, true)) {
            BlockPos anchor = signalAnchor(profile, blockId, info);
            if (anchor != null) anchors.add(anchor.immutable());
        }
    }

    private static BlockPos signalAnchor(BossProfile profile, ResourceLocation blockId, StructureTemplate.StructureBlockInfo info) {
        if (blockId.equals(ALTAR_OF_FIRE) && matches(profile, IGNIS)) return info.pos().above(ALTAR_SPAWN_HEIGHT);
        if (blockId.equals(ALTAR_OF_ABYSS) && matches(profile, LEVIATHAN)) return info.pos().above(ALTAR_SPAWN_HEIGHT);
        if (blockId.equals(CURSED_TOMBSTONE) && matches(profile, MALEDICTUS)) return info.pos().above(TOMBSTONE_SPAWN_HEIGHT);
        if (blockId.equals(CATACLYSM_RESPAWNER) && info.nbt() != null
                && matches(profile, ResourceLocation.tryParse(info.nbt().getString(ENTITY_TYPE_TAG)))) return info.pos();
        if (blockId.equals(BOSS_SPAWNER) && matches(profile, INFERNAL_DRAGON)) return info.pos();
        if (blockId.equals(KRAKEN_SPAWNER) && matches(profile, KRAKEN)) return krakenAnchor(info);
        if (blockId.equals(TELEPORT_MACHINE) && matches(profile, OBLITERATOR)) return info.pos().above();
        return null;
    }

    private static BlockPos krakenAnchor(StructureTemplate.StructureBlockInfo info) {
        Direction facing = info.state().getValues().entrySet().stream()
                .filter(entry -> FACING_PROPERTY.equals(entry.getKey().getName()) && entry.getValue() instanceof Direction)
                .map(entry -> (Direction) entry.getValue()).findFirst().orElse(null);
        if (facing == null) return null;
        return switch (facing) {
            case EAST -> info.pos().offset(KRAKEN_LONGITUDINAL_OFFSET, KRAKEN_VERTICAL_OFFSET, KRAKEN_LATERAL_OFFSET);
            case SOUTH -> info.pos().offset(-KRAKEN_LATERAL_OFFSET, KRAKEN_VERTICAL_OFFSET, KRAKEN_LONGITUDINAL_OFFSET);
            case WEST -> info.pos().offset(-KRAKEN_LONGITUDINAL_OFFSET, KRAKEN_VERTICAL_OFFSET, -KRAKEN_LATERAL_OFFSET);
            default -> info.pos().offset(-KRAKEN_LONGITUDINAL_OFFSET, KRAKEN_VERTICAL_OFFSET, KRAKEN_LATERAL_OFFSET);
        };
    }

    private static StructureTemplate poolTemplate(MinecraftServer server, StructurePoolElement element) {
        StructureTemplateManager manager = server.getStructureManager();
        if (element instanceof SinglePoolElement single) {
            Either<ResourceLocation, StructureTemplate> value = ((SinglePoolElementAccessor) single).jemWorldBossTiers$template();
            return value.map(manager::getOrCreate, template -> template);
        }
        try {
            Method method = element.getClass().getMethod(CUSTOM_TEMPLATE_METHOD, StructureTemplateManager.class);
            Object value = method.invoke(element, manager);
            return value instanceof StructureTemplate template ? template : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Optional<BlockPos> uniqueInside(Set<BlockPos> anchors, ArenaRecord arena) {
        Set<BlockPos> inside = new LinkedHashSet<>();
        anchors.stream().filter(arena.bounds()::isInside).map(BlockPos::immutable).forEach(inside::add);
        return inside.size() == 1 ? Optional.of(inside.iterator().next()) : Optional.empty();
    }

    private static boolean matches(BossProfile profile, ResourceLocation entityType) {
        return entityType != null && profile.entityIds().contains(entityType);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }

    private NativeAnchorMigration() {
    }
}
