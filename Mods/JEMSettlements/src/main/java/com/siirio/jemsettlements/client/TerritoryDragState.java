package com.siirio.jemsettlements.client;

/** Pointer selection lifecycle; COMMIT waits for the server journal acknowledgement. */
public enum TerritoryDragState { IDLE, DRAGGING, PREVIEW, COMMIT, CANCEL }
