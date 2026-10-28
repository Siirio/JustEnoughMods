package com.justenoughmods.achievementguide.integration.battle;
import java.util.ArrayDeque;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
public final class BattleQuestPit {
    // Exact biome branches from the native BeastDecidingProcedureProcedure, no spawn chance simulated.
    private static final Set<String> BIOMES=Set.of("minecraft:badlands","minecraft:basalt_deltas","minecraft:beach","minecraft:cherry_grove","minecraft:crimson_forest","minecraft:dark_forest","minecraft:desert","minecraft:forest","minecraft:ice_spikes","minecraft:mangrove_swamp","minecraft:nether_wastes","minecraft:old_growth_pine_taiga","minecraft:old_growth_spruce_taiga","minecraft:plains","minecraft:savanna","minecraft:snowy_plains","minecraft:snowy_taiga","minecraft:soul_sand_valley","minecraft:sparse_jungle","minecraft:stony_peaks","minecraft:stony_shore","minecraft:swamp","minecraft:warped_forest");
    private record Receipt(BlockPlaceContext context,BlockPos position,BlockState before,ItemStack stack,int count) {}
    private static final ThreadLocal<Calls> CALLS=ThreadLocal.withInitial(Calls::new);
    private static final class Calls { final ArrayDeque<Receipt> frames=new ArrayDeque<>();int overflow; }
    private BattleQuestPit() {}
    public static void begin(BlockPlaceContext context){Calls calls=CALLS.get();if(calls.overflow>0||calls.frames.size()>=16){calls.overflow++;return;}BlockPos position=context.getClickedPos().immutable();calls.frames.push(new Receipt(context,position,context.getLevel().getBlockState(position),context.getItemInHand(),context.getItemInHand().getCount()));}
    public static void end(InteractionResult result){
        Calls calls=CALLS.get();if(calls.overflow>0){calls.overflow--;return;}Receipt receipt=calls.frames.isEmpty()?null:calls.frames.pop();if(calls.frames.isEmpty())CALLS.remove();
        if(receipt==null||!BattleQuestProof.real(receipt.context.getPlayer())||!result.consumesAction()||receipt.count-receipt.stack.getCount()!=1)return;
        var level=receipt.context.getLevel();BlockState placed=level.getBlockState(receipt.position);
        String id=BuiltInRegistries.BLOCK.getKey(placed.getBlock()).toString();
        if((id.equals("luminous_beasts:beast_pit")||id.equals("luminous_beasts:beast_pit_off"))&&placed.getBlock()!=receipt.before.getBlock()
            &&level.getBiome(receipt.position).unwrapKey().map(key->BIOMES.contains(key.location().toString())).orElse(false))
            BattleQuestProof.emit(receipt.context.getPlayer(),"v3_combat_beast_watch_pit");
    }
    public static void clear(){CALLS.remove();}
}
