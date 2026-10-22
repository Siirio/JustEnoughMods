package com.siirio.jemtwelveeyes.client;

/** Loaded only after the optional native map is present. */
final class XaeroMapAdapter {
    private XaeroMapAdapter() { }
    static void open() {
        var session = xaero.map.WorldMapSession.getCurrentSession();
        if (session != null && session.isUsable()) {
            session.getControlsHandler().keyDown(xaero.map.controls.ControlsRegister.keyOpenMap, false, false);
        }
    }
}
