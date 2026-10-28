package com.justenoughmods.achievementguide.integration.creaturecare;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CreatureCareWash {
    private static final class Run {
        TamableAnimal raccoon;
        ItemStack egg = ItemStack.EMPTY;
        boolean wild;
    }
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private CreatureCareWash() {}
    public static void begin() { CURRENT.set(new Run()); }
    public static void washing(TamableAnimal raccoon, ItemStack egg) {
        Run run = CURRENT.get();
        if (run != null && egg.is(Items.EGG)) {
            run.raccoon = raccoon; run.egg = egg.copy(); run.wild = !raccoon.isTame();
        }
    }
    public static void end() {
        Run run = CURRENT.get(); CURRENT.remove();
        if (run == null || run.raccoon == null || !run.wild || !run.raccoon.isTame()) return;
        var animal = run.raccoon;
        if (animal.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            var actor = CreatureCareProof.real(player);
            if (CreatureCareProof.owned(animal, actor) && CreatureCareProof.debited(run.egg, animal.getMainHandItem())) {
                CreatureCareProof.emit(actor, "v3_fauna_woodland_care");
            }
        }
    }
}
