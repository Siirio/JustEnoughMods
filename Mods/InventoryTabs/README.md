# Inventory Tabs JEM compatibility patch

This module owns the disclosed JEM rebuild of Inventory Tabs 0.10.1 for Minecraft 1.20.1 Forge

- Upstream source: `https://github.com/dhyces/InventoryTabs.git`
- Pinned commit: `ed11c477a1cb6e6b184d73df1e8784ee22268a04`
- Upstream license: MIT
- Patched version: `0.10.1-jem.3`

The patch removes login-time construction of every registered entity type in a synthetic level. Entity tabs are instead discovered from actual nearby entities already present in the client level. Block tab discovery, stale-tab removal, hover text, line-of-sight checks and tab opening all require the target chunk to be loaded before reading it

The public `TabProviderRegistry` registration methods, block and entity tab behavior, native mod ID and configuration remain compatible with upstream. Tests and gameplay launches are not part of the production build route

The JEM pagination patch uses one top row per page. Additional tabs move to the next page and remain reachable through the native arrow controls. It removes the upstream second row that overlapped container inventory content

The screen-lifecycle patch binds the tab manager to the container screen at its first render or input entry point. This supports third-party container screens such as Curios that override `init()` without calling the vanilla implementation. The patch preserves native Inventory Tabs behavior and prevents a null current-screen dereference

Prepare the pinned source without building:

```powershell
python Mods/InventoryTabs/prepare.py
```

Build the production artifact with Java 17:

```powershell
python Mods/InventoryTabs/prepare.py --build --java-home C:/Users/user/.jdks/ms-17.0.15
```

The result is `Mods/InventoryTabs/build/libs/inventorytabs-forge-1.20.1-0.10.1-jem.3.jar`
