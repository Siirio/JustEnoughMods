package dev.taste.engine.client.ui;

import com.google.gson.JsonPrimitive;
import dev.taste.engine.config.SettingSpec;

public record SettingRow(SettingSpec spec, JsonPrimitive value, boolean overridden, boolean available, String consumer) { }
