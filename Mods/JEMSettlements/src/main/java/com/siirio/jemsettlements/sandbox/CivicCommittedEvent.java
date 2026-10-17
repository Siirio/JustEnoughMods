package com.siirio.jemsettlements.sandbox;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** A first persistent town milestone or an explicitly delivered, accepted meal. */
public final class CivicCommittedEvent extends Event {
    public final ServerPlayer player;
    public final int amount;
    public final String receipt;
    private final java.util.function.Consumer<String> acknowledgment;
    public CivicCommittedEvent(ServerPlayer player,int amount,String receipt){this(player,amount,receipt,outcome->{});}
    public CivicCommittedEvent(ServerPlayer player,int amount,String receipt,java.util.function.Consumer<String> acknowledgment){this.player=player;this.amount=amount;this.receipt=receipt;this.acknowledgment=acknowledgment;}
    public void acknowledge(String outcome){if(java.util.Set.of("ACCEPTED","DUPLICATE","CAPPED","INELIGIBLE").contains(outcome))acknowledgment.accept(outcome);}
}
