package com.siirio.jemguisandbox.mirror.jemserver.smp;
import java.util.UUID;
public record SettlementUiAction(UUID request, UUID town, String action, UUID target, String text, long amount) { }
