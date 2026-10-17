package com.siirio.jemsettlements.town;

import com.siirio.jemsettlements.payroll.PayrollPeriod;
import com.siirio.jemsettlements.simulation.SettlementSimulationState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class TownRecord implements com.siirio.jemsettlements.payroll.PayrollBook {
    public static final int INITIAL_RADIUS = 48;
    public static final int CURRENT_SCHEMA = 7;
    private int nextStorageNumber = 1;
    private final UUID id;
    private final UUID owner;
    private String name;
    private final ResourceLocation dimension;
    private final BlockPos center;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final List<StorageLink> storage = new ArrayList<>();
    private final List<PayrollPeriod> payroll = new ArrayList<>();
    private SettlementSimulationState simulation = new SettlementSimulationState();
    private int happiness = 50;
    private int taxPercent;
    private long lastHappinessDay = Long.MIN_VALUE;
    private long payrollEpoch;
    private long nextPayrollGameTime;
    private long nextPayrollIndex;
    private int consecutiveMissedPayrolls;
    private int emeraldMissedPayrolls;
    private TownTier tier = TownTier.CAMP;
    private TownSpecialization primarySpecialization = TownSpecialization.NONE;
    private TownSpecialization secondarySpecialization = TownSpecialization.NONE;
    private long lastSpecializationEvaluation;
    private int housingScore = 50, foodScore = 50, safetyScore = 0, jobsScore = 25, communityScore = 40, decorationScore;
    private int territoryRadius = INITIAL_RADIUS;
    private int territoryMinX, territoryMaxX, territoryMinZ, territoryMaxZ;
    private final Set<UUID> expansionReceipts = new LinkedHashSet<>();

    public TownRecord(UUID id, UUID owner, String name, ResourceLocation dimension, BlockPos center) {
        this.id = id; this.owner = owner; this.name = name; this.dimension = dimension; this.center = center.immutable(); members.add(owner);
        territoryMinX = center.getX() - territoryRadius; territoryMaxX = center.getX() + territoryRadius;
        territoryMinZ = center.getZ() - territoryRadius; territoryMaxZ = center.getZ() + territoryRadius;
    }
    public UUID id() { return id; } public UUID owner() { return owner; } public String name() { return name; } public ResourceLocation dimension() { return dimension; }
    public BlockPos center() { return center; } public Set<UUID> members() { return Set.copyOf(members); } public List<StorageLink> storage() { return List.copyOf(storage); }
    public int happiness() { return happiness; } public int taxPercent() { return taxPercent; } public long lastHappinessDay() { return lastHappinessDay; }
    public long payrollEpoch() { return payrollEpoch; } public long nextPayrollGameTime() { return nextPayrollGameTime; } public long nextPayrollIndex() { return nextPayrollIndex; }
    public int consecutiveMissedPayrolls() { return com.siirio.jemsettlements.currency.SettlementCurrency.coins() ? consecutiveMissedPayrolls : emeraldMissedPayrolls; } public List<PayrollPeriod> payroll() { return List.copyOf(payroll); }
    public SettlementSimulationState simulation() { return simulation; } public TownTier tier() { return tier; }
    public TownSpecialization primarySpecialization() { return primarySpecialization; } public TownSpecialization secondarySpecialization() { return secondarySpecialization; }
    public long lastSpecializationEvaluation() { return lastSpecializationEvaluation; }
    public int housingScore() { return housingScore; } public int foodScore() { return foodScore; } public int safetyScore() { return safetyScore; }
    public int jobsScore() { return jobsScore; } public int communityScore() { return communityScore; } public int decorationScore() { return decorationScore; }
    public int territoryRadius() { return territoryRadius; }
    public int territoryMinX() { return territoryMinX; } public int territoryMaxX() { return territoryMaxX; }
    public int territoryMinZ() { return territoryMinZ; } public int territoryMaxZ() { return territoryMaxZ; }
    public long territoryArea() { return (long) (territoryMaxX - territoryMinX + 1) * (territoryMaxZ - territoryMinZ + 1); }
    public void rename(String value) { name = value; }
    public boolean expansionSeen(UUID request) { return expansionReceipts.contains(request); }
    public void recordExpansion(UUID request) { expansionReceipts.add(request); while (expansionReceipts.size() > 64) expansionReceipts.remove(expansionReceipts.iterator().next()); }
    public boolean contains(BlockPos position) { return position.getX() >= territoryMinX && position.getX() <= territoryMaxX && position.getZ() >= territoryMinZ && position.getZ() <= territoryMaxZ; }
    public void territoryRadius(int value) { territoryRadius = Math.max(32, Math.min(256, value)); territoryMinX = center.getX() - territoryRadius; territoryMaxX = center.getX() + territoryRadius; territoryMinZ = center.getZ() - territoryRadius; territoryMaxZ = center.getZ() + territoryRadius; }
    public void territoryBounds(int minX, int minZ, int maxX, int maxZ) {
        territoryMinX = Math.max(center.getX() - 256, Math.min(minX, center.getX())); territoryMaxX = Math.min(center.getX() + 256, Math.max(maxX, center.getX()));
        territoryMinZ = Math.max(center.getZ() - 256, Math.min(minZ, center.getZ())); territoryMaxZ = Math.min(center.getZ() + 256, Math.max(maxZ, center.getZ()));
        territoryRadius = Math.max(Math.max(center.getX() - territoryMinX, territoryMaxX - center.getX()), Math.max(center.getZ() - territoryMinZ, territoryMaxZ - center.getZ()));
    }
    public void lastHappinessDay(long value) { lastHappinessDay = value; }
    public void happiness(int value) { happiness = clamp(value); }
    public void happinessEvidence(int housing, int food, int safety, int jobs, int community, int decoration) { housingScore = clamp(housing); foodScore = clamp(food); safetyScore = clamp(safety); jobsScore = clamp(jobs); communityScore = clamp(community); decorationScore = Math.max(0, Math.min(20, decoration)); }
    public void taxPercent(int value) { if (value != 0 && value != 5 && value != 10 && value != 15) throw new IllegalArgumentException("Unsupported tax rate"); taxPercent = value; }
    public boolean addStorage(StorageLink link) { return storage.stream().noneMatch(value -> value.dimension().equals(link.dimension()) && value.position().equals(link.position())) && storage.size() < 64 && storage.add(link.numbered(nextStorageNumber++)); }
    public void replaceStorage(java.util.List<StorageLink> links) { storage.clear(); storage.addAll(links); }
    public boolean setStorageFeed(StorageLink link, boolean enabled) { for (int i = 0; i < storage.size(); i++) { StorageLink current = storage.get(i); if (current.dimension().equals(link.dimension()) && current.position().equals(link.position())) { storage.set(i, current.withFeedResidents(enabled)); return true; } } return false; }
    public void initializePayroll(long gameTime) { if (nextPayrollGameTime == 0L) { payrollEpoch = gameTime; nextPayrollGameTime = Math.addExact(gameTime, 168_000L); nextPayrollIndex = 0L; } }
    public void schedule(PayrollPeriod period) { payroll.add(period); while (payroll.size() > 32 && payroll.get(0).paid()) payroll.remove(0); nextPayrollIndex = period.index() + 1L; nextPayrollGameTime = Math.addExact(period.dueGameTime(), 168_000L); }
    public void payrollMissed() {
        if (com.siirio.jemsettlements.currency.SettlementCurrency.coins()) consecutiveMissedPayrolls = Math.min(8, consecutiveMissedPayrolls + 1);
        else emeraldMissedPayrolls = Math.min(8, emeraldMissedPayrolls + 1);
    }
    public void payrollRecovered() {
        if (payroll.stream().filter(PayrollPeriod::currentCurrency).allMatch(PayrollPeriod::paid)) {
            if (com.siirio.jemsettlements.currency.SettlementCurrency.coins()) consecutiveMissedPayrolls = 0; else emeraldMissedPayrolls = 0;
        }
    }
    public void upgrade(TownTier value) { if (value.ordinal() > tier.ordinal()) tier = value; }
    public void specializations(TownSpecialization primary, TownSpecialization secondary, long gameTime) { primarySpecialization = primary; secondarySpecialization = secondary; lastSpecializationEvaluation = gameTime; }
    private static int clamp(int value) { return Math.max(0, Math.min(100, value)); }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("Schema", CURRENT_SCHEMA); tag.putUUID("Id", id); tag.putUUID("Owner", owner); tag.putString("Name", name);
        tag.putString("Dimension", dimension.toString()); tag.putLong("Center", center.asLong()); tag.putInt("Happiness", happiness); tag.putInt("Tax", taxPercent); tag.putLong("LastHappinessDay", lastHappinessDay);
        tag.putLong("PayrollEpoch", payrollEpoch); tag.putLong("NextPayrollGameTime", nextPayrollGameTime); tag.putLong("NextPayrollIndex", nextPayrollIndex); tag.putInt("ConsecutiveMissedPayrolls", consecutiveMissedPayrolls); tag.putInt("EmeraldMissedPayrolls", emeraldMissedPayrolls);
        tag.putString("Tier", tier.name()); tag.putString("PrimarySpecialization", primarySpecialization.name()); tag.putString("SecondarySpecialization", secondarySpecialization.name()); tag.putLong("LastSpecializationEvaluation", lastSpecializationEvaluation);
        tag.putInt("HousingScore", housingScore); tag.putInt("FoodScore", foodScore); tag.putInt("SafetyScore", safetyScore); tag.putInt("JobsScore", jobsScore); tag.putInt("CommunityScore", communityScore); tag.putInt("DecorationScore", decorationScore);
        tag.putInt("TerritoryRadius", territoryRadius); tag.putInt("TerritoryMinX", territoryMinX); tag.putInt("TerritoryMaxX", territoryMaxX); tag.putInt("TerritoryMinZ", territoryMinZ); tag.putInt("TerritoryMaxZ", territoryMaxZ);
        ListTag receiptTags = new ListTag(); expansionReceipts.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); receiptTags.add(row); }); tag.put("ExpansionReceipts", receiptTags);
        ListTag memberTags = new ListTag(); members.forEach(value -> { CompoundTag row = new CompoundTag(); row.putUUID("Id", value); memberTags.add(row); }); tag.put("Members", memberTags);
        ListTag storageTags = new ListTag(); storage.forEach(value -> storageTags.add(value.save())); tag.put("Storage", storageTags); tag.putInt("NextStorageNumber", nextStorageNumber);
        ListTag payrollTags = new ListTag(); payroll.forEach(value -> payrollTags.add(value.save())); tag.put("Payroll", payrollTags); tag.put("Simulation", simulation.save()); return tag;
    }
    static TownRecord load(CompoundTag tag) {
        TownRecord town = new TownRecord(tag.getUUID("Id"), tag.getUUID("Owner"), tag.getString("Name"), new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Center")));
        town.happiness(tag.contains("Happiness") ? tag.getInt("Happiness") : 50); town.taxPercent(tag.getInt("Tax")); town.lastHappinessDay(tag.contains("LastHappinessDay") ? tag.getLong("LastHappinessDay") : Long.MIN_VALUE);
        ListTag members = tag.contains("Members") ? tag.getList("Members", Tag.TAG_COMPOUND) : tag.getList("Residents", Tag.TAG_COMPOUND); for (int i = 0; i < members.size(); i++) if (members.getCompound(i).hasUUID("Id")) town.members.add(members.getCompound(i).getUUID("Id"));
        ListTag stores = tag.getList("Storage", Tag.TAG_COMPOUND); for (int i = 0; i < stores.size() && town.storage.size() < 64; i++) town.storage.add(StorageLink.load(stores.getCompound(i)));
        town.nextStorageNumber = Math.max(1, tag.getInt("NextStorageNumber"));
        for (int i=0;i<town.storage.size();i++) { StorageLink link=town.storage.get(i); if(link.number()<=0) { link=link.numbered(town.nextStorageNumber++); town.storage.set(i,link); } town.nextStorageNumber=Math.max(town.nextStorageNumber,link.number()+1); }
        town.payrollEpoch = tag.getLong("PayrollEpoch"); town.nextPayrollGameTime = tag.getLong("NextPayrollGameTime"); town.nextPayrollIndex = tag.getLong("NextPayrollIndex"); town.consecutiveMissedPayrolls = tag.getInt("ConsecutiveMissedPayrolls"); town.emeraldMissedPayrolls = tag.getInt("EmeraldMissedPayrolls");
        try { town.tier = TownTier.valueOf(tag.getString("Tier")); } catch (IllegalArgumentException ignored) { }
        try { town.primarySpecialization = TownSpecialization.valueOf(tag.getString("PrimarySpecialization")); } catch (IllegalArgumentException ignored) { }
        try { town.secondarySpecialization = TownSpecialization.valueOf(tag.getString("SecondarySpecialization")); } catch (IllegalArgumentException ignored) { }
        town.lastSpecializationEvaluation = tag.getLong("LastSpecializationEvaluation"); if (tag.contains("HousingScore")) town.happinessEvidence(tag.getInt("HousingScore"), tag.getInt("FoodScore"), tag.getInt("SafetyScore"), tag.getInt("JobsScore"), tag.getInt("CommunityScore"), tag.getInt("DecorationScore"));
        if (tag.contains("TerritoryMinX")) town.territoryBounds(tag.getInt("TerritoryMinX"), tag.getInt("TerritoryMinZ"), tag.getInt("TerritoryMaxX"), tag.getInt("TerritoryMaxZ"));
        else if (tag.contains("TerritoryRadius")) town.territoryRadius(tag.getInt("TerritoryRadius"));
        ListTag receipts = tag.getList("ExpansionReceipts", Tag.TAG_COMPOUND); for (int i = 0; i < receipts.size() && i < 64; i++) if (receipts.getCompound(i).hasUUID("Id")) town.expansionReceipts.add(receipts.getCompound(i).getUUID("Id"));
        ListTag periods = tag.getList("Payroll", Tag.TAG_COMPOUND); for (int i = 0; i < periods.size(); i++) town.payroll.add(PayrollPeriod.load(periods.getCompound(i)));
        if (tag.contains("Simulation")) town.simulation = SettlementSimulationState.load(tag.getCompound("Simulation")); return town;
    }
}
