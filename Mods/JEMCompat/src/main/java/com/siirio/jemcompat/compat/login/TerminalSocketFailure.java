package com.siirio.jemcompat.compat.login;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

/** A terminated transport is not evidence of a malformed Minecraft packet. */
public final class TerminalSocketFailure {
    private TerminalSocketFailure() { }

    public static boolean matches(Throwable failure) {
        // Decoder/protocol failures are deliberately excluded, even if they wrap IO.
        if (!(failure instanceof IOException)) return false;
        if (failure instanceof ClosedChannelException) return true;
        String message = failure.getMessage();
        return message != null && (message.equals("Connection reset")
                || message.endsWith("Connection reset by peer") || message.endsWith("Broken pipe"));
    }
}
