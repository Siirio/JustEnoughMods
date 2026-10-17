package com.siirio.jemvisualsandbox.workbench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/** Detached demonstration clock. Draws the live effects without firing packets or world queues. */
public final class AbilityPreviewPresentation {
    public static final Vec3 PLAYER = new Vec3(-3, 0, 0), TARGET = new Vec3(3, 0, 0);
    private static final List<Vec3> PEARL_PATH = pearlPath();
    public static final int PEARL_FLIGHT_TICKS = PEARL_PATH.size() - 1;
    private final Map<String, WorkbenchMesh> meshes = new HashMap<>();

    public void dispose() { meshes.clear(); }

    public Vec3 playerPosition(int race, int slot, float age) {
        if (slot == 0 && race == 1 && age >= 14)
            return new Vec3(4.8, 0, 0).lerp(PLAYER, smooth((age - 64) / 24));
        if (slot == 1 && race == 1 && age >= PEARL_FLIGHT_TICKS && age < 24)
            return pearlPosition(PEARL_FLIGHT_TICKS).multiply(1, 0, 1).lerp(PLAYER, smooth((age - 16) / 8));
        if (slot == 1 && race == 2)
            return PLAYER.add(2.4 * smooth(age / 40) * (1 - smooth((age - 66) / 22)), 0, 0);
        if (slot == 1 && race == 4) {
            float advance = .42F * (Math.min(4, Math.max(0, age))
                    + Math.min(4, Math.max(0, age - 10)) + Math.min(5, Math.max(0, age - 22)));
            return PLAYER.add(advance * (1 - smooth((age - 44) / 36)), 0, 0);
        }
        return PLAYER;
    }

    /** Only successful offensive beats receive vanilla hurt feedback. */
    public float hitAge(int race, int slot, int bloomStage, float age) {
        if (slot == 0 && race == 2) {
            float impact = bloomStage >= 2 && age >= 20 && age < 80 ? 20 * (int)(age / 20) : 0;
            return age - impact;
        }
        if (slot == 0 && race == 1) return age - 14;
        if (slot == 0 && race == 3) return age - 15;
        if (slot == 0 && race == 4) return age - 9;
        if (slot == 1 && race == 4) return age - (age >= 27 ? 27 : age >= 14 ? 14 : 4);
        // Rift and pearl teleport, echo, healing, guardian and leaves do not fake a strike.
        return -1;
    }

    public void render(VisualRenderContext context, int race, int slot, int tier, int bloomStage, float age) {
        if (age < 0) return;
        Vec3 player = playerPosition(race, slot, age);
        if (slot == 0) {
            switch (race) {
                case 0 -> {
                    LifeChorusRenderer.draw(context, 30, PLAYER, player, 1.8F, age);
                    LifeChorusRenderer.draw(context, 31, PLAYER, player, 1.8F, age - 13);
                }
                case 1 -> {
                    ElementalRenderer.draw(context, PLAYER.add(1.2, 0, 0), -90, 5, age);
                    ElementalRenderer.draw(context, new Vec3(3.6, 0, 0), -90, 9, age - 14);
                }
                case 2 -> {
                    // The live target-fitted entity_vines mesh has one shared visual.
                    // Stage0 releases normally; stages1–3 hold through tick78/90.
                    double tick = age;
                    if (bloomStage > 0) tick = age < 16 ? age : age < 78 ? 16 : 16 + (age - 78) * 8 / 12;
                    if (bloomStage == 0 || age < 90)
                        mesh(context, "entity_vines", 2, TARGET, 0, tick, .76F, .85F, .76F);
                }
                case 3 -> {
                    SonicWaveRenderer.render(context, PLAYER, age);
                    SonicWaveRenderer.drawImpact(context, TARGET.add(0, .85, 0), age - 15, 0);
                }
                case 4 -> {
                    SurfaceVfx.draw(context, List.of(PLAYER, PLAYER.add(2.5, 0, 0), PLAYER.add(5, 0, 0), PLAYER.add(7.5, 0, 0), PLAYER.add(10, 0, 0)), 0, age);
                    for (int i = 0; i < 5; i++)
                        mesh(context, "eruption" + i % 3, 2, PLAYER.add(1.6 + i * 2, 0, 0), 90, age - i * 4, 1, 1, 1);
                }
                default -> { }
            }
        } else {
            switch (race) {
                case 0 -> {
                    LifeChorusRenderer.draw(context, 35, PLAYER, PLAYER, 1.8F, age);
                    LifeChorusRenderer.draw(context, 27, PLAYER, PLAYER.add(1.5, 0, -1), 2.7F, age);
                    mesh(context, "shield", 1, PLAYER.add(1.3, 0, 0), 270, age, 1, 1, 1);
                }
                case 1 -> {
                    // One loop includes the charged pearl and the distinct Shift+B mode.
                    if (age < PEARL_FLIGHT_TICKS) {
                        Vec3 head = pearlPosition(age);
                        List<Vec3> trail = new ArrayList<>(12);
                        for (int i = 0; i < 12; i++) trail.add(pearlPosition(Math.max(0, age - i * .4F)));
                        PearlAfterfire.draw(context, trail, head, (int)(age / 2) % 8, 1);
                    }
                    UtilityVfx.draw(context, pearlPosition(PEARL_FLIGHT_TICKS), 29, age - PEARL_FLIGHT_TICKS);
                    UtilityVfx.draw(context, player, 22, age - 24);
                }
                case 2 -> {
                    for (int i = 0; i < 5; i++)
                        UtilityVfx.draw(context, PLAYER.add(i * .8, 0, 0), 23, age - i * 4);
                }
                case 3 -> {
                    UtilityVfx.draw(context, player, 24, age);
                    UtilityVfx.draw(context, player, 25, age - 40);
                }
                case 4 -> {
                    for (int i = 0; i < 3; i++)
                        FlameSlashRenderer.draw(context, player.add(1.35, 0, 0), -90, i, age - new int[]{0, 10, 22}[i]);
                }
                default -> { }
            }
        }
    }

    public static Vec3 pearlPosition(float age) {
        float t = net.minecraft.util.Mth.clamp(age, 0, PEARL_FLIGHT_TICKS);
        int first = Math.min(PEARL_FLIGHT_TICKS - 1, (int)t);
        return PEARL_PATH.get(first).lerp(PEARL_PATH.get(first + 1), t - first);
    }

    private static List<Vec3> pearlPath() {
        List<Vec3> path = new ArrayList<>();
        Vec3 point = PLAYER.add(0, 1.52, 0);
        // An aimed-down native1.5 launch lands on the miniature's detached flat support.
        Vec3 velocity = new Vec3(Math.cos(Math.toRadians(20)), -Math.sin(Math.toRadians(20)), 0).scale(1.5);
        path.add(point);
        for (int tick = 0; tick < 20; tick++) {
            Vec3 next = point.add(velocity);
            if (next.y <= .05) {
                path.add(point.lerp(next, (point.y - .05) / (point.y - next.y)));
                break;
            }
            path.add(next);point = next;
            velocity = ProjectileEstimation.airVelocityAfterTick(velocity);
        }
        return List.copyOf(path);
    }

    private void mesh(VisualRenderContext context, String id, int materials, Vec3 origin, float yaw,
                      double ticks, float sx, float sy, float sz) {
        if (ticks < 0) return;
        WorkbenchMesh model = meshes.get(id);
        if (model == null) {
            try { model = WorkbenchMesh.load(id, materials); }
            catch (IOException error) { throw new IllegalStateException("Cannot load real ability preview mesh " + id, error); }
            meshes.put(id, model);
        }
        BlockbenchWorkbench.drawMesh(context, model, origin, yaw, ticks / 20, sx, sy, sz, 15728880);
    }
    private static float smooth(float value) {
        float t = net.minecraft.util.Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
