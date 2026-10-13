# FTB and Xaero integration

FTB Quests `2001.4.22` is an observational guide. The eight SNBT chapters use native schema version 13 and connected native item/checkmark tasks for attainable onboarding, profession, role, exploration, boss preparation, Create, town/economy and event steps. They have no reward tables and never grant XP, Coins, race, boss or town state. Manual checks remain only where FTB has no authoritative JEM criterion bridge (opening an interface or completing a planning gesture). The theme overrides only the documented FTB theme resource and uses JEM brown/copper/cream colors.

Building projects use the existing JEM/Xaero map protocol v5. The server persists authenticated owner, dimension, name, stable color, source rectangles, collaborators and revision. Positive-area overlap merges visually; edge/corner contact does not. Removing a source rectangle deterministically recomputes components on the client. Limits are 20 projects per owner, 64 source rectangles per project, 1,000,000 union blocks² and two mutations per second.

Project regions are metadata and rendering only. `RegionPermissions` exposes no block, chest, PvP, door, teleport or interaction permission decision. Xaero Regions `1.0.8` is intentionally not installed because its exact metadata requires Forge `47.4.20`, incompatible with JEM's pinned `47.4.10`.

