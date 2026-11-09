# JEM Release item retextures

Status: `POTION_CANDIDATE_AWAITING_USER_REVIEW`

The user requested potion and enchanted-book retextures for Major Update 4, with modded content support where the installed rendering contracts permit it

## Ownership

- `tools/build_jem_release_resource_pack.py` remains the deterministic owner of the active `JEM Release v0.3.zip`
- Minecraft 1.20.1 owns the native item models and runtime tint behavior
- This work changes presentation only and does not change potion effects, enchantments, recipes, registries or gameplay state

## Compatibility design

- Standard, splash and lingering potions use one shared bottle family plus Minecraft's runtime potion-color overlay
- Modded potion effects that use the standard Minecraft potion items inherit the new bottle and retain their native runtime color
- The standard enchanted-book item uses one shared texture for all stored enchantments
- Vanilla and modded enchantments stored in `minecraft:enchanted_book` therefore inherit one consistent book retexture without an enchantment allowlist
- Mods that register independent bottle or book items keep their own textures unless a later inspected adapter explicitly adds them

## Candidate boundary

- The first representative asset is [potion-candidate.aseprite](candidate/potion-candidate.aseprite)
- It is a native `16x16` RGB sprite with transparent pixels and separate `Bottle Base` and `Tint Overlay` layers
- The form uses a decisive dark outline, cool glass, a warm stopper and a high-value liquid highlight for inventory readability
- The inspected originals are the Minecraft 1.20.1 potion texture, `Exact Wanted GUI Approach and Style 3.png`, `Exact Wanted GUI Approach and Style 4.png`, `Снимок экрана 2026-10-20 214733.png` and `Снимок экрана 2026-10-20 214948.png`
- No reference pixels, generated pixels, resized pixels or traced pixels are present
- The candidate is not yet inside the managed resource pack and has not changed the client publication
- After user approval, the same visual language will be applied to standard, splash and lingering bottles plus the shared enchanted book

## Review files

- [Native 16x16 PNG](candidate/potion-candidate.png)
- [Checker preview](candidate/potion-candidate-checker-16x.png)
- [Light-background preview](candidate/potion-candidate-light-16x.png)
- [Dark-background preview](candidate/potion-candidate-dark-16x.png)
- [Pixel-grid preview](candidate/potion-candidate-grid-24x.png)
- [Static C0 audit](visual-audit-c0.json)
