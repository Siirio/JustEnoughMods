package dev.taste.engine.effects.water;

public final class RippleSimulation {
    private final int size;
    private float[] current;
    private float[] previous;
    public RippleSimulation(int size) { this.size = Math.max(8, size); current = new float[this.size * this.size]; previous = new float[current.length]; }
    public void disturb(int x, int y, float strength) { if (x > 0 && y > 0 && x < size - 1 && y < size - 1) current[y * size + x] += strength; }
    public void step(float damping) {
        float[] next = new float[current.length];
        for (int y = 1; y < size - 1; y++) for (int x = 1; x < size - 1; x++) {
            int i = y * size + x;
            next[i] = ((current[i - 1] + current[i + 1] + current[i - size] + current[i + size]) * 0.5F - previous[i]) * damping;
        }
        previous = current; current = next;
    }
    public float height(int x, int y) { return current[y * size + x]; }
}
