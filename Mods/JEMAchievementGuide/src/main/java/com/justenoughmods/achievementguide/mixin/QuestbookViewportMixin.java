package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.QuestbookCamera;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.ChapterPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Initial presentation only, using FTB's own camera, widget geometry and persistence. */
@Mixin(value = QuestScreen.class, remap = false)
public abstract class QuestbookViewportMixin {
    @Shadow @Final private ClientQuestFile file;
    @Shadow @Final public QuestPanel questPanel;
    @Shadow @Final private ChapterPanel chapterPanel;
    @Shadow private Chapter selectedChapter;
    @Shadow private int zoom;
    @Shadow public abstract double getQuestButtonSpacing();

    @Unique private static final Map<ClientQuestFile, Map<Long, QuestbookCamera>> jem$cameras = new WeakHashMap<>();
    @Unique private boolean jem$pendingEntry = true;
    @Unique private boolean jem$cameraReady;

    @Unique private boolean jem$isRescueChapter() {
        return selectedChapter != null && selectedChapter.hasTag("jem_rescue_v3");
    }

    @Unique private void jem$rememberCamera() {
        if (jem$cameraReady && jem$isRescueChapter()) {
            var camera = (QuestPanelCameraAccess)questPanel;
            jem$cameras.computeIfAbsent(file, ignored -> new HashMap<>())
                    .put(selectedChapter.id, new QuestbookCamera(zoom, camera.jem$getCenterX(), camera.jem$getCenterY()));
        }
    }

    @Inject(method = "selectChapter", at = @At("HEAD"), remap = false)
    private void jem$leave(Chapter next, CallbackInfo callback) {
        if (next != selectedChapter) {
            jem$rememberCamera();
            jem$pendingEntry = true;
            jem$cameraReady = false;
        }
    }

    @Inject(method = "onClosed", at = @At("HEAD"), remap = false)
    private void jem$rememberClose(CallbackInfo callback) {
        jem$rememberCamera();
    }

    @Inject(method = "open", at = @At("TAIL"), remap = false)
    private void jem$explicitQuestTarget(QuestObject object, boolean focus, CallbackInfo callback) {
        // Native search and #quest links have already focused their exact target.
        if (jem$isRescueChapter() && (object instanceof Quest || object instanceof QuestLink)) {
            jem$pendingEntry = false;
            jem$cameraReady = true;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$enter(CallbackInfo callback) {
        if (!jem$pendingEntry || !jem$isRescueChapter() || questPanel.width < 100 || questPanel.height < 100) return;
        var saved = jem$cameras.getOrDefault(file, Map.of()).get(selectedChapter.id);
        double centerX;
        double centerY;
        if (saved != null) {
            zoom = saved.zoom();
            centerX = saved.x();
            centerY = saved.y();
        } else {
            // The central content window is independent of decorative image bounds.
            int sidebar = Math.max(0, chapterPanel.getX() + chapterPanel.width - questPanel.getX());
            int availableWidth = Math.max(160, questPanel.width - sidebar - 12);
            double unit = Math.min(availableWidth * .84 / 34D, questPanel.height * .84 / 24D);
            double nativeUnitPerZoom = 1.5D + getQuestButtonSpacing() / Math.max(1, zoom);
            zoom = Math.max(4, Math.min(16, (int)Math.round(unit / nativeUnitPerZoom)));
            double actualUnit = zoom * 1.5D + getQuestButtonSpacing();
            centerX = .5 - sidebar / (2D * actualUnit);
            centerY = 0;
        }
        // Align at the restored zoom before using native scroll extents. Store
        // only camera data, so sidebar toggles and edit selections stay native.
        questPanel.refreshWidgets();
        questPanel.alignWidgets();
        questPanel.scrollTo(centerX, centerY);
        var camera = (QuestPanelCameraAccess)questPanel;
        camera.jem$setCenterX(centerX);
        camera.jem$setCenterY(centerY);
        jem$pendingEntry = false;
        jem$cameraReady = true;
    }
}
