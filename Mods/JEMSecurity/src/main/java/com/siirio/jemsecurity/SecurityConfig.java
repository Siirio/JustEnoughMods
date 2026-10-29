package com.siirio.jemsecurity;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraftforge.common.ForgeConfigSpec;

public final class SecurityConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue ANTI_XRAY;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_ATTESTATION;
    public static final ForgeConfigSpec.IntValue ATTESTATION_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_MOD_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_PACK_TOKENS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("security");
        ENABLED = builder.define("enabled", true);
        ANTI_XRAY = builder.comment("Replaces fully hidden ore states only in outgoing chunk data")
                .define("antiXray", true);
        REQUIRE_ATTESTATION = builder.comment("Requires the matching JEM Security client companion")
                .define("requireClientAttestation", true);
        ATTESTATION_TIMEOUT_SECONDS = builder.defineInRange("attestationTimeoutSeconds", 12, 3, 60);
        BANNED_MOD_IDS = builder.comment("Exact client mod IDs. jem_freecam is intentionally allowed")
                .defineListAllowEmpty("bannedClientModIds",
                        List.of("freecam", "xray"),
                        value -> value instanceof String text && !text.isBlank());
        BANNED_PACK_TOKENS = builder.comment("Case-insensitive tokens checked in enabled resource-pack IDs")
                .defineListAllowEmpty("bannedResourcePackTokens",
                        List.of("xray", "x-ray", "ore_finder", "ore-finder"),
                        value -> value instanceof String text && !text.isBlank());
        builder.pop();
        SPEC = builder.build();
    }

    private SecurityConfig() { }

    public static Set<String> bannedMods() {
        return BANNED_MOD_IDS.get().stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public static List<String> bannedPackTokens() {
        return BANNED_PACK_TOKENS.get().stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }
}
