package com.justenoughmods.achievementguide.integration.wildlife;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
public final class WildlifeQuestGust {
    private WildlifeQuestGust() {}
    public static void pushed(Entity gust, Entity target, Vec3 before, Vec3 after) {
        var actor = WildlifeQuestProof.actor(gust, "gust_actor"); if (actor == null || !(target instanceof ItemEntity) || after.equals(before)) return;
        var data = WildlifeQuestProof.data(target); var proof = data.getCompound("gust_push");
        if (!proof.hasUUID("source") || !proof.getUUID("source").equals(gust.getUUID())) { proof = new CompoundTag(); proof.putDouble("x", target.getX()); proof.putDouble("y", target.getY()); proof.putDouble("z", target.getZ()); }
        proof.putUUID("source", gust.getUUID()); proof.putUUID("actor", actor.getUUID()); proof.putDouble("dx", after.x - before.x); proof.putDouble("dy", after.y - before.y); proof.putDouble("dz", after.z - before.z); proof.putLong("until", target.level().getGameTime() + 3); data.put("gust_push", proof);
    }
    public static void moved(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || !item.getPersistentData().contains(WildlifeQuestProof.KEY)) return;
        var data = WildlifeQuestProof.data(item); var proof = data.getCompound("gust_push"); if (!proof.hasUUID("source") || !proof.hasUUID("actor")) return;
        var gust = level.getEntity(proof.getUUID("source")); var actor = WildlifeQuestProof.real(level.getServer().getPlayerList().getPlayer(proof.getUUID("actor")));
        if (gust == null || !gust.isAlive() || actor == null || proof.getLong("until") < level.getGameTime() || !WildlifeQuestProof.matches(gust, "gust_actor", actor.getUUID())) { data.remove("gust_push"); return; }
        var delta = item.position().subtract(proof.getDouble("x"), proof.getDouble("y"), proof.getDouble("z"));
        if (delta.lengthSqr() > 1 && delta.dot(new Vec3(proof.getDouble("dx"), proof.getDouble("dy"), proof.getDouble("dz"))) > 0) { WildlifeQuestProof.emit(actor, "v3_fauna_redstone_creatures"); data.remove("gust_push"); }
    }
}
