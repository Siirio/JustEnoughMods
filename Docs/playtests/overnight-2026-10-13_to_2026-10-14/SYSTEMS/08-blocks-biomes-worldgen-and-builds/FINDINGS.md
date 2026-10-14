# Evidence-backed findings

Runtime environment is versioned per case. Sources and static inclusion are never a system-wide gameplay pass.

## JEMQA-20261014-0012

**MEDIUM — Native tag groups are rejected because required addon references are absent**

Status: **OPEN** · Evidence: **SOURCE_CONFIRMED**. First recorded observation: 2026-10-13T19:22:01.656395+00:00; latest: 2026-10-13T20:30:58.869807+00:00. Recheck: NOT_TESTED.

**Mode/environment:** source/artifact inspection; no survival gameplay implied

**Attempt/reproduction:** Start exact local clone and inspect rejected-native-tags.json, then query impacted items/tags in the Atlas and actual runtime.

**Expected player experience:** Installed recipe/food/native integration tags resolve against actual matching enabled mods.

**Actual behavior and impact:** Isolated runtime TagLoader rejects 6 groups in the captured startup, including forge:cooked_meats, forge:raw_ham and kaleidoscope_nether:mod_items. Missing mandatory entries drop the affected tag; actual crafting/classification effects remain unplayed.

**Established cause / stated uncertainty:** Runtime confirms unresolved mandatory native references. Optional-addon absence, version mismatch and shipped tag typos require individual exact-JAR investigation; no blanket root cause is asserted.

**Proposed owner correction and native reuse:** For each group inspect native source/assets and intended optional dependency before deciding whether an addon, compatibility data or upstream fix is appropriate. Do not invent missing items.

**Acceptance:** Every required group resolves on current exact runtime; corresponding recipes/food/profession behavior works in Survival.

**Cost and regression boundary:** Review owning lifecycle and all consumers before changing behavior

**Exact IDs/selectors:** `tag:forge:cooked_meats`, `tag:forge:raw_ham`, `tag:kaleidoscope_nether:mod_items`

**Source/evidence links:**

- [EVIDENCE/traces/rejected-native-tags.json](../../EVIDENCE/traces/rejected-native-tags.json)
- [EVIDENCE/logs/isolated-startup-excerpts.txt](../../EVIDENCE/logs/isolated-startup-excerpts.txt)
