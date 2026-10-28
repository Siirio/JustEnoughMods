package com.justenoughmods.achievementguide.integration.caves;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
/** Separate root-level provenance preserves native item tags/capability equality. */
public final class CaveQuestStackProof {
    public static final String SAVE = "jemCaveQuestStackV4";
    private CaveQuestStackProof() {}
    public static CompoundTag get(ItemStack stack) {
        CompoundTag proof = ((CaveQuestStackCarrier)(Object)stack).jem$caveReceipt();
        return proof.hasUUID("actor") && proof.hasUUID("source") && !stack.isEmpty()
                && stack.getCount() == proof.getInt("count") && ItemStack.isSameItemSameTags(stack, ItemStack.of(proof.getCompound("item"))) ? proof : new CompoundTag();
    }
    public static void put(ItemStack stack, UUID actor, UUID source, String kind) {
        if (stack.isEmpty() || actor == null || source == null) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor); proof.putUUID("source", source);
        proof.putString("kind", kind); proof.putInt("count", stack.getCount());
        ItemStack sample=stack.copyWithCount(1); ((CaveQuestStackCarrier)(Object)sample).jem$caveReceipt(new CompoundTag());
        proof.put("item",sample.save(new CompoundTag()));
        ((CaveQuestStackCarrier)(Object)stack).jem$caveReceipt(proof);
    }
}
