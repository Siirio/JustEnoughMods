package com.siirio.jemserver.client.smp;

import java.util.ArrayDeque;
import java.util.Deque;

final class BackNavigation {
    private static final int LIMIT = 32;
    private final Deque<SmpRoute> routes = new ArrayDeque<>();
    void remember(SmpRoute route) {
        if (!routes.isEmpty() && routes.peekLast().sameDestination(route)) routes.removeLast();
        if (routes.size() == LIMIT) routes.removeFirst();
        routes.addLast(route);
    }
    boolean available() { return !routes.isEmpty(); }
    SmpRoute back() { return routes.pollLast(); }
}
