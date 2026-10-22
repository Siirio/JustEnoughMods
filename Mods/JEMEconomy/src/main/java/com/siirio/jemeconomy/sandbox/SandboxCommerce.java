package com.siirio.jemeconomy.sandbox;

import com.siirio.jemeconomy.api.EconomyApi;
import com.siirio.jemeconomy.ledger.EconomySavedData;
import com.siirio.jemeconomy.ledger.TransferResult;
import com.siirio.jemidentity.api.IdentityPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

/** Sandbox-only workflow owner. Balances remain exclusively in EconomySavedData.
 * Every inventory mutation is a durable before/after journal. An ambiguous recovery
 * stops that operation; it never guesses whether goods have already been delivered. */
public final class SandboxCommerce extends SavedData {
    private final Map<UUID, CompoundTag> listings = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> claims = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> inventory = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> signs = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> funds = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> residentIncome = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> terminalListings = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> deliveredClaims = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> settledResidentIncome = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> nativePurchases = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> completedNativePurchases = new LinkedHashMap<>();
    private static int listingCursor;
    private static boolean registered;
    private static java.util.function.BiPredicate<ServerPlayer,String> permission=(player,node)->false;
    private static final int MAX_LISTINGS = 2048, MAX_CLAIMS = 8192, MAX_JOURNALS = 32768;

    public static boolean active(MinecraftServer server) {
        return com.siirio.jemeconomy.api.EconomyApi.isEnabled(server) && !server.isDedicatedServer() && ModList.get().isLoaded("jem_professions_sandbox")
                && ModList.get().isLoaded("jem_economy");
    }
    public static void register() {
        if (!registered) { registered = true; MinecraftForge.EVENT_BUS.register(SandboxCommerce.class); }
    }
    public static void permission(java.util.function.BiPredicate<ServerPlayer,String> value){permission=value;}
    public static SandboxCommerce get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SandboxCommerce::load, SandboxCommerce::new, "jem_sandbox_commerce");
    }
    private static UUID account(ServerPlayer player) { return IdentityPolicy.resolve(player).map(p -> p.accountId()).orElse(null); }
    private static UUID named(String text) { return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)); }
    private static UUID vault(UUID listing) { return named("jem:commerce:vault:" + listing); }
    private void persist(MinecraftServer server) {
        retireCompleted();
        setDirty();SandboxDurability.save(server,EconomySavedData.get(server),"jem_economy");SandboxDurability.save(server,this,"jem_sandbox_commerce");
    }
    private void retireCompleted(){
        var listingRows=listings.entrySet().iterator();
        while(listingRows.hasNext()){var entry=listingRows.next();if(unresolved(entry.getValue()))continue;
            entry.getValue().remove("Item");terminalListings.put(entry.getKey(),entry.getValue());listingRows.remove();}
        var claimRows=claims.entrySet().iterator();
        while(claimRows.hasNext()){var entry=claimRows.next();if(!entry.getValue().getBoolean("Done"))continue;
            entry.getValue().remove("Item");deliveredClaims.put(entry.getKey(),entry.getValue());claimRows.remove();}
        var incomeRows=residentIncome.entrySet().iterator();
        while(incomeRows.hasNext()){var entry=incomeRows.next();if(!entry.getValue().getBoolean("Committed")&&!entry.getValue().getBoolean("Reversed")&&!entry.getValue().getBoolean("Cancelled"))continue;
            settledResidentIncome.put(entry.getKey(),entry.getValue());incomeRows.remove();}
        var nativeRows=nativePurchases.entrySet().iterator();
        while(nativeRows.hasNext()){var entry=nativeRows.next();String state=entry.getValue().getString("State");if(!state.equals("DELIVERED")&&!state.equals("ABORTED"))continue;
            entry.getValue().remove("Item");completedNativePurchases.put(entry.getKey(),entry.getValue());nativeRows.remove();}
    }
    private static boolean transferred(TransferResult result) {
        return result.status() == TransferResult.Status.COMMITTED || result.status() == TransferResult.Status.REPLAYED;
    }
    private static boolean safe(ItemStack stack) {
        return !stack.isEmpty() && !stack.hasTag() && !stack.isDamageableItem()
                && !com.siirio.jemeconomy.policy.TradeRestrictions.marketProhibited(stack);
    }
    private static boolean safeStock(ItemStack stack){
        return !stack.isEmpty()&&!com.siirio.jemeconomy.policy.TradeRestrictions.marketProhibited(stack)
                &&!stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()
                &&!stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()
                &&(!stack.hasTag()||!stack.getTag().contains("BlockEntityTag")&&!stack.getTag().contains("EntityTag"))
                &&stack.save(new CompoundTag()).getAsString().length()<=8192;
    }
    private static boolean fingerprint(ItemStack a,ItemStack b){
        return !a.isEmpty()&&!b.isEmpty()&&a.getItem()==b.getItem()&&a.copyWithCount(1).save(new CompoundTag()).equals(b.copyWithCount(1).save(new CompoundTag()));
    }
    private static boolean same(ItemStack a, ItemStack b) {
        return a.isEmpty() ? b.isEmpty() : !b.isEmpty() && a.getCount() == b.getCount() && fingerprint(a,b);
    }
    private static CompoundTag row(UUID id, String name, String detail) {
        CompoundTag row = new CompoundTag(); row.putString("Id", id.toString());
        row.putString("Name", name.substring(0, Math.min(96, name.length())));
        row.putString("Detail", detail.substring(0, Math.min(160, detail.length()))); return row;
    }

    public static boolean action(ServerPlayer player, String kind, CompoundTag input) {
        if (!active(player.getServer()) || account(player) == null || player.isCreative() || player.isSpectator()) return false;
        SandboxCommerce data = get(player.getServer());
        data.recover(player);
        // Unresolved inventory transitions block new work instead of allowing overlapping snapshots.
        if (data.inventory.values().stream().anyMatch(t -> t.getUUID("Player").equals(player.getUUID()) && !t.getBoolean("Done"))) return false;
        String mode = input.getString("Mode");
        if (mode.equals("create")) return data.create(player, kind, input);
        UUID id;
        try { id = UUID.fromString(input.getString("Id")); } catch (IllegalArgumentException invalid) { return false; }
        if (mode.equals("claim")) return data.claim(player, id);
        CompoundTag listing = data.listings.get(id);
        if (listing == null || !listing.getString("State").equals("OPEN")) return false;
        if (listing.getLong("Expiry") <= System.currentTimeMillis() && !listing.getString("Kind").equals("trading_post")) {
            data.close(player.getServer(), listing, false); return false;
        }
        return switch (mode) {
            case "cancel" -> listing.getUUID("Owner").equals(account(player)) && data.close(player.getServer(), listing, true);
            case "fulfill" -> data.fulfill(player, listing);
            case "bid" -> data.bid(player, listing, input.getLong("Price"));
            case "buy" -> data.buy(player, listing);
            case "advertise" -> data.advertise(player, listing);
            case "sign" -> data.sign(player, listing);
            default -> false;
        };
    }
    public static boolean recovery(ServerPlayer player,CompoundTag input){
        return java.util.Set.of("claim","cancel").contains(input.getString("Mode"))&&action(player,"recovery",input);
    }

    private boolean create(ServerPlayer player, String kind, CompoundTag input) {
        if (!java.util.Set.of("buy_orders", "auction", "commission_escrow", "cargo_contract", "trading_post").contains(kind)) return false;
        if (listings.size() >= MAX_LISTINGS || pendingClaims()+4 >= MAX_CLAIMS || inventory.size() >= MAX_JOURNALS) return false;
        UUID owner = account(player);
        long count = listings.values().stream().filter(t -> t.getUUID("Owner").equals(owner) && unresolved(t) && t.getString("Kind").equals(kind)).count();
        if (count >= (kind.equals("buy_orders") ? 3 : 8)) return false;
        ItemStack held = player.getMainHandItem(); int quantity = input.getInt("Quantity"), hours = input.getInt("Hours"); long price = input.getLong("Price");
        boolean stocked=kind.equals("auction")||kind.equals("trading_post");
        if (!(stocked?safeStock(held):safe(held)) || quantity < 1 || quantity > Math.min(64, held.getMaxStackSize()) || price < 1 || price > 1_000_000L) return false;
        if (hours < 1 || hours > (kind.equals("auction") ? 24 : 72)) return false;
        UUID id = UUID.randomUUID(); CompoundTag listing = new CompoundTag();
        listing.putUUID("Id", id); listing.putUUID("Owner", owner); listing.putUUID("Creator", player.getUUID());
        listing.putString("Kind", kind); listing.putString("State", "PREPARED"); listing.putLong("Price", price);
        listing.putLong("Expiry", System.currentTimeMillis() + hours * 3_600_000L); listing.putInt("Quantity", quantity);
        listing.put("Item", held.copyWithCount(quantity).save(new CompoundTag()));
        if (kind.equals("cargo_contract")) {
            var hit = player.pick(6, 0, false); if (!(hit instanceof BlockHitResult block) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
            listing.putLong("Destination", block.getBlockPos().asLong()); listing.putString("Dimension", player.serverLevel().dimension().location().toString());
        }
        listings.put(id, listing); persist(player.getServer());
        if (kind.equals("buy_orders") || kind.equals("commission_escrow") || kind.equals("cargo_contract")) {
            // Exact payment is held in a unique ledger account, never in this saved data.
            var result = EconomyApi.depositToAccount(player, vault(id), price, id);
            if (!transferred(result)) { listing.putString("State", "CANCELLED"); persist(player.getServer()); return false; }
            listing.putString("State", "OPEN"); persist(player.getServer()); return true;
        }
        listing.putString("Pending", "CREATE"); persist(player.getServer());
        if (!remove(player, id, ItemStack.of(listing.getCompound("Item")), "create")) {
            listing.putString("State", "CANCELLED"); listing.remove("Pending"); persist(player.getServer()); return false;
        }
        listing.putString("State", "OPEN"); listing.remove("Pending");inventory.remove(named(id+":create")); persist(player.getServer()); return true;
    }

    private boolean fulfill(ServerPlayer player, CompoundTag listing) {
        if (!java.util.Set.of("buy_orders", "commission_escrow", "cargo_contract").contains(listing.getString("Kind"))
                || listing.getUUID("Owner").equals(account(player)) || pendingClaims()+2 >= MAX_CLAIMS) return false;
        if (listing.getString("Kind").equals("cargo_contract")) {
            BlockPos destination = BlockPos.of(listing.getLong("Destination"));
            if (!listing.getString("Dimension").equals(player.serverLevel().dimension().location().toString()) || !player.serverLevel().hasChunkAt(destination)
                    || player.distanceToSqr(destination.getX()+.5, destination.getY()+.5, destination.getZ()+.5) > 36) return false;
        }
        UUID id = listing.getUUID("Id");
        listing.putUUID("Seller", account(player)); listing.putUUID("SellerPlayer", player.getUUID()); listing.putString("Pending", "FULFILL");
        listing.putString("State", "SETTLING"); persist(player.getServer());
        if (!remove(player, id, ItemStack.of(listing.getCompound("Item")), "fulfill")) {
            listing.putString("State", "OPEN"); listing.remove("Pending"); persist(player.getServer()); return false;
        }
        return finishFulfill(player.getServer(), listing);
    }
    private boolean finishFulfill(MinecraftServer server, CompoundTag listing) {
        UUID id = listing.getUUID("Id");
        if (!transferred(EconomySavedData.get(server).transfer(vault(id), listing.getUUID("Seller"), listing.getLong("Price"), "commerce:pay:" + id))) return false;
        addClaim(server, listing.getUUID("Owner"), ItemStack.of(listing.getCompound("Item")), named(id + ":goods"));
        listing.putString("State", "SOLD"); listing.remove("Pending");inventory.remove(named(id+":fulfill")); persist(server); award(server, listing); return true;
    }
    private boolean bid(ServerPlayer player, CompoundTag listing, long amount) {
        if (!listing.getString("Kind").equals("auction") || listing.getUUID("Owner").equals(account(player)) || amount <= listing.getLong("Bid")
                || amount < listing.getLong("Price") || amount > 1_000_000L) return false;
        // Fund the bidder's full new reservation before returning an old reservation.
        UUID operation = UUID.randomUUID(); boolean sameBidder = listing.hasUUID("Bidder") && listing.getUUID("Bidder").equals(account(player));
        listing.putBoolean("SameBidder",sameBidder);
        listing.putUUID("BidOperation", operation); listing.putUUID("NextBidder", account(player));
        listing.putLong("NextBid", amount); listing.putString("Pending", "BID"); persist(player.getServer());
        UUID destination = sameBidder ? vault(listing.getUUID("BidVault")) : vault(operation);
        if (!transferred(EconomyApi.depositToAccount(player, destination, sameBidder ? amount-listing.getLong("Bid") : amount, operation))) {
            listing.remove("Pending"); persist(player.getServer()); return false;
        }
        return finishBid(player.getServer(), listing);
    }
    private boolean finishBid(MinecraftServer server, CompoundTag listing) {
        if (!listing.getBoolean("SameBidder") && listing.hasUUID("Bidder") && !transferred(EconomySavedData.get(server).transfer(vault(listing.getUUID("BidVault")),
                listing.getUUID("Bidder"), listing.getLong("Bid"), "commerce:outbid:" + listing.getUUID("BidOperation")))) return false;
        listing.putUUID("Bidder", listing.getUUID("NextBidder")); if (!listing.getBoolean("SameBidder")) listing.putUUID("BidVault", listing.getUUID("BidOperation"));
        listing.putLong("Bid", listing.getLong("NextBid")); listing.remove("Pending"); persist(server); return true;
    }
    private boolean buy(ServerPlayer player, CompoundTag listing) {
        if (!listing.getString("Kind").equals("trading_post") || listing.getUUID("Owner").equals(account(player)) || pendingClaims()+2 >= MAX_CLAIMS) return false;
        UUID id = listing.getUUID("Id"); listing.putUUID("Buyer", account(player)); listing.putString("Pending", "BUY"); listing.putString("State", "SETTLING");
        listing.putString("FundingReceipt","accountDeposit:"+named(id + ":buy:" + account(player))); persist(player.getServer());
        if (!transferred(EconomyApi.depositToAccount(player, vault(id), listing.getLong("Price"), named(id + ":buy:" + account(player))))) {
            listing.putString("State", "OPEN"); listing.remove("Pending"); persist(player.getServer()); return false;
        }
        return finishBuy(player.getServer(), listing);
    }
    private boolean finishBuy(MinecraftServer server, CompoundTag listing) {
        UUID id = listing.getUUID("Id");
        if (!transferred(EconomySavedData.get(server).transfer(vault(id), listing.getUUID("Owner"), listing.getLong("Price"), "commerce:shop:" + id))) return false;
        addClaim(server, listing.getUUID("Buyer"), ItemStack.of(listing.getCompound("Item")), named(id + ":shopgoods"));
        listing.putUUID("Seller", listing.getUUID("Owner")); listing.putUUID("SellerPlayer", listing.getUUID("Creator"));
        listing.putString("State", "SOLD"); listing.remove("Pending"); persist(server); award(server, listing); return true;
    }

    private boolean close(MinecraftServer server, CompoundTag listing, boolean cancel) {
        if (!listing.getString("Pending").isEmpty()) return false;
        if (cancel && listing.getString("Kind").equals("auction") && listing.hasUUID("Bidder")) return false;
        UUID id = listing.getUUID("Id"); String kind = listing.getString("Kind");
        if (kind.equals("auction") && listing.hasUUID("Bidder")) {
            if (!transferred(EconomySavedData.get(server).transfer(vault(listing.getUUID("BidVault")), listing.getUUID("Owner"), listing.getLong("Bid"), "commerce:auction:" + id))) return false;
            addClaim(server, listing.getUUID("Bidder"), ItemStack.of(listing.getCompound("Item")), named(id + ":auctiongoods"));
            listing.putUUID("Seller",listing.getUUID("Owner"));listing.putUUID("SellerPlayer",listing.getUUID("Creator"));listing.putUUID("Buyer",listing.getUUID("Bidder"));
        } else if (kind.equals("auction") || kind.equals("trading_post")) {
            addClaim(server, listing.getUUID("Owner"), ItemStack.of(listing.getCompound("Item")), named(id + ":returned"));
        } else if (!transferred(EconomySavedData.get(server).transfer(vault(id), listing.getUUID("Owner"), listing.getLong("Price"), "commerce:return:" + id))) return false;
        listing.putString("State", kind.equals("auction")&&listing.hasUUID("Bidder")?"SOLD":"CLOSED"); persist(server);
        if(listing.getString("State").equals("SOLD"))award(server,listing);return true;
    }

    private void addClaim(MinecraftServer server, UUID owner, ItemStack stack, UUID id) {
        if (claims.containsKey(id)||deliveredClaims.containsKey(id)) return;
        CompoundTag claim = new CompoundTag(); claim.putUUID("Id", id); claim.putUUID("Owner", owner); claim.put("Item", stack.save(new CompoundTag()));
        claims.put(id, claim); persist(server);
    }
    private boolean claim(ServerPlayer player, UUID id) {
        CompoundTag claim = claims.get(id); if (claim == null || claim.getBoolean("Done") || !claim.getUUID("Owner").equals(account(player))) return false;
        ItemStack stack = ItemStack.of(claim.getCompound("Item"));
        ListTag patches = new ListTag(); int remaining = stack.getCount();
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack current = player.getInventory().getItem(slot);
            if (!current.isEmpty() && !fingerprint(current, stack)) continue;
            int moved = Math.min(remaining, stack.getMaxStackSize() - current.getCount()); if (moved <= 0) continue;
            ItemStack after = stack.copyWithCount(current.getCount() + moved); patches.add(patch(slot, current, after)); remaining -= moved;
        }
        if (remaining > 0) return false;
        UUID journal = named(id + ":claim"); if (!apply(player, journal, id, "claim", patches)) return false;
        claim.putBoolean("Done", true);claim.remove("Item");inventory.remove(journal); persist(player.getServer()); return true;
    }
    private boolean remove(ServerPlayer player, UUID listing, ItemStack stack, String purpose) {
        ListTag patches = new ListTag(); int remaining = stack.getCount();
        for (int slot = 0; slot < 36 && remaining > 0; slot++) {
            ItemStack current = player.getInventory().getItem(slot); if (!fingerprint(current, stack)) continue;
            int taken = Math.min(remaining, current.getCount()); ItemStack after = current.copy(); after.shrink(taken);
            patches.add(patch(slot, current, after)); remaining -= taken;
        }
        return remaining == 0 && apply(player, named(listing + ":" + purpose), listing, purpose, patches);
    }
    private static CompoundTag patch(int slot, ItemStack before, ItemStack after) {
        CompoundTag patch = new CompoundTag(); patch.putInt("Slot", slot); patch.put("Before", before.save(new CompoundTag())); patch.put("After", after.save(new CompoundTag())); return patch;
    }
    private boolean apply(ServerPlayer player, UUID id, UUID parent, String purpose, ListTag patches) {
        if (inventory.size() >= MAX_JOURNALS) return false;
        CompoundTag journal = inventory.get(id);
        if (journal == null) {
            journal = new CompoundTag(); journal.putUUID("Id", id); journal.putUUID("Player", player.getUUID()); journal.putUUID("Parent", parent);
            journal.putString("Purpose", purpose); journal.put("Patches", patches); inventory.put(id, journal); persist(player.getServer());
        }
        return reconcile(player, journal);
    }
    private boolean reconcile(ServerPlayer player, CompoundTag journal) {
        if (journal.getBoolean("Done")) return true;
        ListTag patches = journal.getList("Patches", Tag.TAG_COMPOUND); boolean before = true, after = true;
        for (int i = 0; i < patches.size(); i++) {
            CompoundTag patch = patches.getCompound(i); ItemStack current = player.getInventory().getItem(patch.getInt("Slot"));
            before &= same(current, ItemStack.of(patch.getCompound("Before"))); after &= same(current, ItemStack.of(patch.getCompound("After")));
        }
        if (!before && !after) { journal.putBoolean("Quarantined", true); persist(player.getServer()); return false; }
        if (before) for (int i = 0; i < patches.size(); i++) {
            CompoundTag patch = patches.getCompound(i); player.getInventory().setItem(patch.getInt("Slot"), ItemStack.of(patch.getCompound("After")));
        }
        player.getInventory().setChanged(); persistPlayer(player);
        journal.putBoolean("Done", true); persist(player.getServer()); player.containerMenu.broadcastChanges(); return true;
    }
    public static void persistPlayer(ServerPlayer player) {
        // Integrated owner login prefers level.dat's Player tag over playerdata.
        // Save this player, then the small level.dat owner record; no chunks or other players.
        CompoundTag expected = player.saveWithoutId(new CompoundTag());
        ((com.siirio.jemeconomy.mixin.CommercePlayerSaveAccess)player.getServer().getPlayerList()).jem$commerceSave(player);
        if(player.getServer().isSingleplayerOwner(player.getGameProfile()))
            ((com.siirio.jemeconomy.mixin.CommerceServerStorageAccess)player.getServer()).jem$commerceStorage()
                    .saveDataTag(player.getServer().registryAccess(),player.getServer().getWorldData(),player.getServer().getPlayerList().getSingleplayerData());
        java.io.File file = player.getServer().isSingleplayerOwner(player.getGameProfile())
                ? player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("level.dat").toFile()
                : player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(player.getUUID() + ".dat").toFile();
        try {
            CompoundTag actual = net.minecraft.nbt.NbtIo.readCompressed(file);
            if (player.getServer().isSingleplayerOwner(player.getGameProfile())) actual = actual.getCompound("Data").getCompound("Player");
            if (!actual.getList("Inventory", Tag.TAG_COMPOUND).equals(expected.getList("Inventory", Tag.TAG_COMPOUND)))
                throw new IllegalStateException("Commerce inventory was not durably saved");
        } catch (java.io.IOException failure) { throw new IllegalStateException("Commerce player storage unavailable", failure); }
    }
    private void recover(ServerPlayer player) {
        recoverNativePurchases(player);
        boolean changed=false;
        for (CompoundTag journal : new ArrayList<>(inventory.values())) if (journal.getUUID("Player").equals(player.getUUID())) {
            if (!reconcile(player, journal)) continue;
            changed=true;
            UUID parent = journal.getUUID("Parent"); String purpose = journal.getString("Purpose");
            if (purpose.equals("claim")) { CompoundTag claim = claims.get(parent); if (claim != null) {claim.putBoolean("Done", true);claim.remove("Item");} }
            CompoundTag listing = listings.get(parent); if (listing == null) {if(purpose.equals("claim")||terminalListings.containsKey(parent))inventory.remove(journal.getUUID("Id"));continue;}
            if (purpose.equals("create") && listing.getString("State").equals("PREPARED")) { listing.putString("State", "OPEN"); listing.remove("Pending"); }
            if (purpose.equals("fulfill") && listing.getString("State").equals("SETTLING")) finishFulfill(player.getServer(), listing);
            if(purpose.equals("create")&&listing.getString("State").equals("OPEN")||purpose.equals("fulfill")&&listing.getString("State").equals("SOLD"))inventory.remove(journal.getUUID("Id"));
        }
        if(changed)persist(player.getServer());
    }
    private static boolean unresolved(CompoundTag listing){return java.util.Set.of("OPEN","PREPARED","SETTLING").contains(listing.getString("State"));}
    private long pendingClaims(){return claims.size();}
    private static void award(MinecraftServer server, CompoundTag listing) {
        if (!listing.hasUUID("SellerPlayer")||listing.contains("XpHandled")) return;
        ServerPlayer seller = server.getPlayerList().getPlayer(listing.getUUID("SellerPlayer"));
        if (seller != null) MinecraftForge.EVENT_BUS.post(new CommerceCommittedEvent(seller,
                listing.hasUUID("Buyer") ? listing.getUUID("Buyer") : listing.getUUID("Owner"), listing.getUUID("Id"),outcome->{listing.putString("XpHandled",outcome);get(server).persist(server);}));
        // Offline sellers receive this same receipt on login, with profession deduplication.
    }
    private boolean advertise(ServerPlayer player, CompoundTag listing) {
        if (!listing.getUUID("Owner").equals(account(player))) return false;
        if (listing.getBoolean("Advertised")) listing.putBoolean("Advertised", false);
        else {
            if (listings.values().stream().filter(t -> t.getUUID("Owner").equals(account(player)) && t.getString("State").equals("OPEN") && t.getBoolean("Advertised")).count() >= 8) return false;
            listing.putBoolean("Advertised", true);
        }
        persist(player.getServer()); return true;
    }
    private boolean sign(ServerPlayer player, CompoundTag listing) {
        if (!listing.getUUID("Owner").equals(account(player)) || !listing.getString("Kind").equals("trading_post")) return false;
        var hit = player.pick(6, 0, false); if (!(hit instanceof BlockHitResult block) || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return false;
        if (!(player.serverLevel().getBlockEntity(block.getBlockPos()) instanceof SignBlockEntity signBlock) || signBlock.isWaxed()
                || signBlock.getPlayerWhoMayEdit()!=null&&!signBlock.getPlayerWhoMayEdit().equals(player.getUUID())) return false;
        if (!player.serverLevel().mayInteract(player, block.getBlockPos())) return false;
        UUID key = named(player.serverLevel().dimension().location() + ":" + block.getBlockPos().asLong());
        if (signs.containsKey(key) && !signs.get(key).getUUID("Owner").equals(account(player))) return false;
        if (!signs.containsKey(key) && signs.values().stream().filter(t -> t.getUUID("Owner").equals(account(player))&&listings.containsKey(t.getUUID("Listing"))).count() >= 3) return false;
        CompoundTag sign = new CompoundTag(); sign.putUUID("Owner", account(player)); sign.putUUID("Listing", listing.getUUID("Id")); signs.put(key, sign); persist(player.getServer());
        ItemStack item=ItemStack.of(listing.getCompound("Item"));var text=signBlock.getFrontText()
                .setMessage(0,net.minecraft.network.chat.Component.literal("Магазин"))
                .setMessage(1,item.getHoverName()).setMessage(2,net.minecraft.network.chat.Component.literal("×"+item.getCount()+" · "+listing.getLong("Price")+" медн."))
                .setMessage(3,player.getName());
        signBlock.setText(text,true);signBlock.setChanged();player.serverLevel().sendBlockUpdated(block.getBlockPos(),signBlock.getBlockState(),signBlock.getBlockState(),3);return true;
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void broken(net.minecraftforge.event.level.BlockEvent.BreakEvent event){
        if(!(event.getPlayer() instanceof ServerPlayer player)||!active(player.getServer())||event.isCanceled())return;
        var level=player.serverLevel();BlockPos position=event.getPos().immutable();
        player.getServer().execute(()->{
            if(event.isCanceled()||!level.hasChunkAt(position)||level.getBlockEntity(position) instanceof SignBlockEntity)return;
            SandboxCommerce data=get(player.getServer());UUID key=named(level.dimension().location()+":"+position.asLong());
            if(data.signs.remove(key)!=null)data.persist(player.getServer());
        });
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player.getServer())) return;
        SandboxCommerce data = get(player.getServer()); CompoundTag sign = data.signs.get(named(player.serverLevel().dimension().location() + ":" + event.getPos().asLong()));
        if (sign == null || !(player.serverLevel().getBlockEntity(event.getPos()) instanceof SignBlockEntity)) return;
        // Presentation event opens the shared profession form for this actual stocked listing.
        MinecraftForge.EVENT_BUS.post(new ShopSignEvent(player, sign.getUUID("Listing"))); event.setCanceled(true);
    }
    public static final class ShopSignEvent extends net.minecraftforge.eventbus.api.Event {
        public final ServerPlayer player; public final UUID listing;
        public ShopSignEvent(ServerPlayer player, UUID listing) { this.player = player; this.listing = listing; }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer(); if (event.phase != TickEvent.Phase.END || !active(server) || server.getTickCount() % 100 != 0) return;
        SandboxCommerce data = get(server); int budget = 32;
        var all = new ArrayList<>(data.listings.values());
        for (int inspected=0; inspected < Math.min(budget,all.size()); inspected++) {
            CompoundTag listing = all.get(Math.floorMod(listingCursor++,all.size()));
            String pending = listing.getString("Pending");
            if (pending.equals("BID")) {
                UUID bidVault=listing.getBoolean("SameBidder")?listing.getUUID("BidVault"):listing.getUUID("BidOperation");
                if (EconomySavedData.get(server).balance(vault(bidVault)) >= listing.getLong("NextBid")) data.finishBid(server,listing);
                else if (!EconomySavedData.get(server).hasReceipt("accountDeposit:"+listing.getUUID("BidOperation"))) { listing.remove("Pending"); data.persist(server); }
            }
            if (pending.equals("BUY")) {
                String paid=listing.getString("FundingReceipt");
                if (EconomySavedData.get(server).balance(vault(listing.getUUID("Id"))) >= listing.getLong("Price") || EconomySavedData.get(server).hasReceipt("commerce:shop:"+listing.getUUID("Id"))) data.finishBuy(server,listing);
                else if (!paid.isEmpty() && !EconomySavedData.get(server).hasReceipt(paid)) {listing.putString("State","OPEN");listing.remove("Pending");data.persist(server);}
            }
            if (pending.equals("FULFILL")) {
                CompoundTag journal=data.inventory.get(named(listing.getUUID("Id")+":fulfill"));
                if(journal!=null&&journal.getBoolean("Done")) data.finishFulfill(server,listing);
                else if(journal==null) {listing.putString("State","OPEN");listing.remove("Pending");data.persist(server);}
            }
            if(pending.equals("CREATE")) {
                CompoundTag journal=data.inventory.get(named(listing.getUUID("Id")+":create"));
                if(journal!=null&&journal.getBoolean("Done")){listing.putString("State","OPEN");listing.remove("Pending");data.inventory.remove(journal.getUUID("Id"));data.persist(server);}
                else if(journal==null){listing.putString("State","CANCELLED");listing.remove("Pending");data.persist(server);}
            }
            if (listing.getString("State").equals("PREPARED") && pending.isEmpty()
                    && EconomySavedData.get(server).balance(vault(listing.getUUID("Id"))) >= listing.getLong("Price")) { listing.putString("State", "OPEN"); data.persist(server); }
            else if(listing.getString("State").equals("PREPARED")&&pending.isEmpty()&&!EconomySavedData.get(server).hasReceipt("accountDeposit:"+listing.getUUID("Id"))){listing.putString("State","CANCELLED");data.persist(server);}
            ServerPlayer creator=listing.hasUUID("Creator")?server.getPlayerList().getPlayer(listing.getUUID("Creator")):null;
            if(creator!=null&&listing.getString("State").equals("OPEN")&&!permission.test(creator,listing.getString("Kind"))
                    &&!listing.hasUUID("Bidder"))data.close(server,listing,true);
            if (listing.getString("State").equals("OPEN") && !listing.getString("Kind").equals("trading_post") && listing.getLong("Expiry") <= System.currentTimeMillis()) data.close(server, listing, false);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player.getServer())) return;
        SandboxCommerce data = get(player.getServer()); data.recover(player);
        for (CompoundTag listing : new ArrayList<>(data.terminalListings.values())) if (listing.getString("State").equals("SOLD") && !listing.contains("XpHandled") && listing.hasUUID("SellerPlayer") && listing.getUUID("SellerPlayer").equals(player.getUUID())) award(player.getServer(), listing);
    }
    public static CompoundTag snapshot(ServerPlayer player, String node) {
        CompoundTag result = new CompoundTag(); ListTag rows = new ListTag(); result.put("Rows", rows);
        if (!active(player.getServer()) || account(player) == null) return result;
        SandboxCommerce data = get(player.getServer()); UUID owner = account(player);
        for (CompoundTag listing : data.listings.values()) {
            if (rows.size() >= 48) break;
            boolean own = listing.getUUID("Owner").equals(owner);
            if (!listing.getString("State").equals("OPEN")) continue;
            if (node.equals("market_network") ? !own&&!listing.getBoolean("Advertised") : !own && !listing.getString("Kind").equals(node) && !node.equals("stall_sign")) continue;
            if(node.equals("stall_sign")&&!listing.getString("Kind").equals("trading_post"))continue;
            ItemStack item = ItemStack.of(listing.getCompound("Item"));
            String kind=switch(listing.getString("Kind")){case "buy_orders"->"Покупка";case "auction"->"Торги";case "commission_escrow"->"Заказ работы";case "cargo_contract"->"Доставка";case "trading_post"->"Магазин";default->"Объявление";};
            String state=switch(listing.getString("State")){case "OPEN"->"Открыто";case "SOLD"->"Выполнено";case "CLOSED"->"Завершено";case "CANCELLED"->"Отменено";default->"Обрабатывается";};
            CompoundTag row = row(listing.getUUID("Id"), item.getHoverName().getString() + " ×" + item.getCount(), kind + " · " + listing.getLong("Price") + " медных монет · " + state + (own ? " · твоё" : ""));
            row.putString("Kind", listing.getString("Kind")); row.putBoolean("Own", own); row.putLong("Bid", listing.getLong("Bid"));row.putBoolean("Advertised",listing.getBoolean("Advertised"));
            if (listing.contains("Destination")) { row.putLong("Destination", listing.getLong("Destination")); row.putString("Dimension", listing.getString("Dimension")); }
            rows.add(row);
        }
        for (CompoundTag claim : data.claims.values()) if (rows.size() < 64 && !claim.getBoolean("Done") && claim.getUUID("Owner").equals(owner)) {
            ItemStack item = ItemStack.of(claim.getCompound("Item")); CompoundTag row = row(claim.getUUID("Id"), "Забрать: " + item.getHoverName().getString() + " ×" + item.getCount(), "Предметы хранятся до получения; освободите место в сумке."); row.putString("Kind", "claim"); rows.add(row);
        }
        for(CompoundTag journal:data.inventory.values())if(rows.size()<64&&journal.getUUID("Player").equals(player.getUUID())&&journal.getBoolean("Quarantined")&&!journal.getBoolean("Done"))
            rows.add(row(journal.getUUID("Id"),"Получение приостановлено","Сумка изменилась во время сохранения. Предметы не выданы повторно; требуется восстановление операции."));
        result.putLong("Balance", EconomyApi.balance(player)); return result;
    }

    /** Purpose account is a ledger account; this record stores authorization, never a balance. */
    public static boolean fund(ServerPlayer actor, UUID town, String purpose, long amount, UUID request, boolean release) {
        if (!active(actor.getServer()) || account(actor) == null || purpose.isBlank() || purpose.length() > 96 || amount < 1 || amount > 1_000_000) return false;
        var itemId=net.minecraft.resources.ResourceLocation.tryParse(purpose);
        if(itemId==null||!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(itemId)||!safe(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId))))return false;
        SandboxCommerce data = get(actor.getServer()); UUID id = named(town + ":" + purpose); CompoundTag record = data.funds.get(id);
        if(record==null&&data.funds.entrySet().stream().filter(e->e.getValue().getUUID("Town").equals(town)
                &&(e.getValue().hasUUID("ReturnOperation")||EconomySavedData.get(actor.getServer()).balance(named("jem:civic-fund:"+e.getKey()))>0)).count()>=16)return false;
        if (record == null) { record = new CompoundTag(); record.putUUID("Town", town); record.putString("Purpose", purpose); data.funds.put(id, record);data.persist(actor.getServer()); }
        UUID account = named("jem:civic-fund:" + id);
        boolean success = transferred(EconomySavedData.get(actor.getServer()).transfer(release ? account : EconomyApi.townAccount(town), release ? EconomyApi.townAccount(town) : account,
                amount, "civic:fund:" + request)); data.persist(actor.getServer()); return success;
    }
    public static List<CompoundTag> fundRows(ServerPlayer actor, UUID town) {
        List<CompoundTag> result = new ArrayList<>();
        for (var entry : get(actor.getServer()).funds.entrySet()) if (entry.getValue().getUUID("Town").equals(town) && result.size() < 16) {
            UUID account = named("jem:civic-fund:" + entry.getKey());
            if(EconomySavedData.get(actor.getServer()).balance(account)<=0&&!entry.getValue().hasUUID("ReturnOperation"))continue;
            String purpose=entry.getValue().getString("Purpose");var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(purpose));
            CompoundTag row=row(entry.getKey(),new ItemStack(item).getHoverName().getString(), EconomySavedData.get(actor.getServer()).balance(account) + " медных монет");row.putString("Purpose",purpose);result.add(row);
        }
        return result;
    }
    /** Permission removal returns the exact remaining purpose balance to its treasury. */
    public static void releaseFunds(ServerPlayer actor,UUID town){
        if(!active(actor.getServer())||account(actor)==null)return;SandboxCommerce data=get(actor.getServer());
        for(var entry:data.funds.entrySet()){
            CompoundTag record=entry.getValue();if(!record.getUUID("Town").equals(town))continue;
            UUID source=named("jem:civic-fund:"+entry.getKey());long amount=EconomySavedData.get(actor.getServer()).balance(source);
            if(!record.hasUUID("ReturnOperation")){
                if(amount<=0)continue;record.putUUID("ReturnOperation",UUID.randomUUID());record.putLong("ReturnAmount",amount);data.persist(actor.getServer());
            }
            if(transferred(EconomySavedData.get(actor.getServer()).transfer(source,EconomyApi.townAccount(town),record.getLong("ReturnAmount"),"civic:unclaim:"+record.getUUID("ReturnOperation")))){
                record.remove("ReturnOperation");record.remove("ReturnAmount");data.persist(actor.getServer());
            }
        }
    }
    /** Only an actual stocked offer of the predeclared safe item can spend a purpose fund. */
    public static boolean buyWithFund(ServerPlayer actor,UUID town,String purpose,UUID listingId) {
        if(!active(actor.getServer())||account(actor)==null)return false;
        SandboxCommerce data=get(actor.getServer());UUID fundId=named(town+":"+purpose);
        CompoundTag fund=data.funds.get(fundId),listing=data.listings.get(listingId);
        if(fund==null||listing==null||!listing.getString("State").equals("OPEN")||!listing.getString("Kind").equals("trading_post")
                ||listing.getUUID("Owner").equals(account(actor)))return false;
        ItemStack goods=ItemStack.of(listing.getCompound("Item"));
        if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(goods.getItem()).toString().equals(purpose)||data.pendingClaims()+2>=MAX_CLAIMS)return false;
        UUID source=named("jem:civic-fund:"+fundId),operation=UUID.randomUUID();
        listing.putUUID("Buyer",account(actor));listing.putString("State","SETTLING");listing.putString("Pending","BUY");
        listing.putString("FundingReceipt","civic:purchase:"+operation);listing.putString("Purpose",purpose);listing.putUUID("Town",town);data.persist(actor.getServer());
        if(!transferred(EconomySavedData.get(actor.getServer()).transfer(source,vault(listingId),listing.getLong("Price"),listing.getString("FundingReceipt")))){
            listing.putString("State","OPEN");listing.remove("Pending");data.persist(actor.getServer());return false;
        }
        return data.finishBuy(actor.getServer(),listing);
    }
    /** Capture an exact output obligation before any reservation or tax can become durable. */
    public static String prepareNativePurchase(ServerPlayer player,UUID operation,UUID town,int rate,long amount,ItemStack output){
        if(!active(player.getServer())||account(player)==null||output.isEmpty()||output.getCount()>output.getMaxStackSize()||amount<=0)return "jem.economy.trade.unavailable";
        if(!(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu)
                ||!(player.containerMenu instanceof com.siirio.jemeconomy.merchant.NativeMerchantAccess access)
                ||!player.containerMenu.getCarried().isEmpty())return "jem.economy.trade.clear_cursor";
        if(!access.jem$container().getItem(0).isEmpty()||!access.jem$container().getItem(1).isEmpty())return "jem.economy.trade.tax_side_items";
        var data=get(player.getServer());data.recover(player);
        if(data.completedNativePurchases.containsKey(operation)||data.nativePurchases.containsKey(operation))return "jem.economy.trade.unavailable";
        if(data.pendingClaims()+1>=MAX_CLAIMS||data.inventory.values().stream().anyMatch(t->t.getUUID("Player").equals(player.getUUID())&&!t.getBoolean("Done")))return "jem.economy.trade.unavailable";
        int room=0;for(int slot=0;slot<36;slot++){
            ItemStack current=player.getInventory().getItem(slot);
            if(current.isEmpty())room+=output.getMaxStackSize();else if(fingerprint(current,output))room+=Math.max(0,current.getMaxStackSize()-current.getCount());
        }
        if(room<output.getCount())return "jem.economy.no_space";
        CompoundTag record=new CompoundTag();record.putUUID("Id",operation);record.putUUID("Payer",account(player));record.putUUID("Player",player.getUUID());record.putUUID("Town",town);
        record.putInt("Rate",rate);record.putLong("Amount",amount);record.putString("State","PREPARED");record.put("Item",output.copy().save(new CompoundTag()));
        data.nativePurchases.put(operation,record);data.persist(player.getServer());return "";
    }
    public static void abortNativePurchase(ServerPlayer player,UUID operation){
        var data=get(player.getServer());var record=data.nativePurchases.get(operation);if(record==null)return;
        // Never retire a paid goods obligation on an error path.
        if(data.paidNative(record))throw new IllegalStateException("A paid native purchase still owns its output");
        var income=data.residentIncome.get(operation);if(income!=null)income.putBoolean("Cancelled",true);
        record.putString("State","ABORTED");data.persist(player.getServer());
    }
    private boolean paidNative(CompoundTag record){
        UUID operation=record.getUUID("Id");var income=settledResidentIncome.get(operation);if(income==null)income=residentIncome.get(operation);
        return income!=null&&income.getBoolean("Committed")||record.getString("State").equals("PAID")||record.getString("State").equals("DELIVERED");
    }
    public static boolean completeNativePurchase(ServerPlayer player,UUID operation){
        var data=get(player.getServer());var record=data.nativePurchases.get(operation);
        if(record==null)return data.completedNativePurchases.containsKey(operation)&&data.completedNativePurchases.get(operation).getString("State").equals("DELIVERED");
        if(!record.getUUID("Player").equals(player.getUUID())||!record.getUUID("Payer").equals(account(player))||!data.paidNative(record))return false;
        record.putString("State","PAID");data.persist(player.getServer());
        UUID claimId=named(operation+":native-output");data.addClaim(player.getServer(),record.getUUID("Payer"),ItemStack.of(record.getCompound("Item")),claimId);
        if(!data.deliveredClaims.containsKey(claimId)&&!data.claim(player,claimId))return true; // Real paid output stays in its recoverable delivery claim.
        record.putString("State","DELIVERED");data.persist(player.getServer());return true;
    }
    public static boolean nativePurchaseDelivery(ServerPlayer player,UUID operation){
        var data=get(player.getServer());var record=data.nativePurchases.get(operation);if(record==null)record=data.completedNativePurchases.get(operation);
        return record!=null&&(record.getString("State").equals("PAID")||record.getString("State").equals("DELIVERED"));
    }
    public static boolean nativePurchaseDelivered(ServerPlayer player,UUID operation){
        var record=get(player.getServer()).completedNativePurchases.get(operation);return record!=null&&record.getString("State").equals("DELIVERED");
    }
    public static boolean purposeFundsOutstanding(MinecraftServer server,UUID town){
        var data=get(server);return data.funds.entrySet().stream().anyMatch(entry->entry.getValue().getUUID("Town").equals(town)
                &&(entry.getValue().hasUUID("ReturnOperation")||EconomySavedData.get(server).balance(named("jem:civic-fund:"+entry.getKey()))>0));
    }
    public static void flushOwner(MinecraftServer server){if(active(server))get(server).persist(server);}
    private void recoverNativePurchases(ServerPlayer player){
        for(var record:new ArrayList<>(nativePurchases.values())){
            if(!record.getUUID("Player").equals(player.getUUID())||!record.getUUID("Payer").equals(account(player)))continue;
            UUID operation=record.getUUID("Id");var ledger=EconomySavedData.get(player.getServer());
            boolean split=ledger.hasReceipt("residentIncome:"+operation+":market")&&ledger.hasReceipt("residentIncome:"+operation+":tax");
            if(paidNative(record)||split){
                record.putString("State","PAID");persist(player.getServer());completeNativePurchase(player,operation);continue;
            }
            if(ledger.hasReceipt("marketReserve:"+operation)&&!ledger.hasReceipt("marketRefund:"+operation)){
                var refunded=EconomyApi.refundMarketPayment(player,record.getLong("Amount"),operation);
                if(refunded.status()!=EconomyApi.MarketStatus.REFUNDED&&refunded.status()!=EconomyApi.MarketStatus.REPLAYED)continue;
            }
            abortNativePurchase(player,operation);
        }
    }
    public static TransferResult settleResidentIncome(ServerPlayer player,UUID town,int rate,long amount,UUID operation) {
        if(!active(player.getServer())||account(player)==null||town==null||amount<=0||!java.util.Set.of(5,10,15).contains(rate))
            return new TransferResult(TransferResult.Status.INVALID,null,0,0);
        SandboxCommerce data=get(player.getServer());CompoundTag receipt=data.residentIncome.get(operation);
        CompoundTag purchase=data.nativePurchases.get(operation);
        if(purchase==null||!purchase.getUUID("Payer").equals(account(player))||!purchase.getUUID("Town").equals(town)||purchase.getLong("Amount")!=amount||purchase.getInt("Rate")!=rate)
            return new TransferResult(TransferResult.Status.INVALID,null,0,0);
        if(receipt==null)receipt=data.settledResidentIncome.get(operation);
        long tax=(amount/100)*rate+((amount%100)*rate)/100;
        if(tax<=0)return new TransferResult(TransferResult.Status.INVALID,null,0,0);
        if(receipt==null){
            if(data.residentIncome.size()>=32768)return new TransferResult(TransferResult.Status.UNAVAILABLE,null,0,0);
            receipt=new CompoundTag();receipt.putUUID("Town",town);receipt.putUUID("Payer",account(player));receipt.putLong("Amount",amount);receipt.putLong("Tax",tax);receipt.putInt("Rate",rate);
            data.residentIncome.put(operation,receipt);data.persist(player.getServer());
        }
        if(!receipt.getUUID("Town").equals(town)||!receipt.getUUID("Payer").equals(account(player))||receipt.getLong("Amount")!=amount||receipt.getInt("Rate")!=rate||receipt.getBoolean("Reversed"))
            return new TransferResult(TransferResult.Status.INVALID,null,0,0);
        TransferResult result=EconomySavedData.get(player.getServer()).splitTransfer(com.siirio.jemeconomy.ledger.SystemAccount.MARKET_ESCROW.id(),
                com.siirio.jemeconomy.ledger.SystemAccount.NPC_MARKET.id(),amount-tax,EconomyApi.townAccount(town),tax,"residentIncome:"+operation);
        if(transferred(result)){receipt.putBoolean("Committed",true);data.persist(player.getServer());}return result;
    }
    /** Only an actual native refund owner may call this; there is no client 'income' or refund claim. */
    public static boolean reverseResidentIncome(MinecraftServer server,UUID operation) {
        if(!active(server))return false;SandboxCommerce data=get(server);CompoundTag receipt=data.residentIncome.get(operation);
        if(receipt==null)receipt=data.settledResidentIncome.get(operation);
        if(receipt==null||!receipt.getBoolean("Committed"))return false;if(receipt.getBoolean("Reversed"))return true;
        long amount=receipt.getLong("Amount"),tax=receipt.getLong("Tax");
        if(!transferred(EconomySavedData.get(server).mergeTransfer(com.siirio.jemeconomy.ledger.SystemAccount.NPC_MARKET.id(),amount-tax,
                EconomyApi.townAccount(receipt.getUUID("Town")),tax,receipt.getUUID("Payer"),"residentIncomeReverse:"+operation)))return false;
        receipt.putBoolean("Reversed",true);data.persist(server);return true;
    }
    @Override public CompoundTag save(CompoundTag root) {
        root.put("Listings", saveMap(listings)); root.put("Claims", saveMap(claims)); root.put("Inventory", saveMap(inventory)); root.put("Signs", saveMap(signs)); root.put("Funds", saveMap(funds)); root.put("ResidentIncome",saveMap(residentIncome));
        root.put("TerminalListings",saveMap(terminalListings));root.put("DeliveredClaims",saveMap(deliveredClaims));root.put("SettledResidentIncome",saveMap(settledResidentIncome));
        root.put("NativePurchases",saveMap(nativePurchases));root.put("CompletedNativePurchases",saveMap(completedNativePurchases));return root;
    }
    private static CompoundTag saveMap(Map<UUID, CompoundTag> map) { CompoundTag result = new CompoundTag(); map.forEach((id, value) -> result.put(id.toString(), value)); return result; }
    private static void loadMap(CompoundTag root, String key, Map<UUID, CompoundTag> map) { CompoundTag values = root.getCompound(key); for (String id : values.getAllKeys()) map.put(UUID.fromString(id), values.getCompound(id)); }
    private static SandboxCommerce load(CompoundTag root) {
        SandboxCommerce data = new SandboxCommerce(); loadMap(root,"Listings",data.listings); loadMap(root,"Claims",data.claims); loadMap(root,"Inventory",data.inventory); loadMap(root,"Signs",data.signs); loadMap(root,"Funds",data.funds);loadMap(root,"ResidentIncome",data.residentIncome);
        loadMap(root,"TerminalListings",data.terminalListings);loadMap(root,"DeliveredClaims",data.deliveredClaims);loadMap(root,"SettledResidentIncome",data.settledResidentIncome);
        loadMap(root,"NativePurchases",data.nativePurchases);loadMap(root,"CompletedNativePurchases",data.completedNativePurchases);data.retireCompleted();return data;
    }
}
