package com.siirio.jemworldbosstiers.encounter;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RaidRewardPoolTest {
    @Test
    void everyWeightedPairUsesDifferentResources() {
        List<Integer> original = List.of(3, 5, 4, 4, 1, 2, 2);
        for (int first = 0; first < 21; first++) {
            int selected = RaidRewardPool.weightedIndex(original, first);
            List<Integer> remaining = new ArrayList<>(original);
            List<Integer> identities = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6));
            remaining.remove(selected);
            identities.remove(selected);
            for (int second = 0; second < remaining.stream().mapToInt(Integer::intValue).sum(); second++)
                assertNotEquals(selected, identities.get(RaidRewardPool.weightedIndex(remaining, second)).intValue());
        }
    }

    @Test
    void curatedResourcesHaveMonotonicTierCountsAndNoBooks() throws Exception {
        try (var stream = getClass().getResourceAsStream("/data/jem_world_boss_tiers/jem_world_boss_tiers/raid_rewards/raid.json")) {
            assertNotNull(stream);
            var data = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            assertEquals(5, data.getAsJsonArray("book_levels").size());
            var unique = new HashSet<String>();
            for (var element : data.getAsJsonArray("resources")) {
                var entry = element.getAsJsonObject();
                String id = entry.get("item").getAsString();
                assertTrue(unique.add(id));
                assertFalse(id.contains("book"));
                assertTrue(entry.get("weight").getAsInt() > 0);
                int previous = 0;
                var counts = entry.getAsJsonArray("counts");
                assertEquals(5, counts.size());
                for (var count : counts) { assertTrue(count.getAsInt() >= previous); previous = count.getAsInt(); }
            }
            assertTrue(unique.containsAll(List.of("minecraft:diamond_block", "minecraft:iron_block", "minecraft:gold_block",
                    "minecraft:emerald_block", "minecraft:netherite_scrap")));
        }
    }
}
