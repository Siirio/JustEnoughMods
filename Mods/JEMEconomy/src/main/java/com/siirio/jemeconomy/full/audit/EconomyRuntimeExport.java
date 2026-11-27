package com.siirio.jemeconomy.full.audit;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in loaded-registry evidence. No entities are spawned or player/save data changed. */
public final class EconomyRuntimeExport {
    private EconomyRuntimeExport() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (event.getServer().isDedicatedServer() && Boolean.getBoolean("jem.economy.export")) export(event.getServer());
    }

    public static void export(MinecraftServer server) {
        JsonObject document = new JsonObject();
        document.addProperty("scope", "Loaded dedicated-server item registry and recipes; not player acceptance");
        document.addProperty("capturedUtc", java.time.Instant.now().toString());
        document.addProperty("schema",2);
        document.addProperty("dataSource","Current loaded RecipeManager, Forge registries and winning merged recipe resources");
        document.addProperty("tests","No tests or native contract fixtures are executed by this evidence export");
        document.add("artifacts",artifacts(server));
        JsonObject metadata=new JsonObject();
        for(var mod:net.minecraftforge.fml.ModList.get().getMods())metadata.addProperty(mod.getModId(),mod.getVersion().toString());
        document.add("loadedMods",metadata);
        JsonArray items = new JsonArray();
        for (var id : ForgeRegistries.ITEMS.getKeys().stream().sorted().toList()) {
            var item = ForgeRegistries.ITEMS.getValue(id);
            ItemStack stack = new ItemStack(item);
            JsonObject row = new JsonObject(); row.addProperty("id", id.toString());
            row.addProperty("class", item.getClass().getName()); row.addProperty("name", stack.getHoverName().getString());
            row.addProperty("stackSize", stack.getMaxStackSize()); row.addProperty("durability", stack.getMaxDamage());
            row.addProperty("rarity", stack.getRarity().name());
            row.addProperty("isRegistered",true);
            row.addProperty("recordItem",item instanceof net.minecraft.world.item.RecordItem);
            row.addProperty("itemInventoryCapability",stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent());
            row.addProperty("itemFluidCapability",stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent());
            row.addProperty("itemEnergyCapability",stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent());
            row.add("defaultStack",stackJson(stack));
            JsonArray hierarchy=new JsonArray();for(Class<?> type=item.getClass();type!=null;type=type.getSuperclass())hierarchy.add(type.getName());
            row.add("itemHierarchy",hierarchy);
            var remainder=item.getCraftingRemainingItem();
            if(remainder!=null)row.addProperty("craftingRemainder",String.valueOf(ForgeRegistries.ITEMS.getKey(remainder)));
            if(item instanceof net.minecraft.world.item.BlockItem blockItem) {
                var block=blockItem.getBlock();var state=block.defaultBlockState();JsonObject info=new JsonObject();
                info.addProperty("id",String.valueOf(ForgeRegistries.BLOCKS.getKey(block)));
                info.addProperty("class",block.getClass().getName());
                JsonArray types=new JsonArray();for(Class<?> type=block.getClass();type!=null;type=type.getSuperclass())types.add(type.getName());info.add("hierarchy",types);
                info.addProperty("entityBlock",block instanceof net.minecraft.world.level.block.EntityBlock);
                info.addProperty("hasBlockEntity",state.hasBlockEntity());info.addProperty("redstoneSource",state.isSignalSource());
                info.addProperty("lootTable",block.getLootTable().toString());
                JsonArray blockTags=new JsonArray();state.getTags().map(Object::toString).sorted().forEach(blockTags::add);info.add("tags",blockTags);
                JsonArray entityTypes=new JsonArray();for(var entry:ForgeRegistries.BLOCK_ENTITY_TYPES.getEntries())
                    if(entry.getValue().isValid(state))entityTypes.add(entry.getKey().location().toString());info.add("blockEntityTypes",entityTypes);
                info.addProperty("capabilityEvidence","No block entity is instantiated: container/energy/transport behavior requires exact class/source review");
                row.add("block",info);
            }
            JsonArray tags = new JsonArray(); stack.getTags().map(Object::toString).sorted().forEach(tags::add); row.add("tags", tags);
            var food = item.getFoodProperties(stack, null);
            if (food != null) {
                row.addProperty("nutrition", food.getNutrition()); row.addProperty("saturation", food.getSaturationModifier());
                row.addProperty("meat", food.isMeat()); row.addProperty("foodEffects", food.getEffects().size());
            }
            items.add(row);
        }
        document.add("items", items);
        JsonArray recipes = new JsonArray();
        for (var recipe : server.getRecipeManager().getRecipes().stream().sorted(java.util.Comparator.comparing(r -> r.getId().toString())).toList()) {
            JsonObject row = new JsonObject(); row.addProperty("id", recipe.getId().toString());
            row.addProperty("type", String.valueOf(ForgeRegistries.RECIPE_SERIALIZERS.getKey(recipe.getSerializer())));
            row.addProperty("class",recipe.getClass().getName());
            row.addProperty("serializerClass",recipe.getSerializer().getClass().getName());
            var source=new net.minecraft.resources.ResourceLocation(recipe.getId().getNamespace(),"recipes/"+recipe.getId().getPath()+".json");
            var resource=server.getResourceManager().getResource(source);
            if(resource.isPresent()) {
                row.addProperty("resource",source.toString());row.addProperty("sourcePack",resource.get().sourcePackId());
                try(var reader=resource.get().openAsReader()){row.add("definition",com.google.gson.JsonParser.parseReader(reader));}
                catch(java.io.IOException failure){throw new java.io.UncheckedIOException("Cannot read loaded recipe resource "+source,failure);}
            }else row.addProperty("resourceStatus","Dynamic recipe: no merged JSON resource; loaded ingredients/results below are authoritative");
            ItemStack output = recipe.getResultItem(server.registryAccess());
            row.addProperty("output", String.valueOf(ForgeRegistries.ITEMS.getKey(output.getItem()))); row.addProperty("count", output.getCount());
            row.add("outputStack",stackJson(output));
            JsonArray inputs = new JsonArray();
            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                JsonArray choices = new JsonArray();
                for (ItemStack choice : ingredient.getItems()) choices.add(String.valueOf(ForgeRegistries.ITEMS.getKey(choice.getItem())));
                inputs.add(choices);
            }
            row.add("ingredients", inputs); recipes.add(row);
        }
        document.add("recipes", recipes);
        JsonArray policies = new JsonArray();
        for (var policy : com.siirio.jemeconomy.policy.ItemPolicyRegistry.reviewed()) {
            if (!policy.buyable() && !policy.sellable()) continue;
            JsonObject row = new JsonObject(); row.addProperty("item", policy.item().toString());
            row.addProperty("registered", ForgeRegistries.ITEMS.containsKey(policy.item()));
            row.addProperty("buy", policy.buyCopper()); row.addProperty("sell", policy.sellCopper());
            row.addProperty("buyDenominator",policy.buyDenominator());row.addProperty("sellDenominator",policy.sellDenominator());
            row.addProperty("quotaFamily",policy.quotaFamily().toString());row.addProperty("quotaUnits",policy.quotaUnits());
            row.addProperty("outcome",policy.outcome().name());policies.add(row);
        }
        document.add("marketPolicies", policies);
        JsonArray listings=new JsonArray();
        for(var listing:com.siirio.jemeconomy.policy.MarketSupply.listings(server)) {
            JsonObject row=new JsonObject();row.addProperty("id",listing.id().toString());row.add("stack",stackJson(listing.stack()));
            var policy=listing.policy();row.addProperty("outcome",policy.outcome().name());
            row.addProperty("buyNumerator",policy.buyCopper());row.addProperty("buyDenominator",policy.buyDenominator());
            row.addProperty("sellNumerator",policy.sellCopper());row.addProperty("sellDenominator",policy.sellDenominator());
            row.addProperty("group",policy.demandGroup());
            for(int count:new int[]{1,64,256,1024,4096})row.addProperty("price"+count,policy.purchaseTotal(count));
            listings.add(row);
        }
        document.add("marketListings",listings);
        document.add("nativeTrades", com.siirio.jemeconomy.merchant.NativeTradeCatalog.snapshot(server.overworld()));
        document.addProperty("nativeMerchantContracts","Not executed: automated tests and fixtures are prohibited");
        try {
            Path directory = server.getServerDirectory().toPath().resolve("local/economy-audit"); Files.createDirectories(directory);
            Path pending = directory.resolve("loaded-registry.json.tmp"), target = directory.resolve("loaded-registry.json");
            Files.writeString(pending, new GsonBuilder().setPrettyPrinting().create().toJson(document));
            Files.move(pending, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            com.mojang.logging.LogUtils.getLogger().info("JEM economy evidence: {} loaded items, {} recipes exported to {}", items.size(), recipes.size(), target);
        } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException("Cannot save requested economy evidence", failure); }
    }
    private static JsonObject stackJson(ItemStack stack) {
        JsonObject row=new JsonObject();row.addProperty("item",String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
        row.addProperty("count",stack.getCount());if(stack.hasTag())row.addProperty("nbt",stack.getTag().getAsString());return row;
    }

    private static JsonArray artifacts(MinecraftServer server) {
        JsonArray result=new JsonArray();Path directory=server.getServerDirectory().toPath().resolve("mods");
        try(var paths=Files.list(directory)) {
            for(Path path:paths.filter(p->p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                var digest=java.security.MessageDigest.getInstance("SHA-256");
                try(var input=Files.newInputStream(path)){byte[] bytes=new byte[65536];int count;while((count=input.read(bytes))!=-1)digest.update(bytes,0,count);}
                JsonObject row=new JsonObject();row.addProperty("filename",path.getFileName().toString());row.addProperty("sha256",java.util.HexFormat.of().formatHex(digest.digest()));result.add(row);
            }
            return result;
        }catch(java.io.IOException failure){throw new java.io.UncheckedIOException("Cannot bind evidence to current mod artifacts",failure);}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }

}
