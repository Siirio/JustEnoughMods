package com.justenoughmods.achievementguide.integration.spawncare;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.ninni.spawn.server.entity.util.Catchable;
import com.ninni.spawn.server.entity.mob.Clam;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.SavedData;
/** Finite physical capture lineage. Native entities keep native replacement UUIDs and data. */
public final class SpawnCareQuestContainers extends SavedData {
 public record Captured(UUID source,String type,CompoundTag state,CompoundTag payload,CompoundTag observations) {}
 public record Receipt(UUID actor,List<Captured> captured,int pigment) {}
 public record Capture(ServerPlayer actor,Mob mob,ItemStack input,CompoundTag state,CompoundTag payload,CompoundTag observations) {}
 public record Release(SpawnCareQuestRun run,InteractionHand hand,List<ItemStack> inventory) {}
 public record InteractionCapture(Capture capture,InteractionHand hand,Item expected,List<ItemStack> inventory) {}
 private final Map<UUID,Receipt> receipts=new LinkedHashMap<>();private static final int LIMIT=4096;
 public static SpawnCareQuestContainers get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(SpawnCareQuestContainers::load,SpawnCareQuestContainers::new,"jem_spawn_care_containers_v4");}
 public static CompoundTag state(Entity entity){var tag=new CompoundTag();entity.saveWithoutId(tag);for(String key:List.of("UUID","Pos","Motion","Rotation","OnGround","FallDistance"))tag.remove(key);if(tag.contains("ForgeData",10)){var data=tag.getCompound("ForgeData");data.remove(SpawnCareQuestProof.KEY);if(data.isEmpty())tag.remove("ForgeData");}return tag;}
 public static CompoundTag payload(Mob mob){var tag=new CompoundTag();if(mob instanceof Bucketable bucket){var temp=new ItemStack(Items.WATER_BUCKET);bucket.saveToBucketTag(temp);if(temp.hasTag())tag=temp.getTag().copy();}if(mob instanceof Catchable caught){tag=new CompoundTag();caught.saveDataToTag(mob,tag);}tag.putString("id",SpawnCareQuestProof.type(mob));if(mob.hasCustomName())tag.putString("CustomName",mob.getCustomName().getString());return tag;}
 public static Capture before(Player player,InteractionHand hand,Mob mob){var actor=SpawnCareQuestProof.real(player);if(actor==null||!mob.isAlive()||!SpawnCareQuestProof.type(mob).startsWith("spawn:"))return null;return new Capture(actor,mob,player.getItemInHand(hand).copy(),state(mob),payload(mob),SpawnCareQuestProof.data(mob).copy());}
 public static InteractionCapture interactionBefore(Player player,InteractionHand hand,Mob mob){var actor=SpawnCareQuestProof.real(player);if(actor==null||mob.level()!=actor.level()||actor.serverLevel().getEntity(mob.getUUID())!=mob)return null;var input=actor.getItemInHand(hand);Item expected;if(mob instanceof Catchable nativeCatch){if(!nativeCatch.canBeCaught(input))return null;var nativeOutput=nativeCatch.getCaughtItem(input);expected=nativeOutput==null||nativeOutput.isEmpty()?input.getItem():nativeOutput.getItem();}else if(mob instanceof Bucketable nativeBucket){if(!input.is(Items.WATER_BUCKET))return null;expected=nativeBucket.getBucketItemStack().getItem();}else return null;var capture=before(player,hand,mob);return capture==null?null:new InteractionCapture(capture,hand,expected,SpawnCareQuestProof.held(actor).stream().map(ItemStack::copy).toList());}
 public static void interactionCaught(InteractionCapture run,boolean accepted){if(run==null||!accepted)return;var actor=run.capture.actor;var output=actor.getItemInHand(run.hand);if(SpawnCareQuestProof.real(actor)==null||!output.is(run.expected)||output.getCount()!=1||SpawnCareQuestProof.count(SpawnCareQuestProof.held(actor),output)-SpawnCareQuestProof.count(run.inventory,output)!=1)return;caught(run.capture,run.hand,true);}
 public static Captured entry(Capture run){var expected=run.state.copy();if(run.mob instanceof Bucketable)expected.putBoolean("FromBucket",true);if(run.mob instanceof Catchable)expected.putBoolean("HasBeenCaught",true);return new Captured(run.mob.getUUID(),SpawnCareQuestProof.type(run.mob),expected,run.payload.copy(),run.observations.copy());}
 public static void caught(Capture run,InteractionHand hand,boolean accepted){if(run==null||!accepted||!run.mob.isRemoved())return;var output=run.actor.getItemInHand(hand);if(output.isEmpty()||!output.hasTag())return;var nativeTag=output.getTag();if(!(output.getItem() instanceof MobBucketItem)&&!nativeTag.getString("id").equals(SpawnCareQuestProof.type(run.mob)))return;
  // Require every actual native serialized field, including the species payload
  for(String key:run.payload.getAllKeys())if(!key.equals("id")&&!Objects.equals(run.payload.get(key),nativeTag.get(key)))return;
  certify(run.actor,output,List.of(entry(run)),-1);
  if(SpawnCareQuestProof.type(run.mob).equals("spawn:octopus")&&nativeTag.contains("Item",10)&&ItemStack.of(nativeTag.getCompound("Item")).is(Items.NAUTILUS_SHELL)&&ItemStack.of(nativeTag.getCompound("Item")).getCount()==1&&run.input.is(Items.NAUTILUS_SHELL))SpawnCareQuestProof.emit(run.actor,"v3_pet_living_aquarium_octopus");
 }
 public static void certify(ServerPlayer actor,ItemStack output,List<Captured> captured,int pigment){var store=get(actor.serverLevel());if(store.receipts.size()>=LIMIT||captured.size()>16)return;var id=NativeFoodSources.get(actor.serverLevel()).certify(actor.serverLevel(),actor.getUUID(),"spawn_care_container",output,1);if(id!=null){store.receipts.put(id,new Receipt(actor.getUUID(),List.copyOf(captured),pigment));store.setDirty();}}
 public static void certifyPigment(ServerPlayer actor,ItemStack actual,int variant){var store=get(actor.serverLevel());var old=new ArrayList<NativeFoodSources.Claim>();for(var claim:NativeFoodSources.claims(actual)){if(store.receipts.containsKey(claim.batch())){store.receipts.remove(claim.batch());NativeFoodSources.get(actor.serverLevel()).activate(claim.batch(),false);}else old.add(claim);}NativeFoodSources.claims(actual,old);store.setDirty();certify(actor,actual,List.of(),variant);}
 public static Release begin(ServerPlayer actor,InteractionHand hand){if(SpawnCareQuestProof.real(actor)==null)return null;var input=actor.getItemInHand(hand);if(!actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(input,"spawn_care_container")))return null;return new Release(SpawnCareQuestRun.begin(actor,actor,input),hand,SpawnCareQuestProof.held(actor).stream().map(ItemStack::copy).toList());}
 public static void released(Release release,boolean accepted){if(release==null)return;var run=release.run;SpawnCareQuestRun.end(run);if(!accepted)return;var actor=run.actor;var after=actor.getItemInHand(release.hand);
  boolean spent=SpawnCareQuestProof.count(release.inventory,run.input)-SpawnCareQuestProof.count(SpawnCareQuestProof.held(actor),run.input)==1;
  boolean net=run.input.hasTag()&&run.input.getTag().contains("CapturedMobs",9)&&(!after.hasTag()||!after.getTag().contains("CapturedMobs",9));if(!spent&&!net)return;
  var store=get(actor.serverLevel());var physical=NativeFoodSources.get(actor.serverLevel());for(var claim:NativeFoodSources.claims(run.input)){var receipt=store.receipts.get(claim.batch());if(receipt==null||!receipt.actor.equals(actor.getUUID()))continue;
   // Retire accepted native container consumption even if a native release lost data
   if(!physical.consume(actor.getUUID(),run.input,"spawn_care_container",1))return;store.receipts.remove(claim.batch());store.setDirty();
   var candidates=run.created.stream().filter(e->e instanceof Mob&&e.isAlive()&&!e.isRemoved()&&actor.serverLevel().getEntity(e.getUUID())==e).toList();
   if(receipt.pigment>=0){for(var entity:candidates)if(entity instanceof Clam clam&&clam.getPackedVariant()==receipt.pigment&&clam.isInWater()&&!clam.isInWall())SpawnCareQuestProof.emit(actor,"v3_pet_reef_style_appearance");return;}
   var used=new HashSet<UUID>();for(var captured:receipt.captured){boolean matched=false;for(var entity:candidates)if(!used.contains(entity.getUUID())&&SpawnCareQuestProof.type(entity).equals(captured.type)&&!entity.getUUID().equals(captured.source)&&entity.isInWater()&&!entity.isInWall()&&entity.getAirSupply()>0&&captured.state.equals(state(entity))){used.add(entity.getUUID());if(!captured.observations.isEmpty())entity.getPersistentData().put(SpawnCareQuestProof.KEY,captured.observations.copy());matched=true;break;}if(!matched)return;}
   if(!receipt.captured.isEmpty())SpawnCareQuestProof.emit(actor,"v3_pet_living_aquarium");return;
  }
 }
 private static SpawnCareQuestContainers load(CompoundTag tag){var data=new SpawnCareQuestContainers();var rows=tag.getList("receipts",10);for(int i=0;i<Math.min(LIMIT,rows.size());i++){var row=rows.getCompound(i);if(!row.hasUUID("batch")||!row.hasUUID("actor"))continue;var entries=new ArrayList<Captured>();var list=row.getList("captured",10);for(int j=0;j<Math.min(16,list.size());j++){var e=list.getCompound(j);if(e.hasUUID("source"))entries.add(new Captured(e.getUUID("source"),e.getString("type"),e.getCompound("state").copy(),e.getCompound("payload").copy(),e.getCompound("observations").copy()));}data.receipts.put(row.getUUID("batch"),new Receipt(row.getUUID("actor"),entries,row.getInt("pigment")));}return data;}
 @Override public CompoundTag save(CompoundTag tag){var rows=new ListTag();receipts.forEach((id,receipt)->{var row=new CompoundTag();row.putUUID("batch",id);row.putUUID("actor",receipt.actor);row.putInt("pigment",receipt.pigment);var list=new ListTag();for(var e:receipt.captured){var entry=new CompoundTag();entry.putUUID("source",e.source);entry.putString("type",e.type);entry.put("state",e.state.copy());entry.put("payload",e.payload.copy());entry.put("observations",e.observations.copy());list.add(entry);}row.put("captured",list);rows.add(row);});tag.put("receipts",rows);return tag;}
}
