# JEM Performance

JEM Performance is a separate Forge 1.20.1 mod. Install the same JAR on the client and the dedicated server. Integrated singleplayer uses the client's JAR. It requires the official MapSyncer 1.0.4 beta2 Forge 1.20 JAR and Forge 47.4.10 or newer

MapSyncer remains the sole terrain publisher. This mod cancels only MapSyncer's `TICK` timer, whose upstream path forces a full save of every dimension. It scans persisted MCA regions on one background coordinator thread instead. MapSyncer owns region conversion, timestamp tracking, transfer and Xaero output. Minecraft owns region saves. The MapSyncer client still uses its native two-minute auto-sync policy

New terrain appears after Minecraft saves its region and the next scan and client sync complete. This mod does not promise immediate transmission of unsaved chunks. It does not alter player waypoints, boss discovery, terrain generation, mobs, machines or rendering. `SCHEDULED` and explicit MapSyncer commands remain native. A manually requested full generation can still require a world save and should be used during maintenance

The installed MapSyncer artifact is distributed under GPL-3.0. This adapter is also GPL-3.0. The source of the exact MapSyncer release is available from [the upstream repository](https://github.com/RuoChennn/MapSyncer-for-XaeroWorldmap/tree/1.0.4beta2)
