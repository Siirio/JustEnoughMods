package com.siirio.jemserver.smp.events;

import java.util.List;
import net.minecraft.nbt.CompoundTag;

record ActiveEventSnapshot(int tick, List<CompoundTag> rows) {}
