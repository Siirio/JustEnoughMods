# Recovery for the October 21 shipping and market update

This release-scoped adapter runs the installed native AutoModpack planner and
UpdateAttempt outside Minecraft, so a broken old mod projection cannot prevent
the player from reviewing and committing the fixed release. It requires the
matching jem.8 bootstrap and the already fetched MineRENT journal for content
token `43f6ddb2930f8a59a6e61fde5c033078b4a29b31`.

The package supplies exact reviewed Economy, Compat, Server, Settlements and
manifest payload through native ClientObjectStore. The native plan may replace those paths and remove exact
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

`package.py --prepare-only` builds the cumulative friend package without changing
the release baseline or published package reference. Its compiled review hashes
and token must match the prepared release; publishing an old recovery helper with
a new release is forbidden. The existing source utility is a bounded adapter,
not a replacement updater or an approval bypass.
