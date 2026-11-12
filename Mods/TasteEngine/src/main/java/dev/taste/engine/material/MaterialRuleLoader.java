package dev.taste.engine.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.taste.engine.material.MaterialDescription.Family;
import dev.taste.engine.material.MaterialDescription.NormalY;
import dev.taste.engine.material.MaterialDescription.TextureFormat;
import dev.taste.engine.material.MaterialDescription.Wind;
import dev.taste.engine.material.MaterialRule.Field;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** Loads v1 rules from bundled resources, active resource packs and local user files. */
public final class MaterialRuleLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BUILTINS = "/assets/taste/materials/defaults.json";
    private static final String PACK_DIRECTORY = "taste/materials";
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern RGB = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final Set<String> MATCH_KEYS = Set.of(
            "block", "tag", "fluid", "fluidTag", "entity", "entityTag", "sprite", "renderLayer", "state");

    public int load(ResourceManager resources, MaterialResolver resolver) throws IOException {
        List<MaterialRule> builtins;
        InputStream input = MaterialRuleLoader.class.getResourceAsStream(BUILTINS);
        if (input == null) throw new IOException("Missing bundled Taste material rules");
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            builtins = read(reader, BUILTINS, true);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid bundled Taste material rules", exception);
        }

        List<MaterialRule> packs = new ArrayList<>();
        var packFiles = resources.listResourceStacks(PACK_DIRECTORY, id -> id.getPath().endsWith(".json"));
        for (var entry : packFiles.entrySet().stream().sorted(Map.Entry.comparingByKey(
                Comparator.comparing(ResourceLocation::toString))).toList()) {
            for (Resource resource : entry.getValue()) {
                String source = entry.getKey() + " from " + resource.sourcePackId();
                loadExternal(resource::openAsReader, source, packs);
            }
        }

        List<MaterialRule> user = new ArrayList<>();
        Path directory = FMLPaths.CONFIGDIR.get().resolve("taste").resolve("materials");
        if (Files.isDirectory(directory)) {
            try (var files = Files.list(directory)) {
                for (Path path : files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    loadExternal(() -> Files.newBufferedReader(path, StandardCharsets.UTF_8), path.toString(), user);
                }
            }
        }
        resolver.replace(builtins, packs, user);
        return builtins.size() + packs.size() + user.size();
    }

    @FunctionalInterface
    private interface ReaderSource {
        Reader open() throws IOException;
    }

    private static void loadExternal(ReaderSource source, String name, List<MaterialRule> rules) {
        try (Reader reader = source.open()) {
            rules.addAll(read(reader, name, false));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not load Taste material file {}: {}", name, exception.getMessage());
        }
    }

    private static List<MaterialRule> read(Reader reader, String source, boolean bundled) {
        JsonObject root = object(JsonParser.parseReader(reader), "root");
        checkKeys(root, Set.of("schemaVersion", "rules"));
        integer(root.get("schemaVersion"), "schemaVersion", 1, 1);
        JsonElement element = root.get("rules");
        if (element == null || !element.isJsonArray()) throw invalid("rules", "expected an array");
        JsonArray array = element.getAsJsonArray();
        if (array.size() > 4096) throw invalid("rules", "expected at most 4096 rules");

        List<MaterialRule> parsed = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            try {
                JsonObject rule = object(array.get(index), "rule");
                checkKeys(rule, Set.of("id", "priority", "match", "material"));
                if (rule.has("id")) resourceId(rule.get("id"), "id");
                int priority = rule.has("priority") ? integer(rule.get("priority"), "priority", -1000, 1000) : 0;
                MaterialRule.Match match = match(object(rule.get("match"), "match"));
                Map<Field, Object> properties = material(object(rule.get("material"), "material"));
                if (available(match, source, index)) parsed.add(new MaterialRule(priority, match, properties));
            } catch (IllegalArgumentException exception) {
                if (bundled) throw exception;
                LOGGER.warn("Ignored Taste material rule {}[{}]: {}", source, index, exception.getMessage());
            }
        }
        return parsed;
    }

    private static MaterialRule.Match match(JsonObject object) {
        checkKeys(object, MATCH_KEYS);
        if (object.size() == 0) throw invalid("match", "expected at least one predicate");
        ResourceLocation block = optionalId(object, "block");
        ResourceLocation tag = optionalId(object, "tag");
        ResourceLocation fluid = optionalId(object, "fluid");
        ResourceLocation fluidTag = optionalId(object, "fluidTag");
        ResourceLocation entity = optionalId(object, "entity");
        ResourceLocation entityTag = optionalId(object, "entityTag");
        ResourceLocation sprite = optionalId(object, "sprite");
        MaterialQuery.Layer layer = object.has("renderLayer")
                ? option(object.get("renderLayer"), "renderLayer", MaterialQuery.Layer.class) : null;

        boolean blocks = block != null || tag != null;
        boolean fluids = fluid != null || fluidTag != null;
        boolean entities = entity != null || entityTag != null;
        if ((blocks && (fluids || entities)) || (fluids && entities)) {
            throw invalid("match", "block, fluid and entity predicates are mutually exclusive");
        }

        Map<String, String> state = new LinkedHashMap<>();
        if (object.has("state")) {
            if (!blocks) throw invalid("state", "requires a block or tag predicate");
            JsonObject predicates = object(object.get("state"), "state");
            if (predicates.size() > 32) throw invalid("state", "expected at most 32 properties");
            for (var predicate : predicates.entrySet()) {
                state.put(predicate.getKey(), string(predicate.getValue(), "state." + predicate.getKey(), 64));
            }
        }
        int specificity = object.size() - (object.has("state") ? 1 : 0) + state.size();
        return new MaterialRule.Match(block, tag, fluid, fluidTag, entity, entityTag, sprite, layer,
                Map.copyOf(state), specificity);
    }

    private static boolean available(MaterialRule.Match match, String source, int index) {
        ResourceLocation missing = null;
        if (match.block() != null && !BuiltInRegistries.BLOCK.containsKey(match.block())) missing = match.block();
        if (match.fluid() != null && !BuiltInRegistries.FLUID.containsKey(match.fluid())) missing = match.fluid();
        if (match.entity() != null && !BuiltInRegistries.ENTITY_TYPE.containsKey(match.entity())) missing = match.entity();
        if (missing != null) {
            LOGGER.warn("Inactive Taste material rule {}[{}]: registry entry {} is absent", source, index, missing);
            return false;
        }
        if (match.block() != null) {
            Block block = BuiltInRegistries.BLOCK.get(match.block());
            for (var entry : match.state().entrySet()) {
                Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
                if (property == null || property.getValue(entry.getValue()).isEmpty()) {
                    throw invalid("state." + entry.getKey(), "invalid value for " + match.block());
                }
            }
        }
        return true;
    }

    private static Map<Field, Object> material(JsonObject object) {
        if (object.size() == 0) throw invalid("material", "expected at least one property");
        EnumMap<Field, Object> properties = new EnumMap<>(Field.class);
        for (var entry : object.entrySet()) {
            String name = entry.getKey();
            JsonElement value = entry.getValue();
            switch (name) {
                case "family" -> properties.put(Field.FAMILY, option(value, name, Family.class));
                case "textureFormat" -> properties.put(Field.TEXTURE_FORMAT, option(value, name, TextureFormat.class));
                case "normalY" -> properties.put(Field.NORMAL_Y, option(value, name, NormalY.class));
                case "roughness" -> properties.put(Field.ROUGHNESS, decimal(value, name, 0.02, 1));
                case "dielectricF0" -> properties.put(Field.DIELECTRIC_F0, decimal(value, name, 0, 0.898));
                case "metalness" -> properties.put(Field.METALNESS, decimal(value, name, 0, 1));
                case "metalCode" -> properties.put(Field.METAL_CODE, integer(value, name, 230, 255));
                case "opacity" -> properties.put(Field.OPACITY, decimal(value, name, 0, 1));
                case "cutoutThreshold" -> properties.put(Field.CUTOUT_THRESHOLD, decimal(value, name, 0, 1));
                case "emissionColor" -> properties.put(Field.EMISSION_COLOR, color(value, name));
                case "emission" -> properties.put(Field.EMISSION, decimal(value, name, 0, 64));
                case "porosity" -> properties.put(Field.POROSITY, decimal(value, name, 0, 1));
                case "thinScattering" -> properties.put(Field.THIN_SCATTERING, decimal(value, name, 0, 1));
                case "ior" -> properties.put(Field.IOR, decimal(value, name, 1, 2.5));
                case "thickness" -> properties.put(Field.THICKNESS, decimal(value, name, 0, 8));
                case "absorptionColor" -> properties.put(Field.ABSORPTION_COLOR, color(value, name));
                case "absorptionDensity" -> properties.put(Field.ABSORPTION_DENSITY, decimal(value, name, 0, 64));
                case "wind" -> properties.put(Field.WIND, option(value, name, Wind.class));
                case "windWeight" -> properties.put(Field.WIND_WEIGHT, decimal(value, name, 0, 2));
                case "wetness" -> properties.put(Field.WETNESS, decimal(value, name, 0, 1));
                case "snowRetention" -> properties.put(Field.SNOW_RETENTION, decimal(value, name, 0, 1));
                case "rainDroplets" -> properties.put(Field.RAIN_DROPLETS, bool(value, name));
                case "castShadow" -> properties.put(Field.CAST_SHADOW, bool(value, name));
                case "receiveShadow" -> properties.put(Field.RECEIVE_SHADOW, bool(value, name));
                case "castTransmission" -> properties.put(Field.CAST_TRANSMISSION, bool(value, name));
                case "normalMap" -> properties.put(Field.NORMAL_MAP, resourceId(value, name));
                case "specularMap" -> properties.put(Field.SPECULAR_MAP, resourceId(value, name));
                case "heightMap" -> properties.put(Field.HEIGHT_MAP, resourceId(value, name));
                case "localLight" -> localLight(object(value, name), properties);
                default -> throw invalid(name, "unknown material property");
            }
        }
        return Map.copyOf(properties);
    }

    private static void localLight(JsonObject value, Map<Field, Object> properties) {
        checkKeys(value, Set.of("range", "intensity", "color"));
        properties.put(Field.LIGHT_RANGE, decimal(value.get("range"), "localLight.range", 0, 64));
        properties.put(Field.LIGHT_INTENSITY, decimal(value.get("intensity"), "localLight.intensity", 0, 32));
        if (value.has("color")) properties.put(Field.LIGHT_COLOR, color(value.get("color"), "localLight.color"));
    }

    private static JsonObject object(JsonElement value, String field) {
        if (value == null || !value.isJsonObject()) throw invalid(field, "expected an object");
        return value.getAsJsonObject();
    }

    private static void checkKeys(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) throw invalid(key, "unknown property");
        }
    }

    private static ResourceLocation optionalId(JsonObject object, String field) {
        return object.has(field) ? resourceId(object.get(field), field) : null;
    }

    private static ResourceLocation resourceId(JsonElement value, String field) {
        String text = string(value, field, 256);
        if (!RESOURCE_ID.matcher(text).matches()) throw invalid(field, "expected a namespaced resource ID");
        return new ResourceLocation(text);
    }

    private static <T extends Enum<T>> T option(JsonElement value, String field, Class<T> type) {
        String text = string(value, field, 64);
        for (T option : type.getEnumConstants()) {
            if (option.name().toLowerCase(Locale.ROOT).equals(text)) return option;
        }
        throw invalid(field, "unknown value " + text);
    }

    private static int color(JsonElement value, String field) {
        String text = string(value, field, 7);
        if (!RGB.matcher(text).matches()) throw invalid(field, "expected #RRGGBB");
        return Integer.parseInt(text.substring(1), 16);
    }

    private static String string(JsonElement value, String field, int maxLength) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw invalid(field, "expected a string");
        }
        String text = value.getAsString();
        if (text.length() > maxLength) throw invalid(field, "string is too long");
        return text;
    }

    private static int integer(JsonElement value, String field, int minimum, int maximum) {
        number(value, field);
        int integer;
        try {
            integer = value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(field, "expected an integer");
        }
        if (integer < minimum || integer > maximum) throw invalid(field, "integer is outside its bounds");
        return integer;
    }

    private static float decimal(JsonElement value, String field, double minimum, double maximum) {
        number(value, field);
        double decimal = value.getAsDouble();
        if (!Double.isFinite(decimal) || decimal < minimum || decimal > maximum) {
            throw invalid(field, "number is outside its bounds");
        }
        return (float) decimal;
    }

    private static void number(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(field, "expected a number");
        }
    }

    private static boolean bool(JsonElement value, String field) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw invalid(field, "expected a boolean");
        }
        return value.getAsBoolean();
    }

    private static IllegalArgumentException invalid(String field, String reason) {
        return new IllegalArgumentException(field + ": " + reason);
    }
}
