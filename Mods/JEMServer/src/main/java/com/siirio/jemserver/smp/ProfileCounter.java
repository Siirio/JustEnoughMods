package com.siirio.jemserver.smp;

public enum ProfileCounter {
    PARTIES_HOSTED, PARTIES_JOINED, BOSSES_HOSTED, RAIDS_COMPLETED, BOSS_ASSISTS, BLOOD_MOON_CLEARS, FISHING_EVENTS;
    void increment(ProfileState profile) {
        switch (this) {
            case PARTIES_HOSTED -> profile.partiesHosted(profile.partiesHosted() + 1);
            case PARTIES_JOINED -> profile.partiesJoined(profile.partiesJoined() + 1);
            case BOSSES_HOSTED -> profile.bossesHosted(profile.bossesHosted() + 1);
            case RAIDS_COMPLETED -> profile.raidsCompleted(profile.raidsCompleted() + 1);
            case BOSS_ASSISTS -> profile.bossAssists(profile.bossAssists() + 1);
            case BLOOD_MOON_CLEARS -> profile.bloodMoonClears(profile.bloodMoonClears() + 1);
            case FISHING_EVENTS -> profile.fishingEvents(profile.fishingEvents() + 1);
        }
    }
}
