package com.siirio.jemserver.smp;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Typed shop index state. Native stack payloads are immutable copies, not domain storage. */
public final class ShopState implements SmpRecord {
    private static final List<String> FIELDS = List.of("id", "owner", "title", "name", "state", "dimension",
            "position", "quantity", "price", "selectable", "CoinCurrency", "revision", "created", "offer", "payment", "choices", "offers");
    public static final class Stack {
        private final CompoundTag payload;
        public Stack(CompoundTag payload) { this.payload = payload.copy(); }
        public CompoundTag tag() { return payload.copy(); }
        @Override public boolean equals(Object other) { return other instanceof Stack stack && payload.equals(stack.payload); }
        @Override public int hashCode() { return payload.hashCode(); }
    }
    public record Offer(Stack payment, Stack output, int availableTrades) {}

    private final UUID id;
    private final UUID owner;
    private final String title, name, state, dimension;
    private final Long position;
    private final int quantity, price;
    private final boolean selectable, coinCurrency;
    private final Stack offer, payment;
    private final List<Stack> choices;
    private final List<Offer> offers;
    private final CompoundTag extra;
    private final java.util.Set<String> present;
    private int revision;
    private long created;
    private boolean frozen;

    private ShopState(CompoundTag tag) {
        id = tag.getUUID("id");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        title = tag.getString("title"); name = tag.getString("name"); state = tag.getString("state");
        dimension = tag.getString("dimension");
        position = tag.contains("position", Tag.TAG_LONG) ? tag.getLong("position") : null;
        quantity = tag.getInt("quantity"); price = tag.getInt("price");
        selectable = tag.getBoolean("selectable"); coinCurrency = tag.getBoolean("CoinCurrency");
        revision = tag.getInt("revision"); created = tag.getLong("created");
        offer = new Stack(tag.getCompound("offer")); payment = new Stack(tag.getCompound("payment"));
        choices = tag.getList("choices", Tag.TAG_COMPOUND).stream().map(value -> new Stack((CompoundTag) value)).toList();
        offers = tag.getList("offers", Tag.TAG_COMPOUND).stream().map(value -> {
            CompoundTag row = (CompoundTag) value;
            return new Offer(new Stack(row.getCompound("payment")), new Stack(row.getCompound("output")), row.getInt("availableTrades"));
        }).toList();
        present = new java.util.HashSet<>(tag.getAllKeys());
        extra = tag.copy();
        FIELDS.forEach(extra::remove);
    }

    public static ShopState read(CompoundTag tag) { return new ShopState(tag); }
    private ShopState(ShopState source) {
        id = source.id; owner = source.owner; title = source.title; name = source.name;
        state = source.state; dimension = source.dimension; position = source.position;
        quantity = source.quantity; price = source.price; selectable = source.selectable;
        coinCurrency = source.coinCurrency; offer = source.offer; payment = source.payment;
        choices = source.choices; offers = source.offers; extra = source.extra.copy();
        present = new java.util.HashSet<>(source.present); revision = source.revision; created = source.created;
    }
    public ShopState copy() { return new ShopState(this); }
    public ShopState snapshot() { ShopState copy = copy(); copy.frozen = true; return copy; }
    private void writable() { if (frozen) throw new IllegalStateException("Shop snapshot is immutable"); }
    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String title() { return title; }
    public String name() { return name; }
    public String state() { return state; }
    public String dimension() { return dimension; }
    public boolean hasDimension() { return present.contains("dimension"); }
    public boolean hasPosition() { return position != null; }
    public long position() { return position == null ? 0L : position; }
    public int quantity() { return quantity; }
    public int price() { return price; }
    public boolean selectable() { return selectable; }
    public Stack offer() { return offer; }
    public Stack payment() { return payment; }
    public List<Stack> choices() { return choices; }
    public List<Offer> offers() { return offers; }
    public int revision() { return revision; }
    public void revision(int value) { writable(); revision = value; present.add("revision"); }
    public long created() { return created; }
    public void created(long value) { writable(); created = value; present.add("created"); }

    public CompoundTag write() {
        CompoundTag tag = extra.copy();
        tag.putUUID("id", id);
        if (owner != null) tag.putUUID("owner", owner);
        tag.putString("title", title); tag.putString("name", name); tag.putString("state", state);
        if (hasDimension()) tag.putString("dimension", dimension);
        if (position != null) tag.putLong("position", position);
        tag.putInt("quantity", quantity); tag.putInt("price", price);
        tag.putBoolean("selectable", selectable); tag.putBoolean("CoinCurrency", coinCurrency);
        tag.putInt("revision", revision); tag.putLong("created", created);
        tag.put("offer", offer.tag()); tag.put("payment", payment.tag());
        ListTag choiceTags = new ListTag(); choices.forEach(stack -> choiceTags.add(stack.tag()));
        if (present.contains("choices") || !choiceTags.isEmpty()) tag.put("choices", choiceTags);
        ListTag offerTags = new ListTag();
        offers.forEach(value -> {
            CompoundTag row = new CompoundTag();
            row.put("payment", value.payment().tag()); row.put("output", value.output().tag());
            row.putInt("availableTrades", value.availableTrades()); offerTags.add(row);
        });
        if (present.contains("offers") || !offerTags.isEmpty()) tag.put("offers", offerTags);
        for (String key : FIELDS) if (!present.contains(key)) tag.remove(key);
        return tag;
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof ShopState row)) return false;
        return id.equals(row.id) && Objects.equals(owner, row.owner) && title.equals(row.title)
                && name.equals(row.name) && state.equals(row.state) && dimension.equals(row.dimension)
                && Objects.equals(position, row.position) && quantity == row.quantity && price == row.price
                && selectable == row.selectable && coinCurrency == row.coinCurrency && revision == row.revision
                && created == row.created && offer.equals(row.offer) && payment.equals(row.payment)
                && choices.equals(row.choices) && offers.equals(row.offers) && extra.equals(row.extra)
                && present.equals(row.present);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
