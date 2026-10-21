package com.siirio.jemsettlements.sandbox;

import com.siirio.jemsettlements.currency.SettlementCurrency;
import com.siirio.jemsettlements.compat.economy.CivicCommerceAdapter;
import com.siirio.jemsettlements.api.SettlementApi;
import com.siirio.jemsettlements.resident.ResidentMood;
import com.siirio.jemsettlements.runtime.ResidentFoodService;
import com.siirio.jemsettlements.runtime.SettlementAuthority;
import com.siirio.jemsettlements.runtime.SharedStorageService;
import com.siirio.jemsettlements.town.SettlementSavedData;
import com.siirio.jemsettlements.town.TownRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Extra sandbox commands use canonical towns, residents, physical storage and native orders.
 * This saved data contains only action limits, choices, receipts and patrol cursors. */
public final class SandboxCivicRuntime extends SavedData {
    private final Map<UUID, CompoundTag> towns = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> milestones = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> guardRestorations = new LinkedHashMap<>();
    private static int restorationCursor;
    private static boolean registered;
    private static java.util.function.BiPredicate<ServerPlayer,String> permission=(player,node)->false;
    private static int cursor;
    public static void register() {
        if (!registered) { registered = true; MinecraftForge.EVENT_BUS.register(SandboxCivicRuntime.class);
            if (ModList.get().isLoaded("jem_economy")) MinecraftForge.EVENT_BUS.register(com.siirio.jemsettlements.compat.economy.CivicTradeBridge.class); }
    }
    public static void permission(java.util.function.BiPredicate<ServerPlayer,String> permission) {
        SandboxCivicRuntime.permission=permission;
        if (ModList.get().isLoaded("recruits")) SandboxRecruitOrders.permission = permission;
    }
    public static boolean active(MinecraftServer server) { return ModList.get().isLoaded("jem_economy") && CivicCommerceAdapter.active(server) && ModList.get().isLoaded("jem_settlements"); }
    public static SandboxCivicRuntime get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SandboxCivicRuntime::load, SandboxCivicRuntime::new, "jem_sandbox_civic");
    }
    private CompoundTag metadata(UUID town) { return towns.computeIfAbsent(town, key -> new CompoundTag()); }
    void persist(MinecraftServer server) {
        setDirty();
        com.siirio.jemsettlements.payroll.TaxDurability.save(server,SettlementSavedData.get(server),"jem_settlements");
        com.siirio.jemsettlements.payroll.TaxDurability.save(server,this,"jem_sandbox_civic");
    }
    public static boolean beforeDelete(ServerPlayer player,UUID townId){
        if(!active(player.getServer()))return true;var town=SettlementApi.ownedTown(player,townId).orElse(null);if(town==null)return false;
        CivicCommerceAdapter.releaseFunds(player,townId);if(CivicCommerceAdapter.purposeFundsOutstanding(player.getServer(),townId))return false;
        var data=get(player.getServer());if(ModList.get().isLoaded("recruits"))SandboxRecruitOrders.cancel(player,town,data.metadata(townId));
        data.persist(player.getServer());return true;
    }
    public static void afterDelete(ServerPlayer player){
        if(!active(player.getServer()))return;CivicCommerceAdapter.flushOwner(player.getServer());get(player.getServer()).persist(player.getServer());
    }
    boolean guardPending(UUID entity){var row=guardRestorations.get(entity);return row!=null&&!row.getBoolean("Reconciled");}
    void queueGuard(UUID entity,CompoundTag row){
        if(guardPending(entity))return;guardRestorations.put(entity,row);setDirty();
    }
    void restoreGuards(MinecraftServer server){
        if(!ModList.get().isLoaded("recruits"))return;
        var pending=new ArrayList<>(guardRestorations.entrySet());boolean changed=false;
        for(int inspected=0;inspected<Math.min(32,pending.size());inspected++){
            var entry=pending.get(Math.floorMod(restorationCursor++,pending.size()));CompoundTag row=entry.getValue();
            if(row.getBoolean("Reconciled"))continue;
            var record=SettlementSavedData.get(server).workforceEntity(entry.getKey()).orElse(null);
            if(record!=null&&!record.living()){row.putBoolean("Reconciled",true);changed=true;continue;}
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(row.getString("Dimension")));
            var level=server.getLevel(key);if(level==null)continue;var entity=level.getEntity(entry.getKey());if(entity==null)continue;
            if(SandboxRecruitOrders.restore(server,entity,row)){row.putBoolean("Reconciled",true);changed=true;}
        }
        if(changed)persist(server);
    }
    @SubscribeEvent public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||!active(level.getServer())||!ModList.get().isLoaded("recruits"))return;
        var data=get(level.getServer());CompoundTag row=data.guardRestorations.get(event.getEntity().getUUID());if(row==null)return;
        level.getServer().execute(()->{
            if(SandboxRecruitOrders.restore(level.getServer(),event.getEntity(),row)){
                // Keep the small tombstone: if entity NBT rolls back across a crash, its exact old token is restored again.
                if(!row.getBoolean("Reconciled")){row.putBoolean("Reconciled",true);data.persist(level.getServer());}
            }
        });
    }
    public static boolean action(ServerPlayer player, String node, CompoundTag input) {
        if (!active(player.getServer()) || player.isSpectator() || player.isCreative()) return false;
        UUID id; try { id = UUID.fromString(input.getString("Town")); } catch (IllegalArgumentException invalid) { return false; }
        TownRecord town = SettlementApi.ownedTown(player, id).orElse(null);
        if (town == null || !town.dimension().equals(player.serverLevel().dimension().location())) return false;
        SandboxCivicRuntime data = get(player.getServer()); String mode = input.getString("Mode");
        if (mode.equals("deliver_food")) return data.deliverFood(player, town);
        return switch (node) {
            case "invite_residents" -> mode.equals("invite") && data.invite(player, town);
            case "supply_notice" -> (mode.equals("watch") || mode.equals("clear")) && data.watch(player, town, input, mode.equals("clear"));
            case "resident_move" -> mode.equals("move") && data.move(player, town, input);
            case "taxes" -> mode.equals("tax") && data.tax(player, town, input.getInt("Value"));
            case "treasury_audit" -> mode.equals("audit");
            case "civic_fund" -> data.fund(player, town, input);
            case "rally", "patrol_route", "captain_order" -> {
                boolean result=ModList.get().isLoaded("recruits")&&SandboxRecruitOrders.action(player,town,node,input,data.metadata(town.id()));
                if(result)data.persist(player.getServer());yield result;
            }
            default -> false;
        };
    }
    private boolean tax(ServerPlayer player, TownRecord town, int percent) {
        if (percent != 0 && percent != 5 && percent != 10 && percent != 15) return false;
        town.taxPercent(percent);SettlementSavedData.get(player.getServer()).changed(); persist(player.getServer()); return true;
    }
    public static void payment(ServerPlayer player, net.minecraft.world.entity.Entity merchant, java.util.function.BiConsumer<UUID,Integer> townPayment) {
        if (!active(player.getServer()) || merchant == null || !merchant.isAlive()) return;
        var settlements = SettlementSavedData.get(player.getServer());
        var resident = settlements.resident(merchant.getUUID()).filter(ResidentMood::living).orElse(null); if (resident == null) return;
        var town = settlements.town(resident.townId()).orElse(null);
        if (town != null && town.dimension().equals(merchant.level().dimension().location()) && town.contains(merchant.blockPosition())) {
            ServerPlayer owner=player.getServer().getPlayerList().getPlayers().stream().filter(p->SettlementAuthority.principal(p).filter(town.owner()::equals).isPresent()).findFirst().orElse(null);
            if(owner==null||permission.test(owner,"taxes"))townPayment.accept(town.id(), town.taxPercent());
        }
    }
    private List<BlockPos> homes(ServerPlayer player, TownRecord town) {
        List<BlockPos> result = new ArrayList<>(); var level = player.serverLevel(); var settlements = SettlementSavedData.get(player.getServer());
        // Inspect loaded blocks only; never let POI streams load a remote town chunk.
        for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-16,-4,-16), player.blockPosition().offset(16,4,16))) {
            if (result.size() >= 16) break;
            if (!town.contains(pos) || !level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD || state.getValue(BedBlock.OCCUPIED)) continue;
            if (!level.getPoiManager().existsAtPosition(PoiTypes.HOME, pos) || level.getPoiManager().getFreeTickets(pos) < 1) continue;
            if (settlements.residents(town.id()).stream().anyMatch(r -> r.living() && r.home().filter(home -> home.dimension().equals(level.dimension()) && home.pos().equals(pos)).isPresent())) continue;
            result.add(pos.immutable());
        }
        return result;
    }
    private boolean assignHome(ServerPlayer player, Villager villager, BlockPos bed) {
        var level = player.serverLevel(); var current = villager.getBrain().getMemory(MemoryModuleType.HOME).orElse(null);
        if (current != null && (!current.dimension().equals(level.dimension()) || current.pos().equals(bed))) return false;
        var path = villager.getNavigation().createPath(bed, 1); if (path == null || !path.canReach()) return false;
        var taken = level.getPoiManager().take(holder -> holder.is(PoiTypes.HOME), (holder,pos) -> pos.equals(bed), bed, 1);
        if (taken.isEmpty()) return false;
        if (current != null && level.hasChunkAt(current.pos())) level.getPoiManager().release(current.pos());
        villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bed));
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new net.minecraft.world.entity.ai.memory.WalkTarget(bed, .65F, 1));
        return true;
    }
    private boolean invite(ServerPlayer player, TownRecord town) {
        var metadata = metadata(town.id()); long now = player.serverLevel().getGameTime();
        if (metadata.contains("InviteUntil") && now < metadata.getLong("InviteUntil")) return false;
        var homes = homes(player, town); if (homes.isEmpty()) return false;
        var settlements = SettlementSavedData.get(player.getServer()); int joined = 0;
        var villagers = player.serverLevel().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16),
                v -> v.isAlive() && !v.isBaby() && !v.isTrading() && !v.isSleeping() && !v.isLeashed() && !v.isPassenger()
                        && town.contains(v.blockPosition()) && !settlements.residentAssigned(v.getUUID()) && v.getBrain().getMemory(MemoryModuleType.HOME).isEmpty());
        villagers.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (Villager villager : villagers) {
            if (joined >= 2 || homes.isEmpty()) break;
            BlockPos bed = homes.remove(0); if (!assignHome(player,villager,bed)) continue;
            if (SettlementApi.registerResident(player,town.id(),villager) == SettlementApi.Result.ACCEPTED) joined++;
            else { player.serverLevel().getPoiManager().release(bed); villager.getBrain().eraseMemory(MemoryModuleType.HOME); }
        }
        if (joined == 0) return false;
        metadata.putLong("InviteUntil", now + 72_000); persist(player.getServer()); return true;
    }
    private boolean move(ServerPlayer player, TownRecord town, CompoundTag input) {
        UUID id; try { id = UUID.fromString(input.getString("Id")); } catch (IllegalArgumentException invalid) { return false; }
        var settlements = SettlementSavedData.get(player.getServer()); ResidentMood resident = settlements.resident(id).orElse(null);
        var entity = player.serverLevel().getEntity(id); if (!(entity instanceof Villager villager) || resident == null || !resident.living()
                || !resident.townId().equals(town.id()) || resident.mood() < 50 || !villager.isAlive() || villager.hurtTime > 0 || villager.isTrading() || villager.isSleeping() || player.distanceToSqr(villager) > 256) return false;
        CompoundTag metadata = metadata(town.id()); long day = Math.floorDiv(player.serverLevel().getGameTime(),24_000);
        if (metadata.getLong("MoveDay") != day) { metadata.putLong("MoveDay",day); metadata.putInt("Moves",0); }
        if (metadata.getInt("Moves") >= 2) return false;
        var hit = player.pick(6,0,false); if (!(hit instanceof BlockHitResult block) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
        BlockPos bed = block.getBlockPos(); var state = player.serverLevel().getBlockState(bed);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        if (state.getValue(BedBlock.PART) != BedPart.HEAD) bed = bed.relative(state.getValue(BedBlock.FACING));
        if (!homes(player,town).contains(bed) || !assignHome(player,villager,bed)) return false;
        resident.seen(GlobalPos.of(player.serverLevel().dimension(),bed),null); resident.markAmenitiesDirty(player.serverLevel().getGameTime());
        metadata.putInt("Moves",metadata.getInt("Moves")+1);settlements.changed(); persist(player.getServer()); return true;
    }
    private boolean watch(ServerPlayer player, TownRecord town, CompoundTag input, boolean clear) {
        int index = input.getInt("Value"); if (index < 0 || index > 2) return false;
        CompoundTag watches = metadata(town.id()).getCompound("Watches");
        if (clear) watches.remove(Integer.toString(index));
        else {
            ItemStack held = player.getMainHandItem(); if (held.isEmpty() || held.hasTag()) return false;
            CompoundTag watch = new CompoundTag(); watch.putString("Item", ForgeRegistries.ITEMS.getKey(held.getItem()).toString());
            watch.putInt("Minimum",Math.max(1,Math.min(4096,input.getInt("Quantity")))); watches.put(Integer.toString(index),watch);
        }
        metadata(town.id()).put("Watches",watches); persist(player.getServer()); return true;
    }
    private boolean deliverFood(ServerPlayer player, TownRecord town) {
        ItemStack held = player.getMainHandItem(); if (held.isEmpty()) return false;
        int accepted = 0; var settlements = SettlementSavedData.get(player.getServer());
        for (var resident : settlements.residents(town.id())) {
            if (accepted >= 12 || held.isEmpty()) break;
            var entity = player.serverLevel().getEntity(resident.villagerId());
            if (!(entity instanceof Villager villager) || !villager.isAlive() || villager.hurtTime > 0 || player.distanceToSqr(villager) > 36) continue;
            resident.refreshCraving(); long now = player.serverLevel().getGameTime();
            if (!ResidentFoodService.eligible(villager,resident,held,now,stack -> true)) continue;
            boolean[] consumed = {false};
            var choice = new ResidentFoodService.Food(held.copyWithCount(1), () -> { consumed[0] = true; return held.split(1); },null,-10,0);
            ResidentFoodService.feed(player.serverLevel(),null,villager,resident,now,stack -> true,List.of(choice),
                    stack -> stack.getItem().getFoodProperties(stack,villager).getNutrition(),stack -> {},false);
            if (consumed[0]) {
                accepted++; String receipt = "meal:" + town.id() + ":" + resident.villagerId() + ":" + now;
                MinecraftForge.EVENT_BUS.post(new CivicCommittedEvent(player,1,receipt));
            }
        }
        if (accepted == 0) return false;
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();settlements.changed(); persist(player.getServer()); return true;
    }
    private boolean fund(ServerPlayer player, TownRecord town, CompoundTag input) {
        String mode = input.getString("Mode"), purpose = input.getString("Purpose");
        if (mode.equals("reserve") || mode.equals("return")) return CivicCommerceAdapter.fund(player,town.id(),purpose,input.getLong("Price"),UUID.randomUUID(),mode.equals("return"));
        if (mode.equals("spend")) { UUID listing; try { listing = UUID.fromString(input.getString("Id")); } catch (IllegalArgumentException invalid) { return false; }
            return CivicCommerceAdapter.buyWithFund(player,town.id(),purpose,listing);
        }
        return false;
    }
    public static void enrollment(ServerPlayer player, UUID town, UUID resident) {
        if (!active(player.getServer())) return;
        SandboxCivicRuntime data = get(player.getServer());
        // Entity-level lifetime identity prevents unregister/register XP farming across towns.
        UUID id = UUID.nameUUIDFromBytes(("resident:" + resident).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (data.milestones.containsKey(id)) return;
        CompoundTag receipt = new CompoundTag(); receipt.putUUID("Owner",player.getUUID()); receipt.putInt("Amount",20); receipt.putString("Receipt",id.toString());
        data.milestones.put(id,receipt); data.persist(player.getServer());data.awardMilestone(player,receipt);
    }
    private void awardMilestone(ServerPlayer player,CompoundTag receipt){
        if(receipt.contains("XpHandled"))return;
        MinecraftForge.EVENT_BUS.post(new CivicCommittedEvent(player,receipt.getInt("Amount"),receipt.getString("Receipt"),outcome->{receipt.putString("XpHandled",outcome);persist(player.getServer());}));
    }
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player.getServer())) return;
        var data=get(player.getServer());for (var receipt : data.milestones.values()) if (receipt.getUUID("Owner").equals(player.getUUID())&&!receipt.contains("XpHandled"))data.awardMilestone(player,receipt);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        var server = event.getServer(); if (event.phase != TickEvent.Phase.END || !active(server)) return;
        var data=get(server);if(server.getTickCount()%20==0)data.restoreGuards(server);if(server.getTickCount()%100!=0)return;
        var settlements = SettlementSavedData.get(server); var towns = new ArrayList<>(settlements.towns()); if (towns.isEmpty()) return;
        for (int i = 0; i < Math.min(4,towns.size()); i++) {
            var town = towns.get(Math.floorMod(cursor++,towns.size()));
            var player = server.getPlayerList().getPlayers().stream().filter(p -> SettlementAuthority.principal(p).filter(town.owner()::equals).isPresent()).findFirst().orElse(null);
            if (player == null) continue;
            CompoundTag before=data.metadata(town.id()).copy();int previousTax=town.taxPercent();
            if(!permission.test(player,"taxes")&&town.taxPercent()!=0){town.taxPercent(0);settlements.changed();}
            if(!permission.test(player,"civic_fund"))CivicCommerceAdapter.releaseFunds(player,town.id());
            String guardOrder=data.metadata(town.id()).getString("Order");
            if(ModList.get().isLoaded("recruits")&&!guardOrder.isEmpty()&&!permission.test(player,guardOrder))SandboxRecruitOrders.cancel(player,town,data.metadata(town.id()));
            if(town.dimension().equals(player.serverLevel().dimension().location())){
                if(permission.test(player,"supply_notice"))data.warn(player,town);
                if (ModList.get().isLoaded("recruits")) SandboxRecruitOrders.tick(player,town,data.metadata(town.id()));
                data.improvements(player,town);
            }
            if(previousTax!=town.taxPercent()||!before.equals(data.metadata(town.id())))data.persist(server);
        }
    }
    private void improvements(ServerPlayer player, TownRecord town) {
        if (SettlementSavedData.get(player.getServer()).residents(town.id()).stream().noneMatch(ResidentMood::living)) return;
        String[] names = {"housing","food","safety"}; int[] scores = {town.housingScore(),town.foodScore(),town.safetyScore()};
        CompoundTag metadata = metadata(town.id());
        for (int i=0;i<names.length;i++) {
            String key = "Baseline:" + names[i]; if (!metadata.contains(key)) { metadata.putInt(key,scores[i]); continue; }
            if (scores[i] < 75 || scores[i] <= metadata.getInt(key)) continue;
            UUID id = UUID.nameUUIDFromBytes((town.id()+":"+names[i]).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (milestones.containsKey(id)) continue;
            CompoundTag receipt = new CompoundTag(); receipt.putUUID("Owner",player.getUUID()); receipt.putInt("Amount",10); receipt.putString("Receipt",id.toString());
            milestones.put(id,receipt); persist(player.getServer());awardMilestone(player,receipt);
        }
    }
    private void warn(ServerPlayer player, TownRecord town) {
        CompoundTag metadata = metadata(town.id()), watches = metadata.getCompound("Watches");
        long day = Math.floorDiv(player.serverLevel().getGameTime(),24_000); if (metadata.contains("WarningDay") && metadata.getLong("WarningDay") >= day) return;
        List<String> lacking = new ArrayList<>(); int budget = 4096;
        Map<String,Integer> counts = new LinkedHashMap<>(); for (String key : watches.getAllKeys()) counts.put(watches.getCompound(key).getString("Item"),0);
        boolean incomplete = false;
        for (var link : town.storage()) {
            if (!link.dimension().equals(player.serverLevel().dimension().location())) continue;
            var handler = SharedStorageService.handler(player.serverLevel(),link); if (handler.isEmpty()) { incomplete = true; continue; }
            for (int slot=0;slot<handler.get().getSlots();slot++) {
                if (--budget < 0) { incomplete = true; break; }
                ItemStack stack = handler.get().getStackInSlot(slot); if (stack.isEmpty()) continue;
                String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(); if (counts.containsKey(id)) counts.merge(id,stack.getCount(),Integer::sum);
            }
        }
        if (incomplete) return; // Unknown stock is never reported as a shortage.
        for (String key : watches.getAllKeys()) { var watch = watches.getCompound(key); if (counts.getOrDefault(watch.getString("Item"),0) < watch.getInt("Minimum")) lacking.add(watch.getString("Item")); }
        if (!lacking.isEmpty()) { player.sendSystemMessage(Component.literal(town.name()+": мало припасов — "+String.join(", ",lacking))); metadata.putLong("WarningDay",day); }
    }
    public static CompoundTag snapshot(ServerPlayer player, String node) {
        CompoundTag result = new CompoundTag(); ListTag rows = new ListTag(), townRows = new ListTag(); result.put("Rows",rows); result.put("Towns",townRows);
        if (!active(player.getServer())) return result;
        var settlements = SettlementSavedData.get(player.getServer());
        for (var town : SettlementApi.ownedTowns(player).stream().limit(8).toList()) {
            CompoundTag townRow = row(town.id(),town.name(),"Налог "+town.taxPercent()+"% · Казна "+SettlementCurrency.townBalance(player.getServer(), town.id())+" медных монет"); townRows.add(townRow);
            if (node.equals("treasury_audit")) for (var tag : SettlementCurrency.history(player.getServer(), town.id())) {
                if (rows.size() >= 64) break;
                CompoundTag receipt = (CompoundTag) tag; boolean incoming = receipt.getBoolean("Incoming");
                CompoundTag row = row(receipt.getUUID("Receipt"), (incoming ? "Приход: " : "Расход: ") + receipt.getLong("Amount") + " медных монет",
                        accountName(receipt.getUUID("Source"), town) + " → " + accountName(receipt.getUUID("Destination"), town));
                row.putString("Town", town.id().toString()); rows.add(row);
            }
            if (node.equals("civic_fund")) for (var fund : CivicCommerceAdapter.fundRows(player,town.id())) { if(rows.size()>=64)break;fund.putString("Town",town.id().toString()); rows.add(fund); }
            if (node.equals("resident_move") || node.equals("invite_residents")) for (var resident : settlements.residents(town.id())) {
                if (rows.size()>=48 || !resident.living()) break;
                var entity = player.serverLevel().getEntity(resident.villagerId()); if (entity == null) continue;
                CompoundTag row = row(resident.villagerId(),entity.getName().getString(),"Настроение "+resident.mood()+" · Сытость "+resident.satiety()); row.putString("Town",town.id().toString()); rows.add(row);
            }
            if (java.util.Set.of("rally","patrol_route","captain_order").contains(node) && ModList.get().isLoaded("recruits")) SandboxRecruitOrders.rows(player,town,rows);
        }
        if(node.equals("civic_fund")) {
            ListTag offers=CivicCommerceAdapter.snapshot(player,"trading_post").getList("Rows",Tag.TAG_COMPOUND);
            for(int i=0;i<offers.size()&&rows.size()<64;i++)if(offers.getCompound(i).getString("Kind").equals("trading_post")&&!offers.getCompound(i).getBoolean("Own"))rows.add(offers.getCompound(i).copy());
        }
        return result;
    }
    private static String accountName(UUID account,TownRecord town){
        if(account.equals(SettlementCurrency.townAccount(town.id())))return "Городская казна";
        if(account.equals(com.siirio.jemsettlements.currency.SettlementAccounts.NPC_MARKET.id()))return "Торговля с жителями";
        if(account.equals(com.siirio.jemsettlements.currency.SettlementAccounts.MARKET_ESCROW.id()))return "Оплата покупателя";
        if(account.equals(town.owner()))return "Владелец города";
        return "Счёт "+account.toString().substring(0,8);
    }
    static CompoundTag row(UUID id, String name, String detail) {
        CompoundTag result = new CompoundTag(); result.putString("Id",id.toString()); result.putString("Name",name.substring(0,Math.min(96,name.length()))); result.putString("Detail",detail.substring(0,Math.min(160,detail.length()))); return result;
    }
    @Override public CompoundTag save(CompoundTag root) {
        CompoundTag townRows = new CompoundTag(), receiptRows = new CompoundTag(); towns.forEach((id,t) -> townRows.put(id.toString(),t)); milestones.forEach((id,t) -> receiptRows.put(id.toString(),t));
        root.put("Towns",townRows); root.put("Milestones",receiptRows);CompoundTag restorations=new CompoundTag();guardRestorations.forEach((id,t)->restorations.put(id.toString(),t));root.put("GuardRestorations",restorations); return root;
    }
    private static SandboxCivicRuntime load(CompoundTag root) {
        var data = new SandboxCivicRuntime(); var townRows = root.getCompound("Towns"); var receiptRows = root.getCompound("Milestones");
        for (String id : townRows.getAllKeys()) data.towns.put(UUID.fromString(id),townRows.getCompound(id));
        for (String id : receiptRows.getAllKeys()) data.milestones.put(UUID.fromString(id),receiptRows.getCompound(id));
        var restorations=root.getCompound("GuardRestorations");for(String id:restorations.getAllKeys())data.guardRestorations.put(UUID.fromString(id),restorations.getCompound(id));return data;
    }
}
