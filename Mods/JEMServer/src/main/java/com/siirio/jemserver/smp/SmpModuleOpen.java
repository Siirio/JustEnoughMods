package com.siirio.jemserver.smp;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record SmpModuleOpen(Module module) {
    public enum Module { ROLES, PROFESSIONS }

    static void encode(SmpModuleOpen packet, FriendlyByteBuf buffer) { buffer.writeEnum(packet.module); }
    static SmpModuleOpen decode(FriendlyByteBuf buffer) { return new SmpModuleOpen(buffer.readEnum(Module.class)); }

    void open(ServerPlayer player) {
        switch (module) {
            case ROLES -> com.siirio.jemroles.api.RaceApi.openScreen(player);
            case PROFESSIONS -> {
                if (net.minecraftforge.fml.ModList.get().isLoaded("jem_professions")) ProfessionsAdapter.open(player);
            }
        }
    }

    private static final class ProfessionsAdapter {
        private static void open(ServerPlayer player) {
            com.siirio.jemprofessions.api.ProfessionApi.openScreen(player);
        }
    }
}
