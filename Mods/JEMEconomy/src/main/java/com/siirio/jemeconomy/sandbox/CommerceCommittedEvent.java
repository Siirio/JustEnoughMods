package com.siirio.jemeconomy.sandbox;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/** Sent only after the Economy journal owns both goods and the committed payment. */
public final class CommerceCommittedEvent extends Event {
    public final ServerPlayer seller;
    public final UUID receipt;
    public final UUID buyer;
    private final java.util.function.Consumer<String> acknowledgment;
    public CommerceCommittedEvent(ServerPlayer seller, UUID buyer, UUID receipt) {
        this(seller,buyer,receipt,outcome->{});
    }
    public CommerceCommittedEvent(ServerPlayer seller,UUID buyer,UUID receipt,java.util.function.Consumer<String> acknowledgment){
        this.seller=seller;this.buyer=buyer;this.receipt=receipt;this.acknowledgment=acknowledgment;
    }
    public void acknowledge(String outcome){if(java.util.Set.of("ACCEPTED","DUPLICATE","CAPPED","INELIGIBLE").contains(outcome))acknowledgment.accept(outcome);}
}
