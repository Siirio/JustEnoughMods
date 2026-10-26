package com.justenoughmods.inchatftb;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(InChatFTB.MOD_ID)
public final class InChatFTB {
    public static final String MOD_ID = "in_chat_ftb";

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static final class ClientCommands {
        private ClientCommands() {
        }

        @SubscribeEvent
        public static void register(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("inchatftb")
                    .then(Commands.literal("quest")
                            .then(Commands.argument("id", StringArgumentType.word())
                                    .executes(context -> open(StringArgumentType.getString(context, "id"))))));
        }

        private static int open(String hexId) {
            if (!ClientQuestFile.exists()) return 0;
            try {
                long id = Long.parseUnsignedLong(hexId, 16);
                if (ClientQuestFile.INSTANCE.getQuest(id) == null) return 0;
                ClientQuestFile.openBookToQuestObject(id);
                return 1;
            } catch (NumberFormatException invalidId) {
                return 0;
            }
        }
    }
}
