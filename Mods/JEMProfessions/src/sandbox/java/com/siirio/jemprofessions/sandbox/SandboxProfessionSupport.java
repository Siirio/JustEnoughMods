package com.siirio.jemprofessions.sandbox;

import com.siirio.jemidentity.api.IdentityPolicy;
import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.api.ProfessionProgressRequest;
import com.siirio.jemprofessions.progress.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/** Common bounded helpers, never a second XP or unlock owner. */
public final class SandboxProfessionSupport {
    private SandboxProfessionSupport() {}
    public static boolean allowed(ServerPlayer player) {
        return player != null && !(player instanceof FakePlayer) && player.getServer() != null
                && !player.getServer().isDedicatedServer() && IdentityPolicy.resolve(player).isPresent() && !player.isSpectator();
    }
    public static boolean owner(ServerPlayer player) { return allowed(player)&&player.getServer().isSingleplayerOwner(player.getGameProfile()); }
    public static boolean enabled(ServerPlayer player, ProfessionId profession, String node) {
        return allowed(player) && ProfessionApi.enabled(player, profession, node);
    }
    public static ProfessionRecord record(ServerPlayer player) {
        if (!allowed(player)) throw new IllegalArgumentException("Sandbox owner required");
        return ProfessionSavedData.get(player.getServer()).record(IdentityPolicy.resolve(player).orElseThrow().accountId());
    }
    public static boolean cooldown(ServerPlayer player, String key, long ticks) {
        if (!allowed(player) || key.length() > 128) return false;
        ProfessionRecord record = record(player);
        long now = player.serverLevel().getGameTime();
        if (record.cooldown(key) > now) return false;
        record.cooldown(key, now + Math.max(0L, Math.min(ticks, 20L * 86400L * 7L)));
        ProfessionSavedData.get(player.getServer()).changed();
        return true;
    }
    public enum CreditOutcome { ACCEPTED, DUPLICATE, CAPPED, REJECTED }
    public static CreditOutcome credit(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source,
                              int amount, String counter, int cap, String receipt) {
        return credit(player, profession, kind, source, amount, counter, cap, receipt, false);
    }
    public static CreditOutcome creditDurably(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source,
                              int amount, String counter, int cap, String receipt) {
        return credit(player, profession, kind, source, amount, counter, cap, receipt, true);
    }
    private static CreditOutcome credit(ServerPlayer player, ProfessionId profession, SourceKind kind, ResourceLocation source,
                              int amount, String counter, int cap, String receipt, boolean durable) {
        if (!allowed(player) || player.isCreative() || amount <= 0 || receipt == null || receipt.length() > 512) return CreditOutcome.REJECTED;
        SandboxCreditJournal.recover(player);
        if (ProfessionApi.category(profession).flatMap(net.puffish.skillsmod.api.Category::getExperience).isEmpty()) return CreditOutcome.REJECTED;
        UUID audit = UUID.nameUUIDFromBytes((profession + ":" + player.getUUID() + ":" + receipt).getBytes(StandardCharsets.UTF_8));
        ProfessionRecord record = record(player);
        if (record.hasAudit(audit)) {
            if (durable) SandboxCreditJournal.flushOwners(player);
            return CreditOutcome.DUPLICATE;
        }
        long day = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        int grant = counter == null || cap == 0 ? amount : record.reserveDaily(counter, day, amount, cap);
        ProfessionSavedData.get(player.getServer()).changed();
        if (grant <= 0) {
            if (durable) SandboxCreditJournal.flushOwners(player);
            return CreditOutcome.CAPPED;
        }
        if (durable) SandboxCreditJournal.prepare(player, profession, audit, grant, counter, day);
        var result=ProfessionApi.credit(player, new ProfessionProgressRequest(profession, kind, source, grant, audit));
        if (durable) SandboxCreditJournal.recover(player);
        return switch(result.status()) { case ACCEPTED -> CreditOutcome.ACCEPTED;case DUPLICATE -> CreditOutcome.DUPLICATE;default -> CreditOutcome.REJECTED; };
    }
    public static ResourceLocation id(String value) { return new ResourceLocation(value); }
    public static boolean boatBoostClientEnabled() {
        return net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT
                && com.siirio.jemprofessions.client.SandboxClientState.boatEligible();
    }
    public static void message(ServerPlayer player, String ru, String en) {
        if (player != null) player.displayClientMessage(Component.literal(player.getLanguage().startsWith("ru") ? ru : en), true);
    }
}
