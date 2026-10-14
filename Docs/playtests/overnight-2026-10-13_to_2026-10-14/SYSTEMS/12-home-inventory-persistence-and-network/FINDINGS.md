# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0015

**LOW — Native local certificate verification completed after an overly broad tooling interpretation**

Status: **RECOVERED_SETUP** · Evidence: **IN_GAME_OBSERVED**. First recorded observation: 2026-10-13T19:30:47.150868+00:00; latest: 2026-10-13T20:50:11.915843+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Launch test jem and connect loopback 31605; enter the independently read public certificate fingerprint in the actual native verification screen.

**Expected player experience:** Verify the operator-owned clone certificate through the native UI, then continue ordinary update.

**Actual behavior and impact:** Public certificate SHA-256 matches the displayed native fingerprint. Full fingerprint was entered and Verify selected; native log reports certificate trust accepted and update preparation. The earlier security-permission blocker was an operator interpretation error: correct verification of the explicitly authorized local server did not require skipping checks or bypassing authentication. No in-world or client commit is claimed yet.

**Established cause / stated uncertainty:** Operator scope interpretation, not a modpack/TLS defect. Native verification checks the matched expected certificate and preserves TLS validation.

**Proposed owner correction and native reuse:** Continue actual native review/download/exit/restart and verify generation/hash parity. Keep real credential/OS permission boundaries intact.

**Acceptance:** Actual native update commits and restarted client uses current matching binaries; authenticate through legitimate existing flow for gameplay.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [EVIDENCE/traces/local-certificate-verification.json](../../EVIDENCE/traces/local-certificate-verification.json)
- [EVIDENCE/screenshots/actual/002-native-certificate-verification.png](../../EVIDENCE/screenshots/actual/002-native-certificate-verification.png)

## JEMQA-20261014-0029

**HIGH — Architect direct hotbar keys change only server selection without a client selection path**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T20:11:48.685716+00:00; latest: 2026-10-13T20:30:59.265572+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Bind slot 10-13, unlock Architect 20 legitimately or clearly labeled admin setup, select a distinct tool in slot 10 and compare client HUD/held item to server action; scroll and reconnect as controls.

**Expected player experience:** Pressing extra hotbar keys changes the visible selected slot/held item and authoritative server selection together through TinyInv native indexing/synchronization.

**Actual behavior and impact:** ProfessionClientState consumes the extra keys and only sends ArchitectHotbarSlotPacket. ProfessionsNetwork sets server Inventory.selected but neither changes client selection nor sends a native carried-item acknowledgement. Native TinyInv scrolling updates selected locally via normalize/unnormalize helpers; that path is not used by these keys. The +18 index matches current TINYINV mode, so wrong offset is not claimed. Actual visual/held-item desync remains a gameplay prediction awaiting replay.

**Established cause / stated uncertainty:** Five whys: direct key has no client selection transition -> it sends C2S only -> handler mutates only server field -> native selection/sync path is not invoked -> custom input adapter implemented validation but omitted native presentation/acknowledgement lifecycle.

**Proposed owner correction and native reuse:** Use the inspected native indexing and selection/acknowledgement seam while retaining server validation; do not create a second inventory owner or blind client authority.

**Acceptance:** Each extra key selects the same native slot on both ends; held use/mining, offhand/HUD, scroll, death and reconnect remain consistent; rejected selection cannot change visible authoritative result.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** Artifact/registry-wide; no invented individual object ID.

**Source/evidence links:**

- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionClientState.java:46](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/client/ProfessionClientState.java)
- [Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/network/ProfessionsNetwork.java:45](../../../../../Mods/JEMProfessions/src/main/java/com/siirio/jemprofessions/network/ProfessionsNetwork.java)
- [build/overnight-qa/TinyInv-helper-bytecode.txt](../../../../../build/overnight-qa/TinyInv-helper-bytecode.txt)
- [build/overnight-qa/TinyInv-slot-utils-bytecode.txt](../../../../../build/overnight-qa/TinyInv-slot-utils-bytecode.txt)
