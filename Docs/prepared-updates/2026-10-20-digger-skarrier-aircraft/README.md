# Digger, Skarrier and aircraft resource pack — local delivery

The user initially requested preparation only, then explicitly authorized applying the changes to the local Server and Client and investigating connection refusal. Stable profiles, MineRENT and the immutable release baseline remain outside this delivery.

## Digger ownership and behavior

`JEMPackCore` retains the one-level `jem_pack_core:excavation` enchantment, existing Shift+right-click toggle and native inventory synchronization. Its shared active predicate also requires Shift to be released. The shared pattern remains the nine positions in the hit-face plane.

Previously `BlockEvent.BreakEvent` expanded eight neighbors before root permission/cancellation processing finished; each native mining call charged normal wear. The replacement wraps native mining completion once, expands only after successful root removal and budgets only successful blocks that requested normal DiggerItem wear. Three narrow native Mixins provide completion, successful removal and wear seams; one scoped operation owner holds no persisted player state or extra network channel.

For nine ordinary blocks: no Unbreaking costs exactly 4; Unbreaking III costs exactly 0 or 1 with equal probability, without a second Unbreaking roll. Codex chose vanilla Unbreaking I/II rolls over the four-point budget (means 2 and 4/3), and proportional randomized rounding for partial batches. Native zero-hardness zero-wear remains zero. Disabled mode or Shift uses native 1×1 wear. Tool break/replacement stops expansion. Permission/Forge cancellation, harvesting, drops, XP and SilkTouch/Fortune remain native. Tools fully overriding DiggerItem.mineBlock retain their own wear mechanism; universal suppression would overwrite third-party behavior.

## Skarrier and aircraft

See [Skarrier provenance and binary delta](skarrier/README.md) and [native Paxi activation](aircraft/README.md). Skarrier 1.1.1 hotfix is official CurseForge file9164445; both sides must match because new packets were added under the same channel version. No new JEM API adapter is needed; existing Carnager procedure signature remains.

Aircraft activation moves the unchanged 3.0 resource ZIP into Paxi’s mandatory config source. Native AutoModpack owns delivery and application; Paxi owns activation at the next client start. Personal options are untouched. No new Java system is added.

## Connection evidence and limits

At 14:39:44 the old server failed `TaxDurability.save` with native Windows file replacement error 5. It stopped, then localhost:31604 refused connections; a later watchdog crash was secondary. File permissions, free space and closed native streams do not establish which OS actor denied replacement. The preceding claim that Java atomic replacement semantics fully explained the failure was incomplete: the later native replacement also failed. No speculative persistence fallback or new code was added. Restart evidence and final publication/install state are recorded in `delivery.json`; player acceptance remains pending.

No automated tests, fixtures, gameplay scenarios or client launch are performed for this delivery. Production artifact builds, manual source/bytecode inspection and the ordinary server start/native publication are the permitted evidence; they do not prove gameplay acceptance.
