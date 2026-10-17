package com.siirio.jemprofessions.client;

import com.siirio.jemprofessions.sandbox.SandboxProfessionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class SandboxSkillText {
    private SandboxSkillText() {}
    static boolean russian() { return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru"); }
    static Component text(String ru, String en) { return Component.literal(russian()?ru:en); }
    static Component field(String node,String key) {
        var description=SandboxProfessionPolicy.description(node);
        return Component.literal(description.get(key+(russian()?"_ru":"_en")).getAsString());
    }
    static Component name(String node) { return field(node,"name"); }
    static boolean passive(String node) { return SandboxProfessionPolicy.description(node).get("passive").getAsBoolean(); }
}
