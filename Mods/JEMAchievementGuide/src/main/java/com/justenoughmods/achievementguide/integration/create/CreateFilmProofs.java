package com.justenoughmods.achievementguide.integration.create;

import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Installed Exposure1.9.21 one-step native black/white film sequence, including preserved exposures. */
public final class CreateFilmProofs {
    private CreateFilmProofs() {}
    public static boolean completed(ItemStack before, ItemStack after, FluidStack fluid, int debit) {
        if (debit != 250 || !"minecraft:water".equals(String.valueOf(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())))
                || !(CreateRunProofs.item(before, "exposure:black_and_white_film") || CreateRunProofs.item(before, "exposure:high_sensitivity_black_and_white_film"))
                || !CreateRunProofs.item(after, "exposure:developed_black_and_white_film") || before.getCount() < 1 || after.getCount() != 1
                || !before.hasTag() || !after.hasTag() || before.getTag().contains("SequencedAssembly")) return false;
        // Native DataComponents.getFilmFrames stores film_frames. The installed data fixer also accepts legacy Frames.
        for (String key : new String[]{"film_frames", "Frames"}) {
            if (before.getTag().contains(key, Tag.TAG_LIST) && !before.getTag().getList(key, Tag.TAG_COMPOUND).isEmpty()
                    && before.getTag().get(key).equals(after.getTag().get(key))) return true;
        }
        return false;
    }
}
