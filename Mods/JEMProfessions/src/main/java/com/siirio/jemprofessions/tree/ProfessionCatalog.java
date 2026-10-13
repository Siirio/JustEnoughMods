package com.siirio.jemprofessions.tree;

import com.siirio.jemprofessions.progress.ProfessionId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ProfessionCatalog {
    private static final Map<ProfessionId, List<ProfessionNode>> NODES = new EnumMap<>(ProfessionId.class);
    static {
        NODES.put(ProfessionId.ARCHITECT, List.of(n("long_arms_1","long_arms",5,false,""), n("phantom_line","phantom",10,true,""), n("phantom_3","phantom",20,true,""), n("long_arms_2","long_arms",25,false,""), n("phantom_4","phantom",30,true,""), n("phantom_5","phantom",40,true,""), n("long_arms_3","long_arms",50,false,""), n("phantom_7","phantom",50,true,"")));
        NODES.put(ProfessionId.FARMER, List.of(n("crop_sense","fieldwork",5,true,""), n("herd_call","livestock",10,true,""), n("feed_herd","livestock",25,true,""), n("sweep","fieldwork",30,true,""), n("quick_shear","livestock",40,true,""), n("greenhouse_master","greenhouse",50,true,"sereneseasons")));
        NODES.put(ProfessionId.FISHER, List.of(n("target_fishing","fishing",25,true,"tide"), n("double_catch","fishing",50,false,"")));
        NODES.put(ProfessionId.ENGINEER, List.of(n("gear_swap","machines",5,true,"create"), n("track_run","railways",10,true,"create"), n("shaft_builder","machines",15,true,"create"), n("schedule_templates","railways",25,true,"create"), n("factory_copy","machines",30,true,"create"), n("cargo_contract","railways",40,true,"create"), n("machine_mover","machines",50,true,"create")));
        NODES.put(ProfessionId.MERCHANT, List.of(n("buy_orders","contracts",5,true,"jem_economy"), n("stall_sign","markets",10,true,"spuds_shops"), n("auction","contracts",15,true,"jem_economy"), n("commission_escrow","contracts",20,true,"jem_economy"), n("trading_post","markets",30,true,"jem_economy"), n("market_network","markets",40,true,"spuds_shops"), n("grand_market","markets",50,true,"jem_economy")));
        NODES.put(ProfessionId.MAYOR, List.of(n("invite_residents","citizens",5,true,"jem_settlements"), n("supply_notice","citizens",10,true,"jem_settlements"), n("taxes","treasury",15,true,"jem_settlements"), n("resident_move","citizens",25,true,"jem_settlements"), n("rally","guards",30,true,"recruits"), n("patrol_route","guards",40,true,"recruits"), n("town_festival","citizens",50,true,"jem_settlements")));
        NODES.put(ProfessionId.WARRIOR, List.of(n("endurance","survival",5,false,"epicfight"), n("battle_recovery","survival",10,false,"epicfight"), n("parry_master","technique",15,false,"epicfight"), n("counter_step","technique",25,false,"epicfight"), n("second_wind","survival",30,false,"epicfight"), n("field_medic","technique",40,false,"playerrevive"), n("battle_focus","technique",50,false,"epicfight")));
        NODES.put(ProfessionId.MINER, List.of(n("ore_sense","extraction",5,true,""), n("stone_step","survival",10,false,""), n("vein_miner","extraction",15,true,""), n("deep_pockets","extraction",25,true,""), n("lava_shield","survival",30,false,""), n("cave_breath","survival",40,false,""), n("vein_master","extraction",50,true,"")));
        NODES.put(ProfessionId.EXPLORER, List.of(n("wall_climb","mobility",5,true,""), n("ration_saver","expedition",10,false,""), n("fast_traveler","mobility",15,false,""), n("ledge_catch","mobility",25,true,""), n("climbing_rope","mobility",30,true,""), n("river_runner","expedition",40,false,""), n("portable_camp","expedition",50,true,"")));
        NODES.put(ProfessionId.MAGE, List.of(n("mana_saver","focus",5,false,"irons_spellbooks"), n("arcane_sense","support",10,false,"irons_spellbooks"), n("magic_school","focus",15,true,"irons_spellbooks"), n("meditation","focus",25,false,"irons_spellbooks"), n("combo_magic","focus",30,false,"irons_spellbooks"), n("arcane_guard","support",40,true,"irons_spellbooks"), n("magic_circle","support",50,true,"irons_spellbooks")));
    }

    private ProfessionCatalog() {}
    public static List<ProfessionNode> nodes(ProfessionId id) { return NODES.getOrDefault(id, List.of()); }
    private static ProfessionNode n(String id, String branch, int level, boolean toggleable, String dependency) { return new ProfessionNode(id, branch, level, toggleable, dependency); }
}
