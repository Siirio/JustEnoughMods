package com.siirio.jemprofessions.tree;

import com.siirio.jemprofessions.progress.ProfessionId;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** The 57-node product graph. Pufferfish remains the authoritative unlock and point owner. */
public final class ProfessionCatalog {
    private static final Map<ProfessionId, List<ProfessionNode>> NODES = new EnumMap<>(ProfessionId.class);
    static {
        NODES.put(ProfessionId.ARCHITECT, List.of(
                n("long_arms_1","reach",1,"",5,"minecraft:scaffolding"), n("long_arms_2","reach",2,"long_arms_1",25,"minecraft:spyglass"),
                n("phantom_3","area",1,"",20,"minecraft:bricks"), n("phantom_5","area",2,"phantom_3",40,"minecraft:stone_bricks"), n("phantom_7","area",3,"phantom_5",50,"minecraft:chiseled_stone_bricks"),
                n("architect_hotbar","quick_access",1,"",20,"minecraft:chest"), n("architect_supply","quick_access",2,"architect_hotbar",40,"minecraft:shulker_box")));
        NODES.put(ProfessionId.FARMER, List.of(
                n("crop_sense","crop_care",1,"",5,"minecraft:wheat"), n("sweep","crop_care",2,"crop_sense",30,"minecraft:iron_hoe"),
                n("herd_call","livestock",1,"",10,"minecraft:wheat_seeds"), n("feed_herd","livestock",2,"herd_call",25,"minecraft:golden_carrot"), n("quick_shear","livestock",3,"feed_herd",40,"minecraft:shears"),
                n("greenhouse_master","greenhouse",1,"",50,"minecraft:glass")));
        NODES.put(ProfessionId.FISHER, List.of(
                n("target_fishing","targeting",1,"",25,"minecraft:tropical_fish"), n("double_catch","yield",1,"",50,"minecraft:cod")));
        NODES.put(ProfessionId.ENGINEER, List.of(
                n("gear_swap","machines",1,"",5,"create:wrench"), n("shaft_builder","machines",2,"gear_swap",15,"create:shaft"), n("factory_copy","machines",3,"shaft_builder",30,"create:schematic"),
                n("track_run","railways",1,"",10,"create:track"), n("schedule_templates","railways",2,"track_run",25,"create:schedule"), n("cargo_contract","railways",3,"schedule_templates",40,"minecraft:minecart")));
        NODES.put(ProfessionId.MERCHANT, List.of(
                n("buy_orders","contracts",1,"",5,"minecraft:paper"), n("auction","contracts",2,"buy_orders",15,"minecraft:clock"), n("commission_escrow","contracts",3,"auction",20,"minecraft:written_book"),
                n("stall_sign","markets",1,"",10,"minecraft:oak_sign"), n("trading_post","markets",2,"stall_sign",30,"minecraft:emerald"), n("market_network","markets",3,"trading_post",40,"minecraft:ender_chest")));
        NODES.put(ProfessionId.MAYOR, List.of(
                n("invite_residents","citizens",1,"",5,"minecraft:player_head"), n("supply_notice","citizens",2,"invite_residents",10,"minecraft:barrel"), n("resident_move","citizens",3,"supply_notice",25,"minecraft:minecart"),
                n("taxes","treasury",1,"",15,"minecraft:gold_ingot"), n("rally","guards",1,"",30,"minecraft:goat_horn"), n("patrol_route","guards",2,"rally",40,"minecraft:filled_map")));
        NODES.put(ProfessionId.WARRIOR, List.of(
                n("endurance","survival",1,"",5,"minecraft:leather_boots"), n("battle_recovery","survival",2,"endurance",10,"minecraft:glistering_melon_slice"), n("second_wind","survival",3,"battle_recovery",30,"minecraft:rabbit_foot"),
                n("parry_master","technique",1,"",15,"minecraft:shield"), n("counter_step","technique",2,"parry_master",25,"minecraft:iron_sword"), n("field_medic","support",1,"",40,"minecraft:golden_apple")));
        NODES.put(ProfessionId.MINER, List.of(
                n("ore_sense","ore_sense",1,"",5,"minecraft:raw_iron"), n("vein_miner","vein_mining",1,"",15,"minecraft:diamond_ore"), n("vein_master","vein_mining",2,"vein_miner",50,"minecraft:deepslate_diamond_ore"),
                n("stone_step","survival",1,"",10,"minecraft:stone"), n("lava_shield","survival",2,"stone_step",30,"minecraft:lava_bucket"), n("cave_breath","survival",3,"lava_shield",40,"minecraft:heart_of_the_sea")));
        NODES.put(ProfessionId.EXPLORER, List.of(
                n("wall_climb","mobility",1,"",5,"minecraft:ladder"), n("fast_traveler","mobility",2,"wall_climb",15,"minecraft:leather_boots"), n("ledge_catch","mobility",3,"fast_traveler",25,"minecraft:lead"),
                n("ration_saver","expedition",1,"",10,"minecraft:bread"), n("river_runner","expedition",2,"ration_saver",40,"minecraft:oak_boat"), n("portable_camp","expedition",3,"river_runner",50,"minecraft:campfire")));
        NODES.put(ProfessionId.MAGE, List.of(
                n("mana_saver","mana",1,"",5,"irons_spellbooks:arcane_essence"), n("meditation","mana",2,"mana_saver",25,"minecraft:amethyst_shard"), n("combo_magic","mana",3,"meditation",30,"irons_spellbooks:blank_rune"),
                n("magic_school","affinity",1,"",15,"irons_spellbooks:upgrade_orb"), n("arcane_guard","support",1,"",40,"minecraft:enchanted_golden_apple"), n("magic_circle","support",2,"arcane_guard",50,"minecraft:lodestone")));
        validate();
    }

    private ProfessionCatalog() {}
    public static List<ProfessionNode> nodes(ProfessionId id) { return NODES.getOrDefault(id, List.of()); }
    public static ProfessionNode node(ProfessionId profession, String id) {
        return nodes(profession).stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }
    public static List<String> branches(ProfessionId id) { return nodes(id).stream().map(ProfessionNode::branch).distinct().toList(); }
    private static ProfessionNode n(String id, String branch, int rank, String parent, int level, String icon) {
        return new ProfessionNode(id, branch, rank, parent, level, icon);
    }
    private static void validate() {
        int count = 0;
        for (var entry : NODES.entrySet()) {
            HashSet<String> ids = new HashSet<>();
            for (ProfessionNode node : entry.getValue()) {
                count++;
                if (!ids.add(node.id())) throw new IllegalStateException("Duplicate profession node " + node.id());
                if (!node.parentId().isBlank()) {
                    ProfessionNode parent = node(entry.getKey(), node.parentId());
                    if (parent == null || !parent.branch().equals(node.branch()) || parent.rank() >= node.rank() || parent.level() >= node.level()) {
                        throw new IllegalStateException("Invalid profession chain " + node.id());
                    }
                }
            }
        }
        if (count != 57) throw new IllegalStateException("Expected 57 profession nodes, got " + count);
    }
}
