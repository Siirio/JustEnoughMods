package com.siirio.jemeconomy.registry;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.shipping.ShippingBoxMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EconomyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, JemEconomy.MOD_ID);
    public static final RegistryObject<MenuType<ShippingBoxMenu>> SHIPPING_BOX = MENUS.register("shipping_box",
            () -> IForgeMenuType.create(ShippingBoxMenu::fromNetwork));
    // Keep the old registry ID as a compatibility alias; gameplay opens MenuType.MERCHANT.
    public static final RegistryObject<MenuType<MerchantMenu>> COIN_MERCHANT = MENUS.register("coin_merchant",
            () -> IForgeMenuType.create((id, inventory, buffer) -> new MerchantMenu(id, inventory)));
    private EconomyMenus() { }
}
