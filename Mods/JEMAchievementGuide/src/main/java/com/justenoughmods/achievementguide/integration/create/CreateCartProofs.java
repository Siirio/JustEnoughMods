package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;

/** Actual player-supplied cart, its exact native assembly, then native rail movement. */
public final class CreateCartProofs {
    private static final String CART = "jemQuestCartSupplyV1";
    private static final String RUN = "jemQuestCartAssemblyV1";
    private static final String ASSEMBLER = "jemQuestCartAssemblerIdentityV1";
    private CreateCartProofs() {}
    public static void placed(Level world, Entity entity, UseOnContext context) {
        if (!(world instanceof ServerLevel level) || !(entity instanceof AbstractMinecart) || level.getEntity(entity.getUUID()) != entity
                || !(context.getPlayer() instanceof ServerPlayer actor) || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild
                || !(level.getBlockEntity(context.getClickedPos()) instanceof CartAssemblerBlockEntity assembler)) return;
        if (!assembler.getPersistentData().hasUUID(ASSEMBLER)) {
            assembler.getPersistentData().putUUID(ASSEMBLER, UUID.randomUUID()); assembler.setChanged();
        }
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", actor.getUUID()); proof.putLong("position", assembler.getBlockPos().asLong());
        proof.putUUID("assembler", assembler.getPersistentData().getUUID(ASSEMBLER));
        proof.putString("dimension", level.dimension().location().toString());
        entity.getPersistentData().put(CART, proof);
    }
    public static void assembled(CartAssemblerBlockEntity assembler, AbstractMinecart cart) {
        CompoundTag proof = cart.getPersistentData().getCompound(CART);
        if (!(assembler.getLevel() instanceof ServerLevel level) || !proof.hasUUID("actor") || !proof.hasUUID("assembler")
                || !proof.getString("dimension").equals(level.dimension().location().toString())
                || proof.getLong("position") != assembler.getBlockPos().asLong() || !assembler.getPersistentData().hasUUID(ASSEMBLER)
                || !proof.getUUID("assembler").equals(assembler.getPersistentData().getUUID(ASSEMBLER))) return;
        for (Entity entity : cart.getPassengers()) {
            if (entity instanceof OrientedContraptionEntity contraption && level.getEntity(entity.getUUID()) == entity
                    && contraption.getContraption() != null && !contraption.getContraption().getBlocks().isEmpty()) {
                CompoundTag run = new CompoundTag(); run.putUUID("actor", proof.getUUID("actor")); run.putUUID("cart", cart.getUUID());
                entity.getPersistentData().put(RUN, run); cart.getPersistentData().remove(CART); break;
            }
        }
    }
    public static void moved(AbstractMinecart cart, double x, double y, double z) {
        if (!(cart.level() instanceof ServerLevel level) || cart.distanceToSqr(x, y, z) <= .000001) return;
        for (Entity entity : cart.getPassengers()) {
            CompoundTag run = entity.getPersistentData().getCompound(RUN);
            if (entity instanceof OrientedContraptionEntity contraption && run.hasUUID("actor") && run.hasUUID("cart")
                    && cart.getUUID().equals(run.getUUID("cart")) && contraption.getContraption() != null
                    && !contraption.getContraption().getBlocks().isEmpty() && entity.getVehicle() == cart) {
                CreateRunProofs.emit(level, run.getUUID("actor"), "v3_eng_cart"); entity.getPersistentData().remove(RUN);
            }
        }
    }
}
