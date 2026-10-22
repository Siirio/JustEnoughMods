package com.siirio.jemserver.smp;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Operation-shaped commands. Legacy discriminants exist only at the wire/UI boundary. */
public sealed interface SmpAction {
    UUID request();
    UUID id();
    int revision();
    SmpTable table();
    SmpActionKind kind();
    default SmpActionInput input() { return SmpNoInput.INSTANCE; }
    void execute(ServerPlayer player);

    record CreateParty(UUID request, SmpPartyRequest input) implements SmpAction {
        public CreateParty { Objects.requireNonNull(request); Objects.requireNonNull(input); }
        public UUID id() { return null; }
        public int revision() { return 0; }
        public SmpTable table() { return SmpTable.PARTIES; }
        public SmpActionKind kind() { return SmpActionKind.CREATE; }
        public void execute(ServerPlayer player) { SmpActions.create(player, input); }
    }

    sealed interface PartyCommand extends SmpAction {
        default SmpTable table() { return SmpTable.PARTIES; }
        default void execute(ServerPlayer player) { SmpActions.party(player, this); }
    }
    sealed interface EventCommand extends SmpAction {
        default SmpTable table() { return SmpTable.EVENTS; }
        default void execute(ServerPlayer player) { SmpActions.event(player, this); }
    }
    sealed interface ShopCommand extends SmpAction {
        default SmpTable table() { return SmpTable.SHOPS; }
        default void execute(ServerPlayer player) {
            com.siirio.jemserver.smp.shops.ShopIndex.action(player, id(), revision(), kind().key(), request());
        }
    }

    record NavigateParty(UUID request, UUID id, int revision) implements PartyCommand {
        public NavigateParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.NAVIGATE; }
    }

    record SoloParty(UUID request, UUID id, int revision) implements PartyCommand {
        public SoloParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.SOLO; }
    }

    record MultiplayerParty(UUID request, UUID id, int revision) implements PartyCommand {
        public MultiplayerParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.MULTIPLAYER; }
    }

    record InviteParty(UUID request, UUID id, int revision, UUID player) implements PartyCommand {
        public InviteParty { Objects.requireNonNull(request); Objects.requireNonNull(id); Objects.requireNonNull(player); }
        public SmpActionKind kind() { return SmpActionKind.INVITE; }
        public SmpPlayerRequest input() { return new SmpPlayerRequest(player); }
    }

    record JoinParty(UUID request, UUID id, int revision) implements PartyCommand {
        public JoinParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.JOIN; }
    }

    record ArriveParty(UUID request, UUID id, int revision) implements PartyCommand {
        public ArriveParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.ARRIVE; }
    }

    record LeaveParty(UUID request, UUID id, int revision) implements PartyCommand {
        public LeaveParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.LEAVE; }
    }

    record ReadyParty(UUID request, UUID id, int revision) implements PartyCommand {
        public ReadyParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.READY; }
    }

    record PublishParty(UUID request, UUID id, int revision) implements PartyCommand {
        public PublishParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.PUBLISH; }
    }

    record ApproveParty(UUID request, UUID id, int revision, UUID player) implements PartyCommand {
        public ApproveParty { Objects.requireNonNull(request); Objects.requireNonNull(id); Objects.requireNonNull(player); }
        public SmpActionKind kind() { return SmpActionKind.APPROVE; }
        public SmpPlayerRequest input() { return new SmpPlayerRequest(player); }
    }

    record RemoveParty(UUID request, UUID id, int revision, UUID player) implements PartyCommand {
        public RemoveParty { Objects.requireNonNull(request); Objects.requireNonNull(id); Objects.requireNonNull(player); }
        public SmpActionKind kind() { return SmpActionKind.REMOVE; }
        public SmpPlayerRequest input() { return new SmpPlayerRequest(player); }
    }

    record TransferParty(UUID request, UUID id, int revision, UUID player) implements PartyCommand {
        public TransferParty { Objects.requireNonNull(request); Objects.requireNonNull(id); Objects.requireNonNull(player); }
        public SmpActionKind kind() { return SmpActionKind.TRANSFER; }
        public SmpPlayerRequest input() { return new SmpPlayerRequest(player); }
    }

    record CancelParty(UUID request, UUID id, int revision) implements PartyCommand {
        public CancelParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.CANCEL; }
    }

    record DissolveParty(UUID request, UUID id, int revision) implements PartyCommand {
        public DissolveParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.DISSOLVE; }
    }

    record StartParty(UUID request, UUID id, int revision) implements PartyCommand {
        public StartParty { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.START; }
    }

    record NavigateEvent(UUID request, UUID id, int revision) implements EventCommand {
        public NavigateEvent { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.NAVIGATE; }
    }

    record TeleportToEvent(UUID request, UUID id, int revision) implements EventCommand {
        public TeleportToEvent { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.TELEPORT_EVENT; }
    }

    record JoinRaidEvent(UUID request, UUID id, int revision) implements EventCommand {
        public JoinRaidEvent { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.JOIN_RAID; }
    }

    record CreatePartyEvent(UUID request, UUID id, int revision) implements EventCommand {
        public CreatePartyEvent { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.CREATE_PARTY; }
    }

    record SoloEvent(UUID request, UUID id, int revision) implements EventCommand {
        public SoloEvent { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.SOLO; }
    }

    record FavoriteShop(UUID request, UUID id, int revision) implements ShopCommand {
        public FavoriteShop { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.FAVORITE; }
    }

    record BuyShop(UUID request, UUID id, int revision) implements ShopCommand {
        public BuyShop { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.BUY; }
    }

    record NavigateShop(UUID request, UUID id, int revision) implements ShopCommand {
        public NavigateShop { Objects.requireNonNull(request); Objects.requireNonNull(id); }
        public SmpActionKind kind() { return SmpActionKind.NAVIGATE; }
    }

    static SmpAction fromWire(UUID request, SmpTable table, UUID id, int revision,
                              SmpActionKind kind, SmpActionInput input) {
        if (table == SmpTable.PARTIES && kind == SmpActionKind.CREATE && input instanceof SmpPartyRequest party) {
            if (id != null || revision != 0) throw new IllegalArgumentException("Create does not target an existing party");
            return new CreateParty(request, party);
        }
        Objects.requireNonNull(id, "Operation requires an ID");
        if (kind.playerInput(table)) {
            if (!(input instanceof SmpPlayerRequest)) throw new IllegalArgumentException("Player input required");
        } else if (input != SmpNoInput.INSTANCE) throw new IllegalArgumentException("Unexpected action input");
        return switch (table) {
            case PARTIES -> switch (kind) {
                case NAVIGATE -> new NavigateParty(request, id, revision);
                case SOLO -> new SoloParty(request, id, revision);
                case MULTIPLAYER -> new MultiplayerParty(request, id, revision);
                case INVITE -> new InviteParty(request, id, revision, ((SmpPlayerRequest) input).player());
                case JOIN -> new JoinParty(request, id, revision);
                case ARRIVE -> new ArriveParty(request, id, revision);
                case LEAVE -> new LeaveParty(request, id, revision);
                case READY -> new ReadyParty(request, id, revision);
                case PUBLISH -> new PublishParty(request, id, revision);
                case APPROVE -> new ApproveParty(request, id, revision, ((SmpPlayerRequest) input).player());
                case REMOVE -> new RemoveParty(request, id, revision, ((SmpPlayerRequest) input).player());
                case TRANSFER -> new TransferParty(request, id, revision, ((SmpPlayerRequest) input).player());
                case CANCEL -> new CancelParty(request, id, revision);
                case DISSOLVE -> new DissolveParty(request, id, revision);
                case START -> new StartParty(request, id, revision);
                default -> throw new IllegalArgumentException("Unsupported operation");
            };
            case EVENTS -> switch (kind) {
                case NAVIGATE -> new NavigateEvent(request, id, revision);
                case TELEPORT_EVENT -> new TeleportToEvent(request, id, revision);
                case JOIN_RAID -> new JoinRaidEvent(request, id, revision);
                case CREATE_PARTY -> new CreatePartyEvent(request, id, revision);
                case SOLO -> new SoloEvent(request, id, revision);
                default -> throw new IllegalArgumentException("Unsupported operation");
            };
            case SHOPS -> switch (kind) {
                case FAVORITE -> new FavoriteShop(request, id, revision);
                case BUY -> new BuyShop(request, id, revision);
                case NAVIGATE -> new NavigateShop(request, id, revision);
                default -> throw new IllegalArgumentException("Unsupported operation");
            };
            default -> throw new IllegalArgumentException("Unsupported operation domain");
        };
    }

    default void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(request());
        buffer.writeEnum(table());
        buffer.writeNullable(id(), FriendlyByteBuf::writeUUID);
        buffer.writeVarInt(revision());
        buffer.writeEnum(kind());
        if (input() instanceof SmpPartyRequest party) {
            buffer.writeUtf(party.activity(), SmpProtocol.MAX_LANGUAGE);
            buffer.writeUtf(party.title(), SmpProtocol.MAX_TITLE);
            buffer.writeUtf(party.description(), SmpProtocol.MAX_DESCRIPTION);
            buffer.writeUtf(party.language(), SmpProtocol.MAX_LANGUAGE);
            buffer.writeVarInt(party.slots());
            buffer.writeLong(party.scheduled());
            buffer.writeBoolean(party.approval());
            buffer.writeBoolean(party.solo());
        } else if (input() instanceof SmpMessageRequest message) {
            buffer.writeUtf(message.message(), SmpProtocol.MAX_MESSAGE);
        } else if (input() instanceof SmpPlayerRequest player) {
            buffer.writeUUID(player.player());
        }
    }

    public static SmpAction decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > SmpProtocol.MAX_REQUEST_BYTES)
            throw new io.netty.handler.codec.DecoderException("SMP action exceeds byte budget");
        UUID request = buffer.readUUID();
        var table = buffer.readEnum(SmpTable.class);
        UUID id = buffer.readNullable(FriendlyByteBuf::readUUID);
        int revision = buffer.readVarInt();
        var kind = buffer.readEnum(SmpActionKind.class);
        SmpActionInput input = kind == SmpActionKind.CREATE
                ? new SmpPartyRequest(buffer.readUtf(SmpProtocol.MAX_LANGUAGE), buffer.readUtf(SmpProtocol.MAX_TITLE),
                    buffer.readUtf(SmpProtocol.MAX_DESCRIPTION), buffer.readUtf(SmpProtocol.MAX_LANGUAGE),
                    buffer.readVarInt(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean())
                : kind == SmpActionKind.MESSAGE ? new SmpMessageRequest(buffer.readUtf(SmpProtocol.MAX_MESSAGE))
                : kind.playerInput(table) ? new SmpPlayerRequest(buffer.readUUID()) : SmpNoInput.INSTANCE;
        if (buffer.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing SMP action data");
        return fromWire(request, table, id, revision, kind, input);
    }
}
