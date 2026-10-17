package com.siirio.jemprofessions.mixin.sandbox.core;

import com.siirio.jemprofessions.sandbox.core.CoreFishing;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LootPool.class)
public abstract class FishingLootPoolMixin {
    @Shadow @Final private LootPoolEntryContainer[] entries;
    @Redirect(method = "addRandomItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/entries/LootPoolEntryContainer;expand(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)Z"))
    private boolean jem$fishWeight(LootPoolEntryContainer entry, LootContext context, Consumer<LootPoolEntry> output) { return CoreFishing.expand(entry, context, output, entries); }
}
