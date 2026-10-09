package com.siirio.jempackcore.postend;

import com.siirio.jemtwelveeyes.JEMTwelveEyes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PostEndSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JEMTwelveEyes.MOD_ID);
    public static final RegistryObject<SoundEvent> SECRET_GLITCH = SOUNDS.register("secret_glitch", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(JEMTwelveEyes.MOD_ID, "secret_glitch")));
    private PostEndSounds() { }
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
