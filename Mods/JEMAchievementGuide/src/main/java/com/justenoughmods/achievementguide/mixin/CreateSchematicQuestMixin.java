package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateCargoTransfers;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.justenoughmods.achievementguide.integration.create.CreateSchematicProjectile;
import com.justenoughmods.achievementguide.integration.create.CreateSchematicRun;
import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity", remap = false)
public abstract class CreateSchematicQuestMixin implements CreateSchematicRun {
    @Unique private static final String JEM_JOB = "jemQuestCannonRunV1";
    @Unique private UUID jem$finishingJob;

    @Override public void jem$jobCommand(ServerPlayer actor, boolean start) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_JOB);
        owner.setChanged();
        var cannon = (SchematicannonBlockEntity)(Object)this;
        ItemStack schematic = cannon.inventory.getStackInSlot(0);
        if (!start || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || cannon.state != SchematicannonBlockEntity.State.RUNNING || schematic.isEmpty()) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID());
        proof.putUUID("job", UUID.randomUUID());
        proof.putString("schematic", CreateCargoTransfers.fingerprint(schematic));
        owner.getPersistentData().put(JEM_JOB, proof);
        owner.setChanged();
    }

    @Unique private CompoundTag jem$proof() {
        return ((BlockEntity)(Object)this).getPersistentData().getCompound(JEM_JOB);
    }

    @Unique private boolean jem$sameInput(CompoundTag proof) {
        ItemStack input = ((SchematicannonBlockEntity)(Object)this).inventory.getStackInSlot(0);
        return !input.isEmpty() && proof.getString("schematic").equals(CreateCargoTransfers.fingerprint(input));
    }

    @Redirect(method = {"launchBlock", "launchBelt"}, at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
    private boolean jem$launchedForJob(List<Object> items, Object launched) {
        boolean accepted = items.add(launched);
        CompoundTag proof = jem$proof();
        if (accepted && proof.hasUUID("job") && jem$sameInput(proof) && launched instanceof CreateSchematicProjectile projectile) {
            projectile.jem$job(proof.getUUID("job"));
        }
        return accepted;
    }

    @Redirect(method = "tickFlyingBlocks", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/schematics/cannon/LaunchedItem;update(Lnet/minecraft/world/level/Level;)Z"), remap = false)
    private boolean jem$actualPlacedBlock(LaunchedItem projectile, Level world) {
        BlockState before = world.getBlockState(projectile.target);
        boolean completed = projectile.update(world);
        CompoundTag proof = jem$proof();
        if (!completed || !(world instanceof ServerLevel) || !proof.hasUUID("job")
                || !(projectile instanceof CreateSchematicProjectile owned)
                || !proof.getUUID("job").equals(owned.jem$job())) return completed;
        BlockState after = world.getBlockState(projectile.target);
        CompoundTag nativeProjectile = projectile.serializeNBT();
        String expected = nativeProjectile.contains("BlockState")
                ? nativeProjectile.getCompound("BlockState").getString("Name")
                : nativeProjectile.contains("Length") ? "create:belt" : "";
        if (!after.isAir() && !before.equals(after)
                && expected.equals(String.valueOf(ForgeRegistries.BLOCKS.getKey(after.getBlock())))) {
            proof.putBoolean("placed", true);
            ((BlockEntity)(Object)this).setChanged();
        }
        return completed;
    }

    @Inject(method = "finishedPrinting", at = @At("HEAD"), remap = false)
    private void jem$finishing(CallbackInfo callback) {
        CompoundTag proof = jem$proof();
        jem$finishingJob = proof.hasUUID("job") && jem$sameInput(proof) ? proof.getUUID("job") : null;
    }

    @Inject(method = "finishedPrinting", at = @At("TAIL"), remap = false)
    private void jem$finished(CallbackInfo callback) {
        var cannon = (SchematicannonBlockEntity)(Object)this;
        CompoundTag proof = jem$proof();
        if (jem$finishingJob != null && proof.hasUUID("job") && jem$finishingJob.equals(proof.getUUID("job"))
                && cannon.state == SchematicannonBlockEntity.State.STOPPED && "finished".equals(cannon.statusMsg)) {
            proof.putBoolean("finished", true);
            ((BlockEntity)(Object)this).setChanged();
        }
        jem$finishingJob = null;
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$completedJob(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this;
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        CompoundTag proof = jem$proof();
        if (!proof.hasUUID("actor")) return;
        if (proof.getBoolean("finished") && proof.getBoolean("placed")) {
            CreateRunProofs.emit(level, proof.getUUID("actor"), "create_schematic_complete");
            owner.getPersistentData().remove(JEM_JOB);
            owner.setChanged();
        } else if (!proof.getBoolean("finished") && !jem$sameInput(proof)) {
            owner.getPersistentData().remove(JEM_JOB);
            owner.setChanged();
        }
    }
}
