package net.shiroha233.roadweaver.features.path.pathlogic.surface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.shiroha233.roadweaver.config.ConfigService;
import net.shiroha233.roadweaver.config.RoadGenerationConfig;
import net.shiroha233.roadweaver.helpers.Records;

/** Keeps ordinary road grades anchored to recorded terrain instead of raising the low endpoint. */
public final class GroundAnchoredHeightProfile {
    private GroundAnchoredHeightProfile() {
    }

    public static List<Integer> adjustRoadTargets(
            List<Integer> upstream,
            List<Records.RoadSegmentPlacement> segments,
            List<Records.RoadSpan> spans,
            RoadGenerationConfig config) {
        int size = segments.size();
        int[] terrain = new int[size];
        for (int i = 0; i < size; i++) {
            terrain[i] = segments.get(i).middlePos().m_123342_();
        }

        boolean[] bridge = bridgeMask(segments, spans);
        int[] adjusted = terrain.clone();
        for (int i = 0; i < size; i++) {
            if (bridge[i] && upstream != null && upstream.size() == size) {
                adjusted[i] = upstream.get(i);
            }
        }

        applyRuns(adjusted, terrain, bridge, config.slopeLimitEnabled(), config.maxSlopeStepPerTwoSegments());
        List<Integer> result = new ArrayList<>(size);
        for (int value : adjusted) {
            result.add(value);
        }
        return result;
    }

    public static HeightProfileService.HeightProfile adjustPlacementProfile(
            HeightProfileService.HeightProfile upstream,
            List<BlockPos> centers,
            boolean slopeLimitEnabled,
            int maxSlopeStepPerTwoSegments) {
        int size = centers.size();
        int[] terrain = new int[size];
        for (int i = 0; i < size; i++) {
            terrain[i] = centers.get(i).m_123342_();
        }

        int[] adjusted = terrain.clone();
        applyRuns(adjusted, terrain, new boolean[size], slopeLimitEnabled, maxSlopeStepPerTwoSegments);
        return new HeightProfileService.HeightProfile(false, adjusted);
    }

    private static boolean[] bridgeMask(
            List<Records.RoadSegmentPlacement> segments,
            List<Records.RoadSpan> spans) {
        boolean[] result = new boolean[segments.size()];
        if (spans == null || spans.isEmpty()) {
            return result;
        }

        Map<Long, Integer> indices = new HashMap<>();
        for (int i = 0; i < segments.size(); i++) {
            indices.put(segments.get(i).middlePos().m_121878_(), i);
        }
        for (Records.RoadSpan span : spans) {
            if (span.type() != Records.SpanType.BRIDGE) {
                continue;
            }
            Integer start = indices.get(span.start().m_121878_());
            Integer end = indices.get(span.end().m_121878_());
            if (start == null || end == null) {
                continue;
            }
            for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                result[i] = true;
            }
        }
        return result;
    }

    private static void applyRuns(
            int[] adjusted,
            int[] terrain,
            boolean[] bridge,
            boolean slopeLimitEnabled,
            int configuredStep) {
        int index = 0;
        while (index < terrain.length) {
            while (index < terrain.length && bridge[index]) {
                index++;
            }
            int start = index;
            while (index < terrain.length && !bridge[index]) {
                index++;
            }
            int end = index - 1;
            if (start <= end) {
                applyRun(adjusted, terrain, start, end, slopeLimitEnabled, configuredStep);
            }
        }
    }

    private static void applyRun(
            int[] adjusted,
            int[] terrain,
            int start,
            int end,
            boolean slopeLimitEnabled,
            int configuredStep) {
        if (!slopeLimitEnabled || start == end) {
            for (int i = start; i <= end; i++) {
                adjusted[i] = terrain[i];
            }
            return;
        }

        int direction = terrain[start] <= terrain[end] ? 1 : -1;
        int anchor = direction > 0 ? start : end;
        int limit = direction > 0 ? end : start;
        int stepTwo = Math.max(0, Math.min(8, configuredStep));
        int stepOne = Math.max(0, (stepTwo + 1) / 2);
        int maxFill = Math.max(0, Math.min(12, ConfigService.get().causewayMaxDepth()));

        adjusted[anchor] = terrain[anchor];
        int previous = anchor;
        int current = anchor + direction;
        while (direction > 0 ? current <= limit : current >= limit) {
            int value = terrain[current];
            value = clamp(value, adjusted[previous] - stepOne, adjusted[previous] + stepOne);
            int previousTwo = previous - direction;
            if (direction > 0 ? previousTwo >= anchor : previousTwo <= anchor) {
                value = clamp(value, adjusted[previousTwo] - stepTwo, adjusted[previousTwo] + stepTwo);
            }

            // Cutting into a hill is safe. Raising the road far above recorded terrain is not.
            adjusted[current] = Math.min(value, terrain[current] + maxFill);
            previous = current;
            current += direction;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
