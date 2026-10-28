package com.justenoughmods.achievementguide.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.common.util.FakePlayer;

/** One native menu construction, exact freshly generated loot container and native StructureStart. */
public final class StructureLootProof {
    private static final ThreadLocal<Run> CURRENT = new ThreadLocal<>();
    private static final Map<String, String> ACTIONS = actions();
    public record Opened(AbstractContainerMenu menu, Set<String> actions) {}
    private record Receipt(RandomizableContainerBlockEntity container, Set<String> actions) {}
    private record Run(ServerPlayer actor, List<Receipt> receipts) {}
    private StructureLootProof() {}

    public static Opened create(MenuProvider provider, int id, Inventory inventory, Player player) {
        if (!(player instanceof ServerPlayer actor) || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild)
            return new Opened(provider.createMenu(id, inventory, player), Set.of());
        Run previous = CURRENT.get(), run = new Run(actor, new ArrayList<>());
        CURRENT.set(run);
        AbstractContainerMenu menu;
        try { menu = provider.createMenu(id, inventory, player); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        Set<String> accepted = new HashSet<>();
        if (menu != null) for (Receipt receipt : run.receipts) {
            boolean same = menu.slots.stream().anyMatch(slot -> slot.container == receipt.container
                    || slot.container instanceof CompoundContainer compound && compound.contains(receipt.container));
            if (same) accepted.addAll(receipt.actions);
        }
        return new Opened(menu, Set.copyOf(accepted));
    }

    /** Called only after the native deferred loot table has finished filling this exact container. */
    public static void generated(RandomizableContainerBlockEntity container, Player player) {
        Run run = CURRENT.get();
        if (run == null || player != run.actor || run.receipts.size() >= 2 || container.getLevel() != run.actor.serverLevel()) return;
        var level = run.actor.serverLevel();
        var position = container.getBlockPos();
        var origin = container.getPersistentData().getCompound(StructureLootOrigin.KEY);
        if (origin.isEmpty() || !origin.getString("dimension").equals(level.dimension().location().toString())
                || origin.getLong("position") != position.asLong()) return;
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        if (chunk == null) return;
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Set<String> matched = new HashSet<>();
        // Consult only native references for this already loaded container chunk, never a world search.
        for (var structure : chunk.getAllReferences().keySet()) {
            var key = registry.getKey(structure);
            String action = key == null ? null : ACTIONS.get(key.toString());
            if (action == null || !key.toString().equals(origin.getString("structure"))) continue;
            var start = level.structureManager().getStructureAt(position, structure);
            if (start.isValid() && start.getChunkPos().toLong() == origin.getLong("start_chunk")
                    && start.getBoundingBox().isInside(run.actor.blockPosition())
                    && start.getBoundingBox().isInside(position) && level.structureManager().structureHasPieceAt(position, start)) matched.add(action);
        }
        if (!matched.isEmpty()) run.receipts.add(new Receipt(container, Set.copyOf(matched)));
    }

    static String actionFor(String structure) { return ACTIONS.get(structure); }

    private static Map<String, String> actions() {
        Map<String, String> result = new LinkedHashMap<>();
        add(result, "v3_world_desert_temple", "betterdeserttemples:desert_temple");
        add(result, "v3_world_fortress", "betterfortresses:fortress", "mns:mega_fortress", "mns:nether_tower", "mns:large_nether_brick", "mns:medium_house");
        add(result, "v3_world_mineshaft", "mmr:mineshaft", "mmr:desert_mineshaft", "mmr:jungle_mineshaft", "mmr:mesa_mineshaft", "mmr:snowy_mineshaft");
        add(result, "v3_world_monument", "betteroceanmonuments:ocean_monument");
        add(result, "v3_world_ocean_ruins", "mos:ruined_castle", "mos:ruined_church", "mos:small_ocean_temple", "mos:ruins_atlantis", "trek:overworld/rare/sunken_city");
        add(result, "v3_world_ruins", "mvs:castle_ruins", "mtr:jungle_temple", "mtr:badlands_temple", "trek:overworld/medium/jungle_treehouse", "trek:overworld/rare/castle");
        return Map.copyOf(result);
    }
    private static void add(Map<String, String> result, String action, String... structures) {
        for (String structure : structures) result.put(structure, action);
    }
}
