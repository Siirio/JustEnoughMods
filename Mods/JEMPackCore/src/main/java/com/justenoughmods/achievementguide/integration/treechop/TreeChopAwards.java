package com.justenoughmods.achievementguide.integration.treechop;

enum TreeChopAwards {
    NONE,
    TREE,
    TREE_AND_MUSHROOM;

    static TreeChopAwards forFinishedChop(boolean felled, boolean mushroomStem) {
        if (!felled) {
            return NONE;
        }
        return mushroomStem ? TREE_AND_MUSHROOM : TREE;
    }

    boolean isEmpty() {
        return this == NONE;
    }
}
