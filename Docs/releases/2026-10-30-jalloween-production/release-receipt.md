# Jalloween production release receipt

- Released from branch: `develop`
- Reviewed source candidate: `45a8babf0b04b87d38a11f41584636ff7ff5f7f6`
- MineRENT server: `JustEnough`
- Primary address: `ForeverSMP.minerent.io:25565`
- MineRENT state after deployment: `Работает`
- Dedicated-server startup: `Done (250.243s)`
- AutoModpack publication time: `2026-10-30 22:27:20 +05:00`
- AutoModpack content token: `ec0faa0`
- Published catalogue diff: `+189 ~68 -0`
- Post-publication preview: `+0 ~0 -0`
- Release baseline time: `2026-10-30T22:29:52.226476+05:00`
- Previous baseline time: `2026-10-21T16:55:22.444339+05:00`
- Stable bootstrap synchronization: no additions, replacements or deletions required
- Point-update archive: `C:\MyProjects\Minecraft\JustEnoughMods-updates\JustEnoughMods-update-2026-10-30_22-28-57.zip`
- Point-update SHA-256: `fe02f64f40f4be4bbbf974a1a8fbe3fce69e80a0331552e78e3a9720dcd15041`
- MineRENT publication exclusions: `mods/jem-bootstrap.jar`, `options.txt`
- Runtime quest projection: 1 chapter group, 15 chapters, 643 quests and 10 reward tables
- Runtime seasonal state: Halloween night active for 2026-10-30 in `Asia/Almaty`

The user must still complete ordinary player acceptance of the released client and gameplay behavior

## Corrective AutoModpack publication

- Trigger: stable JustEnoughMods aborted during Java module resolution after the first production generation retained retired files
- Established cause: the original MineRENT extraction merged the clean host tree over the old tree, producing `+189 ~68 -0` even though the release boundary contained 29 deletions
- Corrective action started: `2026-10-30 22:52:00 +05:00`
- MineRENT replacement scope: only `automodpack/host-modpack/main`
- Recovery source: the complete canonical local `Server/automodpack/host-modpack/main` tree transferred over SFTP. The existing root archive was inspected but rejected as an incomplete delta and was never published
- Transfer correction: an interrupted duplicate upload temporarily truncated `AudioYT-1.0.1.jar`. The exact canonical file was uploaded again and the clean preview completed without archive errors
- Verified corrective preview: content token `9b91261`, diff `+0 ~0 -29`, with only `main/mods/jem-bootstrap.jar` and `main/options.txt` excluded by the existing rules
- Corrective publication: `2026-10-30 23:29:12 +05:00`, content token `9b91261`, diff `+0 ~0 -29`
- Post-publication preview: `2026-10-30 23:29:36 +05:00`, content token `9b91261`, diff `+0 ~0 -0`, `No catalogue changes`
- MineRENT runtime state after publication: running
- Stable-client application: completed through the ordinary TLauncher and AutoModpack prelaunch path at `2026-10-30 23:42:06 +05:00`
- Stable active token: `9b912612f5202368e236e59e757e513505a74fea`, status `ACTIVE`, `detached=false`, no pending `update-transaction.json`
- Stable projection verification: all 300 managed JARs match `Client/ManagedPack` by SHA-256. The separately installed `jem-bootstrap.jar` is intentionally excluded from the managed projection, and all retired conflicting JARs are absent
- Runtime observation: Forge reached `Mod loading complete - 355 mods loaded`. Existing warnings and later gameplay behavior were not repaired or accepted in this operation
- No stable-profile files, AutoModpack pointers, cache state or observed errors were edited directly
