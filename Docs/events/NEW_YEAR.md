# New Year

New Year is owned by JEMServer's persistent calendar lifecycle. Its default Asia/Almaty window begins December 24 00:00 and ends January 7 00:00, end-exclusive, crossing the calendar year.

The scheduler persists season year, UTC boundaries and idempotent `PLANNED`, `ACTIVE`, `ENDED`, `CLEANED` transition IDs. Restart, mid-window startup and wall-clock rollback must not repeat a transition. Operator preview/force/stop and per-player presentation mute are control surfaces; neither creates a daily-login reward.

New Year retains its saved season, calendar notices, time window, admin preview/force controls, per-player mute and consent-based durable gift escrow. The rejected calm/power activity choices and their mob chores are retired; old activity records remain readable but grant nothing.

`/jem holiday gift <player>` moves the complete held stack into durable server SavedData escrow only while New Year is active and both authenticated players are online. The recipient explicitly accepts the displayed UUID with `/jem holiday accept <uuid>`; the sender can recover an unaccepted stack with `/jem holiday cancel <uuid>`. Delivery and cancellation first prove complete inventory capacity, and accepted/canceled entries are removed exactly once. Gifts remain recoverable after the calendar window rather than being deleted by cleanup.

