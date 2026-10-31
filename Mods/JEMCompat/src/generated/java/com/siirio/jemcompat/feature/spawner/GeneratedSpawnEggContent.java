package com.siirio.jemcompat.feature.spawner;

import com.siirio.jemcompat.JEMCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Generated registrations for eligible creatures whose owning mod has no usable egg item. */
public final class GeneratedSpawnEggContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JEMCompat.MOD_ID);

    static {
        register("spawn_egg_alexsmobs_sea_bear", "alexsmobs:sea_bear");
        register("spawn_egg_blastfromthepast_speartooth", "blastfromthepast:speartooth");
        register("spawn_egg_brewery_beer_elemental", "brewery:beer_elemental");
        register("spawn_egg_cataclysm_drowned_host", "cataclysm:drowned_host");
        register("spawn_egg_cataclysm_onyx", "cataclysm:onyx");
        register("spawn_egg_cataclysm_storm_serpent", "cataclysm:storm_serpent");
        register("spawn_egg_cnc_greywolf", "cnc:greywolf");
        register("spawn_egg_cnc_largepolarbear", "cnc:largepolarbear");
        register("spawn_egg_deep_dark_extended_replicate", "deep_dark_extended:replicate");
        register("spawn_egg_deep_dark_extended_sculk_parasite", "deep_dark_extended:sculk_parasite");
        register("spawn_egg_deep_dark_extended_spreader", "deep_dark_extended:spreader");
        register("spawn_egg_deep_dark_extended_stalker", "deep_dark_extended:stalker");
        register("spawn_egg_incision_skeletoncultist", "incision:skeletoncultist");
        register("spawn_egg_irons_spellbooks_cultist", "irons_spellbooks:cultist");
        register("spawn_egg_minecraft_armadillo", "minecraft:armadillo");
        register("spawn_egg_minecraft_sulfur_cube", "minecraft:sulfur_cube");
        register("spawn_egg_netherexp_warphopper", "netherexp:warphopper");
        register("spawn_egg_netherexp_will_o_wisp", "netherexp:will_o_wisp");
        register("spawn_egg_rottencreatures_flying_scarab", "rottencreatures:flying_scarab");
        register("spawn_egg_primeval_adventures_brygmophyseter", "primeval_adventures:brygmophyseter");
        register("spawn_egg_primeval_adventures_dinornis", "primeval_adventures:dinornis");
        register("spawn_egg_primeval_adventures_dodo", "primeval_adventures:dodo");
        register("spawn_egg_primeval_adventures_glyptodon", "primeval_adventures:glyptodon");
        register("spawn_egg_primeval_adventures_odobenocetops", "primeval_adventures:odobenocetops");
        register("spawn_egg_primeval_adventures_thylacine", "primeval_adventures:thylacine");
    }

    private GeneratedSpawnEggContent() { }

    public static void register(IEventBus bus) { ITEMS.register(bus); }

    @SuppressWarnings("unchecked")
    private static void register(String path, String entityId) {
        var id = new ResourceLocation(entityId);
        ITEMS.register(path, () -> new ForgeSpawnEggItem(
                () -> (EntityType<? extends Mob>) ForgeRegistries.ENTITY_TYPES.getValue(id),
                0x6b5a43, 0xd9c27a, new Item.Properties()));
    }
}
