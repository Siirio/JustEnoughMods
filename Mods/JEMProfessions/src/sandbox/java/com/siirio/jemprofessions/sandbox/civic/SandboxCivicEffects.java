package com.siirio.jemprofessions.sandbox.civic;

import com.siirio.jemeconomy.sandbox.CommerceCommittedEvent;
import com.siirio.jemeconomy.sandbox.SandboxCommerce;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.progress.SourceKind;
import com.siirio.jemprofessions.sandbox.SandboxProfessionSupport;
import com.siirio.jemsettlements.sandbox.CivicCommittedEvent;
import com.siirio.jemsettlements.sandbox.SandboxCivicRuntime;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Profession admission only; Economy and Settlements retain every workflow/state owner. */
public final class SandboxCivicEffects {
    private static final Set<String> MERCHANT = Set.of("buy_orders","auction","commission_escrow","stall_sign","trading_post","market_network");
    private static final Set<String> MAYOR = Set.of("invite_residents","supply_notice","resident_move","taxes","treasury_audit","civic_fund","rally","patrol_route","captain_order");
    private static boolean registered;
    private SandboxCivicEffects() {}
    public static void register() {
        if (registered) return; registered=true;
        MinecraftForge.EVENT_BUS.register(SandboxCivicEffects.class); SandboxCommerce.register(); SandboxCivicRuntime.register();
        SandboxCivicRuntime.permission((player,node)->SandboxProfessionSupport.enabled(player,ProfessionId.MAYOR,node));
        SandboxCommerce.permission((player,node)->node.equals("cargo_contract")?SandboxProfessionSupport.enabled(player,ProfessionId.ENGINEER,node)
                :SandboxProfessionSupport.enabled(player,ProfessionId.MERCHANT,node)
                ||node.equals("trading_post")&&(SandboxProfessionSupport.enabled(player,ProfessionId.MERCHANT,"stall_sign")||SandboxProfessionSupport.enabled(player,ProfessionId.MERCHANT,"market_network")));
    }
    public static boolean activate(ServerPlayer player,String node,CompoundTag input) {
        if (!SandboxProfessionSupport.allowed(player)) return false;
        String mode=input.getString("Mode");
        if (MAYOR.contains(node)) {
            if (!SandboxProfessionSupport.enabled(player,ProfessionId.MAYOR,node)) return false;
            return SandboxCivicRuntime.action(player,node,input);
        }
        if (!MERCHANT.contains(node) && !node.equals("cargo_contract")) return false;
        Set<String> modes=switch(node){
            case "buy_orders","commission_escrow","cargo_contract"->Set.of("create","fulfill","cancel","claim");
            case "auction"->Set.of("create","bid","cancel","claim");
            case "trading_post"->Set.of("create","buy","cancel","claim");
            case "stall_sign"->Set.of("create","sign","cancel","claim");
            case "market_network"->Set.of("create","advertise","fulfill","bid","buy","claim","cancel");
            default->Set.of();
        };
        if(!modes.contains(mode))return false;
        // Customers may buy actual stocked shop goods without inheriting the seller's profession.
        boolean customer=node.equals("trading_post")&&mode.equals("buy");
        if (!customer && !SandboxProfessionSupport.enabled(player,node.equals("cargo_contract")?ProfessionId.ENGINEER:ProfessionId.MERCHANT,node)) return false;
        if (mode.equals("advertise") && !SandboxProfessionSupport.enabled(player,ProfessionId.MERCHANT,"market_network")) return false;
        if (mode.equals("sign") && !SandboxProfessionSupport.enabled(player,ProfessionId.MERCHANT,"stall_sign")) return false;
        if (node.equals("stall_sign") && !Set.of("create","sign","cancel","claim").contains(mode)) return false;
        if (node.equals("market_network") && !Set.of("create","advertise","fulfill","bid","buy","claim","cancel").contains(mode)) return false;
        // Free claims of either higher node can create a real stocked backing shop directly.
        return SandboxCommerce.action(player,(node.equals("stall_sign")||node.equals("market_network"))&&mode.equals("create")?"trading_post":node,input);
    }
    public static CompoundTag snapshot(ServerPlayer player,String node) {
        return MAYOR.contains(node)?SandboxCivicRuntime.snapshot(player,node):SandboxCommerce.snapshot(player,node);
    }
    public static boolean recover(ServerPlayer player,CompoundTag input){
        return SandboxProfessionSupport.allowed(player)&&SandboxCommerce.recovery(player,input);
    }
    @SubscribeEvent public static void commerce(CommerceCommittedEvent event) {
        if (event.seller.isCreative()||event.seller.isSpectator()||event.buyer.equals(com.siirio.jemidentity.api.IdentityPolicy.resolve(event.seller).map(p->p.accountId()).orElse(null))){event.acknowledge("INELIGIBLE");return;}
        var outcome=SandboxProfessionSupport.creditDurably(event.seller,ProfessionId.MERCHANT,SourceKind.ECONOMY_SETTLED,
                new ResourceLocation("jem_professions","committed_sale"),3,"merchant:sales",300,"commerce:"+event.receipt);
        if(outcome!=SandboxProfessionSupport.CreditOutcome.REJECTED)event.acknowledge(outcome.name());
    }
    @SubscribeEvent public static void civic(CivicCommittedEvent event) {
        if(event.player.isCreative()||event.player.isSpectator()){event.acknowledge("INELIGIBLE");return;}
        var outcome=SandboxProfessionSupport.creditDurably(event.player,ProfessionId.MAYOR,SourceKind.SETTLEMENT_MILESTONE,
                new ResourceLocation("jem_professions","civic_result"),event.amount,"mayor:civic",200,event.receipt);
        if(outcome!=SandboxProfessionSupport.CreditOutcome.REJECTED)event.acknowledge(outcome.name());
    }
}
