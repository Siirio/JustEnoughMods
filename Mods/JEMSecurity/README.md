# JEM Security

JEM Security is the dedicated-host security owner for the JEM Forge 1.20.1 pack. It does not hook Embeddium, Oculus or client rendering.

## Authority

- The server records game-mode changes, commands and Creative slot mutations in per-player JSONL journals under the world `jem-security/audit` directory through one bounded daemon writer
- Creative-created stacks receive a provenance UUID and are followed through inventory count changes, drops, pickups, use, placement and container closure
- Anti-Xray replaces fully enclosed ore states only in outgoing chunk data and reveals the real state after the block becomes exposed
- Server action guards reject block and entity actions outside authoritative reach or through solid collision
- The matching client companion reports declared mod IDs and enabled resource-pack IDs. Exact banned IDs and suspicious pack-name tokens are rejected
- JEM Freecam (`jem_freecam`) is explicitly allowed. Its own collision and interaction guards remain the authority for that camera
- An administrator may request a bounded sanitized tail of `latest.log`. The player must click the consent action before any log content is sent

Client attestation is supporting evidence, not proof. A modified client can lie. Anti-Xray, reach checks and authoritative inventory auditing remain effective without trusting the report.

## Commands

- `/jemsecurity status <player>`
- `/jemsecurity history <player> [limit]`
- `/jemsecurity request-log <player>`

Server configuration is world-local at `serverconfig/jem-security-server.toml`.
