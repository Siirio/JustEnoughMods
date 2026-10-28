package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRepackProof;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity", remap = false)
public abstract class CreateRepackagerQuestMixin {
    @Unique private CreateRepackProof jem$run;
    @Inject(method = "attemptToRepackage", at = @At("HEAD"), remap = false)
    private void jem$before(IItemHandler inventory, CallbackInfo callback) { jem$run = new CreateRepackProof(); }
    @Redirect(method = "attemptToRepackage", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;addPackageFragment(Lnet/minecraft/world/item/ItemStack;)I"), remap = false)
    private int jem$nativeFragment(PackageRepackageHelper helper, ItemStack box) {
        jem$run.considered(box); return helper.addPackageFragment(box);
    }
    @Redirect(method = "attemptToRepackage", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/packager/repackager/PackageRepackageHelper;repack(ILnet/minecraft/util/RandomSource;)Ljava/util/List;"), remap = false)
    private List<BigItemStack> jem$nativeOutput(PackageRepackageHelper helper, int order, RandomSource random) {
        List<BigItemStack> result = helper.repack(order, random); jem$run.result(order, result); return result;
    }
    @Redirect(method = "attemptToRepackage", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/IItemHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack jem$actualConsumption(IItemHandler inventory, int slot, int count, boolean simulate) {
        ItemStack result = inventory.extractItem(slot, count, simulate);
        if (!simulate) jem$run.consumed(result);
        return result;
    }
    @Inject(method = "attemptToRepackage", at = @At("RETURN"), remap = false)
    private void jem$after(IItemHandler inventory, CallbackInfo callback) {
        jem$run.committed((PackagerBlockEntity)(Object)this); jem$run = null;
    }
}
