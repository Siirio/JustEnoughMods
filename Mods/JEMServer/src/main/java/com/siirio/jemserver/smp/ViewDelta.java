package com.siirio.jemserver.smp;

import net.minecraft.nbt.*;

public final class ViewDelta {
    public static CompoundTag between(CompoundTag before, CompoundTag after) {
        var patch = new CompoundTag();
        patch.putBoolean("__delta", true);
        var removed = new net.minecraft.nbt.ListTag();
        for (String key : before.getAllKeys())
            if (!after.contains(key)) removed.add(net.minecraft.nbt.StringTag.valueOf(key));
        if (!removed.isEmpty()) patch.put("__removed", removed);
        for (String key : after.getAllKeys()) {
            var next = after.get(key);
            var previous = before.get(key);
            if (java.util.Objects.equals(next, previous)) continue;
            patch.put(
                    key,
                    previous instanceof CompoundTag first && next instanceof CompoundTag second
                            ? between(first, second)
                            : next.copy());
        }
        return patch;
    }

    public static CompoundTag apply(CompoundTag old, CompoundTag patch) {
        var next = old.copy();
        for (Tag tag : patch.getList("__removed", Tag.TAG_STRING)) next.remove(tag.getAsString());
        for (String key : patch.getAllKeys())
            if (!key.equals("__delta") && !key.equals("__removed")) {
                var value = patch.get(key);
                next.put(
                        key,
                        value instanceof CompoundTag child && child.getBoolean("__delta")
                                ? apply(next.getCompound(key), child)
                                : value.copy());
            }
        return next;
    }

    private ViewDelta() {}
}
