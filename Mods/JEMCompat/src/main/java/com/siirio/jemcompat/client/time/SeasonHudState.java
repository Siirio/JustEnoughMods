package com.siirio.jemcompat.client.time;

import net.minecraft.network.chat.Component;
import java.util.List;

/** Presentation snapshot, with no Serene Seasons types crossing the optional-mod boundary. */
public record SeasonHudState(boolean active, double cycleFraction, List<Component> tooltip) {}
