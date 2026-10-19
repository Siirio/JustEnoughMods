package com.siirio.jemsettlements.happiness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit executable contracts for the graph and divider rules used by production room scans. */
public final class ResidentialRoomsContracts {
    private record Cell(int x, int y, int z) { }
    private static int assertions;
    public static void main(String[] args) {
        dividerClassification();
        connectedCornersAndDoor();
        verticallyConnectedFloors();
        fractionalFloorClearance();
        interruptionAndCensus();
        boundAndBlockedSeeds();
        System.out.println("ResidentialRooms contracts passed: " + assertions);
    }

    private static void dividerClassification() {
        check(ResidentialRooms.roomDivider(false, List.of(span(0, 1), span(1, 2))), "two full blocks divide rooms");
        check(ResidentialRooms.roomDivider(true, List.of()), "an open door remains a room separator");
        check(!ResidentialRooms.roomDivider(false, List.of(span(0, 1.5))), "low fence is not a room wall");
        check(!ResidentialRooms.roomDivider(false, List.of(span(0, .5))), "slab is not a room wall");
        check(!ResidentialRooms.roomDivider(false, List.of(span(0, .0625))), "carpet is not a room wall");
        check(!ResidentialRooms.roomDivider(false, List.of()), "noncolliding sign does not divide rooms");
        check(!ResidentialRooms.roomDivider(false, List.of(span(0, 1))), "one-block partition is not a room wall");
        check(!ResidentialRooms.roomDivider(false, List.of(span(.5625, .6))), "bed furniture shares room air");
        check(!ResidentialRooms.roomDivider(false, List.of(span(0, .5), span(1, 2))), "disconnected vertical spans do not invent a full wall");
        check(ResidentialRooms.roomDivider(false, List.of(span(1, 2), span(0, 1))), "shape ordering cannot change a full divider");
    }

    private static void connectedCornersAndDoor() {
        Cell a = new Cell(0, 0, 0), elbow = new Cell(1, 0, 0), b = new Cell(1, 0, 1);
        Map<Cell, List<Cell>> links = Map.of(a, List.of(elbow), elbow, List.of(a, b), b, List.of(elbow));
        var open = graph(a, links, Set.of(), true);
        check(open.census(true, false).equals(Set.of(a, elbow, b)), "L-shaped room connects around a hidden corner");
        boolean low = ResidentialRooms.roomDivider(false, List.of(span(0, 1.5)));
        var furniture = graph(a, links, low ? Set.of(elbow) : Set.of(), true);
        check(furniture.cells.contains(b), "low partition cannot make adjacent beds separate rooms");
        boolean door = ResidentialRooms.roomDivider(true, List.of());
        var divided = graph(a, links, door ? Set.of(elbow) : Set.of(), true);
        check(!divided.cells.contains(b), "open or closed doorway separates private rooms");
        var wall = graph(a, links, Set.of(elbow), true);
        check(wall.census(true, false).equals(Set.of(a)), "full wall excludes other room from its census");
    }

    private static void verticallyConnectedFloors() {
        Cell ground = new Cell(0, 0, 0), stair = new Cell(1, 1, 0), upper = new Cell(2, 1, 0);
        Cell isolated = new Cell(5, 1, 0);
        Map<Cell, List<Cell>> links = Map.of(ground, List.of(stair), stair, List.of(ground, upper),
                upper, List.of(stair), isolated, List.of());
        var area = graph(ground, links, Set.of(), false);
        check(area.census(true, false).equals(Set.of(ground, stair, upper)), "accessible stairs join floors without counting an isolated upper platform");
        var privacy = graph(ground, links, Set.of(), true);
        check(privacy.cells.equals(Set.of(ground)), "private rooms never merge through upper-floor links");
        var obstructed = graph(ground, links, Set.of(stair), false);
        check(!obstructed.cells.contains(upper), "blocked stair headroom cannot credit the upper floor");
    }

    private static void interruptionAndCensus() {
        Cell a = new Cell(0, 0, 0), b = new Cell(1, 0, 0);
        var graph = new ResidentialRooms.Graph<Cell>(4096);
        graph.seed(a);
        RuntimeException unloaded = new RuntimeException("unloaded observation");
        boolean interrupted = false;
        try { graph.advance(cell -> List.of(b), (from, to) -> { throw unloaded; }, cell -> true); }
        catch (RuntimeException actual) { interrupted = actual == unloaded; }
        check(interrupted && graph.frontier.contains(a), "unloaded or budget-yielding observation preserves its frontier");
        check(graph.census(false, true).isEmpty(), "unloaded scan never exposes a partial final housing census");
        check(graph.census(false, false).isEmpty(), "pending scan never exposes a partial final housing census");
        graph.advance(cell -> cell.equals(a) ? List.of(b) : List.of(a), (from, to) -> true, cell -> true);
        Set<Cell> finished = graph.census(true, false);
        check(finished.equals(Set.of(a, b)), "loaded retry resumes and completes the same connected census");
        boolean immutable = false;
        try { finished.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "completed census is immutable");
        check(graph.census(true, true).isEmpty(), "unavailable observation cannot overwrite census with partial positives");
    }

    private static void fractionalFloorClearance() {
        check(!ResidentialRooms.reachableStep(0, 1.5), "1.5-block fence cannot masquerade as an accessible stair");
        check(ResidentialRooms.reachableStep(0, 1), "ordinary one-block native jump remains accessible");
        check(ResidentialRooms.reachableStep(-.5, 0), "half-block slab ascent remains accessible");
        check(ResidentialRooms.reachableStep(1.5, 0), "upward jump ceiling does not forbid descending from a raised surface");
        var slab = ResidentialRooms.sweptHeight(-.5, -.5, 1.95);
        check(Math.abs(slab.min() + .49) < .000001 && Math.abs(slab.max() - 1.45) < .000001,
                "lower-slab floor sweeps from actual fractional feet rather than integer block height");
        check(slab.min() > -.5 && slab.max() < 1.5,
                "two-high lower-slab corridor clears supporting slab and upper-slab ceiling");
        var ascending = ResidentialRooms.sweptHeight(-.5, 0, 1.95);
        var descending = ResidentialRooms.sweptHeight(0, -.5, 1.95);
        check(ascending.equals(descending) && Math.abs(ascending.max() - 1.95) < .000001,
                "fractional ascent and descent reserve clearance over the higher real support");
        var stair = ResidentialRooms.sweptHeight(.5, 1, 1.95);
        check(Math.abs(stair.min() - 1.01) < .000001 && Math.abs(stair.max() - 2.95) < .000001,
                "raised stair approach uses support height for both body bounds");
    }

    private static void boundAndBlockedSeeds() {
        var capped = new ResidentialRooms.Graph<Integer>(4);
        capped.seed(0);
        capped.advance(cell -> List.of(cell + 1), (from, to) -> true, cell -> true);
        check(capped.bounded && capped.cells.size() == 4, "cell ceiling bounds an open connected interior");
        check(capped.census(true, false).isEmpty(), "bounded search cannot masquerade as a complete room");
        var extent = new ResidentialRooms.Graph<Integer>(4096);
        extent.seed(0);
        extent.advance(cell -> List.of(cell + 1), (from, to) -> true, cell -> cell <= 32);
        check(extent.bounded, "reachable scan extent is explicitly unavailable");
        var blocked = new ResidentialRooms.Graph<Integer>(4096);
        check(blocked.advance(cell -> List.of(), (from, to) -> true, cell -> true)
                && blocked.census(true, false).isEmpty() && !blocked.bounded,
                "fully loaded bed with no accessible floor proves complete zero area");
    }

    private static ResidentialRooms.Graph<Cell> graph(Cell seed, Map<Cell, List<Cell>> links, Set<Cell> blocked, boolean privacy) {
        var graph = new ResidentialRooms.Graph<Cell>(4096);
        graph.seed(seed);
        graph.advance(cell -> links.getOrDefault(cell, List.of()), (from, to) -> !blocked.contains(to)
                && (!privacy || from.y() == to.y()), cell -> true);
        return graph;
    }
    private static double[] span(double min, double max) { return new double[] {min, max}; }
    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) throw new AssertionError(message);
    }
}
