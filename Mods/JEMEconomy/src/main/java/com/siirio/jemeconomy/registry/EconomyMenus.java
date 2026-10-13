package com.siirio.jemeconomy.registry;

import com.siirio.jemeconomy.JemEconomy;
import com.siirio.jemeconomy.shipping.ShippingBoxMenu;
import com.siirio.jemeconomy.merchant.CoinMerchantMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EconomyMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, JemEconomy.MOD_ID);
    public static final RegistryObject<MenuType<ShippingBoxMenu>> SHIPPING_BOX = MENUS.register("shipping_box",
            () -> IForgeMenuType.create(ShippingBoxMenu::fromNetwork));
    public static final RegistryObject<MenuType<CoinMerchantMenu>> COIN_MERCHANT = MENUS.register("coin_merchant",
            () -> IForgeMenuType.create(CoinMerchantMenu::fromNetwork));
    private EconomyMenus() { }
}
