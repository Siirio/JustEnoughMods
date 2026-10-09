# JEM Achievement Guide Rules

- This is a Forge 1.20.1 Java 17 advancement runtime with an isolated client guide integration.
- Preserve retained `jem_guide:*` IDs only while their exact player-facing mechanic and proof remain unchanged.
- Keep guide content JSON-driven and localized in English and Russian.
- Runtime must not depend on source folders, scripts, Python, or absolute filesystem paths.
- Build the reobfuscated release JAR and install only that JAR into the Minecraft runtime.
- Preserve the generated custom/native/suppressed counts reported by the current manifest and release validator; deleted IDs must not return or be reused.
- Macaw's Roofs is excluded from the target; no advancement, guide entry, or runtime integration may depend on it.
- Visual advancement parents control layout only and must never become generic gameplay prerequisites.
- Every custom node must declare explicit criteria and an authoritative provider; generic uncalled node criteria are release blockers.
- `jem_guide:building/4` must accept furniture blocks from every installed mod, not only Handcrafted, because the pack presents all modded furniture as one building progression.
- Accept `jem:*` and `jem_guide:*` datapack definitions only when that exact advancement is bundled in the current JAR, because global KubeJS/Paxi or deleted-world remnants must never restore retired tabs in new worlds.
- Never restore `jem_guide:bosses/root`; place its surviving content below `jemcompat:campaign/all_eyes` so the Story root remains exactly the twelve MAIN branches.
- External native advancement roots must never be reparented into JEM tabs, because completing a mod-owned advancement must not make a temporary foreign branch appear in the unified story.
- Player-facing advancement and Patchouli text must describe the world, encounter, or mechanic without naming the source mod, because the pack presents itself as one game.
