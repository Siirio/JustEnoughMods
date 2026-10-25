# Quest Atlas background production

User authorized Gemini to complete missing art without further questions while they sleep. Use Work mode, Gemini 4 Argon Medium. Observed quota: Gemini shared five-hour 100%, shared weekly 99%, Argon weekly 38%. Do not enable paid overages or change account settings.

Work only in C:/MyProjects/Minecraft/JustEnoughMods/Docs/quests/art-20261025. Other agents own gameplay and quest data. Do not change code, config, client profiles, worlds, Git history or deployment. Produce actual PNG files under generated/ with source references and a short result.json receipt. Do not merely describe an image.

Read the exact Pair guidance fully:

- C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/Background Images/gemini-pixel-environment-backgrounds/TASTE.md and TECHNICAL.md
- C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/Logos/gemini-minecraft-pixel-title-emblems/TASTE.md and TECHNICAL.md
- C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/ART_DIRECTION.md

Open all six actual supplied originals from originals/ and sources.json. Also inspect the relevant classified positive originals listed by the Pairs. The six supplied originals are the chosen identity art. The user explicitly requests extending the space around them by continuing each background color so the result works as a whole quest-page background. Outer space means surrounding empty canvas, not a request to turn every theme into cosmic scenery.

Native consumer: FTB Quests 2001.4.22 chapter images, centered x/y and unrotated image. Width/height are quest units, scaled at 24 GUI pixels/unit at default zoom. Quest nodes draw and receive input above the image. Native pan/zoom intentionally extends past content, so a finite PNG must not be described as covering every possible pan.

Production design:

- Preserve each supplied title and subject exactly as its category identity. Extend the quiet surrounding color field with subtle theme-appropriate clusters and very low visual contrast, keeping ample readable quest space
- The six themes are biomes, companions, home, food, engineering and magic. Use their matching original color, not a universal gray or black replacement. Magic may retain its sparse stars. Other themes must not gain arbitrary cosmic stars
- Work at 3072x2048 for each full illustrated background. Deliver RGB or RGBA PNG, no controls, quest nodes, screenshots, watermarks, additional typography or inventory sprites
- A compact original title panel is displayed separately at roughly 12x5 native quest units. Provide a matching quiet full-page extension WITHOUT duplicating the title across the node space, plus an optional combined composition for review. The source JPG will be reused whole as the centered identity panel, with no crude cutout or tiny-icon reduction
- If a seamless center transition requires a new composition, preserve source appearance and supply a clean actual PNG. Do not hide a mismatch behind blurred edges, fake pixelation or a black rectangle
- Begin with home as the representative C0. Inspect it before expanding to the other five. At most two correction rounds per background
- For the additional Boss Busters, Power System, Nightfall and Twelve Eyes identities, follow the brief's existing-game-icon and native localized-title route. You may direct their palette and layout, but do not generate substitute item icons or fake boss portraits
- Supply concise native layout art direction consistent with the user's raw biome constellation, companions river delta, 1–3-node home gallery islands, kinetic rail spine and twelve-leaf tournament bracket. Never invent gameplay dependency edges

The task is explicitly illustrated-background production, not an operational GUI texture repaint or an icon-authoring task. Native controls and game icons stay native. Keep the actual original files and all generated candidates, identify which was selected by your review, and never call an agent pass user approval.

When complete, return exact local PNG paths, dimensions, source mapping and remaining defects. If generation is unavailable, report the specific capability failure without using a different generator or a procedural visual placeholder.
