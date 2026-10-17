package com.siirio.jemprofessions.sandbox.core;

import com.siirio.jemprofessions.progress.ProfessionId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

final class CoreBuilding {
    private static final Set<UUID> SUPPLY = new HashSet<>();
    private CoreBuilding() { }
    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        if (node.equals("architect_hotbar")) { SandboxCoreEffects.tell(player, "Доступны 13 ячеек быстрого доступа TinyInv.", "TinyInv provides 13 quick slots."); return true; }
        if (node.equals("architect_supply")) { refill(player, Math.max(1, Math.min(126, input.contains("Value") ? input.getInt("Value") : 126))); return true; }
        int bonus = SandboxCoreEffects.has(player, ProfessionId.ARCHITECT, "long_arms_3") ? 7 : SandboxCoreEffects.has(player, ProfessionId.ARCHITECT, "long_arms_2") ? 5 : SandboxCoreEffects.has(player, ProfessionId.ARCHITECT, "long_arms_1") ? 3 : 0;
        var hit = CoreActions.ray(player, 4.5 + bonus);
        if (hit.getType() != HitResult.Type.BLOCK) { SandboxCoreEffects.tell(player, "Выбери грань блока.", "Aim at a block face."); return true; }
        int slot = player.getInventory().selected;
        if (!CoreActions.safeBlock(player.getMainHandItem())) { SandboxCoreEffects.tell(player, "Возьми строительный блок без содержимого в руку.", "Hold a building block without stored contents."); return true; }
        int size = switch (node) { case "phantom_3" -> 3; case "phantom_5" -> 5; case "phantom_7" -> 7; default -> 1; };
        var data = SandboxCoreData.get(player.getServer()); var settings = data.settings(player.getUUID());
        BlockPos center = hit.getBlockPos().relative(hit.getDirection()); Direction face = hit.getDirection();
        if (node.equals("architect_command")) {
            if (input.getString("Mode").equals("select") || settings.size < 1 || !settings.planeDimension.equals(SandboxCoreEffects.dimension(player))) {
                int chosen = input.getInt("Value"); if (chosen != 1 && chosen != 3 && chosen != 5 && chosen != 7) chosen = 3;
                settings.planeDimension = SandboxCoreEffects.dimension(player); settings.plane = hit.getBlockPos().asLong(); settings.face = face.get3DDataValue(); settings.size = chosen; data.setDirty();
                SandboxCoreEffects.tell(player, "Выбрана плоскость " + chosen + "×" + chosen + ". Повтори действие, чтобы построить следующую из своих блоков.",
                        "Selected plane " + chosen + "×" + chosen + ". Activate again to build the adjacent plane from your blocks."); return true;
            }
            size = settings.size; face = Direction.from3DDataValue(settings.face);
            BlockPos previous = BlockPos.of(settings.plane);
            if (hit.getDirection() != face || hit.getBlockPos().distSqr(previous) > size * size || previous.distToCenterSqr(player.position()) > 196) {
                SandboxCoreEffects.tell(player, "Выбери внешнюю грань последней плоскости.", "Aim at the outside face of the last plane."); return true;
            }
            center = previous.relative(face);
        }
        List<CoreActions.Placement> initial = CoreActions.plane(player, center, face, size, slot);
        List<CoreActions.Placement> plan = allocate(player, initial, player.getMainHandItem());
        if (plan.size() != initial.size()) { SandboxCoreEffects.tell(player, "Не хватает одинаковых блоков для всей плоскости.", "Not enough matching blocks for the entire plane."); return true; }
        if (CoreActions.place(player, plan)) {
            settings.planeDimension = SandboxCoreEffects.dimension(player); settings.plane = center.asLong(); settings.face = face.get3DDataValue(); settings.size = size; data.setDirty();
            SandboxCoreEffects.tell(player, "Установлено блоков: " + plan.size(), "Placed blocks: " + plan.size());
        } else SandboxCoreEffects.tell(player, "Плоскость заблокирована или установка запрещена. Материалы сохранены.", "The plane is obstructed or placement is denied. Materials were preserved.");
        return true;
    }
    private static List<CoreActions.Placement> allocate(ServerPlayer player, List<CoreActions.Placement> initial, ItemStack chosen) {
        int[] available = new int[player.getInventory().items.size()];
        for (int i = 0; i < available.length; i++) if (ItemStack.isSameItemSameTags(chosen, player.getInventory().getItem(i))) available[i] = player.getInventory().getItem(i).getCount();
        List<CoreActions.Placement> plan = new ArrayList<>(); int selected = player.getInventory().selected;
        for (CoreActions.Placement part : initial) {
            int slot = available[selected] > 0 ? selected : -1;
            if (slot < 0) for (int i = 0; i < available.length; i++) if (available[i] > 0) { slot = i; break; }
            if (slot < 0) break; available[slot]--; plan.add(new CoreActions.Placement(part.pos(), part.face(), slot));
        }
        return plan;
    }
    static void rememberSupply(ServerPlayer player, BlockState state) {
        var data = SandboxCoreData.get(player.getServer());
        data.settings(player.getUUID()).supply = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem()).toString(); data.setDirty();
        if (SandboxCoreEffects.has(player, ProfessionId.ARCHITECT, "architect_supply")) SUPPLY.add(player.getUUID());
    }
    static void tick(ServerPlayer player) {
        if (SUPPLY.remove(player.getUUID()) && player.getMainHandItem().isEmpty() && SandboxCoreEffects.has(player, ProfessionId.ARCHITECT, "architect_supply")) refill(player, 126);
    }
    private static void refill(ServerPlayer player, int maximum) {
        if (!player.getMainHandItem().isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(SandboxCoreData.get(player.getServer()).settings(player.getUUID()).supply);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return;
        var item = BuiltInRegistries.ITEM.get(id); ItemStack hand = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (i == player.getInventory().selected) continue;
            ItemStack stack = player.getInventory().getItem(i); if (!stack.is(item) || !CoreActions.safeBlock(stack)) continue;
            if (hand.isEmpty()) hand = stack.split(Math.min(maximum, stack.getMaxStackSize()));
            else if (ItemStack.isSameItemSameTags(hand, stack)) {
                int count = Math.min(stack.getCount(), Math.min(maximum - hand.getCount(), hand.getMaxStackSize() - hand.getCount()));
                hand.grow(count); stack.shrink(count);
            }
            if (!hand.isEmpty() && hand.getCount() >= Math.min(maximum, hand.getMaxStackSize())) break;
        }
        if (!hand.isEmpty()) { player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, hand); player.getInventory().setChanged(); player.containerMenu.broadcastChanges(); }
    }
    static void logout(UUID id) { SUPPLY.remove(id); }
    static void clear() { SUPPLY.clear(); }
}
