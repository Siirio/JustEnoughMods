package com.siirio.jemeconomy.market;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.*;
import com.siirio.jemeconomy.policy.MarketSupply;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Durable funded orders into existing owner-authorized settlement storage. No money is stored here.
 * A process failure leaves an explicit quarantined obligation; no guessed replay of external inventories. */
public final class MarketBulkOrders extends SavedData {
    private static final String NAME="jem_market_orders";
    private final Map<String,CompoundTag> orders=new LinkedHashMap<>();
    private record Destination(String key, BlockPos position, IItemHandlerModifiable inventory) {}
    private record Placement(Destination destination,int slot,ItemStack before,ItemStack after) {}
    private MarketBulkOrders() {}
    public static MarketBulkOrders get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketBulkOrders::load,MarketBulkOrders::new,NAME);
    }
    private static EconomyApi.MarketPurchaseResult result(EconomyApi.MarketPurchaseStatus status,long price,int count) {
        return new EconomyApi.MarketPurchaseResult(status,price,count);
    }
    public synchronized EconomyApi.MarketPurchaseResult purchase(ServerPlayer player,MarketSupply.Listing listing,
                                                                int quantity,long revision,UUID request) {
        var principal=IdentityPolicy.resolve(player);
        if(principal.isEmpty())return result(EconomyApi.MarketPurchaseStatus.UNAUTHENTICATED,0,0);
        if(!Set.of(64,256,1024,4096).contains(quantity)||request==null||!listing.policy().buyable()||listing.policy().bulk1024()==null)
            return result(EconomyApi.MarketPurchaseStatus.INVALID,0,0);
        String key="bulk:"+principal.get().accountId()+":"+request;
        var old=orders.get(key);
        if(old!=null) {
            if(!old.getString("Item").equals(listing.id().toString())||old.getInt("Quantity")!=quantity)
                return result(EconomyApi.MarketPurchaseStatus.INVALID,0,0);
            if(old.getString("Phase").equals("DONE"))return result(EconomyApi.MarketPurchaseStatus.REPLAYED,old.getLong("Price"),quantity);
            if(old.getString("Phase").equals("CANCELLED")) {
                // A rejected request stays rejected; its replay must not become a pending account obligation.
                var failure = old.getString("Failure").equals("INSUFFICIENT_FUNDS")
                        ? EconomyApi.MarketPurchaseStatus.INSUFFICIENT_FUNDS : EconomyApi.MarketPurchaseStatus.UNAVAILABLE;
                return result(failure,0,0);
            }
            old.putString("Phase","QUARANTINED");setDirty();persist(player.getServer(),this,NAME);
            return result(EconomyApi.MarketPurchaseStatus.QUARANTINED,0,0);
        }
        if(orders.size()>=50000||orders.values().stream().anyMatch(row->row.getUUID("Account").equals(principal.get().accountId())
                &&!Set.of("DONE","CANCELLED").contains(row.getString("Phase"))))
            return result(EconomyApi.MarketPurchaseStatus.QUARANTINED,0,0);
        var market=MarketSavedData.get(player.getServer());
        if(market.revision(player.getServer())!=revision)return result(EconomyApi.MarketPurchaseStatus.STALE,0,0);
        if(market.purchaseRemaining(player.getServer(),principal.get().accountId(),listing.policy())<quantity)
            return result(EconomyApi.MarketPurchaseStatus.UNAVAILABLE,0,0);
        List<Placement> plan=new ArrayList<>();int remaining=quantity;
        for(var destination:destinations(player)) {
            var inventory=destination.inventory;
            for(int slot=0;slot<inventory.getSlots()&&remaining>0;slot++) {
                ItemStack before=inventory.getStackInSlot(slot).copy();
                if(!before.isEmpty()&&!ItemStack.isSameItemSameTags(before,listing.stack()))continue;
                int capacity=Math.min(listing.stack().getMaxStackSize(),inventory.getSlotLimit(slot));
                int take=Math.min(remaining,capacity-before.getCount());
                if(take<=0||!inventory.isItemValid(slot,listing.stack()))continue;
                ItemStack probe=listing.stack().copyWithCount(take);
                if(!inventory.insertItem(slot,probe,true).isEmpty())continue;
                ItemStack after=listing.stack().copyWithCount(before.getCount()+take);
                plan.add(new Placement(destination,slot,before,after));remaining-=take;
            }
        }
        if(remaining>0)return result(EconomyApi.MarketPurchaseStatus.NO_SPACE,0,0);
        long price=listing.policy().purchaseTotal(quantity);
        CompoundTag order=new CompoundTag();order.putUUID("Account",principal.get().accountId());order.putUUID("Player",player.getUUID());
        order.putString("Item",listing.id().toString());order.putInt("Quantity",quantity);order.putLong("Price",price);order.putString("Phase","PREPARED");
        ListTag slots=new ListTag();for(var placement:plan) {
            CompoundTag row=new CompoundTag();row.putString("Destination",placement.destination.key);row.putInt("Slot",placement.slot);
            row.put("Before",placement.before.save(new CompoundTag()));row.put("After",placement.after.save(new CompoundTag()));slots.add(row);
        }
        order.put("Slots",slots);orders.put(key,order);setDirty();persist(player.getServer(),this,NAME);
        var ledger=EconomySavedData.get(player.getServer());
        var funding=ledger.transfer(principal.get().accountId(),SystemAccount.MARKET_ESCROW.id(),price,key+":fund");
        if(funding.status()!=TransferResult.Status.COMMITTED) {
            order.putString("Phase","CANCELLED");
            order.putString("Failure", funding.status()==TransferResult.Status.INSUFFICIENT_FUNDS ? "INSUFFICIENT_FUNDS" : "UNAVAILABLE");
            setDirty();persist(player.getServer(),this,NAME);
            return result(funding.status()==TransferResult.Status.INSUFFICIENT_FUNDS?EconomyApi.MarketPurchaseStatus.INSUFFICIENT_FUNDS:EconomyApi.MarketPurchaseStatus.UNAVAILABLE,0,0);
        }
        persist(player.getServer(),ledger,"jem_economy");order.putString("Phase","FUNDED");setDirty();persist(player.getServer(),this,NAME);
        // No callbacks between this access/capacity preflight and direct native slot mutation.
        for(var placement:plan) {
            if(!ItemStack.matches(placement.before,placement.destination.inventory.getStackInSlot(placement.slot))) {
                order.putString("Phase","QUARANTINED");setDirty();persist(player.getServer(),this,NAME);
                return result(EconomyApi.MarketPurchaseStatus.QUARANTINED,0,0);
            }
        }
        for(var placement:plan) {
            placement.destination.inventory.setStackInSlot(placement.slot,placement.after.copy());
            var entity=player.serverLevel().getBlockEntity(placement.destination.position);if(entity!=null)entity.setChanged();
        }
        // Commit native chunk storage before declaring delivery. Failure retains FUNDED evidence for quarantine.
        player.getServer().saveEverything(false,true,true);
        market.recordPurchase(player.getServer(),principal.get().accountId(),listing.policy(),quantity);
        persist(player.getServer(),market,"jem_economy_market");
        var settled=ledger.transfer(SystemAccount.MARKET_ESCROW.id(),SystemAccount.NPC_MARKET.id(),price,key+":settle");
        if(settled.status()!=TransferResult.Status.COMMITTED)throw new IllegalStateException("Funded bulk delivery requires reconciliation");
        persist(player.getServer(),ledger,"jem_economy");
        order.putString("Phase","DONE");setDirty();persist(player.getServer(),this,NAME);
        return result(EconomyApi.MarketPurchaseStatus.COMMITTED,price,quantity);
    }
    /** Exact installed settlement API; no access inferred from block proximity or a client-supplied coordinate. */
    private static List<Destination> destinations(ServerPlayer player) {
        List<Destination> personal = new ArrayList<>();
        personal.add(new Destination("player:"+player.getUUID(), player.blockPosition(), new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory())));
        if(!net.minecraftforge.fml.ModList.get().isLoaded("jem_settlements"))return personal;
        try {
            var api=Class.forName("com.siirio.jemsettlements.api.SettlementApi");
            var service=Class.forName("com.siirio.jemsettlements.runtime.SharedStorageService");
            var linkClass=Class.forName("com.siirio.jemsettlements.town.StorageLink");
            List<?> towns=(List<?>)api.getMethod("ownedTowns",ServerPlayer.class).invoke(null,player);
            List<Destination> result=new ArrayList<>(personal);Set<Long> positions=new HashSet<>();
            for(var town:towns) for(var link:(List<?>)town.getClass().getMethod("storage").invoke(town)) {
                var dimension=(net.minecraft.resources.ResourceLocation)linkClass.getMethod("dimension").invoke(link);
                var position=(BlockPos)linkClass.getMethod("position").invoke(link);
                var second=(BlockPos)linkClass.getMethod("second").invoke(link);
                if(!dimension.equals(player.serverLevel().dimension().location())||!player.serverLevel().hasChunkAt(position)
                        ||player.distanceToSqr(position.getX()+.5,position.getY()+.5,position.getZ()+.5)>128*128
                        ||positions.contains(position.asLong())||second!=null&&positions.contains(second.asLong()))continue;
                Optional<?> resolved=(Optional<?>)service.getMethod("handler",net.minecraft.server.level.ServerLevel.class,linkClass).invoke(null,player.serverLevel(),link);
                if(resolved.isEmpty()||!(resolved.get() instanceof IItemHandlerModifiable inventory)||inventory.getSlots()>4096)continue;
                positions.add(position.asLong());if(second!=null)positions.add(second.asLong());
                result.add(new Destination(dimension+"@"+position.asLong(),position,inventory));
            }
            result.sort(Comparator.comparing((Destination value) -> !value.key.startsWith("player:")).thenComparing(Destination::key));return result;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Installed settlement storage API changed",failure);}
    }
    /** Same native NBT format and atomic replacement as the existing financial durability owner. */
    private static void persist(MinecraftServer server,SavedData data,String name) {
        var path=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data");
        var target=path.resolve(name+".dat");var temporary=path.resolve(name+".bulk.tmp");
        CompoundTag root=new CompoundTag();root.put("data",data.save(new CompoundTag()));NbtUtils.addCurrentDataVersion(root);
        try {
            java.nio.file.Files.createDirectories(path);NbtIo.writeCompressed(root,temporary.toFile());
            try(var channel=java.nio.channels.FileChannel.open(temporary,java.nio.file.StandardOpenOption.WRITE)){channel.force(true);}
            if (net.minecraft.Util.getPlatform() == net.minecraft.Util.OS.WINDOWS) {
                // Same native replacement contract used by the installed tax durability owner.
                // JDK atomic move ignores REPLACE_EXISTING on Windows; never delete the committed file first.
                var windows = com.sun.jna.platform.win32.Kernel32.INSTANCE;
                int flags = com.sun.jna.platform.win32.WinBase.MOVEFILE_REPLACE_EXISTING
                        | com.sun.jna.platform.win32.WinBase.MOVEFILE_WRITE_THROUGH;
                if (!windows.MoveFileEx(temporary.toAbsolutePath().toString(), target.toAbsolutePath().toString(),
                        new com.sun.jna.platform.win32.WinDef.DWORD(flags)))
                    throw new java.io.IOException("Native bulk journal replacement failed: " + windows.GetLastError());
            } else {
                java.nio.file.Files.move(temporary,target,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            if (!root.equals(NbtIo.readCompressed(target.toFile()))) throw new java.io.IOException("Bulk journal save mismatch: " + name);
            data.setDirty(false);
        }catch(java.io.IOException failure){data.setDirty();throw new IllegalStateException("Bulk journal durability failed; retain pending operation",failure);}
    }
    @Override public synchronized CompoundTag save(CompoundTag tag) {
        CompoundTag rows=new CompoundTag();orders.forEach(rows::put);tag.put("Orders",rows);return tag;
    }
    private static MarketBulkOrders load(CompoundTag tag) {
        var data=new MarketBulkOrders();var rows=tag.getCompound("Orders");
        for(String key:rows.getAllKeys()) {
            var row=rows.getCompound(key);String phase=row.getString("Phase");
            if(!Set.of("DONE","CANCELLED").contains(phase))row.putString("Phase","QUARANTINED");
            data.orders.put(key,row);
        }
        return data;
    }
}
