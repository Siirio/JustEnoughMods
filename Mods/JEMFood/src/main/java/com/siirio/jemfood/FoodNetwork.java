package com.siirio.jemfood;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.List;

/** Bounded S2C presentation only. There is no client gameplay mutation packet. */
public final class FoodNetwork {
    public record Detail(String trait, double power, String effect, int remaining, int cooldown, boolean waiting) {}
    public record Snapshot(int variety, String dish, int tier, int remaining, List<Detail> traits) {}
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(JEMFood.ID, "food"))
            .networkProtocolVersion(() -> "2").clientAcceptedVersions("2"::equals).serverAcceptedVersions("2"::equals).simpleChannel();
    private FoodNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.variety()); buffer.writeUtf(packet.dish(), 256);
                    buffer.writeVarInt(packet.tier()); buffer.writeVarInt(packet.remaining());
                    buffer.writeVarInt(packet.traits().size());
                    for (var detail : packet.traits()) {
                        buffer.writeUtf(detail.trait(), 32); buffer.writeDouble(detail.power());
                        buffer.writeUtf(detail.effect(), 256); buffer.writeVarInt(detail.remaining());
                        buffer.writeVarInt(detail.cooldown()); buffer.writeBoolean(detail.waiting());
                    }
                })
                .decoder(buffer -> {
                    int count = Math.max(0, Math.min(5, buffer.readVarInt()));
                    String dish = buffer.readUtf(256);
                    int tier = Math.max(0, Math.min(3, buffer.readVarInt()));
                    int remaining = Math.max(0, Math.min(4800, buffer.readVarInt()));
                    int size = buffer.readVarInt();
                    if (size < 0 || size > 4) throw new IllegalArgumentException("Invalid food trait count");
                    var details = new java.util.ArrayList<Detail>();
                    for (int i = 0; i < size; i++) details.add(new Detail(buffer.readUtf(32), buffer.readDouble(),
                            buffer.readUtf(256), Math.max(0, buffer.readVarInt()), Math.max(0, buffer.readVarInt()), buffer.readBoolean()));
                    return new Snapshot(count, dish, tier, remaining, List.copyOf(details));
                })
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> com.siirio.jemfood.client.FoodTooltips.receive(packet)))
                .add();
    }
    static Snapshot snapshot(ServerPlayer player, FoodState state) {
        var dish = state.active(player);
        if (dish == null) return new Snapshot(Math.min(5, state.meals.size()), "", 0, 0, List.of());
        long now = FoodState.now(player);
        int remaining = (int) Math.max(0, state.expires - now);
        var details = new java.util.ArrayList<>(dish.traits().stream().map(name -> {
            var trait = FoodCatalog.trait(name);
            int duration = remaining;
            String effectId = "";
            boolean waiting = false;
            if (trait.bridge()) {
                effectId = trait.fallback().toString();
                var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(trait.fallback());
                duration = FoodState.owns(player, effect) ? player.getEffect(effect).getDuration() : 0;
                waiting = player.hasEffect(effect) && !FoodState.owns(player, effect);
            }
            if (name.equals("secondwind")) duration = (int) Math.max(0, state.windUntil - now);
            double power = name.equals("secondwind") && duration > 0 ? state.windPower : trait.value(dish.tier());
            return new Detail(name, power, effectId, duration,
                    (int) Math.max(0, state.cooldowns.getOrDefault(name, 0L) - now), waiting);
        }).toList());
        if (state.windUntil > now && !dish.traits().contains("secondwind"))
            details.add(new Detail("secondwind", state.windPower, "", (int) (state.windUntil - now),
                    (int) Math.max(0, state.cooldowns.getOrDefault("secondwind", 0L) - now), false));
        return new Snapshot(Math.min(5, state.meals.size()), dish.id().toString(), dish.tier(), remaining, List.copyOf(details));
    }
    static void send(ServerPlayer player, Snapshot snapshot) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot);
    }
}
