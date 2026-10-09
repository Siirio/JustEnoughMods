package com.siirio.jemserver.smp.compat;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "jem_server", value = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
public final class PhantomArmorAbility {
    private static final String ACTIVE_UNTIL = "jem_phantom_active_until";
    private static final String COOLDOWN_UNTIL = "jem_phantom_cooldown_until";
    private static final String PLAYER_ACTIVE_UNTIL = "jem_phantom_player_active_until";
    private static final int ACTIVE_TICKS = 200;
    private static final int COOLDOWN_TICKS = 600;
    private static final List<EquipmentSlot> SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final List<String> PARTS = List.of("helmet", "chestplate", "leggings", "boots");

    public static boolean handle(Player player, int type) {
        if (type != 0 || !wearsPhantomPiece(player)) return false;
        if (player.level().isClientSide) return true;
        long now = player.level().getGameTime();
        if (!wearsCompleteState(player, "") || SLOTS.stream().map(player::getItemBySlot).anyMatch(stack -> stack.getOrCreateTag().getLong(COOLDOWN_UNTIL) > now)) return true;
        for (int index = 0; index < SLOTS.size(); index++) {
            ItemStack stack = player.getItemBySlot(SLOTS.get(index));
            stack.getOrCreateTag().putLong(ACTIVE_UNTIL, now + ACTIVE_TICKS);
            stack.getOrCreateTag().putLong(COOLDOWN_UNTIL, now + COOLDOWN_TICKS);
            player.setItemSlot(SLOTS.get(index), convert(stack, "invisible_"));
        }
        player.getPersistentData().putLong(PLAYER_ACTIVE_UNTIL, now + ACTIVE_TICKS);
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ACTIVE_TICKS, 1, false, false));
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        long now = event.player.level().getGameTime();
        boolean recoveredLegacyState = false;
        for (int slot = 0; slot < event.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = event.player.getInventory().getItem(slot);
            if (!isPhantom(stack)) continue;
            long activeUntil = stack.getOrCreateTag().getLong(ACTIVE_UNTIL);
            long cooldownUntil = stack.getOrCreateTag().getLong(COOLDOWN_UNTIL);
            recoveredLegacyState |= (activeUntil == 0 || cooldownUntil == 0) && !isBase(stack);
            String state = activeUntil == 0 || cooldownUntil == 0 || now >= cooldownUntil ? "" : now < activeUntil ? "invisible_" : "charge_";
            ItemStack converted = convert(stack, state);
            if (state.isEmpty()) {
                converted.getOrCreateTag().remove(ACTIVE_UNTIL);
                converted.getOrCreateTag().remove(COOLDOWN_UNTIL);
            }
            if (converted != stack) event.player.getInventory().setItem(slot, converted);
        }
        var data = event.player.getPersistentData();
        if (!data.contains(PLAYER_ACTIVE_UNTIL)) {
            if (recoveredLegacyState && !event.player.isCreative() && !event.player.isSpectator()) {
                event.player.getAbilities().invulnerable = false;
                event.player.onUpdateAbilities();
            }
            return;
        }
        if (now < data.getLong(PLAYER_ACTIVE_UNTIL)) {
            event.player.getAbilities().invulnerable = true;
            return;
        }
        data.remove(PLAYER_ACTIVE_UNTIL);
        if (!event.player.isCreative() && !event.player.isSpectator()) {
            event.player.getAbilities().invulnerable = false;
            event.player.onUpdateAbilities();
        }
    }

    private static boolean wearsPhantomPiece(Player player) {
        return SLOTS.stream().map(player::getItemBySlot).anyMatch(PhantomArmorAbility::isPhantom);
    }

    private static boolean wearsCompleteState(Player player, String state) {
        for (int index = 0; index < SLOTS.size(); index++) if (!id(player.getItemBySlot(SLOTS.get(index))).equals(id(state, PARTS.get(index)))) return false;
        return true;
    }

    private static boolean isPhantom(ItemStack stack) {
        String path = id(stack).getPath();
        return id(stack).getNamespace().equals("deep_dark_regrowth") && path.startsWith("phantom_armor_");
    }

    private static boolean isBase(ItemStack stack) {
        ResourceLocation stackId = id(stack);
        return PARTS.stream().anyMatch(part -> stackId.equals(id("", part)));
    }

    private static ItemStack convert(ItemStack source, String state) {
        ResourceLocation sourceId = id(source);
        if (!sourceId.getNamespace().equals("deep_dark_regrowth")) return source;
        String part = PARTS.stream().filter(sourceId.getPath()::endsWith).findFirst().orElse(null);
        if (part == null) return source;
        Item target = ForgeRegistries.ITEMS.getValue(id(state, part));
        if (target == null || source.getItem() == target) return source;
        ItemStack result = new ItemStack(target, source.getCount());
        if (source.hasTag()) result.setTag(source.getTag().copy());
        return result;
    }

    private static ResourceLocation id(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    private static ResourceLocation id(String state, String part) {
        return new ResourceLocation("deep_dark_regrowth", "phantom_armor_" + state + part);
    }

    private PhantomArmorAbility() {}
}
