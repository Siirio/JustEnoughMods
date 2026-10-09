package com.siirio.jemcompat.feature.create;

import com.siirio.jemcompat.advancement.AdvancementAwards;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DrillWear {
    public static final String WEAR_KEY = "JemDrillWear";
    public static final String REPAIRS_KEY = "JemDrillRepairs";
    public static final int MAX_REPAIRS = 3;
    private static final String REPLACEMENT_POS_KEY = "JemDrillReplacementPos";
    private static final int MAX_WEAR = 1000;
    private static final int DULL_WEAR = 500;
    private static final int NORMAL_WEAR = 1;
    private static final int OBSIDIAN_WEAR = 500;
    private static final float DULL_SPEED_MULTIPLIER = 0.5F;
    private static final ResourceLocation DRILL_WORK = advancement("drill_work");
    private static final ResourceLocation DRILL_REPLACED = advancement("drill_replaced");
    private static final ResourceLocation MECHANICAL_DRILL = new ResourceLocation("create", "mechanical_drill");

    private DrillWear() {
    }

    public static boolean isDrill(MovementContext context) {
        return MECHANICAL_DRILL.equals(BuiltInRegistries.BLOCK.getKey(context.state.getBlock()));
    }

    public static void recordBreak(MovementContext context, BlockState brokenState) {
        if (!isDrill(context) || context.world.isClientSide) {
            return;
        }
        CompoundTag data = MovingDrillState.data(context);
        boolean replaced = context.contraption.getActors().stream()
                .map(actor -> actor.getRight().blockEntityData)
                .filter(Objects::nonNull)
                .anyMatch(tag -> tag.contains(REPLACEMENT_POS_KEY) && tag.getLong(REPLACEMENT_POS_KEY) == context.localPos.asLong());
        if (replaced) {
            context.contraption.getActors().stream()
                    .map(actor -> actor.getRight().blockEntityData)
                    .filter(Objects::nonNull)
                    .forEach(tag -> tag.remove(REPLACEMENT_POS_KEY));
            player(context).ifPresent(player -> AdvancementAwards.award(player, DRILL_REPLACED));
        }
        int previousWear = data.getInt(WEAR_KEY);
        int wear = Math.min(MAX_WEAR, previousWear + (brokenState.is(Blocks.OBSIDIAN) ? OBSIDIAN_WEAR : NORMAL_WEAR));
        data.putInt(WEAR_KEY, wear);
        player(context).ifPresent(player -> {
            if (brokenState.is(BlockTags.BASE_STONE_OVERWORLD)) {
                AdvancementAwards.award(player, DRILL_WORK);
            }
        });
        if (wear >= MAX_WEAR) {
            context.contraption.getActors().forEach(actor -> {
                MovementContext other = actor.getRight();
                if (other != context) {
                    MovingDrillState.data(other).putLong(REPLACEMENT_POS_KEY, context.localPos.asLong());
                }
            });
            destroy(context);
        }
    }

    public static float adjustSpeed(MovementContext context, float speed) {
        return isDrill(context) && MovingDrillState.data(context).getInt(WEAR_KEY) >= DULL_WEAR
                ? speed * DULL_SPEED_MULTIPLIER
                : speed;
    }

    public static boolean destroyForBedrock(MovementContext context, BlockState state) {
        if (!isDrill(context) || !state.is(Blocks.BEDROCK) || context.world.isClientSide) {
            return false;
        }
        destroy(context);
        return true;
    }

    private static Optional<ServerPlayer> player(MovementContext context) {
        if (!(context.world instanceof ServerLevel level) || context.contraption.entity == null) {
            return Optional.empty();
        }
        Optional<UUID> controller = context.contraption.entity.getControllingPlayer();
        return controller.map(id -> level.getServer().getPlayerList().getPlayer(id));
    }

    private static void destroy(MovementContext context) {
        if (context.disabled) {
            return;
        }
        context.disabled = true;
        BlockPos localPos = context.localPos;
        context.contraption.entity.setBlock(localPos, new StructureBlockInfo(localPos, Blocks.AIR.defaultBlockState(), null));
        context.world.playSound(null, BlockPos.containing(context.position), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static ResourceLocation advancement(String path) {
        return new ResourceLocation("jem", "engineering/" + path);
    }
}
