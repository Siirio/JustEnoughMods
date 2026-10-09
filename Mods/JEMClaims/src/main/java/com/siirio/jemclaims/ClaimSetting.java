package com.siirio.jemclaims;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ClaimSetting {
    CONTAINERS("Контейнеры", Items.CHEST, ClaimPermission.OPENCONTAINER),
    FIRE("Огонь", Items.FLINT_AND_STEEL, ClaimPermission.FIRESPREAD),
    PVP("PvP", Items.DIAMOND_SWORD, ClaimPermission.HURTPLAYER),
    EXPLOSIONS("Взрывы", Items.TNT, ClaimPermission.EXPLOSIONS),
    PENETRATION("Проникновение извне", Items.PISTON, ClaimPermission.WATERBORDER, ClaimPermission.PISTONBORDER, ClaimPermission.CREATE),
    PLACE("Ставить блоки", Items.GRASS_BLOCK, ClaimPermission.PLACE),
    BREAK("Ломать блоки", Items.DIAMOND_PICKAXE, ClaimPermission.BREAK),
    BEDS("Кровати", Items.RED_BED, ClaimPermission.BED);

    public final String label;
    public final Item icon;
    public final List<ClaimPermission> permissions;

    ClaimSetting(String label, Item icon, ClaimPermission... permissions) {
        this.label = label;
        this.icon = icon;
        this.permissions = List.of(permissions);
    }

    public boolean memberScoped() {
        return permissions.stream().noneMatch(permission -> permission.global);
    }
}
