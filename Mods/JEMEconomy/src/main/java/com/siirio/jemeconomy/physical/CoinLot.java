package com.siirio.jemeconomy.physical;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public record CoinLot(UUID id, UUID issuanceOperation, CoinDenomination denomination, int issued, int redeemed, int reserved) {
    public int available() {
        return Math.max(0, issued - redeemed - reserved);
    }

    public CoinLot reserve(int count) {
        return new CoinLot(id, issuanceOperation, denomination, issued, redeemed, Math.addExact(reserved, count));
    }

    public CoinLot commitReserved(int count) {
        return new CoinLot(id, issuanceOperation, denomination, issued, Math.addExact(redeemed, count), reserved - count);
    }

    public CoinLot cancelReserved(int count) {
        return new CoinLot(id, issuanceOperation, denomination, issued, redeemed, reserved - count);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Operation", issuanceOperation);
        tag.putString("Denomination", denomination.id());
        tag.putInt("Issued", issued);
        tag.putInt("Redeemed", redeemed);
        tag.putInt("Reserved", reserved);
        return tag;
    }

    public static CoinLot load(CompoundTag tag) {
        CoinDenomination denomination = CoinDenomination.byId(tag.getString("Denomination")).orElse(CoinDenomination.COPPER);
        int issued = Math.max(0, tag.getInt("Issued"));
        int redeemed = Math.min(issued, Math.max(0, tag.getInt("Redeemed")));
        int reserved = Math.min(issued - redeemed, Math.max(0, tag.getInt("Reserved")));
        return new CoinLot(tag.getUUID("Id"), tag.getUUID("Operation"), denomination, issued, redeemed, reserved);
    }
}
