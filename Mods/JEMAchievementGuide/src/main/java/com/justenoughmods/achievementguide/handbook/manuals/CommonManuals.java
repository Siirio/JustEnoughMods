package com.justenoughmods.achievementguide.handbook.manuals;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Only opens native read-only menus; no arbitrary class, page, position or NBT comes from the client. */
public final class CommonManuals {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("jem_advancements", "native_manuals"), () -> "1", "1"::equals, "1"::equals);
    private static final ResourceLocation BOOK_ITEM = new ResourceLocation("patchouli", "guide_book");
    private static boolean registered;
    private static final Map<ServerPlayer, Integer> LAST_REQUEST = new WeakHashMap<>();

    private CommonManuals() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode, Request::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void open(Route route) {
        CHANNEL.sendToServer(new Request(route.ordinal()));
    }

    public enum Route {
        CNC("cnc", "net.imasillylittleguy.cnc.procedures.FieldGuideOpenProcedure"),
        LUMINOUS("luminous_beasts", "net.mcreator.luminousbeasts.procedures.GotoPage1Procedure"),
        UNEARTHED("unearthed_journey", "net.mcreator.unearthedjourney.procedures.JourneyGuideRightclickedProcedure");

        private final String mod;
        private final String procedure;

        Route(String mod, String procedure) {
            this.mod = mod;
            this.procedure = procedure;
        }
    }

    private static boolean carriesHandbook(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (BOOK_ITEM.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())) && stack.hasTag()
                    && "jem_advancements:just_enough_guide".equals(stack.getTag().getString("patchouli:book"))) return true;
        }
        return false;
    }

    private record Request(int route) {
        private static void encode(Request request, FriendlyByteBuf buffer) {
            buffer.writeByte(request.route);
        }

        private static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readUnsignedByte());
        }

        private static void handle(Request request, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.isAlive() || request.route >= Route.values().length) return;
                Integer previous = LAST_REQUEST.get(player);
                if (previous != null && player.tickCount - previous < 20) return;
                LAST_REQUEST.put(player, player.tickCount);
                if (!carriesHandbook(player)) {
                    player.displayClientMessage(Component.translatable("jem.handbook.manual.status.carry_handbook"), false);
                    return;
                }
                if (player.containerMenu != player.inventoryMenu) {
                    player.displayClientMessage(Component.translatable("jem.handbook.manual.status.close_container"), false);
                    return;
                }
                Route route = Route.values()[request.route];
                if (!ModList.get().isLoaded(route.mod)) return;
                try {
                    Class.forName(route.procedure).getMethod("execute", LevelAccessor.class,
                                    double.class, double.class, double.class, Entity.class)
                            .invoke(null, player.level(), player.getX(), player.getY(), player.getZ(), player);
                } catch (ReflectiveOperationException | LinkageError error) {
                    LOGGER.error("Cannot open native handbook menu {}", route.mod, error);
                    player.displayClientMessage(Component.translatable("jem.handbook.manual.failed",
                            Component.translatable("item." + route.mod + "." + switch (route) {
                                case CNC -> "field_guide";
                                case LUMINOUS -> "ultimate_bestiary";
                                case UNEARTHED -> "journey_guide";
                            })), false);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
