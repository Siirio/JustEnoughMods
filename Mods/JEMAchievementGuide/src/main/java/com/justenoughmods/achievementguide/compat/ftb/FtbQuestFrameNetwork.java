package com.justenoughmods.achievementguide.compat.ftb;

import com.justenoughmods.achievementguide.mixin.FtbQuestRawIconAccess;
import java.util.Optional;
import java.util.function.Supplier;
import net.mehvahdjukaar.advframes.blocks.AdvancementFrameBlockTile;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FtbQuestFrameNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("jem_advancements", "ftb_quest_frames"), () -> "1", "1"::equals, "1"::equals);
    private static boolean registered;
    private FtbQuestFrameNetwork() { }

    static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, Select.class, Select::encode, Select::decode, Select::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void select(BlockPos pos, long questId) { CHANNEL.sendToServer(new Select(pos, questId)); }

    private record Select(BlockPos pos, long questId) {
        private static void encode(Select message, FriendlyByteBuf buffer) { buffer.writeBlockPos(message.pos); buffer.writeLong(message.questId); }
        private static Select decode(FriendlyByteBuf buffer) { return new Select(buffer.readBlockPos(), buffer.readLong()); }
        private static void handle(Select message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null || !player.level().isLoaded(message.pos)
                        || player.distanceToSqr(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5) > 64.0) return;
                var quest = dev.ftb.mods.ftbquests.quest.ServerQuestFile.INSTANCE.getQuest(message.questId);
                if (quest == null || !dev.ftb.mods.ftbquests.quest.TeamData.get(player).isCompleted(quest)) return;
                if (!(player.level().getBlockEntity(message.pos) instanceof AdvancementFrameBlockTile tile)) return;
                DisplayInfo display = displayFor(quest);
                ((FtbQuestFrameAccess) tile).jem$setFtbQuest(message.questId, display, player);
                tile.setChanged();
                player.level().sendBlockUpdated(message.pos, tile.getBlockState(), tile.getBlockState(), 3);
            });
            context.setPacketHandled(true);
        }
    }

    /** Rebuilds presentation from the current quest definition, never from a persisted stale snapshot. */
    public static DisplayInfo displayFor(dev.ftb.mods.ftbquests.quest.Quest quest) {
                ItemStack icon = ((FtbQuestRawIconAccess) (Object) quest).jem$getRawIcon().copy();
                if (icon.isEmpty()) icon = new ItemStack(Items.WRITABLE_BOOK);
                String rawTitle = quest.getRawTitle();
                Component title = rawTitle.isBlank()
                        ? Component.literal("FTB Quest " + dev.ftb.mods.ftbquests.quest.QuestObjectBase.getCodeString(quest))
                        : dev.ftb.mods.ftbquests.util.TextUtils.parseRawText(rawTitle);
                Component description = Component.translatable("jem.quest_frame.description",
                        dev.ftb.mods.ftbquests.quest.QuestObjectBase.getCodeString(quest));
                return new DisplayInfo(icon, title, description, null, FrameType.TASK, false, false, false);
    }
}
