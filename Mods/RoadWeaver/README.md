# RoadWeaver ground-anchored road patch

This module owns the disclosed JEM patch for RoadWeaver 2.1.1 on Minecraft 1.20.1 Forge

The upstream road height smoother applies both a forward and a backward slope pass. When a road joins low terrain to a much higher structure, the backward pass can raise the low end of the complete road. The terrain adapter then fills every air block from the natural surface to that raised target height. This produces the large artificial walls shown in the Major Update 4 world

The JEM patch keeps the lower end of each ordinary road run anchored to its recorded terrain height. It then applies the configured slope limit in one direction toward the higher end. A road target can be no more than the configured causeway depth above its recorded terrain. The road can cut into rising terrain, but it cannot create an unbounded land column in the sky. Native bridge spans remain under RoadWeaver bridge control

The patch also recalculates placement profiles from each road's recorded terrain points. This applies the corrected rule to ungenerated chunks of roads that were planned before the patch. Blocks already generated in the world are not removed

`prepare.py --build` downloads the exact upstream JAR, verifies its SHA-256, compiles the small helper class, inserts two narrow calls into the upstream bytecode, and writes `build/libs/roadweaver-forge-2.1.1-1.20.1-jem.1.jar`

RoadWeaver is MIT licensed. The upstream license and all upstream resources remain in the patched JAR
