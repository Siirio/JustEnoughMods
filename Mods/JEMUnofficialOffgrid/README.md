# Unofficial Offgrid

Unofficial Offgrid is a Forge 1.20.1 addon for the unmodified Offgrid 1.0.4 JAR

Offgrid remains the only owner of multipart storage, synchronization, rendering, breaking, mixed slabs and placement. This addon extends only Offgrid's block eligibility with the shape-based relative-placement classification used by Olaf's Relatively Placed Blocks 2.0.2

The extension rejects block entities, immovable blocks, blocks that fill at least two dimensions, tall shapes and crop-like empty-collision blocks with an `age` or `*_amount` property. It does not override an Offgrid blocklog route or remove any block that Offgrid already supports

Both the client and logical server require the same addon and Offgrid 1.0.4 artifact

The inspected Olaf 2.0.2 JAR includes `LICENSE_relativeblocks` with CC0 1.0. The original Offgrid JAR is not modified, unpacked or redistributed inside this addon
