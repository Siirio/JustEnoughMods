package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateRailCouplerRun;
import com.justenoughmods.achievementguide.integration.create.CreateRunProofs;
import com.railwayteam.railways.content.coupling.TrainUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.railwayteam.railways.content.coupling.coupler.TrackCouplerBlockEntity", remap = false)
public abstract class RailCouplerQuestMixin implements CreateRailCouplerRun {
    @Unique private static final String JEM_EDGE = "jemQuestRailCouplerEdgeV1";
    @Unique private ServerPlayer jem$operator;
    @Override public void jem$nativeEdge(ServerPlayer actor) {
        BlockEntity owner = (BlockEntity)(Object)this;
        owner.getPersistentData().remove(JEM_EDGE); owner.setChanged();
        if (actor == null || owner.getLevel() != actor.serverLevel()) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID());
        proof.putString("configuration", owner.getBlockState().toString()); owner.getPersistentData().put(JEM_EDGE, proof);
    }
    @Inject(method = "onPowered", at = @At("HEAD"), remap = false)
    private void jem$begin(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this; CompoundTag proof = owner.getPersistentData().getCompound(JEM_EDGE);
        jem$operator = owner.getLevel() instanceof ServerLevel level && proof.hasUUID("actor")
                && owner.getBlockState().toString().equals(proof.getString("configuration"))
                ? level.getServer().getPlayerList().getPlayer(proof.getUUID("actor")) : null;
        owner.getPersistentData().remove(JEM_EDGE); owner.setChanged();
    }
    @Inject(method = "onPowered", at = @At("RETURN"), remap = false)
    private void jem$end(CallbackInfo callback) { jem$operator = null; }
    @Inject(method = "onUnpowered", at = @At("HEAD"), remap = false)
    private void jem$cancel(CallbackInfo callback) {
        BlockEntity owner = (BlockEntity)(Object)this; owner.getPersistentData().remove(JEM_EDGE); owner.setChanged();
    }
    @Unique private boolean jem$registered(Train train) { return train != null && Create.RAILWAYS.trains.get(train.id) == train; }
    @Unique private boolean jem$observed(Train train) { return jem$operator != null && jem$registered(train) && train.carriages.size() > 0 && train.carriages.size() <= 1024; }
    @Redirect(method = "onPowered", at = @At(value = "INVOKE", target = "Lcom/railwayteam/railways/content/coupling/TrainUtils;splitTrain(Lcom/simibubi/create/content/trains/entity/Train;I)Lcom/simibubi/create/content/trains/entity/Train;"), remap = false)
    private Train jem$actualSplit(Train train, int number) {
        List<Carriage> before = jem$observed(train) ? List.copyOf(train.carriages) : List.of();
        Train result = TrainUtils.splitTrain(train, number);
        if (!before.isEmpty() && result != train && jem$registered(train) && jem$registered(result)
                && !train.carriages.isEmpty() && !result.carriages.isEmpty() && train.carriages.size() + result.carriages.size() == before.size()) {
            List<Carriage> after = new ArrayList<>(train.carriages); after.addAll(result.carriages);
            if (new HashSet<>(before).equals(new HashSet<>(after)) && after.size() == new HashSet<>(after).size())
                CreateRunProofs.emit(jem$operator.serverLevel(), jem$operator.getUUID(), "v3_eng_rail_couple");
        }
        return result;
    }
    @Redirect(method = "onPowered", at = @At(value = "INVOKE", target = "Lcom/railwayteam/railways/content/coupling/TrainUtils;combineTrains(Lcom/simibubi/create/content/trains/entity/Train;Lcom/simibubi/create/content/trains/entity/Train;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;I)Lcom/simibubi/create/content/trains/entity/Train;"), remap = false)
    private Train jem$actualCombine(Train front, Train back, BlockPos drop, Level world, int spacing) {
        List<Carriage> before = new ArrayList<>();
        if (front != back && jem$observed(front) && jem$observed(back) && front.carriages.size() + back.carriages.size() <= 1024) {
            before.addAll(front.carriages); before.addAll(back.carriages);
        }
        Train result = TrainUtils.combineTrains(front, back, drop, world, spacing);
        if (!before.isEmpty() && result == front && jem$registered(front) && !jem$registered(back) && back.carriages.isEmpty()
                && front.carriages.size() == before.size() && new HashSet<>(front.carriages).equals(new HashSet<>(before))
                && front.carriages.stream().allMatch(carriage -> carriage.train == front))
            CreateRunProofs.emit(jem$operator.serverLevel(), jem$operator.getUUID(), "v3_eng_rail_couple");
        return result;
    }
}
