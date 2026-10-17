package com.siirio.jemsettlements.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
public record StorageLabelsPacket(CompoundTag view) {
    static void encode(StorageLabelsPacket packet,FriendlyByteBuf buffer){buffer.writeNbt(packet.view);}
    static StorageLabelsPacket decode(FriendlyByteBuf buffer){CompoundTag tag=buffer.readNbt();return new StorageLabelsPacket(tag==null?new CompoundTag():tag);}
}
