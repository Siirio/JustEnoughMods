package com.siirio.jemprofessions.sandbox.nativecompat;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Uses Iron's pre-debit cast event and native, synchronized mana/attribute owners. */
public final class SandboxMagicEffects {
    private static final UUID SCHOOL = uuid("school");
    private static final UUID MEDITATION = uuid("meditation");
    private static final UUID CIRCLE_REGEN = uuid("circle_regen");
    private static final Map<ServerPlayer, Casting> CASTING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Circle> CIRCLES = new WeakHashMap<>();
    private static final Map<ServerPlayer, MobEffectInstance> SANCTUARY_EFFECTS = new WeakHashMap<>();

    private SandboxMagicEffects() {}
    static void register() { MinecraftForge.EVENT_BUS.register(SandboxMagicEffects.class); }
    private static boolean has(ServerPlayer p, String node) { return SandboxProfessionSupport.enabled(p, ProfessionId.MAGE, node); }
    private static UUID uuid(String suffix) { return UUID.nameUUIDFromBytes(("jem:mage:" + suffix).getBytes(StandardCharsets.UTF_8)); }

    static boolean activate(ServerPlayer player, String node, CompoundTag input) {
        if (node.equals("magic_school") || node.equals("school_master") || node.equals("arcane_specialist")) {
            if (!has(player, node)) return false;
            String option = input.getString("Option");
            ResourceLocation id = ResourceLocation.tryParse(option.contains(":") ? option : "irons_spellbooks:" + option);
            SchoolType school = id == null ? null : SchoolRegistry.getSchool(id);
            if (school == null || power(school) == null) {
                SandboxProfessionSupport.message(player, "Выбери настоящую школу магии.", "Choose a registered magic school.");
                return true;
            }
            SandboxNativeEffects.state(player).putString("School", id.toString());
            reconcileSchool(player);
            SandboxProfessionSupport.message(player, "Школа выбрана: " + school.getDisplayName().getString(), "Selected school: " + school.getDisplayName().getString());
            return true;
        }
        if (!node.equals("magic_circle") && !node.equals("sanctuary")) return false;
        if (!has(player, node) || !player.isAlive()) return false;
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < 50) {
            SandboxProfessionSupport.message(player, "Для круга нужно 50 маны.", "The circle needs 50 mana.");
            return true;
        }
        if (SandboxProfessionSupport.record(player).cooldown("mage:circle") > player.serverLevel().getGameTime()) {
            SandboxProfessionSupport.message(player, "Магический круг ещё восстанавливается.", "Magic Circle is still on cooldown.");
            return true;
        }
        if (!debit(player, magic, 50)) {
            SandboxProfessionSupport.message(player, "Расход маны отменён правилами магии.", "Native magic rules canceled the mana payment.");
            return true;
        }
        SandboxProfessionSupport.cooldown(player, "mage:circle", 1200);
        CIRCLES.put(player, new Circle(player.level().dimension().location(), player.position(), player.serverLevel().getGameTime() + 160));
        SandboxProfessionSupport.message(player, "Круг действует 8 секунд в радиусе 6 блоков.", "Circle active for 8 seconds within 6 blocks.");
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SandboxProfessionSupport.allowed(player)) return;
        long now = player.serverLevel().getGameTime();
        Casting state = CASTING.computeIfAbsent(player, p -> new Casting(p.position(), now));
        state.lastActivity = now;
        ResourceLocation spell = ResourceLocation.tryParse(event.getSpellId());
        boolean paid = event.getCastSource().consumesMana() && event.getManaCost() > 0
                && !MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(event.getSpellId());
        if (paid) {
            float multiplier = has(player, "mana_saver") ? 0.95F : 1F;
            if (has(player, "combo_magic") && state.comboUntil >= now && state.comboUntil != 0) {
                multiplier *= 0.80F;
            }
            event.setManaCost(Math.max(1, Math.round(event.getManaCost() * multiplier)));
        }
        while (!state.pending.isEmpty() && state.pending.peekFirst().tick != now) state.pending.removeFirst();
        if (state.pending.size() >= 8) state.pending.removeFirst();
        state.pending.addLast(new Pending(event, MagicData.getPlayerMagicData(player).getMana(), now,
                paid, has(player, "combo_magic") && state.comboUntil >= now && state.comboUntil != 0));
    }

    /** The public cast event precedes a cancelable debit; observe its result at the exact native onCast seam. */
    public static void beforeNativeCast(ServerPlayer player, String spellId) {
        Casting state = CASTING.get(player);
        Pending pending = state == null ? null : state.pending.peekLast();
        if (pending == null || !pending.event.getSpellId().equals(spellId)) return;
        pending.confirmed = pending.paid && pending.event.getManaCost() > 0
                && MagicData.getPlayerMagicData(player).getMana() <= pending.before - pending.event.getManaCost();
        if (pending.confirmed && pending.combo) state.comboUntil = 0;
    }

    public static void afterNativeCast(ServerPlayer player, String spellId) {
        Casting state = CASTING.get(player);
        Pending pending = state == null ? null : state.pending.peekLast();
        if (pending == null || !pending.event.getSpellId().equals(spellId)) return;
        state.pending.removeLast();
        SpellOnCastEvent event = pending.event;
        ResourceLocation spell = ResourceLocation.tryParse(spellId);
        long now = player.serverLevel().getGameTime();
        if (pending.confirmed && SandboxNativeEffects.eligible(player) && spell != null && event.getCastSource() != CastSource.COMMAND) {
            SandboxProfessionSupport.credit(player, ProfessionId.MAGE, SourceKind.SPELL_CAST, spell,
                    Math.max(1, event.getSpellLevel()), "mage:casts", 300,
                    "mage:cast:" + now + ":" + spell + ":" + event.getCastSource());
            if (has(player, "combo_magic")) {
                while (!state.recent.isEmpty() && now - state.recent.peekFirst().tick > 200) state.recent.removeFirst();
                state.recent.removeIf(row -> row.spell.equals(spell));
                state.recent.addLast(new Cast(spell, now));
                if (state.recent.size() >= 3) {
                    state.recent.clear();
                    state.comboUntil = now + 100;
                    SandboxProfessionSupport.message(player, "Комбинация: следующее заклинание за 5 секунд дешевле на 20%.", "Combo: your next spell within 5 seconds costs 20% less.");
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void guard(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SandboxProfessionSupport.allowed(player)
                || event.getAmount() <= 0 || !has(player, "arcane_guard")) return;
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < 25) return;
        if (!debit(player, magic, 25)) return;
        event.setAmount(Math.max(0, event.getAmount() - 2));
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 8, .3, .5, .3, .05);
    }

    static void tick(ServerPlayer player) {
        if (player.getServer().isDedicatedServer() || !player.isAlive()) {
            CIRCLES.remove(player);
            CASTING.remove(player);
            removeAttributes(player);
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (SandboxProfessionSupport.allowed(player)) {
            Casting state = CASTING.computeIfAbsent(player, p -> new Casting(p.position(), now));
            if (state.position.distanceToSqr(player.position()) > 0.0001 || MagicData.getPlayerMagicData(player).isCasting()) state.lastActivity = now;
            state.position = player.position();
            modifier(player.getAttribute(AttributeRegistry.MANA_REGEN.get()), MEDITATION,
                    has(player, "meditation") && now - state.lastActivity >= 200 ? .05 : 0);
            reconcileSchool(player);
            if (!has(player, "combo_magic")) { state.recent.clear(); state.comboUntil = 0; }
        }
        CIRCLES.entrySet().removeIf(entry -> entry.getKey().isRemoved() || !entry.getKey().isAlive()
                || entry.getKey().serverLevel().getGameTime() >= entry.getValue().until
                || !(has(entry.getKey(), "magic_circle") || has(entry.getKey(), "sanctuary")));
        boolean inside = false;
        boolean sanctuary = false;
        for (Map.Entry<ServerPlayer, Circle> entry : CIRCLES.entrySet()) {
                Circle circle = entry.getValue();
                ServerPlayer owner = entry.getKey();
                if (!circle.dimension.equals(player.level().dimension().location()) || circle.center.distanceToSqr(player.position()) > 36
                        || !(owner == player || owner.isAlliedTo(player))) continue;
                inside = true;
                sanctuary |= has(owner, "sanctuary");
        }
        if (player.tickCount % 10 == 0) {
            Circle own = CIRCLES.get(player);
            if (own != null) for (int i = 0; i < 32; i++) {
                double angle = i * Math.PI / 16;
                player.serverLevel().sendParticles(ParticleTypes.ENCHANT, own.center.x + Math.cos(angle) * 6,
                        own.center.y + .15, own.center.z + Math.sin(angle) * 6, 1, 0, .1, 0, .01);
            }
        }
        modifier(player.getAttribute(AttributeRegistry.MANA_REGEN.get()), CIRCLE_REGEN, inside ? .10 : 0);
        if (sanctuary && player.getEffect(MobEffects.REGENERATION) == null) {
            // Let the native effect complete its 50-tick cadence rather than resetting a short duration every tick.
            MobEffectInstance effect = new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true);
            if (player.addEffect(effect)) {
                SANCTUARY_EFFECTS.put(player, effect);
                SandboxNativeEffects.state(player).putBoolean("CircleRegeneration", true);
            }
        } else if (!sanctuary) removeSanctuary(player);
    }

    private static void reconcileSchool(ServerPlayer player) {
        String selected = SandboxNativeEffects.state(player).getString("School");
        double amount = has(player, "arcane_specialist") ? .16 : has(player, "school_master") ? .12 : has(player, "magic_school") ? .08 : 0;
        for (SchoolType school : SchoolRegistry.REGISTRY.get().getValues()) {
            Attribute attr = power(school);
            if (attr != null) modifier(player.getAttribute(attr), SCHOOL, school.getId().toString().equals(selected) ? amount : 0);
        }
    }

    private static Attribute power(SchoolType school) {
        return ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(school.getId().getNamespace(), school.getId().getPath() + "_spell_power"));
    }

    private static void removeAttributes(ServerPlayer player) {
        for (SchoolType school : SchoolRegistry.REGISTRY.get().getValues()) {
            Attribute attr = power(school);
            if (attr != null) modifier(player.getAttribute(attr), SCHOOL, 0);
        }
        modifier(player.getAttribute(AttributeRegistry.MANA_REGEN.get()), MEDITATION, 0);
        modifier(player.getAttribute(AttributeRegistry.MANA_REGEN.get()), CIRCLE_REGEN, 0);
        removeSanctuary(player);
    }

    static void logout(ServerPlayer player) {
        CIRCLES.remove(player);
        CASTING.remove(player);
        removeAttributes(player);
    }

    private static void modifier(AttributeInstance attr, UUID id, double amount) {
        if (attr == null) return;
        AttributeModifier current = attr.getModifier(id);
        if (current != null && current.getAmount() == amount && amount != 0) return;
        attr.removeModifier(id);
        if (amount != 0) attr.addTransientModifier(new AttributeModifier(id, "JEM sandbox mage", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static boolean debit(ServerPlayer player, MagicData magic, int amount) {
        float before = magic.getMana();
        magic.setMana(before - amount);
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return magic.getMana() <= before - amount;
    }

    private static void removeSanctuary(ServerPlayer player) {
        MobEffectInstance owned = SANCTUARY_EFFECTS.remove(player);
        MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
        boolean marked = SandboxNativeEffects.state(player).getBoolean("CircleRegeneration");
        if (current != null && (current == owned || owned == null && marked) && current.getAmplifier() == 0
                && current.getDuration() <= 50 && current.isAmbient()) player.removeEffect(MobEffects.REGENERATION);
        SandboxNativeEffects.state(player).remove("CircleRegeneration");
    }

    private static final class Casting {
        private Vec3 position;
        private long lastActivity;
        private long comboUntil;
        private final ArrayDeque<Cast> recent = new ArrayDeque<>();
        private final ArrayDeque<Pending> pending = new ArrayDeque<>();
        private Casting(Vec3 position, long now) { this.position = position; lastActivity = now; }
    }
    private static final class Pending {
        private final SpellOnCastEvent event;
        private final float before;
        private final long tick;
        private final boolean paid;
        private final boolean combo;
        private boolean confirmed;
        private Pending(SpellOnCastEvent event, float before, long tick, boolean paid, boolean combo) {
            this.event = event; this.before = before; this.tick = tick; this.paid = paid; this.combo = combo;
        }
    }
    private record Cast(ResourceLocation spell, long tick) {}
    private record Circle(ResourceLocation dimension, Vec3 center, long until) {}
}
