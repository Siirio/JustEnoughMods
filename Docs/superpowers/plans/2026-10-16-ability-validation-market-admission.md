# Ability validation and market admission

User approved the chat plan on 2026-10-16 with “do this all”.

## Scope and decisions

- Disable the narrator keyboard shortcut while preserving accessibility settings and racial B utility.
- Make race activation, rejection reasons and cooldown authoritative across V and Iron's Spells; cover all signatures and utilities, and inspect profession activation owners.
- Expand market purchases to wood, wool, glass, paintings and the existing Building/Create-redstone categories. New purchases do not automatically authorize NPC buyback.
- Keep one permanent NPC prohibition resource, enforced before offers, quotation and final delivery, including original barter and persisted offers. Raw ore blocks are prohibited; ingots and gems are not implicitly prohibited.
- Exclude only the base `create` namespace from the public market. Addons retain category admission; suitable villagers retain their independent reviewed merchandise.
- Preserve native merchant layouts, stock/restock, backed currency, replay guards and dedicated-server boundaries.

## Execution

- [x] Narrator shortcut and shared server-safe category owner in JEMCompat.
- [x] Race activation/cooldown lifecycle and localized failures; inspect active profession adapters.
- [x] Purchase-only policies, exact prohibitions, painting variants and economy exports.
- [x] Review complete diff; build only required production artifacts, without tests, linters or gameplay.
- [x] Synchronize fresh artifacts to ordinary Server and ManagedPack, native preview/publication, Atlas refresh and validation.
- [x] Update canonical documentation and CHANGELOG; delivery uses exact task paths on main.

Project instructions override generic skill test/worktree procedures: use the current checkout, preserve unrelated runtime changes, and do not create or run automated tests or launch gameplay. Stable profiles, MineRENT and the immutable release baseline are outside this task.

## Acceptance boundary

Production build, artifact agreement, Atlas validation and native pack publication are delivery evidence. Actual player behavior remains `in testing` until the user's acceptance; no automated or gameplay acceptance is claimed.

Delivered local generation: 18, content token `7b90199`, staged/published manifest SHA-1 `b479c75beb82aec9b80e3be94568033a3045a804`. Independent source reviews found and resolved category parity, dedicated-server client-type linkage, untagged ore identities and localized painting search issues.
