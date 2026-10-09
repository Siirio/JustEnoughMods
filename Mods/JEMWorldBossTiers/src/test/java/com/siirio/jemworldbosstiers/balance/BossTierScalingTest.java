package com.siirio.jemworldbosstiers.balance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BossTierScalingTest {
    @Test void healthAboveAttributeCapRetainsAllTierAndPartyFactors() {
        var attribute = new net.minecraft.world.entity.ai.attributes.AttributeInstance(
                new net.minecraft.world.entity.ai.attributes.RangedAttribute("test.health", 1000, 1, 1024), value -> {});
        attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                java.util.UUID.randomUUID(), "tier", .75, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                java.util.UUID.randomUUID(), "party", 1.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        assertEquals(1024, attribute.getValue());
        assertEquals(4900, EncounterScaler.effectiveHealth(attribute), 0.00001);
    }
    @Test void additiveBoostAppliesOnceForEveryTierIncludingTierOne() {
        double[] expected = {1.15, 1.30, 1.45, 1.60, 1.75};
        for (int tier = 1; tier <= expected.length; tier++) assertEquals(expected[tier - 1], BossTierScaling.boost(tier), 0.000001);
        assertEquals(1.15, BossTierScaling.boost(0), 0.000001);
        assertEquals(1.75, BossTierScaling.boost(99), 0.000001);
    }
}
