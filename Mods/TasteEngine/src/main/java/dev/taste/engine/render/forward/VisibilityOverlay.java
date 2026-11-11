package dev.taste.engine.render.forward;

import java.util.ArrayList;
import java.util.List;

/** Ordered gameplay-important overlays remain visible after artistic grading. */
public final class VisibilityOverlay {
    public record Command(String id, int textureId, float opacity, int order) { }
    private final List<Command> commands = new ArrayList<>();
    public void submit(Command command) { commands.add(command); }
    public List<Command> drain() {
        List<Command> result = commands.stream().sorted(java.util.Comparator.comparingInt(Command::order)).toList();
        commands.clear();
        return result;
    }
}
