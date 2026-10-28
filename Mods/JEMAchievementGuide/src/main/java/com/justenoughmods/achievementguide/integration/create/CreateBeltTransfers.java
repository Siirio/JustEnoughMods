package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Only the native powered belt's real direct insertion call is scoped here. */
public final class CreateBeltTransfers {
    private static final ThreadLocal<TransportedItemStack> INPUT = new ThreadLocal<>();
    private CreateBeltTransfers() {}
    public static ItemStack insertion(BeltBlockEntity belt, TransportedItemStack input, Supplier<ItemStack> nativeCall) {
        if (((BlockEntity)(Object)belt).getLevel() == null || ((BlockEntity)(Object)belt).getLevel().isClientSide || belt.getSpeed() == 0) {
            return nativeCall.get();
        }
        TransportedItemStack previous = INPUT.get();
        INPUT.set(input);
        try { return nativeCall.get(); }
        finally { if (previous == null) INPUT.remove(); else INPUT.set(previous); }
    }
    public static boolean poweredInput(TransportedItemStack input) { return INPUT.get() == input; }
}
