package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One read-only snapshot of a real native Workshop button operation. */
public final class SkarrierRepairProof {
    private static final ThreadLocal<ArrayDeque<Run>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    private record Run(ServerPlayer actor, AbstractContainerMenu menu, Slot material, Slot target,
                       ItemStack beforeMaterial, ItemStack beforeTarget) {}
    private SkarrierRepairProof() {}

    public static void begin(ServerPlayer actor) {
        AbstractContainerMenu menu = actor.containerMenu;
        Slot material = null, target = null;
        if (menu instanceof Supplier<?> supplier && supplier.get() instanceof Map<?, ?> slots
                && slots.get(0) instanceof Slot source && slots.get(1) instanceof Slot gear) {
            material = source;
            target = gear;
        }
        CALLS.get().push(new Run(actor, menu, material, target,
                material == null ? ItemStack.EMPTY : material.getItem().copy(),
                target == null ? ItemStack.EMPTY : target.getItem().copy()));
    }

    public static void end(ServerPlayer actor) {
        var calls = CALLS.get();
        Run run = calls.isEmpty() ? null : calls.pop();
        if (calls.isEmpty()) CALLS.remove();
        if (run == null || run.actor != actor || run.menu != actor.containerMenu || run.material == null || run.target == null) return;
        ItemStack material = run.material.getItem(), target = run.target.getItem();
        boolean consumed = (material.isEmpty() || ItemStack.isSameItemSameTags(material, run.beforeMaterial))
                && run.beforeMaterial.getCount() - material.getCount() == 1;
        String kind = switch (BuiltInRegistries.ITEM.getKey(run.beforeMaterial.getItem()).toString()) {
            case "skarrier_mobs:venom_sack" -> "VenomSack";
            case "skarrier_mobs:serrated_shred" -> "SerratedShred";
            case "skarrier_mobs:bowlder_dust" -> "BowlderDust";
            case "skarrier_mobs:wretched_fang" -> "WretchedFang";
            case "skarrier_mobs:magmadermis" -> "Magmadermis";
            case "skarrier_mobs:warden_vein" -> "WardenVein";
            case "skarrier_mobs:wrought_scale" -> "WroughtScale";
            case "skarrier_mobs:ammonite_shell" -> "AmmoniteShell";
            case "skarrier_mobs:semi_living_flesh" -> "SLFlesh";
            default -> "";
        };
        if (consumed && !kind.isEmpty() && !target.isEmpty() && target.is(run.beforeTarget.getItem())
                && target.getCount() == run.beforeTarget.getCount() && target.getTag() != null
                && kind.equals(target.getTag().getString("CoatingApplied")) && (kind.equals("SLFlesh") || target.getTag().getDouble(kind + "Coating") > 0)
                && (run.beforeTarget.getTag() == null || !kind.equals(run.beforeTarget.getTag().getString("CoatingApplied"))
                || target.getTag().getDouble(kind + "Coating") > run.beforeTarget.getTag().getDouble(kind + "Coating"))) {
            SkarrierCoatingProof.applied(actor, target, kind);
        }
        if (!consumed
                || !"skarrier_mobs:dragger_silk".equals(BuiltInRegistries.ITEM.getKey(run.beforeMaterial.getItem()).toString())
                || run.beforeTarget.isEmpty() || !run.beforeTarget.isDamageableItem() || run.beforeTarget.getDamageValue() <= 0) return;
        if (target.isEmpty()
                || !target.is(run.beforeTarget.getItem()) || target.getCount() != run.beforeTarget.getCount()
                || target.getDamageValue() >= run.beforeTarget.getDamageValue()) return;
        ItemStack expected = run.beforeTarget.copy();
        expected.setDamageValue(target.getDamageValue());
        if (ItemStack.isSameItemSameTags(target, expected)) QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_coat_repair");
    }
}
