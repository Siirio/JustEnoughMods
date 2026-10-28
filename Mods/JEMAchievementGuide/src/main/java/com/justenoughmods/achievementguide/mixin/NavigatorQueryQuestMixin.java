package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateNavigatorProofs;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.NavigableGraph;
import de.mrjulsen.crn.data.navigation.Route;
import de.mrjulsen.crn.network.packets.pain.NavigatePacketData;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "de.mrjulsen.crn.network.packets.pain.NavigatePacketData", remap = false)
public abstract class NavigatorQueryQuestMixin {
    @Redirect(method = "handle", at = @At(value = "INVOKE", target = "Lde/mrjulsen/crn/data/navigation/NavigableGraph;searchRoutes(Lde/mrjulsen/crn/data/StationTag;Lde/mrjulsen/crn/data/StationTag;Ljava/util/UUID;Z)Ljava/util/List;"), remap = false)
    private static List<Route> jem$authoritativeQuery(StationTag start, StationTag end, UUID playerId, boolean avoidTransfers,
                                                     NavigatePacketData.Request packet, NetworkPacketContext context) {
        ServerPlayer actor = context.getPlayer() instanceof ServerPlayer player && player.getUUID().equals(playerId) ? player : null;
        var settings = CreateNavigatorProofs.eligible(actor) ? CreateNavigatorProofs.constraint(UserSettings.getSettingsFor(playerId, true)) : null;
        List<Route> result = NavigableGraph.searchRoutes(start, end, playerId, avoidTransfers);
        CreateNavigatorProofs.queried(actor, start, end, result, settings);
        return result;
    }
}
