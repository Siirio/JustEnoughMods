package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class WildlifeQuestWormDeathMixin implements WildlifeQuestDeathProof {
    @Shadow protected boolean dead;
    @Unique private DamageSource jem$source;
    @Unique private LivingDeathEvent jem$approval;
    @Unique private Set<UUID> jem$participants = Set.of();
    @Unique private boolean jem$oldDead;
    @Inject(method = "die", at = @At("HEAD"))
    private void jem$before(DamageSource source, CallbackInfo callback) { jem$source = source; jem$approval = null; jem$participants = Set.of(); jem$oldDead = dead; }
    public void jem$wildlifeApproved(LivingDeathEvent event, Set<UUID> participants) { if (event.getEntity() == (Object)this && event.getSource() == jem$source && !jem$oldDead) { jem$approval = event; jem$participants = Set.copyOf(participants); } }
    @Inject(method = "die", at = @At("RETURN"))
    private void jem$after(DamageSource source, CallbackInfo callback) {
        if (source == jem$source && dead && !jem$oldDead && jem$approval != null && !jem$approval.isCanceled() && !jem$participants.isEmpty()) {
            var proof = WildlifeQuestProof.data((LivingEntity)(Object)this); ListTag actors = new ListTag(); jem$participants.forEach(id -> actors.add(NbtUtils.createUUID(id))); proof.put("worm_native_participants", actors);
        }
        jem$source = null; jem$approval = null; jem$participants = Set.of();
    }
}
