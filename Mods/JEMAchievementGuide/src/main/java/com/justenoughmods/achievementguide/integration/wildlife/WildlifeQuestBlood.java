package com.justenoughmods.achievementguide.integration.wildlife;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class WildlifeQuestBlood {
    public static final class Run {
        public final ServerPlayer actor; public final ItemStack weapon; public final ItemStack before; public final WildlifeQuestRun projectiles;
        public ItemStack ammo; public ItemStack ammoBefore;
        public Run(ServerPlayer actor, ItemStack stack) { this.actor = actor; weapon = stack; before = stack.copy(); projectiles = WildlifeQuestRun.begin(actor, actor, stack); }
    }
    private WildlifeQuestBlood() {}
    public static Run begin(LivingEntity user, ItemStack weapon) { return user instanceof Player player && WildlifeQuestProof.real(player) != null ? new Run((ServerPlayer)player, weapon) : null; }
    public static void end(Run run) {
        if (run == null) return; WildlifeQuestRun.end(run.projectiles);
        var store = NativeFoodSources.get(run.actor.serverLevel()); String kind = WildlifeQuestProof.item(run.before).equals("alexsmobs:blood_sprayer") ? "wildlife_blood_charge" : "wildlife_hemolymph_charge";
        String fuel = kind.equals("wildlife_blood_charge") ? "alexsmobs:blood_sac" : "alexsmobs:hemolymph_sac";
        if (run.before.getDamageValue() >= run.before.getMaxDamage() - 1 && run.weapon.getDamageValue() == 0 && run.ammo != null && run.ammoBefore != null
                && WildlifeQuestProof.item(run.ammoBefore).equals(fuel) && WildlifeQuestProof.debited(run.ammoBefore, run.ammo, 1)) {
            var tools = WildlifeQuestTools.get(run.actor.serverLevel());
            if (tools.origin(run.before) != null) {
                var oldClaims = NativeFoodSources.claims(run.before).stream().map(NativeFoodSources.Claim::batch).toList(); var oldOwner = store.owner(run.before, kind);
                if (oldOwner != null) store.consume(oldOwner, run.before, kind, 1);
                NativeFoodSources.claims(run.weapon, NativeFoodSources.claims(run.weapon).stream().filter(c -> !oldClaims.contains(c.batch())).toList()); tools.retired(run.before);
            }
            var batch = store.certify(run.actor.serverLevel(), run.actor.getUUID(), kind, run.weapon, 1); WildlifeQuestTools.get(run.actor.serverLevel()).record(batch, batch, run.weapon);
        }
        if (run.before.getDamageValue() >= run.before.getMaxDamage() - 1) return;
        var tools = WildlifeQuestTools.get(run.actor.serverLevel()); var origin = tools.origin(run.weapon); if (origin == null) origin = tools.origin(run.before);
        if (origin == null || !run.actor.getUUID().equals(store.owner(run.weapon.isEmpty() ? run.before : run.weapon, kind))) return;
        for (var projectile : run.projectiles.created) {
            if (run.actor.serverLevel().getEntity(projectile.getUUID()) != projectile) continue;
            if (kind.equals("wildlife_blood_charge") && !WildlifeQuestProof.type(projectile).equals("alexsmobs:mosquito_spit") || kind.equals("wildlife_hemolymph_charge") && !WildlifeQuestProof.type(projectile).equals("alexsmobs:hemolymph")) continue;
            WildlifeQuestProof.mark(projectile, "blood_actor", run.actor.getUUID()); WildlifeQuestProof.data(projectile).putUUID("blood_weapon", origin); WildlifeQuestProof.data(projectile).putString("blood_kind", kind);
        }
    }
    public static void hit(net.minecraft.world.entity.Entity projectile, LivingEntity target, float health, boolean accepted) {
        var actor = WildlifeQuestProof.actor(projectile, "blood_actor"); var proof = WildlifeQuestProof.data(projectile);
        if (actor != null && accepted && target.getHealth() < health && WildlifeQuestProof.hostile(target, actor) && proof.hasUUID("blood_weapon")
                && WildlifeQuestTools.consume(actor, proof.getString("blood_kind"), proof.getUUID("blood_weapon"))) WildlifeQuestProof.emit(actor,
                    proof.getString("blood_kind").equals("wildlife_blood_charge") ? "v3_fauna_blood_line" : "v3_fauna_blood_line_hemolymph");
    }
}
