package com.justenoughmods.achievementguide.integration.prehistoric;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.nbt.CompoundTag;
public final class WorldPrehistoricQuestFurnace {
 public static final String KEY="jemWorldPrehistoricPowderV4";
 public record Burn(ItemStack input,ItemStack output,CompoundTag receipt,String recipe){}
 private WorldPrehistoricQuestFurnace(){}
 public static Burn before(AbstractFurnaceBlockEntity owner,Recipe<?> recipe){return new Burn(owner.getItem(0).copy(),owner.getItem(2).copy(),owner.getPersistentData().getCompound(KEY).copy(),recipe==null?"":recipe.getId().toString());}
 public static boolean matches(ItemStack stack,CompoundTag proof){return proof.contains("item",10) && proof.getCompound("item").equals(stack.save(new CompoundTag()));}
 public static void after(AbstractFurnaceBlockEntity owner,Burn before,boolean accepted){if(!accepted || before==null || owner.getLevel()==null || owner.getLevel().isClientSide || !(before.recipe().equals("unearthed_journey:bituminous_rock_smelt_recipe") || before.recipe().equals("unearthed_journey:bituminous_rock_blast_recipe")) || !WorldPrehistoricQuestProof.id(before.input()).equals("unearthed_journey:bituminous_rock") || before.input().getCount()-owner.getItem(0).getCount()!=1 || !WorldPrehistoricQuestProof.id(owner.getItem(2)).equals("unearthed_journey:bituminous_powder"))return;var now=owner.getItem(2);int added=now.getCount()-before.output().getCount();if(added<=0 || !before.output().isEmpty() && !ItemStack.isSameItemSameTags(before.output(),now))return;int proven=matches(before.output(),before.receipt())?before.receipt().getInt("proven"):0;var t=new CompoundTag();t.put("item",now.save(new CompoundTag()));t.putInt("proven",Math.min(now.getCount(),proven+added));t.putString("recipe",before.recipe());owner.getPersistentData().put(KEY,t);owner.setChanged();}
}
