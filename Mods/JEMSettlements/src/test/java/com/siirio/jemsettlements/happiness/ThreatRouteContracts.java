package com.siirio.jemsettlements.happiness;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Access;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Cell;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Result;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Sight;
import com.siirio.jemsettlements.happiness.ThreatRouteModel.Threat;

/** Executable contracts against the exact pure policies used by production observations. */
public final class ThreatRouteContracts {
    private static int assertions;
    private static final class Grid implements ThreatRouteModel.Grid {
        final Set<Cell> blocked = new HashSet<>(), missing = new HashSet<>();
        @Override public Access stand(Cell cell) {
            if (missing.contains(cell)) return Access.UNKNOWN;
            if (cell.y() != 0 || cell.x() < 0 || cell.x() > 6 || cell.z() < 0 || cell.z() > 4 || blocked.contains(cell)) return Access.BLOCKED;
            return Access.OPEN;
        }
        @Override public Access step(Cell from, Cell to) { return Access.OPEN; }
        void wall() { for (int z = 0; z <= 4; z++) blocked.add(new Cell(3, 0, z)); }
    }
    private static ThreatRouteModel.Search search(Grid grid, int cap) { return new ThreatRouteModel.Search(grid, new Cell(1, 0, 2), new Cell(5, 0, 2), 8, cap); }
    private static Result finish(ThreatRouteModel.Search search) {
        for (int i = 0; i < 100 && search.result() == Result.PENDING; i++) search.advance(4);
        return search.result();
    }
    private static void check(boolean value, String description) { assertions++; if (!value) throw new AssertionError(description); }
    public static void main(String[] args) {
        Grid open = new Grid();
        var sliced = search(open, 128);
        check(sliced.advance(0) == Result.PENDING && sliced.visited() == 0, "zero budget performs no route work");
        check(sliced.advance(1) == Result.PENDING, "route work yields after one cell");
        check(finish(sliced) == Result.REACHABLE, "open accessible route reaches resident");
        Grid wall = new Grid(); wall.wall();
        check(finish(search(wall, 128)) == Result.BLOCKED, "solid continuous wall cannot be bypassed");
        check(ThreatRouteModel.assess(Result.BLOCKED, Sight.BLOCKED).threat() == Threat.NONE, "hidden inaccessible monster causes no fear");
        check(ThreatRouteModel.assess(Result.BLOCKED, Sight.BLOCKED).complete(), "closed reachable region is a completed safe observation");
        check(ThreatRouteModel.assess(Result.BLOCKED, Sight.CLEAR).threat() == Threat.NORMAL, "direct visible monster remains a threat across a gap");
        check(ThreatRouteModel.assess(Result.PENDING, Sight.CLEAR).complete(), "direct sight needs no collision flood");
        check(ThreatRouteModel.assess(Result.BLOCKED, Sight.GLASS).threat() == Threat.GLASS, "glass sight is mild without an accessible route");
        Grid aroundCorner = new Grid(); aroundCorner.wall(); aroundCorner.blocked.remove(new Cell(3, 0, 0));
        check(finish(search(aroundCorner, 128)) == Result.REACHABLE, "open corridor around an opaque corner remains threatening");
        check(ThreatRouteModel.assess(Result.REACHABLE, Sight.BLOCKED).threat() == Threat.NORMAL, "reachable hidden monster causes ordinary fear");
        Grid door = new Grid(); door.wall();
        check(finish(search(door, 128)) == Result.BLOCKED, "closed intact door blocks cheap collision route");
        door.blocked.remove(new Cell(3, 0, 2));
        check(finish(search(door, 128)) == Result.REACHABLE, "native running door-break signal can open its scoped door in the world seam");
        Grid missing = new Grid(); missing.wall(); missing.blocked.remove(new Cell(3, 0, 2)); missing.missing.add(new Cell(3, 0, 2));
        check(finish(search(missing, 128)) == Result.UNKNOWN, "missing chunks are unknown instead of fictitious impassable walls");
        check(finish(search(open, 2)) == Result.UNKNOWN, "node cap conservatively preserves unknown");
        ThreatRouteModel.Grid unbounded = new ThreatRouteModel.Grid() {
            @Override public Access stand(Cell cell) { return cell.y() == 0 ? Access.OPEN : Access.BLOCKED; }
            @Override public Access step(Cell from, Cell to) { return Access.OPEN; }
        };
        var bounded = new ThreatRouteModel.Search(unbounded, new Cell(0, 0, 0), new Cell(4, 0, 0), 1, 64);
        check(finish(bounded) == Result.UNKNOWN, "reachable extent boundary cannot certify a detour outside the search cube as blocked");
        ThreatRouteModel.Grid sealedEdges = new ThreatRouteModel.Grid() {
            @Override public Access stand(Cell cell) { return cell.y() == 0 ? Access.OPEN : Access.BLOCKED; }
            @Override public Access step(Cell from, Cell to) { return Math.abs(to.x()) > 1 || Math.abs(to.z()) > 1 ? Access.BLOCKED : Access.OPEN; }
        };
        check(finish(new ThreatRouteModel.Search(sealedEdges, new Cell(0, 0, 0), new Cell(4, 0, 0), 1, 64)) == Result.BLOCKED,
                "physically blocked extent crossings still certify an enclosed region");
        int fenceOwnerY = 0;
        boolean foundFenceCollision = false;
        for (int ownerY = ThreatCollisionPolicy.firstOwner(1.01); ownerY <= ThreatCollisionPolicy.lastOwner(2.79); ownerY++) {
            if (ownerY == fenceOwnerY) foundFenceCollision = ThreatCollisionPolicy.overlaps(.375, 0, .375, .625, 1.5, .625,
                    .2, 1.01, .2, .8, 2.79, .8);
        }
        check(foundFenceCollision, "actual 1.5-block fence collision is checked even though its owning block lies below NPC feet");
        check(!ThreatCollisionPolicy.supported(1.5, 1), "fence top cannot certify support at an invented one-block height");
        check(!ThreatCollisionPolicy.supported(.5, 1), "half slab cannot certify floating support at the next integer height");
        check(ThreatCollisionPolicy.supported(1, 1), "full floor collision certifies actual integer standing height");
        check(!ThreatCollisionPolicy.canStep(0, 1, .6), "cheap walking route does not invent a jump beyond native step height");
        check(ThreatCollisionPolicy.canStep(0, .5, .6), "native half-block step remains permitted by geometry policy");
        check(ThreatRouteModel.fear(20, 0, false, 20, 1) == 20, "unfinished scan preserves acute fear");
        check(ThreatRouteModel.fear(20, 0, true, 20, 1) == 19, "completed safe scan recovers one point");
        check(ThreatRouteModel.fear(0, 5, true, 20, 1) == 5, "glass signal projects mild fear");
        check(ThreatRouteModel.fear(10, 10, true, 20, 1) == 10, "ordinary fear does not accumulate");
        check(ThreatRouteModel.fear(20, 10, true, 8, 1) == 8, "configuration reload clamps existing fear");
        check(CrowdingPolicy.penalty(100, 20, 9, 30, true, 0) == 13, "crowding uses useful area divided by registered census times nine");
        check(CrowdingPolicy.penalty(360, 120, 9, 30, true, 0) == 20, "120 registered residents count even if only four native actors are loaded");
        check(CrowdingPolicy.penalty(36, 4, 9, 30, true, 29) == 0, "nine useful floor cells per resident removes crowding");
        check(CrowdingPolicy.penalty(0, 120, 9, 30, false, 17) == 17, "unknown room preserves last known crowding rather than inventing zero");
        check(CrowdingPolicy.penalty(0, 120, 9, 30, true, 0) == 30, "crowding is capped at thirty");
        var floors = new ArrayList<LightingPolicy.Point>();
        for (int y : new int[] {64, 76}) for (int x = 0; x < 12; x++) for (int z = 0; z < 12; z++) floors.add(new LightingPolicy.Point(x, y, z));
        var samples = LightingPolicy.sample(floors, 12);
        check(samples.size() <= 12 && !samples.isEmpty(), "room lighting reads at most twelve accessible floor points");
        check(samples.stream().anyMatch(p -> p.y() == 64) && samples.stream().anyMatch(p -> p.y() == 76), "stacked floors with identical XZ both receive samples");
        check(samples.stream().anyMatch(p -> p.x() < 6) && samples.stream().anyMatch(p -> p.x() >= 6)
                && samples.stream().anyMatch(p -> p.z() < 6) && samples.stream().anyMatch(p -> p.z() >= 6), "lighting samples span the room footprint");
        int minimumBlockLight = samples.stream().mapToInt(p -> p.y() == 76 ? 0 : 12).min().orElseThrow();
        check(LightingPolicy.penalty(minimumBlockLight, 8, 4, 5, 10) == 10, "dark upper floor receives severe block-light penalty despite daytime sky light");
        check(LightingPolicy.penalty(8, 8, 4, 5, 10) == 0, "block light eight is good");
        check(LightingPolicy.penalty(7, 8, 4, 5, 10) == 5 && LightingPolicy.penalty(4, 8, 4, 5, 10) == 5, "block light four through seven is mild");
        check(LightingPolicy.penalty(3, 8, 4, 5, 10) == 10, "block light three is severe");
        System.out.println("Threat, crowding and lighting contracts passed: " + assertions);
    }
}
