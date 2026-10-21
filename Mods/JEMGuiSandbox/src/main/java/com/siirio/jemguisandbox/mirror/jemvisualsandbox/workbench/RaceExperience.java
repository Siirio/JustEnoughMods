package com.siirio.jemguisandbox.mirror.jemvisualsandbox.workbench;
import java.util.UUID;
public final class RaceExperience {
    public record Status(int race,boolean selected,int[] tiers,int[] progress,long signatureRemaining,
                         long secondaryRemaining,boolean developerMode,int bloomUpgrade,
                         long reselectRemaining,boolean testProfile,UUID nonce) {
        public Status {tiers=tiers.clone();progress=progress.clone();}
        @Override public int[] tiers(){return tiers.clone();}
        @Override public int[] progress(){return progress.clone();}
    }
}
