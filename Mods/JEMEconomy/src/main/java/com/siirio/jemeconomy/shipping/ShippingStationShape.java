package com.siirio.jemeconomy.shipping;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision and outline use the same authored body as the native block model. */
final class ShippingStationShape {
    private static final EnumMap<Direction, VoxelShape> SHAPES = load();
    private ShippingStationShape() {}
    static VoxelShape get(Direction direction) { return SHAPES.get(direction); }
    private static EnumMap<Direction, VoxelShape> load() {
        var result = new EnumMap<Direction, VoxelShape>(Direction.class);
        try (var stream = ShippingStationShape.class.getResourceAsStream("/assets/jem_economy/models/block/shipping_box.json")) {
            if (stream == null) throw new IllegalStateException("Shipping station body is missing");
            var elements = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("elements");
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                VoxelShape shape = Shapes.empty();
                int turns = switch (direction) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
                for (var entry : elements) {
                    var element = entry.getAsJsonObject();
                    if (element.has("rotation")) throw new IllegalStateException("Shipping station collision requires axis-aligned body cuboids");
                    var from = element.getAsJsonArray("from"); var to = element.getAsJsonArray("to");
                    double x1=from.get(0).getAsDouble(), z1=from.get(2).getAsDouble(), x2=to.get(0).getAsDouble(), z2=to.get(2).getAsDouble();
                    for (int turn=0; turn<turns; turn++) { double previous=x1; x1=16-z2; z2=x2; x2=16-z1; z1=previous; }
                    shape=Shapes.or(shape, Block.box(x1,from.get(1).getAsDouble(),z1,x2,to.get(1).getAsDouble(),z2));
                }
                result.put(direction,shape.optimize());
            }
            return result;
        } catch (java.io.IOException invalid) { throw new IllegalStateException("Cannot read shipping station body", invalid); }
    }
}
