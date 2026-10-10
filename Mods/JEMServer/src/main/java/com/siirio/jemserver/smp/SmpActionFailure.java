package com.siirio.jemserver.smp;

public final class SmpActionFailure extends IllegalArgumentException {
    private final String code;

    public SmpActionFailure(String code) {
        super(code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
