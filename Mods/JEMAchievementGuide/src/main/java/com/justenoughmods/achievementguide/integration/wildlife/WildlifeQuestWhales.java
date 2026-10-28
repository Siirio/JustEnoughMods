package com.justenoughmods.achievementguide.integration.wildlife;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;
public final class WildlifeQuestWhales {
    private WildlifeQuestWhales() {}
    public static boolean attacked(LivingEntity whale, UUID actor) {
        var data = WildlifeQuestProof.data(whale); if (data.getBoolean("rescue_attackers_overflow")) return true;
        for (Tag value : data.getList("rescue_attackers", 11)) if (NbtUtils.loadUUID(value).equals(actor)) return true; return false;
    }
    public static void attacker(LivingEntity whale, UUID actor) {
        if (attacked(whale, actor)) return; var data = WildlifeQuestProof.data(whale); var actors = data.getList("rescue_attackers", 11);
        if (actors.size() >= 128) data.putBoolean("rescue_attackers_overflow", true);
        else { actors.add(NbtUtils.createUUID(actor)); data.put("rescue_attackers", actors); }
    }
}
