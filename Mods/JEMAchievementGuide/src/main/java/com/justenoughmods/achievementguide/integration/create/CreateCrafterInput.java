package com.justenoughmods.achievementguide.integration.create;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

/** Synchronous scope of the native player's actual ingredient insertion. */
public final class CreateCrafterInput {
    private record Input(UUID actor, String fingerprint) {}
    private static final ThreadLocal<Input> INPUT = new ThreadLocal<>();
    private CreateCrafterInput() {}

    public static ItemStack insert(ServerPlayer actor, ItemStack item, Supplier<ItemStack> nativeCall) {
        if (actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild) return nativeCall.get();
        Input previous = INPUT.get();
        INPUT.set(new Input(actor.getUUID(), CreateCargoTransfers.fingerprint(item)));
        try { return nativeCall.get(); }
        finally { if (previous == null) INPUT.remove(); else INPUT.set(previous); }
    }

    public static UUID supplier(ItemStack item) {
        Input input = INPUT.get();
        return input != null && !item.isEmpty() && input.fingerprint().equals(CreateCargoTransfers.fingerprint(item))
                ? input.actor() : null;
    }
}
