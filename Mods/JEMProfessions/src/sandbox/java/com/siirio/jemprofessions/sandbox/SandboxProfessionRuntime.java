package com.siirio.jemprofessions.sandbox;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.network.ProfessionsNetwork;
import com.siirio.jemprofessions.tree.ProfessionCatalog;
import com.siirio.jemprofessions.sandbox.core.SandboxCoreEffects;
import com.siirio.jemprofessions.sandbox.nativecompat.SandboxNativeEffects;
import com.siirio.jemprofessions.sandbox.civic.SandboxCivicEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.puffish.skillsmod.api.Skill;

/** Sandbox-only bounded input; every effect recomputes targets and validates its own native operation. */
public final class SandboxProfessionRuntime {
    private static SimpleChannel channel;
    private SandboxProfessionRuntime() {}
    public static void register() { MinecraftForge.EVENT_BUS.register(SandboxProfessionRuntime.class); }
    public static void registerPackets(SimpleChannel nativeChannel) {
        channel = nativeChannel;
        channel.messageBuilder(Request.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Request::encode).decoder(Request::decode).consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (SandboxProfessionSupport.allowed(player) && SandboxProfessionSupport.cooldown(player, "sandbox:request", 2)) handle(player, packet);
                    context.get().setPacketHandled(true);
                }).add();
        channel.messageBuilder(State.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(State::encode).decoder(State::decode).consumerMainThread((packet, context) -> {
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                            () -> () -> com.siirio.jemprofessions.client.SandboxClientState.accept(packet));
                    context.get().setPacketHandled(true);
                }).add();
        SandboxNativeEffects.registerPackets(channel);
    }
    public static void request(ProfessionId profession, String node, String action, CompoundTag input) {
        channel.sendToServer(new Request(profession, node, action, input));
    }
    private static void handle(ServerPlayer player, Request request) {
        String node = request.node(); ProfessionId profession = request.profession();
        if (request.action().equals("selected")) {
            String selected = SandboxPreferences.get(player).selected(player);
            for (ProfessionId candidate : ProfessionId.values()) if (ProfessionCatalog.node(candidate, selected) != null) {
                profession = candidate; node = selected; break;
            }
        }
        if (ProfessionCatalog.node(profession, node) == null) return;
        if ((request.action().equals("claim")||request.action().equals("unclaim"))&&!SandboxProfessionSupport.owner(player)) return;
        switch (request.action()) {
            case "claim" -> ProfessionApi.category(profession).ifPresent(category -> category.getSkill(request.node()).ifPresent(skill -> {
                if (skill.getState(player) != Skill.State.UNLOCKED) {
                    ResourceLocation source = testSource(request.node());
                    category.setPoints(player, source, 1);
                    SandboxSkillAdmission.freeClaim(player, request.profession(), request.node(), () -> skill.unlock(player));
                    if (skill.getState(player) != Skill.State.UNLOCKED) category.setPoints(player, source, 0);
                }
            }));
            case "unclaim" -> ProfessionApi.category(profession).ifPresent(category -> category.getSkill(request.node()).ifPresent(skill -> {
                if (skill.getState(player) == Skill.State.UNLOCKED) skill.lock(player);
                category.setPoints(player, testSource(request.node()), 0);
            }));
            case "select" -> {
                if (ProfessionApi.enabled(player, profession, node)) SandboxPreferences.get(player).selected(player, node);
            }
            case "activate", "selected", "configure" -> {
                if (!ProfessionApi.enabled(player, profession, node)) {
                    SandboxProfessionSupport.message(player, "Сначала изучи этот навык.", "Unlock this skill first."); break;
                }
                if (!SandboxCoreEffects.activate(player, node, request.input())
                        && !SandboxNativeEffects.activate(player, node, request.input()))
                    SandboxCivicEffects.activate(player, node, request.input());
            }
            case "customer" -> {
                if (node.equals("trading_post") && request.input().getString("Mode").equals("buy"))
                    SandboxCivicEffects.activate(player,node,request.input());
            }
            case "recovery" -> {
                String mode=request.input().getString("Mode");
                if (mode.equals("claim")||mode.equals("cancel")) SandboxCivicEffects.recover(player,request.input());
            }
            case "view" -> { }
            default -> { return; }
        }
        ProfessionsNetwork.sendStatus(player, false);
        sendState(player, profession, node);
    }
    public static ResourceLocation testSource(String node) { return new ResourceLocation("jem_professions", "sandbox/" + node); }
    private static void sendState(ServerPlayer player, ProfessionId profession, String node) {
        CompoundTag extra = SandboxCivicEffects.snapshot(player, node);
        if (extra == null) extra = new CompoundTag();
        net.minecraft.nbt.ListTag testClaims = new net.minecraft.nbt.ListTag();
        for (ProfessionId id : ProfessionId.values()) ProfessionApi.category(id).ifPresent(category -> {
            for (var skill : ProfessionCatalog.nodes(id)) if (category.getPoints(player, testSource(skill.id())) > 0)
                testClaims.add(net.minecraft.nbt.StringTag.valueOf(skill.id()));
        });
        extra.put("TestClaims", testClaims);
        extra.putBoolean("Owner",SandboxProfessionSupport.owner(player));
        channel.send(PacketDistributor.PLAYER.with(() -> player), new State(profession, node,
                SandboxPreferences.get(player).selected(player), extra));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SandboxProfessionSupport.allowed(player)) {
            SandboxCreditJournal.recover(player);
            SandboxSkillAdmission.legacy(player, () -> ProfessionApi.migrateLegacyProgress(player));
        }
    }
    @SubscribeEvent public static void sign(com.siirio.jemeconomy.sandbox.SandboxCommerce.ShopSignEvent event) {
        if (!SandboxProfessionSupport.allowed(event.player)) return;
        CompoundTag extra=SandboxCivicEffects.snapshot(event.player,"trading_post");
        extra.putString("OpenShop",event.listing.toString());
        channel.send(PacketDistributor.PLAYER.with(() -> event.player),new State(ProfessionId.MERCHANT,"trading_post",
                SandboxPreferences.get(event.player).selected(event.player),extra));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && SandboxProfessionSupport.allowed(player) && player.tickCount % 20 == 0) ProfessionsNetwork.sendStatus(player, false);
    }
    public record Request(ProfessionId profession, String node, String action, CompoundTag input) {
        static void encode(Request value, FriendlyByteBuf buf) {
            buf.writeEnum(value.profession); buf.writeUtf(value.node,64); buf.writeUtf(value.action,16); buf.writeNbt(value.input);
        }
        static Request decode(FriendlyByteBuf buf) {
            ProfessionId id=buf.readEnum(ProfessionId.class); String node=buf.readUtf(64), action=buf.readUtf(16);
            CompoundTag input=buf.readNbt(new NbtAccounter(8192L));
            return new Request(id,node,action,input == null ? new CompoundTag() : input);
        }
    }
    public record State(ProfessionId profession, String node, String selected, CompoundTag extra) {
        static void encode(State value, FriendlyByteBuf buf) {
            buf.writeEnum(value.profession); buf.writeUtf(value.node,64); buf.writeUtf(value.selected,64); buf.writeNbt(value.extra);
        }
        static State decode(FriendlyByteBuf buf) {
            ProfessionId id=buf.readEnum(ProfessionId.class); String node=buf.readUtf(64), selected=buf.readUtf(64);
            CompoundTag extra=buf.readNbt(new NbtAccounter(131072L));
            return new State(id,node,selected,extra == null ? new CompoundTag() : extra);
        }
    }
}
