package com.justenoughmods.achievementguide.integration.food;
import java.util.List;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
/** Public normal helper DTOs remain accessible after injected methods merge into vanilla targets. */
public final class FoodTransferSnapshots {
 public record EntityMerge(ItemEntity destination,ItemEntity source,int sourceCount,List<NativeFoodSources.Claim> from,List<NativeFoodSources.Claim> to){}
 public record MenuDestination(int slot,ItemStack before,List<NativeFoodSources.Claim> claims){}
 public record MenuTransfer(ItemStack source,ItemStack before,List<NativeFoodSources.Claim> claims,List<MenuDestination> destinations){}
 public record SlotTransfer(ItemStack source,int count,int destinationCount,List<NativeFoodSources.Claim> from,List<NativeFoodSources.Claim> to){}
 public record VoidMeal(ServerPlayer actor, ItemStack stack, int count){}
 public static final class PitchforkRun {
  private final ServerPlayer actor;
  private boolean grew;
  public PitchforkRun(ServerPlayer actor){this.actor=actor;}
  public ServerPlayer actor(){return actor;}
  public boolean grew(){return grew;}
  public void growth(){grew=true;}
 }
 public static final class SeashellReceipt {
  private final UUID actor;
  private final ItemStack shell;
  private final int count;
  private boolean received;
  public SeashellReceipt(UUID actor,ItemStack shell){this.actor=actor;this.shell=shell;count=shell.getCount();}
  public UUID actor(){return actor;}
  public ItemStack shell(){return shell;}
  public int count(){return count;}
  public boolean received(){return received;}
  public void receipt(){received=true;}
 }
 private FoodTransferSnapshots(){}
}
