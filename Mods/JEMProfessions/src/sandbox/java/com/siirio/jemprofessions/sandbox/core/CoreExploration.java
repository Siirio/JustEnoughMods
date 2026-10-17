package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.progress.ProfessionId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

final class CoreExploration {
    private static final Map<UUID, Camp> CAMPS = new HashMap<>();
    private static final Map<UUID, Hang> HANGS = new HashMap<>();
    private static final UUID HANG_GRAVITY = UUID.fromString("1b8f1120-26d1-4e2b-b83a-fb9d2dd07352");
    private CoreExploration() { }
    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        String mode = input.getString("Mode");
        if (node.equals("portable_camp")) { camp(player, mode); return true; }
        if (mode.equals("cancel")) { release(player.getUUID(), false); return true; }
        if (node.equals("ledge_catch") && mode.equals("hold")) {
            Hang hang = HANGS.get(player.getUUID()); if (hang != null) hang.held = player.tickCount; return true;
        }
        Vec3 arrival = ledge(player); if (arrival == null) {
            SandboxCoreEffects.tell(player, "Выбери близкий уступ до двух блоков с безопасным местом наверху.", "Aim at a nearby ledge up to two blocks high with safe standing space."); return true;
        }
        if (node.equals("wall_climb")) {
            SandboxCoreEffects.teleported(player); player.teleportTo(arrival.x, arrival.y, arrival.z); return true;
        }
        if (HANGS.containsKey(player.getUUID())) return true;
        int string = CoreActions.slot(player, stack -> stack.is(Items.STRING));
        if (string < 0) { SandboxCoreEffects.tell(player, "Нужна одна нить.", "One string is required."); return true; }
        var hit = CoreActions.ray(player, 3.5); Direction face = hit.getDirection();
        Vec3 hanging = arrival.add(face.getStepX() * .85, -1.3, face.getStepZ() * .85);
        if (!clearBody(player, hanging)) { SandboxCoreEffects.tell(player, "У уступа нет места, чтобы повиснуть.", "There is no room to hang below this ledge."); return true; }
        var gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity == null) { SandboxCoreEffects.tell(player, "У игрока отсутствует атрибут гравитации Forge.", "The player's Forge gravity attribute is missing."); return true; }
        player.getInventory().getItem(string).shrink(1); player.containerMenu.broadcastChanges();
        // Native AttributeInstance.save serializes only permanent modifiers. This synchronized
        // transient modifier cannot persist through a crash and never changes Entity.NoGravity.
        gravity.removeModifier(HANG_GRAVITY);
        gravity.addTransientModifier(new AttributeModifier(HANG_GRAVITY, "JEM ledge hold", -1D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        HANGS.put(player.getUUID(), new Hang(player, SandboxCoreEffects.dimension(player), hanging, arrival, player.tickCount));
        SandboxCoreEffects.teleported(player); player.teleportTo(hanging.x, hanging.y, hanging.z);
        SandboxCoreEffects.tell(player, "Удерживай действие две секунды, чтобы подтянуться.", "Hold the action for two seconds to pull up."); return true;
    }
    private static boolean clearBody(ServerPlayer player, Vec3 point) {
        var box = player.getBoundingBox().move(point.subtract(player.position())).inflate(.02);
        if (!player.serverLevel().getWorldBorder().isWithinBounds(box) || !player.serverLevel().noCollision(player, box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (!CoreActions.permitted(player, pos, player.getMainHandItem()) || !player.serverLevel().getFluidState(pos).isEmpty()) return false;
        return true;
    }
    private static Vec3 ledge(ServerPlayer player) {
        if (!player.isAlive() || player.isPassenger() || player.isFallFlying() || player.fallDistance > 2) return null;
        // A ledge tool cannot rescue a player falling through open air or the void.
        boolean floor = false;
        for (int y = 1; y <= 3; y++) { BlockPos pos = player.blockPosition().below(y); if (!player.serverLevel().hasChunkAt(pos)) return null; if (!player.serverLevel().getBlockState(pos).getCollisionShape(player.serverLevel(), pos).isEmpty()) floor = true; }
        if (!floor) return null;
        var hit = CoreActions.ray(player, 3.5); if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getAxis() == Direction.Axis.Y) return null;
        for (int rise = 1; rise <= 2; rise++) {
            Vec3 arrival = new Vec3(hit.getBlockPos().getX() + .5, player.blockPosition().getY() + rise, hit.getBlockPos().getZ() + .5);
            if (arrival.y - player.getY() > 2.01 || arrival.y <= player.getY() || !CoreActions.safeArrival(player, arrival)) continue;
            return arrival;
        }
        return null;
    }
    static void tick(ServerPlayer player) {
        Hang hang = HANGS.get(player.getUUID());
        if (hang != null) {
            if (!player.isAlive() || player.isSpectator() || !SandboxCoreEffects.has(player, ProfessionId.EXPLORER, "ledge_catch")
                    || !hang.dimension.equals(SandboxCoreEffects.dimension(player)) || player.tickCount - hang.held > 12
                    || player.isPassenger() || player.position().distanceToSqr(hang.hanging) > 2.25 || !clearBody(player, hang.hanging)) release(player.getUUID(), false);
            else if (player.tickCount - hang.started >= 40) release(player.getUUID(), CoreActions.safeArrival(player, hang.arrival));
            else { player.setDeltaMovement(Vec3.ZERO); }
        }
        Camp camp = CAMPS.get(player.getUUID());
        if (camp != null && (player.tickCount > camp.until || !player.isAlive() || !SandboxCoreEffects.has(player, ProfessionId.EXPLORER, "portable_camp") || !camp.dimension.equals(SandboxCoreEffects.dimension(player)))) CAMPS.remove(player.getUUID());
    }
    private static void release(UUID id, boolean complete) {
        Hang hang = HANGS.remove(id); if (hang == null) return;
        var gravity = hang.player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (gravity != null) gravity.removeModifier(HANG_GRAVITY);
        if (complete && hang.player.isAlive()) { SandboxCoreEffects.teleported(hang.player); hang.player.teleportTo(hang.arrival.x, hang.arrival.y, hang.arrival.z); }
    }
    private static void camp(ServerPlayer player, String mode) {
        if (mode.equals("cancel")) { CAMPS.remove(player.getUUID()); SandboxCoreEffects.tell(player, "Установка лагеря отменена.", "Camp placement cancelled."); return; }
        if (mode.equals("confirm")) {
            Camp camp = CAMPS.remove(player.getUUID());
            if (camp == null || camp.until < player.tickCount || !camp.dimension.equals(SandboxCoreEffects.dimension(player)) || camp.pos.distToCenterSqr(player.position()) > 64 || camp.facing != player.getDirection()) {
                SandboxCoreEffects.tell(player, "Сначала выбери новое место лагеря.", "Preview a new camp location first."); return;
            }
            int fire = CoreActions.slot(player, stack -> stack.is(Items.CAMPFIRE) && CoreActions.safeBlock(stack));
            int bed = CoreActions.slot(player, stack -> stack.is(ItemTags.BEDS) && CoreActions.safeBlock(stack));
            if (fire < 0 || bed < 0) { SandboxCoreEffects.tell(player, "Нужны обычный костёр и кровать в сумке.", "A campfire and bed are required in your inventory."); return; }
            // Native bed placement validates the head, support and dimension rules; the transaction rolls both items back on denial.
            List<CoreActions.Placement> plan = List.of(new CoreActions.Placement(camp.pos, Direction.UP, fire), new CoreActions.Placement(camp.bed, Direction.UP, bed));
            if (CoreActions.place(player, plan)) SandboxCoreEffects.tell(player, "Лагерь установлен.", "Camp placed.");
            else SandboxCoreEffects.tell(player, "Установка лагеря заблокирована. Материалы сохранены.", "Camp placement is blocked. Materials were preserved.");
            return;
        }
        var hit = CoreActions.ray(player, 6); if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) { SandboxCoreEffects.tell(player, "Выбери ровную свободную землю.", "Aim at clear level ground."); return; }
        BlockPos fire = hit.getBlockPos().above(), bed = fire.relative(player.getDirection().getClockWise(), 2);
        if (!CoreActions.permitted(player, fire, player.getMainHandItem()) || !CoreActions.permitted(player, bed, player.getMainHandItem())) return;
        CAMPS.put(player.getUUID(), new Camp(SandboxCoreEffects.dimension(player), fire, bed, player.getDirection(), player.tickCount + 200));
        for (BlockPos pos : List.of(fire, bed, bed.relative(player.getDirection()))) player.serverLevel().sendParticles(player, ParticleTypes.END_ROD, false, pos.getX() + .5, pos.getY() + .15, pos.getZ() + .5, 8, .3, .05, .3, .005);
        SandboxCoreEffects.tell(player, "Место лагеря отмечено на 10 секунд. Подтверди установку или отмени.", "Camp preview lasts ten seconds. Confirm or cancel placement.");
    }
    static void logout(UUID id) { CAMPS.remove(id); release(id, false); }
    static void hurt(ServerPlayer player) { release(player.getUUID(), false); }
    static void clear() { CAMPS.clear(); for (UUID id : List.copyOf(HANGS.keySet())) release(id, false); }
    private record Camp(String dimension, BlockPos pos, BlockPos bed, Direction facing, int until) { }
    private static final class Hang {
        final ServerPlayer player; final String dimension; final Vec3 hanging, arrival; final int started; int held;
        Hang(ServerPlayer player, String dimension, Vec3 hanging, Vec3 arrival, int started) { this.player = player; this.dimension = dimension; this.hanging = hanging; this.arrival = arrival; this.started = started; this.held = started; }
    }
}
