# Recovery for the October 21 Xaero startup crash

This release-scoped adapter runs the installed native AutoModpack planner and
UpdateAttempt outside Minecraft, so a broken old mod projection cannot prevent
the player from reviewing and committing the fixed release. It requires the
matching jem.8 bootstrap and the already fetched MineRENT journal for content
token `14522bb63bb0ef90ecb9a71e1650cb2bb19402c6`.

The package supplies only the exact Settlements/manifest payload through native
ClientObjectStore. The native plan may replace those two paths and remove exact
gameDir JAR duplicates only when they remain in the published projection.
Conflicts, other changes, a different release, a different bootstrap, pending
transactions and running Minecraft stop recovery. The player's confirmation
dialog is the sole approval point. No active pointer, trust policy or server
mirror is edited, no raw managed file is installed and no cache is removed.

Production compilation uses Java 17 and the matching bootstrap/profile
dependencies. Repair.ps1 finds the installed launcher runtime and libraries;
Repair.cmd opens it for the player. The generated JAR is a standalone utility,
never an installed mod or a served AutoModpack artifact. This workflow does not
launch Minecraft. Compilation does not establish player runtime acceptance.
