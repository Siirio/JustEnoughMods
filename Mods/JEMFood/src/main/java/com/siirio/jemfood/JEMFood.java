package com.siirio.jemfood;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(JEMFood.ID)
public final class JEMFood {
    public static final String ID = "jemfood";
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ID);
    public static final RegistryObject<MobEffect> BLESSING = EFFECTS.register("food_blessing", Blessing::new);

    public JEMFood() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        EFFECTS.register(bus);
        FoodNetwork.register();
    }

    private static final class Blessing extends MobEffect {
        private Blessing() { super(MobEffectCategory.BENEFICIAL, 0xD7A558); }
        @Override
        public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
            consumer.accept(new com.siirio.jemfood.client.BlessingPresentation());
        }
    }
}
