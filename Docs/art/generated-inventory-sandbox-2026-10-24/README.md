# Generated shiny inventory sandbox trial

The user requested trying the generated image as a resource pack in `jem sandbox`, explicitly without Aseprite. This is an experimental E-screen atlas replacement, not a geometry-approved native texture or the completed three-GUI pack.

The source PNG is copied byte-for-byte into `pack/assets/minecraft/textures/gui/container/inventory.png`. The generator returned 1254x1254 RGBA rather than the requested 256x256. No resizing, cropping, repainting, alpha repair or Aseprite work was performed. Protected auxiliary pixels and exact UV alignment are not established. Visible alignment defects are expected and remain open.

`pack/` owns the two-file resource-pack source. `delivery.json` records the PNG and ZIP hashes and the sandbox installation. The ZIP is installed in the exact `jem sandbox` resourcepacks directory and appended at highest priority in its existing resource-pack selection. Other options and resource-pack selections are preserved. A byte-exact copy of the previous options file is kept under that profile's `visual-backups/generated-inventory-20261024/options.txt`.

Open the ordinary player inventory with E on the next manual sandbox launch. Disable `JEM_Shiny_Inventory_Experimental.zip` in Resource Packs to return to the previous appearance. No client or game was launched during delivery. Actual loading, slot alignment, auxiliary sprites and gameplay appearance remain UNOBSERVED. No anvil or librarian texture is included.

Local Server, Client/ManagedPack, AutoModpack generations, `test jem`, stable profiles, MineRENT and the immutable release baseline are outside this sandbox-only trial. No builds or tests were run.
