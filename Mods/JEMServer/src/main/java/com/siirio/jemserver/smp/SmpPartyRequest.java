package com.siirio.jemserver.smp;

public record SmpPartyRequest(String activity, String title, String description, String language,
                              int slots, long scheduled, boolean approval, boolean solo) implements SmpActionInput {
    public static SmpPartyRequest hosted(String title, boolean solo) {
        return new SmpPartyRequest("BOSS", title, "", "", 0, 0, false, solo);
    }
}
