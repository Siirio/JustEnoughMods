# Boss Raid

Current contract, 2026-10-25: Raid is a dedicated-only variation of the existing hosted Boss Fight lifecycle. It requires JEMServer. The current integration supersedes historical barrier-free, reinforcement-wave and source-only delivery descriptions. Exact build, regression and native-installation evidence is recorded in `encounter-integrity-delivery-20261025.json`.

Raid eligibility derives from a compatible registered BossProfile, historical legitimate defeat and the boss's unlocked registered structure arena. A native structure-to-boss registration is sufficient to make that profile own the shared six-face arena even when `hosted_encounter` is false. There is no manual Raid boss-type allowlist, and profiles without a registered structure receive no synthetic arena or barrier. One party reserves an arena through WorldTierData/RaidArenaApi. Ordinary manual Raid remains available independently of scheduled-event opt-in, while integrated/LAN never enables Raid.

Preparation holds native combat and records provenance/reservation only. It does not apply Raid attributes or model size. Start validates all accepted, ready, online and authorized participants and destinations. EncounterContext freezes the same member UUIDs as WorldBossTiers. The party, event, canonical attempt and participation receipts become durable before native combat releases. Failed Start reverses movement, reservation, participation changes and Raid modifiers, retaining retry intent on storage failure.

| Component | Raid behavior |
| --- | --- |
| Health | Existing default multiplier 2.5 over the frozen participant/native World Tier profile |
| Damage | Existing default multiplier 1.25 over frozen participant factors, preserving native component formulas |
| Size | Intended multiplier 1.35 applied once at Start, bounded by registered volume and actual collision |
| Native mechanics | Original attacks, abilities, phases, projectiles and summons remain owned by the installed boss mod |
| JEM mechanics | No authored Raid reinforcement waves, extra Raid attacks or reinforcement stat layer |
| Barrier | The same six-face physical structure-volume shell as Boss Fight, solid throughout combat |
| Participation | Once per player UUID/canonical arena, committed with the exact Start attempt |

If the enlarged body cannot safely fit, use only a verified collision-safe scale. Never clip or suffocate the boss, and do not substitute a relocated roof spawn. Complete prior Pehkui state and attributes are restored after failure or termination. Native summoned entities have no JEM Raid count cap and are never discarded merely for exceeding the removed wave budget. Player-owned or conflicting entities are not adopted as boss summons. Legacy wave counters are retired without deleting native entities.

The shared combat owner enforces accepted frozen participants, outsider damage/target denial, earned contribution/support eligibility and immediate actual-death elimination. A member's voluntary exit forfeits only that member. Host forfeit or expired host absence fails the whole attempt. Actual host death alone does not cancel remaining members. Both disconnect owners use the same configured grace. Reconnect cannot reactivate eliminated members or erase an expired absence outcome.

Each eligible recipient receives a full independent copy of captured native loot through JEMRewards. Raid retains the existing tagged-resource multiplier and personal cache: two distinct weighted resources and one enchanted book. No skill books or fractional shared pool. Frozen batch storage must be durably verified before victory and cleanup finalize. Offline/full-inventory rewards and pending XP survive independently. Full details and first-write failure limits are owned by [Boss Fight](BOSS_FIGHT.md#failure-safe-personal-settlement).

Raid may acquire one boss immediately while ordinary cooldown is pending. Reuse the legitimate living normal boss when available. Otherwise require a verified anchor, canonical ownership and complete saved-entity loading before creating one exact native identity. Pending creation records native NBT/UUID before insertion and acknowledges the native entity write. Unknown or unloaded saved UUIDs block replacement.

After victory, defeat, timeout or cancellation, preserve durable retirement/replacement intent, restore normal attributes/model size and converge to one dormant normal boss. An existing surviving converted normal boss may be restored in place. A legitimately dead or explicitly retired Raid UUID receives a durable retirement receipt before replacement. A stale saved copy of that UUID cannot become a new live claim. Recovery can wait for entity loading or storage repair, but it never treats unknown identity as death or reports unresolved cleanup as settled.

Ordinary cooldown remains a separate existing ScheduledRespawns record. Raid acquisition/restoration does not cancel, shorten or restart it. A later ordinary legitimate death owns its own three-Minecraft-day due time. Retry and restart preserve the original death identity and due time.

Surviving online members are safely ejected outside the sealed arena through a scoped server operation before the approved return/home flow. Offline living members retain ejection/return intent until login. A failed teleport or player save retains RECOVERY rather than clearing origins. NativeDeath follows normal bed/world-spawn respawn and never rejoins the attempt. Official Xaero markers retain native controls/privacy and share the canonical structure layer rather than a second Raid rectangle.

## Acceptance and exclusions

- Focused opt-in regressions are explicitly authorized by the current task, with exact results in the delivery receipt
- Source inspection and a successful production build do not prove multiplayer behavior
- Actual installed-mod native attacks, portals, multipart bodies, mounts, crash/restart recovery and two-player outcomes remain unverified until observed in an authorized runtime session
- No gameplay client launch, MineRENT deployment, stable profile update or release-baseline promotion is implied
- No new GUI design, boss-type allowlist, replacement native AI or independent arena/reward owner is introduced

## Historical notes

The following sections describe earlier investigations. Their distribution and barrier statements are superseded by the current contract above.

## Hosted lifecycle repair — 2026-10-19

Historical note: before2026-10-21, the shared boundary presence predicate controlled a boss virtual wall. That proven boss-wall policy is restored by the explicit 2026-10-24 request with the current typed state and ownership architecture, arena/revival identity remains canonical. Blood Moon/Resource Rush geometry is unchanged.

Waiting-area immunity never applies to an ALIVE participant of another active hosted encounter. HostedEncounters owns that classification, preventing overlapping waiting bosses from canceling active boss damage. Held bosses skip their whole native entity tick at the physically dedicated ServerLevel dispatcher: NoAI alone cannot pause Bosses Rise custom combat/cinematics/bar/music. Start clears the existing hold marker and resumes native behavior; tracking still suppresses dormant bars. That 2026-10-19 source checkpoint excluded integrated singleplayer through JemServerMixinPlugin; the 2026-10-22 universal ownership section supersedes that distribution gate. Native phases, damage values, rewards and progression are unchanged.

The supplied crab crash is an obsolete BOSS prompt passed to EventEntryKind.valueOf; source producer was already corrected in4dc02558. The prompt API now accepts only EventEntryKind, preserving protocol9 and rejecting invalid producers at compilation. Helvar second-phase no-damage is consistent with overlapping lobby immunity; the exact live incident is not observed locally. All fixes remain in testing until player acceptance.

## Structural arena volume — 2026-10-19

Historical geometry note: the2026-10-19 correction restricted structural boss walls to the native3D structure bounds. On 2026-10-21 the user removed those boss walls, then explicitly restored the prior logic on 2026-10-24. Native structure bounds still classify arenas and safe positions; ordinary Blood Moon/Resource Rush walls remain full height.

Restored arenaEntered containment is typed and persisted in PartyMember. Entry is accepted only after the complete player/vehicle box is inside the native three-dimensional structure. Old entry flags lacking the full-volume marker do not silently trap surface players. Membership, progression, reward eligibility, native full-column boss lookup and revival retain their canonical owners.
