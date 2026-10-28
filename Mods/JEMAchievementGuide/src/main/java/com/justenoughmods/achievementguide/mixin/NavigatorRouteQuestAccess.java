package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.NavigatorRouteAccess;
import de.mrjulsen.crn.data.navigation.RoutePart;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "de.mrjulsen.crn.data.navigation.Route", remap = false)
public interface NavigatorRouteQuestAccess extends NavigatorRouteAccess {
    @Override @Accessor("parts") List<RoutePart> jem$parts();
}
