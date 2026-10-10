package com.siirio.jemserver.smp.events;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record EventAttackPacket(UUID eventId,ResourceLocation dimension,AttackGeometry geometry,int duration,int color) {}
