package com.siirio.jemvillagertalking;

import com.siirio.jemvillagertalking.client.speech.VillagerSpeechClient;
import com.siirio.jemvillagertalking.speech.VillagerSpeechEvents;
import com.siirio.jemvillagertalking.speech.VillagerSpeechNetwork;
import com.siirio.jemvillagertalking.speech.VillagerReactionRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(JEMVillagerTalking.MOD_ID)
public final class JEMVillagerTalking {
    public static final String MOD_ID = "jem_villager_talking";

    public JEMVillagerTalking() {
        VillagerReactionRegistry.bootstrap();
        MinecraftForge.EVENT_BUS.register(VillagerSpeechEvents.class);
        VillagerSpeechNetwork.register();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VillagerSpeechClient::register);
    }
}
