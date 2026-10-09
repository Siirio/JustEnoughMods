package com.siirio.jemworldbosstiers.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentRefreshPolicyTest {
    @Test
    void refreshesEquippedRewardsOnlyWhenWorldTierChanges() {
        assertTrue(EquipmentRefreshPolicy.required(1, 2));
        assertTrue(EquipmentRefreshPolicy.required(4, 5));
        assertFalse(EquipmentRefreshPolicy.required(2, 2));
    }
}
