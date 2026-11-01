# Simpler Exposure

Standalone Forge 1.20.1 companion for Exposure 1.9.21.

It preserves Exposure's camera, camera stand, capture, saved-image identifier, metadata and networking. Each successful server-authoritative shot creates one native Photograph immediately. The photograph enters the photographer's inventory or drops at their feet when the inventory is full.

Only normal black-and-white and color film can accept frames. Film capacity is configurable from 1 to 16 and defaults to 16. The module replaces only those two film recipes with cheaper vanilla-material recipes and suppresses normal-survival recipes for developing, high-sensitivity film, the Lightroom and the Interplanar Projector. Existing items and saved photographs are not deleted.

This project has no dependency on private JEM modules. Supply the exact Exposure 1.9.21 JAR through `jemRuntimeRoot`, `jem_instance` or `JEM_INSTANCE` when compiling.
