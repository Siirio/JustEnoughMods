# Halloween

Halloween is owned by JEMServer's persistent calendar lifecycle. Its default Asia/Almaty window is October 29 00:00 through November 1 00:00, end-exclusive.

The scheduler persists year, UTC boundaries and idempotent `PLANNED`, `ACTIVE`, `ENDED`, `CLEANED` transition IDs. Restart, mid-window startup and wall-clock rollback must not repeat a transition. Operator preview/force/stop and per-player presentation mute are control surfaces; neither creates a daily-login reward.

Players opt in with `/jem holiday join calm` or `/jem holiday join power`. Calm progress observes four real pumpkin pickups and three ordinary fish catches, persists per authenticated account and awards each small real-item completion once. Power creates one bounded local group of five tagged zombies around the consenting player when loaded collision-safe positions exist; only those tagged mobs advance that player's objective. It does not change blocks, raid towns, create a global threat or mint Coins.

Mute state suppresses actual lifecycle/login notices. Activity progress survives reconnects and restarts. The current implementation creates no transient block POI, so cleanup never touches world blocks or player builds.

