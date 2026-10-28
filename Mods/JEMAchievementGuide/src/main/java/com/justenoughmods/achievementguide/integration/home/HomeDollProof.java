package com.justenoughmods.achievementguide.integration.home;

import com.github.ysbbbbbb.kaleidoscopedoll.block.entity.DollGiftBoxBlockEntiy;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.item.CustomDollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem;
import com.github.ysbbbbbb.kaleidoscopedoll.item.TweaksToolItem;
import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.joml.Vector3f;

public final class HomeDollProof {
    private static final class Opening {
        final ServerPlayer actor;
        final BlockPos pos;
        final DollGiftBoxBlockEntiy box;
        final ItemStack expected;
        ItemStack nativeOutput;
        final List<ItemEntity> drops = new ArrayList<>();
        Opening(ServerPlayer actor, BlockPos pos, DollGiftBoxBlockEntiy box) {
            this.actor = actor; this.pos = pos.immutable(); this.box = box;
            expected = box == null ? ItemStack.EMPTY : box.getDollItemStack();
        }
    }
    private record Transform(ServerPlayer player, DollEntity doll, List<Vector3f> values, float yaw, float pitch) {}
    private static final ThreadLocal<Deque<Opening>> OPENINGS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Transform>> TRANSFORMS = ThreadLocal.withInitial(ArrayDeque::new);

    public static void begin(ServerPlayer player, BlockPos pos) {
        if (OPENINGS.get().size() >= 16) OPENINGS.get().clear();
        DollGiftBoxBlockEntiy box = HomeInteractionProof.actor(player) != null
                && player.level().getBlockEntity(pos) instanceof DollGiftBoxBlockEntiy nativeBox ? nativeBox : null;
        OPENINGS.get().push(new Opening(player, pos, box));
    }
    public static void drops(LootParams.Builder parameters, List<ItemStack> nativeDrops) {
        Opening opening = OPENINGS.get().peek();
        if (opening == null || opening.box == null || opening.expected.isEmpty() || nativeDrops.isEmpty()
                || parameters.getOptionalParameter(LootContextParams.THIS_ENTITY) != opening.actor
                || parameters.getOptionalParameter(LootContextParams.BLOCK_ENTITY) != opening.box) return;
        // The native override appends the actual stored doll after the ordinary loot table.
        ItemStack last = nativeDrops.get(nativeDrops.size() - 1);
        if ((last.getItem() instanceof DollItem || last.getItem() instanceof DollEntityItem || last.getItem() instanceof CustomDollItem)
                && ItemStack.isSameItemSameTags(last, opening.expected) && last.getCount() == opening.expected.getCount()) opening.nativeOutput = last;
    }
    public static void inserted(Entity entity, boolean accepted) {
        Opening opening = OPENINGS.get().peek();
        if (accepted && opening != null && entity instanceof ItemEntity drop && drop.level() == opening.actor.level()
                && drop.getItem() == opening.nativeOutput && opening.drops.size() < 4) opening.drops.add(drop);
    }
    public static void finish(ServerPlayer player, BlockPos pos, boolean accepted) {
        Opening opening = OPENINGS.get().poll();
        if (!accepted || opening == null || opening.actor != player || !opening.pos.equals(pos) || opening.box == null
                || HomeInteractionProof.actor(player) == null || player.level().getBlockEntity(pos) == opening.box
                || player.level().getBlockState(pos).getBlock() == opening.box.getBlockState().getBlock()) return;
        for (ItemEntity drop : opening.drops) if (!drop.isRemoved() && drop.getItem() == opening.nativeOutput)
            FoodDroppedProof.update(drop, "v3_home_toy_story", drop.getItem().getCount(), player.getUUID());
    }

    public static void beforeTransform(ServerPlayer player, int id) {
        if (TRANSFORMS.get().size() >= 16) TRANSFORMS.get().clear();
        DollEntity doll = player != null && player.level().getEntity(id) instanceof DollEntity nativeDoll ? nativeDoll : null;
        TRANSFORMS.get().push(new Transform(player, doll, doll == null ? List.of() : values(doll),
                doll == null ? 0 : doll.getYRot(), doll == null ? 0 : doll.getXRot()));
    }
    public static void afterTransform(ServerPlayer player, int id) {
        Transform before = TRANSFORMS.get().poll();
        if (before == null || before.player != player || before.doll == null || HomeInteractionProof.actor(player) == null
                || before.doll.isRemoved() || player.level().getEntity(id) != before.doll
                || !(player.getMainHandItem().getItem() instanceof TweaksToolItem || player.getOffhandItem().getItem() instanceof TweaksToolItem)) return;
        List<Vector3f> after = values(before.doll);
        if (after.stream().allMatch(value -> Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z))
                && Float.isFinite(before.doll.getYRot()) && Float.isFinite(before.doll.getXRot())
                && (!after.equals(before.values) || before.yaw != before.doll.getYRot() || before.pitch != before.doll.getXRot()))
            HomeInteractionProof.emit(player, "v3_home_toy_story_tweak");
    }
    private static List<Vector3f> values(DollEntity doll) {
        return List.of(new Vector3f(doll.getDisplayScale()), new Vector3f(doll.getDisplayTranslation()),
                new Vector3f(doll.getItemScale()), new Vector3f(doll.getItemTranslation()), new Vector3f(doll.getItemRotation()));
    }
    private HomeDollProof() {}
}
