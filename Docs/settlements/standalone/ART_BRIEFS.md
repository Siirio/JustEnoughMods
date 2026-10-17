# Artwork production briefs

Status: **production contracts only; no generated or accepted production sprites**. Artwork starts after UI/data contracts and one control-family candidate review. Use the placement wireframe and coordinates in `GUI_SPEC.md` as the mockup for each group; it is not runtime evidence.

## Active direction and sources

Exact Taste Engine pair: `C:/MyProjects/Coding/Solo/Ultimate-Skills/Taste-Engine/Pairs/Minecraft/GUI/Forge-1.20.1-jem-smp-screens/{TASTE,TECHNICAL}.md`. Taste is READY for labelled pixel/material preferences; technical workflow remains PROVISIONAL and runtime output unverified. The present user brief specifies this deliverable's compact native style, overriding unrelated broader themes.

User-supplied references:

- Native Minecraft GUI: [supplied image](https://images.openai.com/static-rsc-4/OBDK8OjcVBNV7rba9RaXPlCT3GVMo1jE9qI5xGhaM7kwYYSjZdoLkoJy3-AIPHOiJTznrbynDB45Yhzi9RC9kkwuG9JWwavD3glhGCMT5RcHo-_VBzXuvU7KU-0wGg5JobrTJrKFVoS7ncNQ_WnKHDX0blURxXkhfBXuZoflHzs?purpose=inline). User extraction: compact icons, inventory-like proportions, readable buttons.
- Antique Atlas territory atmosphere: [supplied image](https://images.openai.com/static-rsc-4/Ou8wQOqyFSR77p6WK_eLRw3MkmIHMkEBSg5sLe0gzL4tSEo4yBIOQJi6FQFFggEuGUJKbfJem4OCmdjFf9BcCT-eIWcKof2x76DjuXrWTOYpNHiddkVfixJydjHfrl99Mk2f3LBS_XVILLWF0Otdj7_71PsaMBKno50uxwU5pE0?purpose=inline). User extraction: exploration-map atmosphere, legible terrain, settlement markers.

Browser text retrieval returned no inspectable pixels for these static image URLs during this audit. Therefore no image-specific color, corner, material or licensing inference has been made. Artist must visually inspect original references and relevant vanilla1.20.1 GUI assets before drawing; retain attribution and screenshot evidence. Never crop reference art into production files. No selected reference implies permission to copy Antique Atlas assets.

## Shared export contract

Proposed runtime base: `Mods/JEMSettlements/src/main/resources/assets/jem_settlements/textures/gui/`. Editable layered sources: `Docs/settlements/assets/source/`; group previews: `Docs/settlements/assets/review/`. Keep source and exact exported PNG association in a manifest before integration. Use external layered pixel tools; no texture painting in Blockbench.

Palette proposal by Codex (not user approval): outline `#292824`, stone shadow `#55534C`, stone mid `#807D70`, stone light `#B6B09D`, parchment shadow `#A08A62`, parchment base `#C9B88D`, parchment light `#E6D7AE`, wood dark `#4D3828`, wood mid `#775438`, wood light `#A37A4B`, muted gold `#B89B4E`, gold light `#E0C677`, red `#B74739`, orange `#C47B36`, yellow `#C2AB45`, green `#6E984F`. Text rendered separately; choose actual text/background contrast during native inspection. Transparent pixels have alpha0, solid sprite pixels alpha255; programmatic overlays control translucent territory. No blur/AA/gradients/baked text. Nearest-neighbor, integer pixel positions; fixed-corner nine-slices never scaled fractionally.

Each icon occupies its own PNG. If atlased later:2px transparent gutter and edge extrusion around every sprite; UVs use content bounds, never interpolate adjacent sprites. Illustrations are never stretched. Export contract includes exact dimensions, no automatic padding/resizing.

## A — Core control family

| Filename under base | Exact PNG size | Slice / states / usage / placement |
| --- | --- | --- |
| `settlement_panel.png` |16×16 |3px fixed corners;3px edges tile; restrained stone frame/parchment interior; shell `(0,0,352,240)` |
| `settlement_panel_small.png` |16×16 |3px slices; simpler inset stone tray; content viewport and lists |
| `settlement_button.png` |16×20 |3px slices; shallow stone bevel;20px rendered height; real action footer |
| `settlement_button_hover.png` |16×20 |Same silhouette/corners, lighter rim; no enlarged hitbox |
| `settlement_button_pressed.png` |16×20 |Same silhouette, inverted bevel; label moves1px in code |
| `settlement_button_disabled.png` |16×20 |Same silhouette, reduced value contrast; readable label rendered separately |
| `settlement_tabs.png` |16×80 |Four16×20 vertical cells: default/hover/selected/disabled; each3px slices; sidebar20px rows; compact selector reuses buttons |
| `settlement_tooltip.png` |16×16 |3px slices; dark wood/stone backing, quiet gold rim; mouse/focus tooltip |
| `settlement_dialog.png` |16×16 |3px slices; stronger stone corners, subdued wood frame; confirmation width224 |
| `settlement_book_page.png` |128×160 |Unstretched parchment with quiet margin; content8px inset; book route only |
| `status_track.png` |16×8 |2px fixed end caps; middle tiles; two separate labelled bars |
| `status_fill.png` |1×4 |Solid neutral mask; color and integer fill width supplied by renderer |

Editable layers: frame/corners/edges/surface/state accents separately. Transparent exterior corners only; opaque text area. No animation frames for core controls. Hover/press is discrete native state; optional brief code fade must not blur pixels. Preview on dark cave and bright grass, native1× and nearest4×, with long Russian button labels and every state. Avoid ornate corners, broad empty brown fields, huge background paintings and decorative fake slots. First review candidate contains panel+four buttons+tabs+two bars, not the full art set.

## B — Navigation and action icons

All exact16×16 RGBA, transparent exterior, no animation. Folder `icons/`; filenames:

`settlements.png`, `map.png`, `residents.png`, `happiness.png`, `food.png`, `workers.png`, `recruits.png`, `treasury.png`, `settings.png`, `help.png`, `add.png`, `remove.png`, `edit.png`, `search.png`, `filter.png`, `expand.png`, `locate.png`, `warning.png`, `close.png`, `back.png`.

Composition: one readable dominant silhouette inside14×14,1px transparent safety margin. Settlement=small roof/bell; map=folded parchment; resident=native villager head; happiness=clear smiling face; food=meal/bread silhouette; workers=tool; recruits=shield; treasury=coin pouch; settings=compact gear; help=book; actions use familiar plus/minus/pencil/magnifier/funnel/outward-corner/target/triangle/cross/arrow silhouettes. These are proposals to review, not approved artist improvisations. Gold is emphasis, not the color of every icon. Palette uses shared stone/wood/parchment with restrained role accent.

Default sprite only: button hover/pressed/disabled/focus belongs to control rendering, avoiding20×4 redundant icon variants. Placement: sidebar `(12, row+2,16,16)`, header emblem `(8,12,16,16)`, header actions20×20, resident detail actions/footer. Preview whole sidebar and toolbar at native size with labels; no isolated glossy icon presentation. No baked letters, realistic gloss, fantasy class crests or invented profession icons.

## C — Status indicators

Folder `status/`, exact12×12 RGBA, transparent, no animation: `happiness.png`, `hunger.png`, `healthy.png`, `hungry.png`, `unhappy.png`, `hostile.png`, `worker_active.png`, `worker_idle.png`, `storage_full.png`, `payment_due.png`, `rebellion.png`, `settlement_alert.png`.

Each must differ in silhouette: face/food bowl/check/empty bowl/frown/raised angry brow/moving tool/resting tool/closed packed chest/coin+clock/broken shield/alert flag. Critical symbol includes shape and localized text, not red alone. Use shared value hierarchy with green/yellow/red accents. Place beside resident bars `(row right edge-16,row+4)`, worker/recruit state, selector warning and notification icon inset8. No looping blinking. Preview all indicators side by side, including desaturated view; status bars use track/mask from A, not12 separate fill textures.

## D — Map markers and controls

Folder `map/`, exact16×16 RGBA transparent: `settlement_marker.png`, `player_marker.png`, `settlement_center.png`, `expansion_cursor.png`, `invalid_claim.png`, `selected_claim_border.png`, `zoom_in.png`, `zoom_out.png`, `center_settlement.png`, `center_player.png`.

Settlement=roof/bell pin, center=small anchor diamond, player=directional arrow with outline; distinguish at1×. Marker anchor `(8,14)` except player `(8,8)`; rotation only player. Invalid=diagonal cross; expansion=bracket/plus; selected border is a2px corner/edge mask with transparent center,2px fixed nine-slice corners. No art animation. Programmatically draw filled **entire real territory** and selected added strip; no fixed map image pretending to be real terrain. Proposed low-alpha own fill and different hue/line style for other public territories; fill alpha decided in runtime legibility review. Do not reveal personal waypoints or unexplored terrain.

Placement mockup: main content viewport,20px controls top-right, quote/footer below; markers transformed by the same pan/zoom as hit geometry. Preview known/unknown terrain, adjacent differently marked towns, valid/invalid/selected union and a multi-block strip with correct total price. Avoid antialiased arrows, decorative compass obscuring terrain and outline-only ownership.

## E — Five canonical coins

Reuse actual assets and item rendering from the single canonical currency owner: `jem_economy:copper_coin`, `silver_coin`, `gold_coin`, `aether_coin`, `arcane_coin`. Do not create replacement settlement coin PNGs or duplicate registry definitions. Runtime16×16 inventory presentation. If canonical source art needs extraction into the shared currency artifact, preserve exact bytes and licenses/source identity; no image generation for this group.

Placement: treasury balance, deposit/withdraw denomination input, founding quote and expansion total. The unit quote always displays canonical Gold item+1; total displays exact added blocks × price. Preview all five native icons on both list backgrounds; aether/arcane must retain their existing distinct appearance. No emerald, sixth denomination or generic yellow circles.

## F — Resident and optional-role presentation

Live entity render is preferred, not a fabricated PNG portrait. No new profession illustration authorized until registered capabilities and native roles are enumerated. Existing supported Workers roles: farmer/miner/fisher/cook/builder/courier/merchant/lumberjack/animal farmer; Recruit is distinct and depends on Recruits. If needed, role PNGs are exact16×16 transparent, named from the verified role ID under `roles/`. Use actual Minecraft item icons where they accurately describe the role; do not generate every enum entry automatically.

Placement mockup:64×112 resident portrait column,16×16 list icon; unloaded entity uses explicit profession/role symbol and unavailable label. Existing villager profession remains distinct from settlement job. No invented armor, fantasy occupations, auto-worker conversion or fake entity when chunks are unloaded. Native trading assets/layout remain owned by Minecraft.

## G — Guidebook diagrams

Folder `guide/`; each exact96×64 RGBA with transparent exterior; no animation, no baked text. Filenames: `founding.png`, `territory.png`, `feeding.png`, `housing.png`, `food_satisfaction.png`, `safety.png`, `jobs.png`, `community.png`, `decoration.png`, `assign_work.png`, `chest_delivery.png`, `payroll.png`, `rebellion.png`.

Use shared palette plus native item/block colors where essential. One clear cause/action/result composition per diagram; no unrelated scenery. Founding shows actual bell; territory shows rectangle+added strip; feeding real item->villager; housing bed/home; satisfaction request->food; safety native recruit; jobs actual work site; community actual resident mood; decoration real amenity placement; assignment native worker/work-area; delivery physical input/output chest; payroll treasury->recruit with7-day label rendered in code; rebellion three unpaid period symbols->hostile recruit, defeat->paid hire recovery. Optional systems explicitly captioned as requiring their mod; guide must not imply they exist in clean installation.

Placement: centered within112×144 book content, image96×64 at top with text below; long paragraphs continue next page. Preview each diagram at native1× in the actual page region with Russian and English captions outside PNG. Avoid narrator-like decorative scenes, invented recipes/yields, huge character paintings and ambiguous arrows. No claim that these assets explain a mechanic until source/action correspondence is reviewed.

## Handoff and acceptance

Every group supplies layered source, exact PNGs, manifest with size/slice/anchor/state/usage, native1× sheet and nearest4× sheet on bright/dark backgrounds. First inspect one control family and one real populated screen before generating the rest. Runtime review must include small GUI scale, long Russian, focus/disabled states and actual data. Any temporary material is labelled candidate and kept replaceable; no unfinished art declared approved. No art assets were generated during this design checkpoint.
