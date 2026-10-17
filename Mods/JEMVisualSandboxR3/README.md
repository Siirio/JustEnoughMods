# Standalone JEM Races / sandbox

Canonical player flow, progression, exact ability differences, authority and
execution limits: [Docs/races/README.md](../../Docs/races/README.md).

This module now owns its actual `JemRacesSandbox` entrypoint and a complete
standalone JAR. The pinned historical binary is no longer composed into output.
Source-owned resources and approved meshes remain. Optional Iron's integration
uses an isolated runtime adapter and compile-only3.15.6 API.

Build the production `jar`/`reobfJar` only when authorized. `prepare.py --profile
"C:/Users/user/AppData/Roaming/.minecraft/versions/jem sandbox"` stages the fresh
JAR, backs up previous sandbox artifacts and installs only into that exact named
profile. It does not launch Minecraft or copy into ordinary Server/ManagedPack.

The user prohibits tests and checks for this delivery. No test/QA/verification
command or gameplay launch forms part of installation. Compile/package success
is not runtime or visual acceptance. Editable textures/models remain in the
canonical `Docs/races/assets` folders, including the approved locked guardian
animation and its new whole-surface one-shot extension.
