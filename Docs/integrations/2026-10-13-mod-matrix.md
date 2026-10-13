# JEM 2026-10-13 Integration Matrix

All artifacts below are unmodified official releases. Client-and-server artifacts are stored in `Client/ManagedPack/mods` and `Server/mods`. Server Login is intentionally server-only.

| Artifact | Mod id | Side | License | SHA-256 | Source |
| --- | --- | --- | --- | --- | --- |
| `serverlogin-1.0.3.jar` | `serverlogin` | Server only (`displayTest=IGNORE_SERVER_VERSION`) | MIT | `FA89193521B8A40DAE3157B9787AC0C496D2A71E28255F72E5F31B6FCFCA64D2` | Modrinth version `hfNV8isR` |
| `irons_spellbooks-1.20.1-3.15.6.jar` | `irons_spellbooks` | Both | All Rights Reserved | `61996BB3CDB9601D83376F81295967F19A6F036563BB5A0E0F672967344A6FA7` | Modrinth version `Az9kyOso` / CurseForge file `7907340` |
| `irons_lib-1.20.1-1.0.2.jar` | `irons_lib` | Both | All Rights Reserved | `7C1AFCD2772AA1CED0C30678A6BF1BF9C605A482D18E2B03D51152E05D4A2800` | Modrinth version `FPYSRrdq` |
| `player-animation-lib-forge-1.0.2-rc1+1.20.jar` | `playeranimator` | Both | MIT | `90D9965CB9EFDBDA29FDC5610BE3914CF7008BF5C392FF34F7AB25F96A852691` | Modrinth version `xe2EVE6q` |
| `recruits-1.20.1-1.15.2.jar` | `recruits` | Both | All Rights Reserved | `4B53C1B752E886BA10985AAD2DF30D668971810230EB77867E07428A21D1AF7F` | CurseForge file `8339846` |
| `workers-1.20.1-2.0.3.jar` | `workers` | Both | All Rights Reserved | `C7C59A7A87BF51EC5FFBADFE5340F854F92397591A52AB5AF1DF8BFFD71D941E` | CurseForge file `8351157` |
| `ftb-library-forge-2001.2.13.jar` | `ftblibrary` | Both | All Rights Reserved | `41810281B65336265CAE2C88412022D02F2F0A3F28C66A06173D86F98ACF14A7` | CurseForge file `8226927` |
| `ftb-teams-forge-2001.3.2.jar` | `ftbteams` | Both | All Rights Reserved | `B1CB4D82EA9F137A87E12A7AAF3329619294B56F7D70337DFD557A76E10B6E07` | CurseForge file `7499810` |
| `ftb-quests-forge-2001.4.22.jar` | `ftbquests` | Both | All Rights Reserved | `9EA9D159A78A2724CFE0457B024680FDB52A2126EFF9CFA5280A30578F647A68` | CurseForge file `8078538` |
| `ftb-xmod-compat-forge-2.1.3.jar` | `ftbxmodcompat` | Both | All Rights Reserved | `16A8EB9DE88D5C33D29CB076CC39DFB337B2C54348E1EF1EFE9DD60CF85E5F72` | CurseForge file `6402486` |

## Exact dependency findings

- Server Login requires Forge `[47.4.10,48)` and Minecraft `[1.20.1,1.20.2)`. JEM reads only its public, connection-bound `ru.iwoss.serverlogin.gate.AuthGate.isLocked(ServerPlayer)` state; it never reads credentials or the account database.
- Iron's Spells requires Forge `47.4.0+`, Player Animator `1.0.2-rc1+1.20+`, GeckoLib `4.8.2+`, Curios `5.14.1+1.20.1+`, and Iron's Lib `[1.20.1-1,1.20.1-2)`. The installed pack satisfies these ranges.
- Workers requires Recruits `1.15.0+`; the selected Recruits `1.15.2` satisfies it.
- FTB Library/Teams/Quests/XMod Compat accept the installed Architectury `9.2.14`; their mutual minimum versions are satisfied by the selected set.
- Xaero Regions 1.0.8 was inspected and rejected because it requires Forge `47.4.20+`, while the canonical client/server runtime is Forge `47.4.10`. Its downloaded copies were quarantined outside the pack. Building territories stay in JEM's existing Xaero protocol-v5 overlay and never become permission claims.

## Identity contract

- `online-mode=false` is not an automatic feature-disable condition.
- On a dedicated server, an account-bound JEM mutation is allowed only after Server Login has unlocked the exact live connection. The persistent key is that authenticated session's player UUID; display names are metadata only.
- A reconnect gets a new connection and is locked again, preventing an old UUID session flag from being inherited.
- Integrated singleplayer trusts the local owner UUID for Roles and Professions. Shared-server Economy, Settlements, and SMP event ownership remain disabled there.
