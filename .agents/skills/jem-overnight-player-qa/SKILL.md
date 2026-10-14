---
name: jem-overnight-player-qa
description: Execute explicitly requested overnight JustEnoughMods player QA with a protected local environment, Atlas-linked evidence, durable checkpoints and a truthful morning report. Never launch games during ordinary development.
---

# JEM Overnight Player QA

Use only after the human explicitly requests gameplay QA. Permission lasts for the
named run; this skill never supplies permission to build, test, launch, deploy or
change device settings on its own. Keep any user-supplied one-shot brief executable
without this skill and follow its specific scope/deadline.

Read root instructions, logic locks, `Docs/DEVELOPMENT.md`, `tools/workspace.json`
and `Catalog/README.md`; run `python tools/jem.py status`. Use the canonical General
skills for defect diagnosis and behavior preservation and the project Registry
Keeper for gameplay changes rather than redefining their workflows here.

## Secure the report first

Create `Docs/playtests/overnight-<start>_to_<end>/` immediately. Save `README.md`,
`ENVIRONMENT.md`, `RUN_METADATA.json`, `SESSION_STATE.json`, `TIMELINE.md` and
`RESUME.md` before setup. Default deadline is the next 08:00 Asia/Almaty only when
the human did not provide a deadline. Record UTC and local timestamps.

Probe actual desktop control, screenshot inspection, processes, model limits and
RAM headroom. A terminal is not proof of interactive control. Use a supported
bounded chat automation if available; state its ID, stop condition and dependence
on the awake host/app. Never claim continuous activity without timestamps. Do not
invent a reasoning daemon or change OS power/security settings without permission.

## Protect environment ownership

- MineRENT and stable `JustEnoughMods` are excluded unless separately authorized.
  Development gameplay uses `test jem`, with actual gameDir verified from fresh
  launcher/process/log evidence.
- Verify a hash-indexed original-world/config/auth snapshot outside Git before
  mutation. Prefer a disposable cloned server on different loopback ports/world.
  Preserve authentication; never invent, expose or bypass private credentials.
- Compare source SHA, build output, canonical `Client/ManagedPack`, server JARs and
  native client active projection independently. A pushed source is not installed
  behavior. Use exact installed third-party contracts and reobfuscated JEM JARs.
- Resolve each module's actual distributable task/classifier before installing.
  `JEMServer` and `JEMClaims` apply `JEMMenuSupport/embed.gradle`: ordinary `jar`
  builds slim, while full output requires `jarJar`/`reobfJarJar`. Verify fresh output
  hash/timestamp, embedded library and a current owner class. Equal installed hashes
  prove parity only; a successful slim build cannot certify an older full JAR.
  Inspect configured Mixin refmap and current injection mapping in the final full
  JAR; fresh and ordinary repeated builds must retain generated metadata. Treat
  absent maps as a packaging failure before attempting native installation.
- AutoModpack alone commits client installations. Official `stage-pack-sync`
  implementation may use explicitly isolated target paths, followed by real local
  `/jempack preview`, checked delta, `/jempack publish`, native confirmation,
  download, exit/restart and hash/generation verification. No raw client copying,
  fabricated metadata or second canonical managed pack.
- Never promote release baselines, publish production generations or create friend
  packages during local QA. Keep a kill switch and a deadline stop for owned JVMs.

## Breadth and evidence

Index all Atlas-observed IDs as references and coverage statuses, without copying
canonical object facts into a competing registry. Separately count registry
population, source-reviewed samples and actual played samples. Rotate through
onboarding; economy; professions/races; gear/combat; bosses/progression; settlements;
mounts/AI; blocks/worldgen; native machinery/transport; guides/events; GUI/atmosphere;
inventory/home/network; performance and legacy interactions.

For each finding record ID, severity, timestamps, exact input/repro/test mode,
expected player experience, actual evidence, fun/usability impact, exact IDs and
Atlas/source paths, demonstrated cause or labeled hypothesis, proposed owner fix,
native reuse, acceptance and recheck. Evidence levels: `IN_GAME_OBSERVED`,
`SOURCE_CONFIRMED`, `STATIC_ATLAS_ONLY`, `HYPOTHESIS`, `BLOCKED`, `NOT_TESTED`.
Scenario outcomes: `PASS`, `FAIL`, `PARTIAL`, `BLOCKED`, `NOT_TESTED`; PASS requires
observing the expected outcome. Commands/Creative preparation never prove Survival
accessibility. Do not fabricate a second authenticated player, FPS, screenshots or
personal gameplay experience.

Keep raw/private data outside tracked reports; curate redacted logs and hash-indexed
real screenshots. Separate actual, after, concepts and references. Use exact Taste
Engine pairs for art decisions; missing taste blocks subjective redesign, not
objective readability inspection or independent source audits.

## Checkpoint, fix and finish

Checkpoint after significant actions and at least every 15 active minutes. Include
process IDs, ports/world, native generation/update state, last observed screen,
evidence locations, uncommitted files, blockers and exact next action without
secrets. On resumption verify actual processes/screens instead of trusting state.

After a substantial report exists, fix severe blockers or high-impact verified
defects in their owner. Follow Registry Keeper for same-task impact history.
Capture baseline, build and install locally, replay exact steps and regressions.
Use `IMPLEMENTED_UNVERIFIED` until runtime replay succeeds; never equate compilation
with a gameplay fix or override the repository's user-approval resolution rule.

Reserve the final 45 minutes for consolidation. Verify links and honest coverage,
deduplicate findings, stop only task-owned runtime processes and document restore
instructions. Commit/push compact reviewed task files according to repository
rules, excluding credentials, worlds, raw dumps and unrelated pre-existing edits.
Disable the run's chat automation when its stop condition is reached.

Final response links the report and states confirmed active time, actual gameplay
counts, artifact/generation parity or precise blocker, key findings, verified versus
unverified changes, Git status/push outcome and exact resumption queue.
