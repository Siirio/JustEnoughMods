# JEM Rewards

Independent Forge 1.20.1 / Java 17 item-reward owner (`jem_rewards`). No SMP or Economy dependency. Install the matching artifact on server and client; `jem_rewards:main` protocol 1 is mandatory.

JEM producers enqueue exact personal ItemStack snapshots with stable source/attempt IDs. Boss distribution freezes every eligible recipient and per-recipient roll in one recoverable batch. Each participant receives a full copy. Native world loot outside these JEM producers is unaffected.

Normal inventory E shows two columns of four native-texture quick-take slots and pages. Left-click transfers into free inventory space; leftovers remain pending. No cursor ownership, deposit, throw, hotbar or drag path mutates rewards. Server validates session, revision, page and exact item selection. Recipe-book collision uses a native reveal button; JEI excludes the panel rectangle.

`jem_rewards.dat` owns pending snapshots, issue/import receipts, batch journals and claim recovery. Inventory transfer saves a native player receipt before finalizing the reward list. Every later mutation resolves interrupted claims first. Exact offline legacy SMP/boss rewards migrate once without loading SMP classes. The original account owner retains starter-kit eligibility and a durable recipient binding. Checked temporary-file replacement preserves failed saves as dirty state for retry. No implementation can guarantee survival of an operation whose first disk write fails and whose process is then lost.

Production compilation is distinct from acceptance. Gameplay, crash/reconnect scenarios and GUI-scale/resource-pack compatibility have not been exercised under the project's no-test/no-launch instruction.

Resource metadata is owned by `src/main/resources/pack.mcmeta` (Minecraft 1.20.1 pack format 15). It must be present at the production JAR root so Forge can include the mod translations without a broken-resource warning. Packaging and local delivery evidence: [resource-metadata-delivery.json](resource-metadata-delivery.json).
