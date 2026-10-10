package com.siirio.jemserver.smp;

public sealed interface SmpActionInput permits SmpNoInput, SmpPartyRequest, SmpPlayerRequest, SmpMessageRequest {}
