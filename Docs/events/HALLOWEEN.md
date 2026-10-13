# Halloween

Halloween is owned by JEMServer's persistent calendar lifecycle. Its default Asia/Almaty window is October 29 00:00 through November 1 00:00, end-exclusive.

The scheduler persists year, UTC boundaries and idempotent `PLANNED`, `ACTIVE`, `ENDED`, `CLEANED` transition IDs. Restart, mid-window startup and wall-clock rollback must not repeat a transition. Operator preview/force/stop and per-player presentation mute are control surfaces; neither creates a daily-login reward.

Halloween retains its saved season, calendar notices, time window, admin preview/force controls and per-player mute. The rejected calm/power activity choices and their mob chores are retired; old activity records remain readable but grant nothing.

Mute state suppresses actual lifecycle/login notices. The current implementation creates no transient block POI, so cleanup never touches world blocks or player builds.

