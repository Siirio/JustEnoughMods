package dev.taste.engine.render;

import java.util.*;

/** Original SSA-style graph. Logical values are written once; storage aliases are checked separately. */
public final class RenderGraph {
    private RenderGraph() {}
    public record Resource(String name, String storage) {
        public Resource {
            if (name == null || name.isBlank() || storage == null || storage.isBlank())
                throw new IllegalArgumentException("Resource name/storage required");
        }
    }
    @FunctionalInterface public interface Action { void run() throws Exception; }
    @FunctionalInterface public interface StateScopes { AutoCloseable capture(String pass) throws Exception; }
    public record Pass(String name, Set<Resource> reads, Set<Resource> writes, Action action) {
        public Pass {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Pass name required");
            reads = Set.copyOf(reads);
            writes = Set.copyOf(writes);
            Objects.requireNonNull(action);
        }
    }
    public record Compiled(List<Pass> passes) {
        public Compiled { passes = List.copyOf(passes); }
        /** The adapter scope restores framebuffer, viewport, programs, bindings, and all touched GL state. */
        public void execute(StateScopes scopes) throws Exception {
            for (Pass pass : passes) {
                try (AutoCloseable scope = Objects.requireNonNull(scopes.capture(pass.name))) {
                    pass.action.run();
                } catch (Exception failure) {
                    throw new Exception("Render pass failed: " + pass.name, failure);
                }
            }
        }
    }

    public static Compiled compile(Set<Resource> imports, Set<Resource> exports, List<Pass> source) {
        imports = Set.copyOf(imports);
        exports = Set.copyOf(exports);
        List<Pass> passes = List.copyOf(source);
        Map<Resource, Integer> writers = new HashMap<>();
        Map<String, Resource> names = new HashMap<>();
        Set<String> passNames = new HashSet<>();
        Set<Resource> all = new HashSet<>(imports);
        for (int i = 0; i < passes.size(); i++) {
            Pass p = passes.get(i);
            if (!passNames.add(p.name)) fail("Duplicate pass " + p.name);
            Set<Resource> touched = new HashSet<>(p.reads);
            for (Resource r : p.writes) {
                if (p.reads.contains(r)) fail("Read/write feedback in " + p.name + ": " + r.name);
                if (imports.contains(r) || writers.putIfAbsent(r, i) != null) fail("Multiple writers/import overwrite: " + r.name);
                touched.add(r);
            }
            Map<String, Resource> storage = new HashMap<>();
            for (Resource r : touched) {
                Resource prior = storage.putIfAbsent(r.storage, r);
                if (prior != null && (p.writes.contains(r) || p.writes.contains(prior)))
                    fail("Physical framebuffer feedback in " + p.name + ": " + r.storage);
            }
            all.addAll(touched);
        }
        for (Resource r : all) {
            Resource prior = names.putIfAbsent(r.name, r);
            if (prior != null && !prior.equals(r)) fail("Ambiguous logical resource " + r.name);
        }
        for (Resource r : exports) if (!imports.contains(r) && !writers.containsKey(r)) fail("Unproduced export " + r.name);
        List<Set<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < passes.size(); i++) edges.add(new HashSet<>());
        int[] inDegree = new int[passes.size()];
        for (int i = 0; i < passes.size(); i++) for (Resource r : passes.get(i).reads) {
            Integer producer = writers.get(r);
            if (producer == null) {
                if (!imports.contains(r)) fail("Read before definition: " + r.name);
            } else if (edges.get(producer).add(i)) inDegree[i]++;
        }
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (int i = 0; i < passes.size(); i++) if (inDegree[i] == 0) ready.add(i);
        List<Pass> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            int i = ready.remove();
            ordered.add(passes.get(i));
            for (int after : edges.get(i)) if (--inDegree[after] == 0) ready.add(after);
        }
        if (ordered.size() != passes.size()) fail("Cyclic resource dependency");

        // Inclusive lifetimes: simultaneous read/write aliases and exported-value clobbers are forbidden.
        Map<Resource, int[]> lifetime = new HashMap<>();
        for (Resource r : imports) lifetime.put(r, new int[]{-1, -1});
        for (int i = 0; i < ordered.size(); i++) {
            for (Resource r : ordered.get(i).writes) lifetime.put(r, new int[]{i, i});
            for (Resource r : ordered.get(i).reads) lifetime.get(r)[1] = i;
        }
        for (Resource r : exports) lifetime.get(r)[1] = ordered.size();
        List<Resource> values = new ArrayList<>(lifetime.keySet());
        for (int i = 0; i < values.size(); i++) for (int j = i + 1; j < values.size(); j++) {
            Resource a = values.get(i), b = values.get(j);
            int[] x = lifetime.get(a), y = lifetime.get(b);
            if (a.storage.equals(b.storage) && x[0] <= y[1] && y[0] <= x[1])
                fail("Overlapping storage lifetimes: " + a.name + " / " + b.name);
        }
        return new Compiled(ordered);
    }
    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
