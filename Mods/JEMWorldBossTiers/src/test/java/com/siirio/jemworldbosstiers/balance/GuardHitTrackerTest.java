package com.siirio.jemworldbosstiers.balance;

/** Run with Java assertions enabled; no Forge launch or external test library. */
public final class GuardHitTrackerTest {
    public static void main(String[] args) {
        Object wearer = new Object();
        GuardHitTracker tracker = new GuardHitTracker();
        tracker.begin(wearer, 20, 0);
        assert tracker.reserve(wearer);
        assert !tracker.finish(wearer, 20, 0) : "Canceled/immune hit must refund guard";
        tracker.begin(wearer, 20, 0);
        assert tracker.reserve(wearer) : "A rejected hit cannot reserve the next hit";
        assert tracker.finish(wearer, 16, 0) : "Real health loss commits guard";

        tracker.begin(wearer, 20, 8);
        assert tracker.reserve(wearer);
        assert tracker.finish(wearer, 20, 3) : "Absorbed accepted damage consumes guard";

        tracker.begin(wearer, 20, 0);
        assert tracker.reserve(wearer);
        tracker.begin(wearer, 20, 0);
        assert !tracker.reserve(wearer) : "Nested damage cannot double reserve";
        assert !tracker.finish(wearer, 18, 0);
        assert !tracker.finish(wearer, 18, 0) : "Nested damage cannot commit rejected parent";

        tracker.begin(wearer, 20, 0);
        assert tracker.reserve(wearer);
        tracker.begin(wearer, 20, 0);
        assert !tracker.reserve(wearer);
        assert !tracker.finish(wearer, 18, 0);
        assert tracker.finish(wearer, 15, 0) : "Parent's own accepted damage still commits";

        Object other = new Object();
        tracker.begin(wearer, 20, 0);
        assert tracker.reserve(wearer);
        tracker.begin(other, 30, 0);
        assert tracker.reserve(other) : "Other entities reserve independently";
        assert tracker.finish(other, 25, 0);
        assert !tracker.finish(wearer, 20, 0);
        assert tracker.isEmpty() : "Every hurt return releases transient state";
        assert !tracker.reserve(wearer) : "Events outside native hurt cannot spend guard";
        System.out.println("Guard hurt transactions: 7 scenarios passed");
    }
}
