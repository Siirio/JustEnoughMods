# New Year

New Year is owned by JEMServer's persistent calendar lifecycle. Its default Asia/Almaty window begins December 24 00:00 and ends January 7 00:00, end-exclusive, crossing the calendar year.

The scheduler persists season year, UTC boundaries and idempotent `PLANNED`, `ACTIVE`, `ENDED`, `CLEANED` transition IDs. Restart, mid-window startup and wall-clock rollback must not repeat a transition. Operator preview/force/stop and per-player presentation mute are control surfaces; neither creates a daily-login reward.

Players opt in with `/jem holiday join calm` or `/jem holiday join power`. Calm progress observes three ordinary fish and awards the winter collectible once. Power creates one bounded local group of five tagged Strays around the consenting player when loaded collision-safe positions exist; it changes no blocks and never targets an offline settlement.

`/jem holiday gift <player>` moves the complete held stack into durable server SavedData escrow only while New Year is active and both authenticated players are online. The recipient explicitly accepts the displayed UUID with `/jem holiday accept <uuid>`; the sender can recover an unaccepted stack with `/jem holiday cancel <uuid>`. Delivery and cancellation first prove complete inventory capacity, and accepted/canceled entries are removed exactly once. Gifts remain recoverable after the calendar window rather than being deleted by cleanup.

