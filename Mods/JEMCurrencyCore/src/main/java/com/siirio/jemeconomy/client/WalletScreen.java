package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.network.EconomyNetwork;
import com.siirio.jemeconomy.network.WalletActionPacket;
import com.siirio.jemeconomy.network.WalletStatusPacket;
import com.siirio.jemeconomy.physical.CoinDenomination;
import com.siirio.jemcurrency.CurrencyItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class WalletScreen extends Screen {
    private WalletStatusPacket status;
    private EditBox amount;
    private EditBox recipient;
    public WalletScreen(WalletStatusPacket status) { super(Component.translatable("jem.economy.wallet.title")); this.status = status; }
    public void accept(WalletStatusPacket next) { status = next; }

    @Override protected void init() {
        int x = width / 2 - 150, y = height / 2 - 104;
        amount = new EditBox(font, x + 16, y + 104, 168, 20, Component.translatable("jem.economy.wallet.amount"));
        amount.setHint(Component.translatable("jem.economy.wallet.amount"));
        amount.setFilter(value -> value.isBlank() || value.matches("[0-9]{1,10}"));
        addRenderableWidget(amount);
        addRenderableWidget(Button.builder(Component.translatable("jem.economy.wallet.withdraw"), button -> {
            try { long value = Long.parseLong(amount.getValue()); EconomyNetwork.action(WalletActionPacket.Action.WITHDRAW, value); }
            catch (NumberFormatException ignored) { }
        }).bounds(x + 192, y + 104, 92, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("jem.economy.wallet.deposit_all"), button ->
                EconomyNetwork.action(WalletActionPacket.Action.DEPOSIT_ALL, 0L)).bounds(x + 16, y + 132, 268, 20).build());
        recipient = new EditBox(font, x + 16, y + 160, 168, 20, Component.translatable("jem.economy.wallet.recipient"));
        recipient.setHint(Component.translatable("jem.economy.wallet.recipient")); recipient.setMaxLength(16); addRenderableWidget(recipient);
        addRenderableWidget(Button.builder(Component.translatable("jem.economy.wallet.send"), button -> {
            try { EconomyNetwork.action(WalletActionPacket.Action.SEND, Long.parseLong(amount.getValue()), recipient.getValue()); }
            catch (NumberFormatException ignored) { }
        }).bounds(x + 192, y + 160, 92, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 150, y = height / 2 - 104;
        graphics.fill(x, y, x + 300, y + 238, 0xEF2C211B);
        graphics.fill(x, y, x + 300, y + 3, 0xFFC27A3B);
        graphics.drawCenteredString(font, title, width / 2, y + 10, 0xFFFFE6C2);
        graphics.drawString(font, Component.translatable("jem.economy.wallet.digital_label"), x + 16, y + 30, 0xFFFFD18A, false);
        graphics.drawString(font, Component.translatable("jem.economy.wallet.cash_label"), x + 16, y + 44, 0xFFD9C6A9, false);
        CoinPriceRenderer.draw(graphics, font, status.balance(), x + 146, y + 25, 138, 0xFFFFD18A);
        CoinPriceRenderer.draw(graphics, font, status.heldValue(), x + 146, y + 41, 138, 0xFFD9C6A9);
        int[] counts = status.counts();
        for (CoinDenomination denomination : CoinDenomination.values()) {
            int index = denomination.ordinal(), iconX = x + 18 + index * 54;
            graphics.renderItem(new ItemStack(CurrencyItems.item(denomination)), iconX, y + 62);
            graphics.drawString(font, "×" + counts[index], iconX + 18, y + 67, 0xFFE8D8BF, false);
        }
        if (status.invalid() > 0) graphics.drawString(font, Component.translatable("jem.economy.wallet.invalid", status.invalid()), x + 16, y + 88, 0xFFFF7777, false);
        graphics.drawString(font, Component.translatable("jem.economy.wallet.demand", Component.translatable("jem.economy.demand." + status.primaryDemand()),
                Component.translatable("jem.economy.demand." + status.secondaryDemand())), x + 16, y + 188, 0xFFFFD18A, false);
        graphics.drawString(font, Component.translatable("jem.economy.wallet.limits_label"), x + 16, y + 202, 0xFFD9C6A9, false);
        CoinPriceRenderer.draw(graphics, font, status.accountRemaining(), x + 140, y + 198, 65, 0xFFD9C6A9);
        CoinPriceRenderer.draw(graphics, font, status.globalRemaining(), x + 213, y + 198, 71, 0xFFD9C6A9);
        graphics.drawString(font, Component.translatable("jem.economy.wallet.market_update", com.siirio.jemidentity.api.MinecraftDays.format(status.ticksUntilUpdate())), x + 16, y + 216, 0xFFD9C6A9, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String grouped(long value) { return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', ' '); }
}
