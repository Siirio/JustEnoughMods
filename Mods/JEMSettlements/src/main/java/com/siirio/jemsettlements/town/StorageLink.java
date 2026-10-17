package com.siirio.jemsettlements.town;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** One logical store; both chest halves share its stable number. */
public record StorageLink(ResourceLocation dimension, BlockPos position, BlockPos second, int number,
                          boolean feedResidents, boolean productionInput, boolean productionOutput) {
    public StorageLink(ResourceLocation dimension, BlockPos position) { this(dimension, position, null, 0, true, true, true); }
    public boolean contains(BlockPos pos) { return position.equals(pos) || pos.equals(second); }
    public StorageLink at(BlockPos first, BlockPos other) { return new StorageLink(dimension,first,other,number,true,productionInput,productionOutput); }
    public StorageLink numbered(int value) { return new StorageLink(dimension,position,second,value,true,productionInput,productionOutput); }
    public StorageLink withFeedResidents(boolean enabled) { return new StorageLink(dimension,position,second,number,enabled,productionInput,productionOutput); }
    public StorageLink withProduction(boolean input, boolean output) { return new StorageLink(dimension,position,second,number,feedResidents,input,output); }
    CompoundTag save() {
        CompoundTag tag=new CompoundTag(); tag.putString("Dimension",dimension.toString());tag.putLong("Position",position.asLong());
        if(second!=null)tag.putLong("Second",second.asLong());tag.putInt("Number",number);
        tag.putBoolean("FeedResidents",feedResidents);tag.putBoolean("ProductionInput",productionInput);tag.putBoolean("ProductionOutput",productionOutput);return tag;
    }
    static StorageLink load(CompoundTag tag) {
        boolean legacy=!tag.contains("ProductionInput")&&!tag.contains("ProductionOutput");
        return new StorageLink(new ResourceLocation(tag.getString("Dimension")),BlockPos.of(tag.getLong("Position")),tag.contains("Second")?BlockPos.of(tag.getLong("Second")):null,tag.getInt("Number"),true,legacy||tag.getBoolean("ProductionInput"),legacy||tag.getBoolean("ProductionOutput"));
    }
}
