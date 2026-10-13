package com.siirio.jemclaims;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ClaimSetting {
    CONTAINERS("containers", Items.CHEST, ClaimPermission.OPENCONTAINER),
    FIRE("fire", Items.FLINT_AND_STEEL, ClaimPermission.FIRESPREAD),
    PVP("pvp", Items.DIAMOND_SWORD, ClaimPermission.HURTPLAYER),
    EXPLOSIONS("explosions", Items.TNT, ClaimPermission.EXPLOSIONS),
    PENETRATION("penetration", Items.PISTON, ClaimPermission.WATERBORDER, ClaimPermission.PISTONBORDER, ClaimPermission.CREATE),
    PLACE("place", Items.GRASS_BLOCK, ClaimPermission.PLACE),
    BREAK("break", Items.DIAMOND_PICKAXE, ClaimPermission.BREAK),
    BEDS("beds", Items.RED_BED, ClaimPermission.BED);

    private final String key;
    public final Item icon;
    public final List<ClaimPermission> permissions;

    ClaimSetting(String key, Item icon, ClaimPermission... permissions) {
        this.key = key;
        this.icon = icon;
        this.permissions = List.of(permissions);
    }

    public String key() {
        return key;
    }

    public boolean memberScoped() {
        return permissions.stream().noneMatch(permission -> permission.global);
    }
}
