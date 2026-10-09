package com.justenoughmods.achievementguide.integration.treechop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TreeChopAwardsTest {
    @Test
    void unsuccessfulChopAwardsNothing() {
        assertTrue(TreeChopAwards.forFinishedChop(false, false).isEmpty());
    }

    @Test
    void felledTreeAwardsTreeAchievement() {
        assertEquals(TreeChopAwards.TREE, TreeChopAwards.forFinishedChop(true, false));
    }

    @Test
    void felledMushroomAwardsTreeAndMushroomAchievements() {
        assertEquals(TreeChopAwards.TREE_AND_MUSHROOM, TreeChopAwards.forFinishedChop(true, true));
    }
}
