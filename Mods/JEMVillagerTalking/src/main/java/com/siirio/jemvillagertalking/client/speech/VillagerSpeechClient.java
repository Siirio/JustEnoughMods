package com.siirio.jemvillagertalking.client.speech;

import net.minecraftforge.common.MinecraftForge;

public final class VillagerSpeechClient {
    private VillagerSpeechClient() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientVillagerSpeech.class);
    }
}
