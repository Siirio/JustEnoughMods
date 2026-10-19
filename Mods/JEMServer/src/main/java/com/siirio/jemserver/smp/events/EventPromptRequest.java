package com.siirio.jemserver.smp.events;

import java.util.UUID;
import com.siirio.jemserver.smp.BossHostingPrompt;
import com.siirio.jemserver.smp.SmpActionFailure;
import com.siirio.jemserver.smp.SmpEnvironment;
import com.siirio.jemserver.smp.SmpRequests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** A chat link requests presentation only; admission/start remain with the hosted encounter owner. */
public record EventPromptRequest(Kind kind, UUID id) {
    public enum Kind { ARENA, BOSS }
    private static final String PREFIX = "/jem-ui ";

    public ClickEvent clickEvent() {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, PREFIX + kind.name().toLowerCase(java.util.Locale.ROOT) + " " + id);
    }

    public static EventPromptRequest fromClick(ClickEvent click) {
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND) return null;
        String value = click.getValue();
        // Old scrollback links use the same validated GUI path, never removed public commands.
        String payload = value.startsWith(PREFIX) ? value.substring(PREFIX.length())
                : value.startsWith("/smp arena ") || value.startsWith("/smp boss ") ? value.substring(5) : null;
        if (payload == null) return null;
        String[] parts = payload.split(" ", -1);
        if (parts.length != 2) return null;
        try { return new EventPromptRequest(Kind.valueOf(parts[0].toUpperCase(java.util.Locale.ROOT)), UUID.fromString(parts[1])); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    static void encode(EventPromptRequest request, FriendlyByteBuf buffer) {
        buffer.writeEnum(request.kind());
        buffer.writeUUID(request.id());
    }

    static EventPromptRequest decode(FriendlyByteBuf buffer) {
        return new EventPromptRequest(buffer.readEnum(Kind.class), buffer.readUUID());
    }

    void open(ServerPlayer player) {
        if (!SmpEnvironment.active(player) || !SmpRequests.allow(player)) return;
        try {
            switch (kind) {
                case ARENA -> StructureStaging.prompt(player, id);
                case BOSS -> BossHostingPrompt.prompt(player, id);
            }
        } catch (SmpActionFailure failure) {
            player.sendSystemMessage(Component.translatable("jem.smp.error." + failure.code()));
        }
    }
}
