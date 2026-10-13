package com.siirio.jemsettlements.simulation;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Durable resource obligation. It is not inventory or market stock until DELIVERED. */
public final class ProductionObligation {
    public enum Status { PENDING_DELIVERY, NEEDS_INPUTS, OUTPUT_FULL, STORAGE_UNLOADED, QUARANTINED, DELIVERED }
    private final String key;
    private final UUID workerId;
    private final String ruleId;
    private final int simulatedDays;
    private final ResourceLocation inputItem;
    private final int inputCount;
    private final ResourceLocation outputItem;
    private final int outputCount;
    private Status status = Status.PENDING_DELIVERY;
    private String detail = "pending";
    private ResourceLocation deliveredDimension;
    private BlockPos deliveredPosition;

    public ProductionObligation(String key, UUID workerId, String ruleId, int simulatedDays,
                                ResourceLocation inputItem, int inputCount, ResourceLocation outputItem, int outputCount) {
        this.key = key; this.workerId = workerId; this.ruleId = ruleId; this.simulatedDays = simulatedDays;
        this.inputItem = inputItem; this.inputCount = inputCount; this.outputItem = outputItem; this.outputCount = outputCount;
    }
    public String key() { return key; }
    public UUID workerId() { return workerId; }
    public String ruleId() { return ruleId; }
    public int simulatedDays() { return simulatedDays; }
    public ResourceLocation inputItem() { return inputItem; }
    public int inputCount() { return inputCount; }
    public ResourceLocation outputItem() { return outputItem; }
    public int outputCount() { return outputCount; }
    public Status status() { return status; }
    public String detail() { return detail; }
    public boolean delivered() { return status == Status.DELIVERED; }
    public void blocked(Status value, String reason) { if (!delivered()) { status = value; detail = reason; } }
    public void delivered(ResourceLocation dimension, BlockPos position) { status = Status.DELIVERED; detail = "ready_in_chest"; deliveredDimension = dimension; deliveredPosition = position.immutable(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putString("Key", key); tag.putUUID("Worker", workerId); tag.putString("Rule", ruleId);
        tag.putInt("Days", simulatedDays); tag.putString("Input", inputItem.toString()); tag.putInt("InputCount", inputCount);
        tag.putString("Output", outputItem.toString()); tag.putInt("OutputCount", outputCount); tag.putString("Status", status.name()); tag.putString("Detail", detail);
        if (deliveredDimension != null && deliveredPosition != null) { tag.putString("DeliveredDimension", deliveredDimension.toString()); tag.putLong("DeliveredPosition", deliveredPosition.asLong()); }
        return tag;
    }
    public static ProductionObligation load(CompoundTag tag) {
        ProductionObligation value = new ProductionObligation(tag.getString("Key"), tag.getUUID("Worker"), tag.getString("Rule"), tag.getInt("Days"),
                new ResourceLocation(tag.getString("Input")), tag.getInt("InputCount"), new ResourceLocation(tag.getString("Output")), tag.getInt("OutputCount"));
        try { value.status = Status.valueOf(tag.getString("Status")); } catch (IllegalArgumentException ignored) { value.status = Status.QUARANTINED; }
        value.detail = tag.getString("Detail");
        if (tag.contains("DeliveredDimension") && tag.contains("DeliveredPosition")) { value.deliveredDimension = new ResourceLocation(tag.getString("DeliveredDimension")); value.deliveredPosition = BlockPos.of(tag.getLong("DeliveredPosition")); }
        return value;
    }
}
