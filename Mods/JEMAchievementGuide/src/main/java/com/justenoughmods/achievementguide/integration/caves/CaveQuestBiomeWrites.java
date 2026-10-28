package com.justenoughmods.achievementguide.integration.caves;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import com.github.alexmodguy.alexscaves.server.block.blockentity.ConversionCrucibleBlockEntity;
public final class CaveQuestBiomeWrites {
    public record Cell(int x,int y,int z,Holder<Biome> result){}
    public static final class Run {public final ConversionCrucibleBlockEntity owner;public final ServerPlayer actor;public boolean valid=true,changed;public int cells;Run(ConversionCrucibleBlockEntity owner,ServerPlayer actor){this.owner=owner;this.actor=actor;}}
    private static final ThreadLocal<Run> CURRENT=new ThreadLocal<>();private CaveQuestBiomeWrites(){}
    public static void begin(ConversionCrucibleBlockEntity owner,boolean completedNativeTime){
        CURRENT.remove();var proof=owner.getPersistentData().getCompound("jemCaveCrucibleV4");
        if(!completedNativeTime || !(owner.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || !proof.hasUUID("actor") || !proof.getBoolean("final")
                || owner.getFilledLevel()!=ConversionCrucibleBlockEntity.MAX_FILL_AMOUNT || owner.isWitchMode() || owner.getConvertingToBiome()==null || !owner.getConvertingToBiome().location().toString().equals(proof.getString("biome")))return;
        var actor=CaveQuestProof.actor(level,proof.getUUID("actor"));if(actor!=null)CURRENT.set(new Run(owner,actor));
    }
    public static void fill(ConversionCrucibleBlockEntity owner,ChunkAccess chunk,BiomeResolver resolver,Climate.Sampler sampler){
        var run=CURRENT.get();if(run==null || run.owner!=owner){chunk.fillBiomesFromNoise(resolver,sampler);return;}
        var changes=new ArrayList<Cell>();
        chunk.fillBiomesFromNoise((x,y,z,noise)->{
            Holder<Biome> before=chunk.getNoiseBiome(x,y,z),result=resolver.getNoiseBiome(x,y,z,noise);
            if(!before.equals(result)){
                if(++run.cells>4096)run.valid=false;
                else {var pos=new BlockPos(QuartPos.toBlock(x),QuartPos.toBlock(y),QuartPos.toBlock(z));if(!CaveQuestProof.permitted(run.actor,pos))run.valid=false;changes.add(new Cell(x,y,z,result));}
            }return result;
        },sampler);
        for(var cell:changes)if(chunk.getNoiseBiome(cell.x(),cell.y(),cell.z()).equals(cell.result()))run.changed=true;else run.valid=false;
    }
    public static void end(ConversionCrucibleBlockEntity owner){var run=CURRENT.get();CURRENT.remove();
        if(run!=null && run.owner==owner && run.valid && run.changed)CaveQuestProof.emit(run.actor,"v3_cave_landscape_recipe");
        if(owner.getPersistentData().contains("jemCaveCrucibleV4")){owner.getPersistentData().remove("jemCaveCrucibleV4");owner.setChanged();}
    }
}
