# JEI Catalog Manifest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace unstable JEI runtime category inference with one generated and checked-in exact item-to-leaf-category manifest

**Architecture:** A build-time Python tool seeds and validates a category manifest from the Registry Atlas while preserving existing assignments. JEMCompat loads the manifest once and uses direct registry-ID lookup for JEI and JEM Economy

**Tech Stack:** Python 3, JSON, Forge 1.20.1, Java 17, Gson, JEI 15

**Spec:** `Docs/catalog/JEI_CATALOG_MANIFEST.md`

## Global Constraints

- Do not add or run automated tests or test fixtures
- Do not launch Minecraft
- Preserve one primary leaf category per item
- Preserve accepted `CHANGELOG.md` assignments
- Build only the required production artifact under the standing project authorization
- Publish and apply the managed client update only through native JEM Pack Sync

## Review Focus

- Duplicate item IDs must fail validation instead of using the last value
- Parent categories must fail validation as assignment targets
- Unknown live items must appear in `OTHER` without changing the manifest
- Existing manifest entries must not move when the generator runs again
- JEM Economy building and nature checks must use the same manifest result as JEI

---

### Task 1: Author and validate the durable manifest

**Files:**
- Create: `tools/generate_jei_catalog.py`
- Create: `Mods/JEMCompat/src/main/resources/assets/jemcompat/jei_catalog.json`

**Interfaces:**
- Produces: schema version 1 JSON with `assignments`, where each leaf category contains sorted unique registry IDs

- [x] Implement Atlas item discovery and accepted category seeding
- [x] Preserve every existing assignment when updating the manifest
- [x] Add validation for duplicate IDs, invalid IDs, parent categories, and incomplete schema
- [x] Generate the initial exact assignment file
- [x] Run the tool in validation mode and inspect category counts

### Task 2: Replace runtime inference with manifest lookup

**Files:**
- Create: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/catalog/JemCatalogManifest.java`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/catalog/JemCatalogCategories.java`
- Modify: `Mods/JEMCompat/src/main/java/com/siirio/jemcompat/client/jei/CuratedJeiCatalog.java`

**Interfaces:**
- Produces: `JemCatalogManifest.category(ItemStack)` and `JemCatalogCategories.classify(ItemStack)`
- Preserves: reflective `isMarketBuilding(ItemStack)` contract

- [x] Load and validate the checked-in manifest once
- [x] Reduce `JemCatalogCategories` to the shared manifest adapter
- [x] Remove JEI recipe-catalyst and weapon inference passes
- [x] Aggregate both navigation parents from their leaf lists
- [x] Preserve JEM Economy's old reflective contract through the manifest-backed compatibility shim
- [x] Review all callers for removed signatures and caches
- [x] Give the curated search EditBox one render and input owner so standard JEI mode hides it and clears focus

### Task 3: Record, package, and publish the change

**Files:**
- Create: `Catalog/changes/records/compat.jei-exact-catalog-manifest-20261121.json`
- Modify: `CHANGELOG.md`
- Modify: generated Atlas effective surfaces
- Replace: `Client/ManagedPack/mods/jemcompat-*.jar`
- Replace: `Server/mods/jemcompat-*.jar` when the development server inventory owns the same universal artifact

**Interfaces:**
- Consumes: validated manifest and source implementation
- Produces: fresh JEMCompat artifact and published Pack Sync generation

- [x] Add the Atlas change record
- [ ] Rebuild the Atlas effective surfaces after the pre-existing `develop` artifact drift is reconciled
- [x] Add the user decision and diagnosed root cause to `CHANGELOG.md` with status `in testing`
- [x] Build the required JEMCompat production artifact without tests
- [x] Synchronize the exact artifact through the documented module packaging path
- [x] Stage and preview native JEM Pack Sync
- [ ] Publish native JEM Pack Sync after the unrelated `+8 ~7 -5` develop-to-Test rollback is reconciled
- [ ] Apply the published generation to the closed `Test JustEnoughMods` profile and confirm its active token and JAR hash
- [ ] Review exact diffs, commit exact paths, push `develop`, and confirm the remote commit
