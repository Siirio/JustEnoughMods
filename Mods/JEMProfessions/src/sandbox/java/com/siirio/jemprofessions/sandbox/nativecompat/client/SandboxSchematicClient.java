package com.siirio.jemprofessions.sandbox.nativecompat.client;

import com.mojang.logging.LogUtils;
import com.siirio.jemprofessions.sandbox.nativecompat.SandboxSchematicProjection;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.foundation.utility.CreatePaths;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;

/** Client-only optional Create boundary. No filename/path is accepted from the wire. */
public final class SandboxSchematicClient {
    private SandboxSchematicClient() {}

    public static void accept(SandboxSchematicProjection.Projection packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !minecraft.player.getUUID().equals(packet.owner())) {
            SandboxSchematicProjection.reply(packet.nonce(), false);
            return;
        }
        String name = SandboxSchematicProjection.filename(packet.owner(), packet.slot(), packet.version());
        Path file = CreatePaths.SCHEMATICS_DIR.resolve(name);
        try {
            SandboxSchematicProjection.write(file, packet.data());
            // Exercise the actual native client loading path before acknowledging readiness, not a parallel renderer.
            var item = SchematicItem.create(minecraft.level, name, packet.owner().toString());
            Vec3i size = SchematicItem.loadSchematic(minecraft.level, item).getSize();
            var expected = packet.data().getList("size", net.minecraft.nbt.Tag.TAG_INT);
            boolean ready = size.getX() == expected.getInt(0) && size.getY() == expected.getInt(1) && size.getZ() == expected.getInt(2);
            SandboxSchematicProjection.reply(packet.nonce(), ready);
        } catch (IOException | IllegalArgumentException failure) {
            LogUtils.getLogger().error("Could not project owned sandbox Create schematic {}", file, failure);
            SandboxSchematicProjection.reply(packet.nonce(), false);
        }
    }
}
