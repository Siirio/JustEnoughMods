package com.justenoughmods.achievementguide.integration;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.Event;

/** Bounded, synchronous observations of native Skarrier operations. No gameplay calculation. */
public final class SkarrierWorkshopProof {
    private static final String COATING = "JemQuestCoatingV4";
    private static final String QUAKE = "JemQuestQuakeV4";
    private static final String OUTPUT = "JemQuestWorkshopOutputV4";
    private static final String FANG = "JemQuestForlornV4";
    private static final int LIMIT = 16;
    private static final ThreadLocal<State> CALLS = ThreadLocal.withInitial(State::new);
    private static final ThreadLocal<Hurts> HURTS = ThreadLocal.withInitial(Hurts::new);
    private static final ThreadLocal<Works> WORKS = ThreadLocal.withInitial(Works::new);
    private static final ThreadLocal<Deaths> DEATHS = ThreadLocal.withInitial(Deaths::new);
    private static final class Deaths { final ArrayDeque<Death> calls = new ArrayDeque<>(); int overflow; }
    private record Kill(ServerPlayer actor, String action) {}
    private record Death(LivingEntity target, boolean deadBefore, List<Kill> kills) {}
    private static final ThreadLocal<Falls> FALLS = ThreadLocal.withInitial(Falls::new);
    private static final class Falls { final ArrayDeque<Fall> calls = new ArrayDeque<>(); int overflow; }
    private static final class Fall { final LivingEntity actor; boolean shockwave; Fall(LivingEntity actor) { this.actor = actor; } }
    private static final ThreadLocal<Merges> MERGES = ThreadLocal.withInitial(Merges::new);
    private static final class Merges { final ArrayDeque<Merge> calls = new ArrayDeque<>(); int overflow; }
    private record Merge(ItemEntity destination, ItemEntity source, ItemStack to, ItemStack from, CompoundTag toProof, CompoundTag fromProof) {}
    private static final class Works { final ArrayDeque<Work> calls = new ArrayDeque<>(); int overflow; }
    private static final class Work {
        final ServerPlayer actor; final BlockPos pos; final net.minecraft.world.level.block.state.BlockState before;
        boolean experience;
        Work(ServerPlayer actor, BlockPos pos) { this.actor = actor; this.pos = pos.immutable(); this.before = actor == null ? null : actor.level().getBlockState(pos); }
    }
    private static final class State { final ArrayDeque<Run> calls = new ArrayDeque<>(); int overflow; }
    private static final class Hurts { final ArrayDeque<Hurt> calls = new ArrayDeque<>(); int overflow; }
    private record Hurt(LivingEntity target, float vitality, List<ServerPlayer> actors) {}
    private static final class Run {
        final String route;
        final ServerPlayer actor;
        final Entity target;
        final ItemStack main, off, use, item;
        final ItemStack mainBefore, offBefore, useBefore, itemBefore;
        final Event event;
        final int fire, frozen, xp;
        final double quakeCooldown;
        final Vec3 velocity;
        final boolean valid, burning;
        BlockPos pos;
        net.minecraft.world.level.block.state.BlockState block;
        final List<ItemEntity> outputs = new ArrayList<>();
        boolean effect, damage, output;
        Run(String route, ServerPlayer actor, Entity target, Event event, ItemStack item, boolean valid) {
            this.route = route; this.actor = actor; this.target = target; this.event = event; this.item = item;
            main = actor == null ? ItemStack.EMPTY : actor.getMainHandItem();
            off = actor == null ? ItemStack.EMPTY : actor.getOffhandItem();
            use = actor == null ? ItemStack.EMPTY : actor.getUseItem();
            mainBefore = main.copy(); offBefore = off.copy(); useBefore = use.copy(); itemBefore = item.copy();
            fire = target == null ? 0 : target.getRemainingFireTicks();
            burning = target != null && target.isOnFire();
            quakeCooldown = target == null ? -1 : target.getPersistentData().getDouble("QuakeCooldown");
            frozen = actor == null ? 0 : actor.getTicksFrozen();
            velocity = target == null ? Vec3.ZERO : target.getDeltaMovement();
            xp = actor == null ? 0 : actor.totalExperience;
            this.valid = valid;
        }
    }
    private SkarrierWorkshopProof() {}

    public static boolean real(Entity entity) {
        return entity instanceof ServerPlayer actor && !actor.getAbilities().instabuild && !actor.isSpectator()
                && !(actor instanceof FakePlayer) && actor.serverLevel().getServer().isSameThread();
    }
    public static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private static String effectId(MobEffectInstance effect) { return BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).toString(); }
    private static double charges(ItemStack stack, String kind) { return stack.getTag() == null ? 0 : stack.getTag().getDouble(kind + "Coating"); }
    private static boolean coating(ItemStack stack, String kind, ServerPlayer actor) {
        CompoundTag tag = stack.getTag(), proof = stack.getTagElement(COATING);
        return tag != null && kind.equals(tag.getString("CoatingApplied")) && proof != null && proof.hasUUID("actor")
                && actor.getUUID().equals(proof.getUUID("actor")) && kind.equals(proof.getString("kind"));
    }
    private static boolean spent(ItemStack before, ItemStack after, String kind) {
        double count = charges(before, kind);
        return Double.isFinite(count) && count > 0 && count - charges(after, kind) == 1;
    }
    private static boolean live(Event event) { return event == null || !event.isCancelable() || !event.isCanceled(); }
    private static Run current() {
        State state = CALLS.get();
        return state.overflow == 0 && !state.calls.isEmpty() ? state.calls.peek() : null;
    }
    public static void begin(String route, Entity owner, Entity target, Event event) { begin(route, owner, target, event, ItemStack.EMPTY); }
    public static void begin(String route, Entity owner, Entity target, Event event, ItemStack item) {
        State state = CALLS.get();
        if (state.overflow > 0 || state.calls.size() >= LIMIT) { state.overflow++; return; }
        ServerPlayer actor = owner instanceof ServerPlayer player ? player : null;
        boolean valid = real(owner) && live(event) && (target == null || target.level() == owner.level());
        state.calls.push(new Run(route, actor, target, event, item, valid));
    }
    public static void end() {
        State state = CALLS.get();
        if (state.overflow > 0) { state.overflow--; return; }
        Run run = state.calls.isEmpty() ? null : state.calls.pop();
        if (state.calls.isEmpty()) CALLS.remove();
        if (run == null || !run.valid || !live(run.event) || !real(run.actor)) return;
        ServerPlayer actor = run.actor;
        switch (run.route) {
            case "fire_weapon" -> {
                if (run.event instanceof LivingHurtEvent hurt && run.target != null && run.burning
                        && hurt.getSource().getEntity() == actor && hurt.getEntity() == run.target
                        && hurt.getAmount() > 0 && actor.getMainHandItem() == run.main
                        && coating(run.mainBefore, "Magmadermis", actor) && spent(run.mainBefore, run.main, "Magmadermis")) {
                    Hurts hurts = HURTS.get();
                    if (hurts.overflow == 0 && !hurts.calls.isEmpty() && hurts.calls.peek().target == run.target
                            && hurts.calls.peek().actors.size() < LIMIT) hurts.calls.peek().actors.add(actor);
                }
            }
            case "fire_shield" -> {
                if (run.event instanceof ShieldBlockEvent shield && shield.getEntity() == actor && shield.getBlockedDamage() > 0
                        && run.target != null && run.target.isOnFire() && run.target.getRemainingFireTicks() > run.fire
                        && coating(run.useBefore, "Magmadermis", actor) && spent(run.useBefore, run.use, "Magmadermis")
                        && (run.use.getDamageValue() > run.useBefore.getDamageValue() || run.use.getCount() < run.useBefore.getCount())) shieldCommitted(actor, shield, "coat_fire");
            }
            case "warden" -> {
                if (run.event instanceof LivingDeathEvent death && death.getSource().getEntity() == actor && death.getEntity().isDeadOrDying()
                        && actor.getMainHandItem() == run.main && coating(run.mainBefore, "WardenVein", actor)
                        && run.main.getDamageValue() < run.mainBefore.getDamageValue() && spent(run.mainBefore, run.main, "WardenVein")) killed(death.getEntity(), actor, "coat_sustain");
            }
            case "wrought" -> {
                if (run.event instanceof ShieldBlockEvent shield && shield.getEntity() == actor && shield.getBlockedDamage() > 0
                        && coating(run.useBefore, "WroughtScale", actor) && run.use.getDamageValue() < run.useBefore.getDamageValue()
                        && spent(run.useBefore, run.use, "WroughtScale")) shieldCommitted(actor, shield, "coat_sustain");
            }
            case "brush", "acid" -> {
                boolean committed = run.pos != null && run.block != null && !run.block.isAir()
                        && actor.level().getBlockState(run.pos).isAir() && run.output;
                if (run.route.equals("brush")) committed &= actor.getMainHandItem() == run.main && run.mainBefore.is(Items.BRUSH)
                        && run.block.is(Blocks.GRAVEL) && coating(run.mainBefore, "AmmoniteShell", actor)
                        && spent(run.mainBefore, run.main, "AmmoniteShell");
                else committed &= id(run.itemBefore).equals("skarrier_mobs:acidic_bile")
                        && run.itemBefore.getCount() - run.item.getCount() == 1;
                if (committed) for (ItemEntity output : run.outputs) {
                    CompoundTag proof = new CompoundTag(); proof.putUUID("actor", actor.getUUID());
                    proof.putString("action", run.route.equals("brush") ? "brush_shell" : "ore_acid");
                    outputProof(output, proof, output.getItem().getCount());
                }
            }
            case "freezing_attack", "seething" -> { if (run.effect) emit(actor, "totem_elements"); }
            case "freezing_guard" -> {
                if (id(run.offBefore).equals("skarrier_mobs:totem_of_freezing") && run.frozen > 0 && actor.getTicksFrozen() == 0) emit(actor, "totem_elements");
            }
            case "repulsion" -> {
                if (run.target != null && !run.target.getDeltaMovement().equals(run.velocity)
                        && id(run.offBefore).equals("skarrier_mobs:totem_of_repulsion")
                        && actor.getCooldowns().isOnCooldown(run.offBefore.getItem())
                        && (run.off.getDamageValue() > run.offBefore.getDamageValue() || run.off.getCount() < run.offBefore.getCount())) emit(actor, "totem_elements");
            }
            case "empower" -> {
                if (run.effect && actor.getOffhandItem() == run.off && coating(run.offBefore, "SLFlesh", actor)
                        && actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != null
                        && actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                            .getModifier(UUID.fromString("213f9ca3-cc6f-309e-920b-1885d1d2b3c8")) != null) emit(actor, "totem_empower");
            }
            case "deep_repair" -> {
                if (run.event instanceof LivingDeathEvent death && death.getSource().getEntity() == actor && death.getEntity().isDeadOrDying()
                        && actor.getMainHandItem() == run.main && id(run.mainBefore).startsWith("skarrier_mobs:deep_alloy_")
                        && run.main.getDamageValue() < run.mainBefore.getDamageValue()) killed(death.getEntity(), actor, "deep_alloy");
            }
            case "deep_work" -> {
                Works works = WORKS.get();
                if (actor.totalExperience > run.xp && works.overflow == 0 && !works.calls.isEmpty() && works.calls.peek().actor == actor)
                    works.calls.peek().experience = true;
            }
            case "deep_sonic" -> { if (run.damage) emit(actor, "deep_alloy"); }
            case "impact" -> {
                if (run.damage && run.event instanceof net.minecraftforge.event.entity.living.LivingFallEvent) {
                    Falls falls = FALLS.get();
                    if (falls.overflow == 0 && !falls.calls.isEmpty() && falls.calls.peek().actor == actor) falls.calls.peek().shockwave = true;
                } else if (run.damage && actor.isAlive()) emit(actor, "impact_gear");
            }
            case "serraxe", "barrier_hit" -> { if (run.effect) emit(actor, "serrated_defense"); }
            case "tranquil" -> { if (run.effect && actor.getCooldowns().isOnCooldown(run.itemBefore.getItem())) emit(actor, "tranquil_food"); }
            case "leeching" -> {
                if (run.effect && id(run.itemBefore).equals("skarrier_mobs:leeching_sack") && run.itemBefore.getCount() - run.item.getCount() == 1) emit(actor, "tranquil_food");
            }
            case "crown" -> { if (run.effect) emit(actor, "infernal_crown"); }
            case "quake_feed" -> { if (run.target != null) quakeFed(run.target, actor, run.itemBefore, run.item); }
            case "quake_request" -> quakeRequested(actor, run.quakeCooldown);
            case "quake_stomp" -> { if (run.damage) emit(actor, "quake_ride_stomp"); }
            default -> { }
        }
    }
    private static void emit(ServerPlayer actor, String action) { QuestActionTrigger.INSTANCE.emit(actor, "v3_combat_" + action); }
    private static void shieldCommitted(ServerPlayer actor, ShieldBlockEvent event, String action) {
        com.justenoughmods.achievementguide.integration.combattools.CombatDeferredProof.defer(event, () -> {
            if (event.getEntity() == actor && Float.isFinite(event.getBlockedDamage()) && event.getBlockedDamage() > 0) emit(actor, action);
        });
    }

    public static void effect(LivingEntity target, MobEffectInstance effect, boolean accepted) {
        Run run = current();
        if (run == null || !run.valid || !accepted || effect.getDuration() <= 0 || target.level().isClientSide) return;
        MobEffectInstance actual = target.getEffect(effect.getEffect());
        if (actual == null || actual.getDuration() <= 0) return;
        String key = effectId(effect);
        boolean same = target == run.target;
        run.effect |= switch (run.route) {
            case "freezing_attack" -> same && id(run.offBefore).equals("skarrier_mobs:totem_of_freezing") && key.equals("skarrier_mobs:frostbite");
            case "seething" -> target != run.actor && id(run.offBefore).equals("skarrier_mobs:totem_of_seething") && key.equals("skarrier_mobs:fire_hex");
            case "empower" -> target == run.actor && key.equals("skarrier_mobs:semi_living_flesh_penalty_effect");
            case "serraxe" -> same && id(run.mainBefore).equals("skarrier_mobs:serraxe") && key.equals("skarrier_mobs:hemorrhage");
            case "barrier_hit" -> same && key.equals("skarrier_mobs:armor_crumbling");
            case "tranquil" -> target == run.actor && id(run.itemBefore).equals("skarrier_mobs:heart_of_tranquility") && key.equals("skarrier_mobs:tranquility");
            case "leeching" -> target == run.actor && key.equals("skarrier_mobs:leeching_touch");
            case "crown" -> target == run.actor && id(run.actor.getItemBySlot(EquipmentSlot.HEAD)).equals("skarrier_mobs:infernal_crown_helmet")
                    && key.equals("skarrier_mobs:infernal_crown_damage_bonus");
            default -> false;
        };
    }
    public static void beginUse(Entity actor, ItemStack stack) {
        begin(id(stack).equals("skarrier_mobs:heart_of_tranquility") ? "tranquil" : "leeching", actor, actor, null, stack);
    }
    public static void beginBlock(String route, Entity owner, Event event, ItemStack item, BlockPos pos) {
        begin(route, owner, null, event, item);
        Run run = current();
        if (run != null && run.valid) { run.pos = pos.immutable(); run.block = owner.level().getBlockState(pos); }
    }
    public static int inventoryCount(Player player, ItemStack stack) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack actual = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, actual)) count += actual.getCount();
        }
        return count;
    }
    public static int knownOutput(CompoundTag proof, ItemStack stack) {
        return proof.hasUUID("actor") && proof.contains("output") && ItemStack.isSameItemSameTags(ItemStack.of(proof.getCompound("output")), stack)
                ? Math.max(0, Math.min(proof.getInt("known"), stack.getCount())) : 0;
    }
    public static void outputProof(ItemEntity output, CompoundTag proof, int known) {
        if (known <= 0 || output.getItem().isEmpty()) { output.getPersistentData().remove(OUTPUT); return; }
        CompoundTag copy = proof.copy(); copy.putInt("known", Math.min(known, output.getItem().getCount()));
        copy.put("output", output.getItem().save(new CompoundTag())); output.getPersistentData().put(OUTPUT, copy);
    }
    public static void pickedUp(ItemEntity output, Player actor, ItemStack before, int inventoryBefore, CompoundTag proof) {
        if (before.isEmpty() || actor.level().isClientSide) return;
        int taken = before.getCount() - output.getItem().getCount(), known = knownOutput(proof, before);
        if (taken <= 0 || inventoryCount(actor, before) - inventoryBefore < taken) return;
        int certified = Math.max(0, taken - (before.getCount() - known));
        outputProof(output, proof, known - certified);
        String action = proof.getString("action");
        if (real(actor) && certified > 0 && proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor"))
                && (action.equals("brush_shell") || action.equals("ore_acid"))) emit((ServerPlayer)actor, action);
    }
    public static void mergeBegin(ItemEntity destination, ItemEntity source) {
        Merges merges = MERGES.get();
        if (merges.overflow > 0 || merges.calls.size() >= LIMIT) { merges.overflow++; return; }
        merges.calls.push(new Merge(destination, source, destination.getItem().copy(), source.getItem().copy(),
                destination.getPersistentData().getCompound(OUTPUT).copy(), source.getPersistentData().getCompound(OUTPUT).copy()));
    }
    public static void mergeEnd(ItemEntity destination, ItemEntity source) {
        Merges merges = MERGES.get();
        if (merges.overflow > 0) { merges.overflow--; return; }
        Merge merge = merges.calls.isEmpty() ? null : merges.calls.pop();
        if (merges.calls.isEmpty()) MERGES.remove();
        if (merge == null || merge.destination != destination || merge.source != source) return;
        int transferred = merge.from.getCount() - source.getItem().getCount();
        if (transferred <= 0 || destination.getItem().getCount() - merge.to.getCount() != transferred) return;
        int toKnown = knownOutput(merge.toProof, merge.to), fromKnown = knownOutput(merge.fromProof, merge.from);
        int certified = Math.max(0, transferred - (merge.from.getCount() - fromKnown));
        boolean conflict = certified > 0 && toKnown > 0 && (!merge.toProof.getString("action").equals(merge.fromProof.getString("action"))
                || !merge.toProof.getUUID("actor").equals(merge.fromProof.getUUID("actor")));
        outputProof(destination, toKnown > 0 ? merge.toProof : merge.fromProof, conflict ? 0 : toKnown + certified);
        outputProof(source, merge.fromProof, fromKnown - certified);
    }
    public static float vitality(Entity target) { return target instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : Float.NaN; }
    public static void damage(Entity target, boolean accepted, float before) {
        Run run = current();
        if (run != null && run.valid && target != run.actor && accepted && Float.isFinite(before) && vitality(target) < before) run.damage = true;
    }
    public static void spawned(Entity entity, boolean accepted) {
        Run run = current();
        if (run == null || !run.valid || !accepted || entity.level() != run.actor.level()) return;
        if (entity instanceof ItemEntity item && !item.getItem().isEmpty() && run.outputs.size() < LIMIT) {
            run.output = true; run.outputs.add(item);
        }
        if (run.route.equals("barrier_launch") && entity instanceof Projectile projectile
                && projectile.getOwner() == run.actor && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals("skarrier_mobs:wretched_tooth_cleaving")) {
            if (run.event instanceof ShieldBlockEvent shield) com.justenoughmods.achievementguide.integration.combattools.CombatDeferredProof.defer(shield, () -> {
                if (!entity.isRemoved() && projectile.getOwner() == run.actor && entity.level() == run.actor.level()
                        && Float.isFinite(shield.getBlockedDamage()) && shield.getBlockedDamage() > 0) {
                    CompoundTag proof = new CompoundTag(); proof.putUUID("actor", run.actor.getUUID());
                    entity.getPersistentData().put(FANG, proof);
                }
            });
        }
    }
    public static Entity barrierOwner(Entity projectile) {
        if (!(projectile instanceof Projectile shot) || !(shot.getOwner() instanceof ServerPlayer actor)) return null;
        CompoundTag proof = projectile.getPersistentData().getCompound(FANG);
        return proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor")) ? actor : null;
    }
    public static void deathBegin(LivingEntity target, boolean dead) {
        Deaths deaths = DEATHS.get();
        if (deaths.overflow > 0 || deaths.calls.size() >= LIMIT) { deaths.overflow++; return; }
        deaths.calls.push(new Death(target, dead, new ArrayList<>()));
    }
    private static void killed(LivingEntity target, ServerPlayer actor, String action) {
        Deaths deaths = DEATHS.get();
        if (deaths.overflow == 0 && !deaths.calls.isEmpty() && deaths.calls.peek().target == target && deaths.calls.peek().kills.size() < LIMIT)
            deaths.calls.peek().kills.add(new Kill(actor, action));
    }
    public static void deathEnd(LivingEntity target, boolean dead) {
        Deaths deaths = DEATHS.get();
        if (deaths.overflow > 0) { deaths.overflow--; return; }
        Death death = deaths.calls.isEmpty() ? null : deaths.calls.pop();
        if (deaths.calls.isEmpty()) DEATHS.remove();
        if (death != null && death.target == target && !death.deadBefore && dead)
            for (Kill kill : death.kills) emit(kill.actor, kill.action);
    }
    public static void fallBegin(LivingEntity actor) {
        Falls falls = FALLS.get();
        if (falls.overflow > 0 || falls.calls.size() >= LIMIT) { falls.overflow++; return; }
        falls.calls.push(new Fall(actor));
    }
    public static void fallEnd(LivingEntity actor) {
        Falls falls = FALLS.get();
        if (falls.overflow > 0) { falls.overflow--; return; }
        Fall fall = falls.calls.isEmpty() ? null : falls.calls.pop();
        if (falls.calls.isEmpty()) FALLS.remove();
        if (fall != null && fall.actor == actor && fall.shockwave && actor.isAlive() && real(actor)) emit((ServerPlayer)actor, "impact_gear");
    }
    public static void workBegin(Entity actor, BlockPos pos) {
        Works works = WORKS.get();
        if (works.overflow > 0 || works.calls.size() >= LIMIT) { works.overflow++; return; }
        works.calls.push(new Work(real(actor) ? (ServerPlayer)actor : null, pos));
    }
    public static void workEnd(boolean success) {
        Works works = WORKS.get();
        if (works.overflow > 0) { works.overflow--; return; }
        Work work = works.calls.isEmpty() ? null : works.calls.pop();
        if (works.calls.isEmpty()) WORKS.remove();
        if (work != null && work.actor != null && work.experience && success
                && !work.before.equals(work.actor.level().getBlockState(work.pos))) emit(work.actor, "deep_alloy");
    }
    public static void hurtBegin(LivingEntity target) {
        Hurts state = HURTS.get();
        if (state.overflow > 0 || state.calls.size() >= LIMIT) { state.overflow++; return; }
        state.calls.push(new Hurt(target, vitality(target), new ArrayList<>()));
    }
    public static void hurtEnd(LivingEntity target, boolean accepted) {
        Hurts state = HURTS.get();
        if (state.overflow > 0) { state.overflow--; return; }
        Hurt hurt = state.calls.isEmpty() ? null : state.calls.pop();
        if (state.calls.isEmpty()) HURTS.remove();
        if (hurt != null && hurt.target == target && accepted && vitality(target) < hurt.vitality)
            for (ServerPlayer actor : hurt.actors) emit(actor, "coat_fire");
    }
    public static void quakeFed(Entity mount, Player player, ItemStack before, ItemStack actual) {
        if (!real(player) || !before.is(Blocks.CACTUS.asItem()) || before.getCount() - actual.getCount() != 1
                || !(mount instanceof TamableAnimal tame) || !tame.isOwnedBy(player)) return;
        CompoundTag proof = new CompoundTag(); proof.putUUID("actor", player.getUUID());
        mount.getPersistentData().put(QUAKE, proof);
        emit((ServerPlayer)player, "quake_ride_fed");
    }
    public static Entity quakeActor(Entity mount) {
        if (!(mount instanceof TamableAnimal tame) || !(mount.getFirstPassenger() instanceof ServerPlayer actor)
                || !real(actor) || !tame.isOwnedBy(actor)) return null;
        CompoundTag proof = mount.getPersistentData().getCompound(QUAKE);
        return proof.hasUUID("actor") && actor.getUUID().equals(proof.getUUID("actor")) ? actor : null;
    }
    public static void quakeMounted(Entity mount, Entity actor) {
        if (quakeActor(mount) == actor) emit((ServerPlayer)actor, "quake_ride_mounted");
    }
    public static void quakeRequested(Entity actor, double before) {
        Entity mount = actor == null ? null : actor.getVehicle();
        if (mount == null || quakeActor(mount) != actor || before != 0 || mount.getPersistentData().getDouble("QuakeCooldown") <= 0
                || !mount.getPersistentData().getBoolean("QuakeAttacking")) return;
        mount.getPersistentData().getCompound(QUAKE).putLong("request", mount.level().getGameTime());
    }
    public static Entity quakeStomper(Entity mount) {
        Entity actor = quakeActor(mount);
        if (actor == null) return null;
        CompoundTag proof = mount.getPersistentData().getCompound(QUAKE);
        long elapsed = mount.level().getGameTime() - proof.getLong("request");
        return proof.contains("request") && elapsed >= 0 && elapsed <= 100 && mount.getPersistentData().getBoolean("QuakeAttacking") ? actor : null;
    }
}
