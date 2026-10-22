package com.siirio.jemguisandbox.mirror.jemvisualsandbox.workbench;
import java.util.UUID;
public final class RaceExperience {
    public record Status(int race,boolean selected,int[] tiers,int[] progress,long signatureRemaining,
                         long secondaryRemaining,boolean developerMode,int bloomUpgrade,
                         long reselectRemaining,boolean testProfile,UUID nonce,boolean healingPenalty) {
        public Status {tiers=tiers.clone();progress=progress.clone();}
        public int tierAt(int index){return tiers[index];}
        public int progressAt(int index){return progress[index];}
        @Override public int[] tiers(){return tiers.clone();}
        @Override public int[] progress(){return progress.clone();}
    }
}
