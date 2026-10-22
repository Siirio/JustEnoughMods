package com.siirio.jemguisandbox.mirror.jemserver.client.smp;
import net.minecraft.world.item.ItemStack;
public final class JeiItemLink {
    public static boolean open(ItemStack stack) {
        if (stack.isEmpty()) return false;
        com.siirio.jemguisandbox.Demo.itemPreview(stack);
        return true;
    }
}
