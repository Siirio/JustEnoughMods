package com.siirio.jemprofessions.tree;

import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.tree.ProfessionNode.Availability;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Stable directed graphs. Profession level unlocks nodes for free; toggles are preferences. */
public final class ProfessionCatalog {
    private static final Map<ProfessionId, List<ProfessionNode>> NODES = new EnumMap<>(ProfessionId.class);
    static {
        NODES.put(ProfessionId.ARCHITECT, List.of(
                u("long_arms_1","reach",1,"",5,"architect.reach.3"), u("long_arms_2","reach",2,"long_arms_1",25,"architect.reach.5"),
                u("phantom_3","area",1,"",20,"architect.area.3"), u("phantom_5","area",2,"phantom_3",40,"architect.area.5"), u("phantom_7","area",3,"phantom_5",50,"architect.area.7"),
                a("architect_hotbar","quick_access",1,"",20,false,"tinyinv","architect.hotbar.13"), u("architect_supply","quick_access",2,"architect_hotbar",40,"architect.supply.126")));
        NODES.put(ProfessionId.FARMER, List.of(u("crop_sense","crop_care",1,"",5,"farmer.crop_care.1"),u("sweep","crop_care",2,"crop_sense",30,"farmer.crop_care.2"),u("herd_call","livestock",1,"",10,"farmer.livestock.1"),u("feed_herd","livestock",2,"herd_call",25,"farmer.livestock.2"),u("quick_shear","livestock",3,"feed_herd",40,"farmer.livestock.3"),u("greenhouse_master","greenhouse",1,"",50,"farmer.greenhouse.1","sereneseasons")));
        NODES.put(ProfessionId.FISHER, List.of(u("target_fishing","targeting",1,"",25,"fisher.target.1","tide"),a("double_catch","yield",1,"",50,false,"","fisher.yield.1")));
        NODES.put(ProfessionId.ENGINEER, List.of(u("gear_swap","machines",1,"",5,"engineer.machine.1","create"),u("shaft_builder","machines",2,"gear_swap",15,"engineer.machine.2","create"),u("factory_copy","machines",3,"shaft_builder",30,"engineer.machine.3","create"),u("track_run","railways",1,"",10,"engineer.rail.1","create"),u("schedule_templates","railways",2,"track_run",25,"engineer.rail.2","create"),u("cargo_contract","railways",3,"schedule_templates",40,"engineer.rail.3","create")));
        NODES.put(ProfessionId.MERCHANT, List.of(u("buy_orders","contracts",1,"",5,"merchant.contract.1","jem_economy"),u("auction","contracts",2,"buy_orders",15,"merchant.contract.2","jem_economy"),u("commission_escrow","contracts",3,"auction",20,"merchant.contract.3","jem_economy"),u("stall_sign","markets",1,"",10,"merchant.market.1","spuds_shops"),u("trading_post","markets",2,"stall_sign",30,"merchant.market.2","jem_economy"),u("market_network","markets",3,"trading_post",40,"merchant.market.3","spuds_shops")));
        NODES.put(ProfessionId.MAYOR, List.of(u("invite_residents","citizens",1,"",5,"mayor.citizens.1","jem_settlements"),u("supply_notice","citizens",2,"invite_residents",10,"mayor.citizens.2","jem_settlements"),u("resident_move","citizens",3,"supply_notice",25,"mayor.citizens.3","jem_settlements"),u("taxes","treasury",1,"",15,"mayor.treasury.1","jem_settlements"),u("rally","guards",1,"",30,"mayor.guards.1","recruits"),u("patrol_route","guards",2,"rally",40,"mayor.guards.2","recruits")));
        NODES.put(ProfessionId.WARRIOR, List.of(u("endurance","survival",1,"",5,"warrior.survival.1","epicfight"),u("battle_recovery","survival",2,"endurance",10,"warrior.survival.2","epicfight"),u("second_wind","survival",3,"battle_recovery",30,"warrior.survival.3","epicfight"),u("parry_master","technique",1,"",15,"warrior.technique.1","epicfight"),u("counter_step","technique",2,"parry_master",25,"warrior.technique.2","epicfight"),u("field_medic","support",1,"",40,"warrior.support.1","playerrevive")));
        NODES.put(ProfessionId.MINER, List.of(a("ore_sense","ore_sense",1,"",5,true,"","miner.ore_sense.1"),a("vein_miner","vein_mining",1,"",15,true,"","miner.vein.1"),a("vein_master","vein_mining",2,"vein_miner",50,true,"","miner.vein.2"),a("stone_step","survival",1,"",10,false,"","miner.survival.1"),a("lava_shield","survival",2,"stone_step",30,false,"","miner.survival.2"),a("cave_breath","survival",3,"lava_shield",40,false,"","miner.survival.3")));
        NODES.put(ProfessionId.EXPLORER, List.of(u("wall_climb","mobility",1,"",5,"explorer.mobility.1"),u("fast_traveler","mobility",2,"wall_climb",15,"explorer.mobility.2"),u("ledge_catch","mobility",3,"fast_traveler",25,"explorer.mobility.3"),u("ration_saver","expedition",1,"",10,"explorer.expedition.1"),u("river_runner","expedition",2,"ration_saver",40,"explorer.expedition.2"),u("portable_camp","expedition",3,"river_runner",50,"explorer.expedition.3")));
        NODES.put(ProfessionId.MAGE, List.of(u("mana_saver","mana",1,"",5,"mage.mana.1","irons_spellbooks"),u("meditation","mana",2,"mana_saver",25,"mage.mana.2","irons_spellbooks"),u("combo_magic","mana",3,"meditation",30,"mage.mana.3","irons_spellbooks"),u("magic_school","affinity",1,"",15,"mage.affinity.1","irons_spellbooks"),u("arcane_guard","support",1,"",40,"mage.support.1","irons_spellbooks"),u("magic_circle","support",2,"arcane_guard",50,"mage.support.2","irons_spellbooks")));
        validate();
    }
    private ProfessionCatalog() {}
    public static List<ProfessionNode> nodes(ProfessionId id) { return NODES.getOrDefault(id, List.of()); }
    public static ProfessionNode node(ProfessionId profession, String id) { return nodes(profession).stream().filter(value -> value.id().equals(id)).findFirst().orElse(null); }
    public static boolean parentSatisfied(ProfessionId profession, ProfessionNode node, int level) { if (node.parentId().isBlank()) return true; ProfessionNode parent = node(profession, node.parentId()); return parent != null && level >= parent.level() && parentSatisfied(profession, parent, level); }
    private static ProfessionNode a(String id,String branch,int rank,String parent,int level,boolean toggle,String dep,String effect){return new ProfessionNode(id,branch,rank,parent,level,toggle,dep,Availability.AVAILABLE,"jem.professions.node."+id);}
    private static ProfessionNode u(String id,String branch,int rank,String parent,int level,String effect){return u(id,branch,rank,parent,level,effect,"");}
    private static ProfessionNode u(String id,String branch,int rank,String parent,int level,String effect,String dep){return new ProfessionNode(id,branch,rank,parent,level,true,dep,Availability.UNSUPPORTED_NATIVE_API,"jem.professions.node."+id);}
    private static void validate() { for (var entry : NODES.entrySet()) { HashSet<String> ids = new HashSet<>(); for (ProfessionNode node : entry.getValue()) { if (!ids.add(node.id())) throw new IllegalStateException("Duplicate profession node " + node.id()); if (!node.parentId().isBlank()) { ProfessionNode parent = entry.getValue().stream().filter(value -> value.id().equals(node.parentId())).findFirst().orElseThrow(() -> new IllegalStateException("Missing parent " + node.parentId())); if (!parent.branch().equals(node.branch()) || parent.rank() >= node.rank() || parent.level() >= node.level()) throw new IllegalStateException("Invalid rank chain " + node.id()); } } } }
}
