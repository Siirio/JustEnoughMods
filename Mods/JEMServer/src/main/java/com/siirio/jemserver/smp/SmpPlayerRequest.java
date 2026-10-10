package com.siirio.jemserver.smp;

import java.util.UUID;

public record SmpPlayerRequest(UUID player) implements SmpActionInput {}
