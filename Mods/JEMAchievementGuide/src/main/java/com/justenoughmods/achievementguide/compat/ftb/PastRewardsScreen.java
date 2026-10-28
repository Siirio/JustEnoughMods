package com.justenoughmods.achievementguide.compat.ftb;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** Uses the audited native reward-choice list, reward icons and reward tooltips without another chapter. */
public final class PastRewardsScreen extends AbstractButtonListScreen {
    private final PastRewardsNetwork.Snapshot snapshot;
    private PastRewardsScreen(PastRewardsNetwork.Snapshot snapshot) {
        this.snapshot = snapshot;
        setTitle(Component.translatable("jem.questbook.past_rewards"));
        setHasSearchBox(true);
        showBottomPanel(false);
        showCloseButton(true);
    }
    @Override protected void doCancel() { ClientQuestFile.openGui(); }
    @Override protected void doAccept() { ClientQuestFile.openGui(); }
    public static void open(PastRewardsNetwork.Snapshot snapshot) {
        if (PastRewardsNetwork.enabled() && ClientQuestFile.INSTANCE != null) new PastRewardsScreen(snapshot).openGui();
    }
    @Override public void addButtons(Panel panel) {
        var file = ClientQuestFile.INSTANCE;
        if (snapshot.rows().isEmpty()) panel.add(new SimpleTextButton(panel, Component.translatable("jem.questbook.past_rewards.empty"), Icon.empty()) {
            @Override public boolean isEnabled() { return false; }
            @Override public void onClicked(MouseButton button) { }
        });
        for (PastRewardsNetwork.Row row : snapshot.rows()) {
            var claim = row.claim();
            if (!claim.available()) {
                panel.add(new SimpleTextButton(panel, Component.translatable("jem.questbook.past_rewards.unavailable"), ItemIcon.getItemIcon(Items.BOOK.getDefaultInstance())) {
                    @Override public boolean isEnabled() { return false; }
                    @Override public void onClicked(MouseButton button) { }
                });
                continue;
            }
            var quest = file.getQuest(claim.quest());
            Reward parent = file.getReward(claim.parent());
            Reward reward = parent;
            if (parent instanceof ChoiceReward choice && choice.getTable() != null) reward = choice.getTable().getWeightedRewards().stream()
                    .map(weight -> weight.getReward()).filter(option -> option.id == claim.option()).findFirst().orElse(null);
            if (quest == null || reward == null) continue;
            Reward presented = reward;
            Component title = quest.getTitle().copy().append(" / ").append(row.compensation().isEmpty() ? reward.getTitle() : row.compensation().getHoverName());
            panel.add(new SimpleTextButton(panel, title, row.compensation().isEmpty() ? reward.getIcon() : ItemIcon.getItemIcon(row.compensation())) {
                private long submittingUntil;
                @Override public boolean isEnabled() { return claim.available() && System.currentTimeMillis() >= submittingUntil; }
                @Override public void addMouseOverText(TooltipList list) {
                    super.addMouseOverText(list);
                    presented.addMouseOverText(list);
                    if (claim.compensation()) list.translate("jem.questbook.past_rewards.compensation");
                }
                @Override public void onClicked(MouseButton button) {
                    if (!isEnabled()) return;
                    submittingUntil = System.currentTimeMillis() + 350;
                    playClickSound();
                    PastRewardsNetwork.claim(claim, snapshot.offset());
                }
            });
        }
        if (snapshot.offset() > 0) panel.add(new SimpleTextButton(panel, Component.translatable("gui.back"), Icon.empty()) {
            @Override public void onClicked(MouseButton button) { PastRewardsNetwork.request(Math.max(0, snapshot.offset() - 64)); }
        });
        if (snapshot.rows().size() == 64) panel.add(new SimpleTextButton(panel, Component.translatable("spectatorMenu.next_page"), Icon.empty()) {
            @Override public void onClicked(MouseButton button) { PastRewardsNetwork.request(snapshot.offset() + 64); }
        });
    }
}
