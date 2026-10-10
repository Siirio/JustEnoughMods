package com.siirio.jemtwelveeyes.client;

import java.util.List;
import net.minecraft.util.FormattedCharSequence;

record GateWarningLayout(List<FormattedCharSequence> titleLines, List<FormattedCharSequence> detailLines,
                         float scale, int panelWidth, int panelHeight) {
}
