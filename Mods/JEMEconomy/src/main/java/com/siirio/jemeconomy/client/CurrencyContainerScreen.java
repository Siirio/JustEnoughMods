package com.siirio.jemeconomy.client;

import com.siirio.jemeconomy.network.ContainerCurrencyStatusPacket;

public interface CurrencyContainerScreen {
    void acceptCurrency(ContainerCurrencyStatusPacket status);
}
