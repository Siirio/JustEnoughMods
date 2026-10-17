package com.siirio.jemsettlements.mixin;

import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.payroll.RecruitRank;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.network.MessageHireFromNobleVillager;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The packet's item price and villager requirement are client supplied. Resolve the
 * existing native offer/config before allowing its native spawning/hire flow. */
@Mixin(value = MessageHireFromNobleVillager.class, remap = false)
public abstract class NobleHiringMixin {
    @Shadow private UUID nobleUUID;
    @Shadow private UUID groupUUID;
    @Shadow private ResourceLocation resource;
    @Shadow private boolean needsVillager;
    @Shadow private boolean closing;
    @Inject(method = "executeServerSide", at = @At("HEAD"), cancellable = true)
    private void jem$validateQuote(NetworkEvent.Context context, CallbackInfo ci) {
        var player = context.getSender();
        if (player == null || !SettlementAuthority.active(player)) return;
        var entity = player.serverLevel().getEntity(nobleUUID);
        if (!(entity instanceof VillagerNobleEntity noble) || !noble.isAlive() || player.distanceToSqr(noble) > 1024D) { ci.cancel(); return; }
        if (closing) return;
        var group = com.talhanation.recruits.RecruitEvents.recruitsGroupsManager.getGroup(groupUUID);
        // Native spawn/convert dereferences group after hire has already charged.
        // Reject stale/forged/missing groups before creating or hiring anything.
        if (group == null || !player.getUUID().equals(group.getPlayerUUID())) { ci.cancel(); return; }
        com.siirio.jemsettlements.compat.recruits.RecruitHiringService.beginNoble();
        var principal = com.siirio.jemsettlements.runtime.SettlementAuthority.principal(player);
        if (principal.isEmpty()) { ci.cancel(); return; }
        needsVillager = RecruitsServerConfig.NobleVillagerNeedsVillagers.get();
        if (resource == null || RecruitRank.of(resource) == RecruitRank.UNSUPPORTED
                || noble.getTrades().stream().noneMatch(trade -> resource.equals(trade.resourceLocation) && trade.uses < trade.maxUses)) ci.cancel();
    }
    @Inject(method = "executeServerSide", at = @At("RETURN"))
    private void jem$refreshQuote(NetworkEvent.Context context, CallbackInfo ci) {
        var player = context.getSender();
        if (player != null && SettlementAuthority.active(player) && !closing
                && player.serverLevel().getEntity(nobleUUID) instanceof VillagerNobleEntity noble)
            com.siirio.jemsettlements.network.SettlementNetwork.sendNobleQuote(player, noble);
    }
    @Redirect(method = {"executeServerSide", "createRecruit"}, at = @At(value = "INVOKE", target = "Lcom/talhanation/recruits/entities/VillagerNobleEntity;doTrade(Lnet/minecraft/resources/ResourceLocation;)V"))
    private void jem$successfulStock(VillagerNobleEntity noble, ResourceLocation type) {
        if (com.siirio.jemsettlements.compat.recruits.RecruitHiringService.nobleHired()) noble.doTrade(type);
    }
}
