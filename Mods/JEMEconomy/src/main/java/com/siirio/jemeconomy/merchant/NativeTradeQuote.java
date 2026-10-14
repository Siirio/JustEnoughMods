package com.siirio.jemeconomy.merchant;

public record NativeTradeQuote(int offerIndex, Direction direction, long copper, boolean available, String reasonKey) {
    public enum Direction { PLAYER_BUYS, PLAYER_SELLS, ITEM_BARTER, UNPRICED }
}
