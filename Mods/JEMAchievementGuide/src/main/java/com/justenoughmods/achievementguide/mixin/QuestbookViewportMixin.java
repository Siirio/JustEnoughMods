package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.client.questbook.QuestbookCamera;
import com.justenoughmods.achievementguide.client.questbook.QuestbookLayout;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
    @Unique private int jem$viewportWidth;
    @Unique private int jem$viewportHeight;
    @Unique private int jem$sidebar;
    @Unique private int jem$layoutRevision = -1;
    @Unique private boolean jem$targetFocused;
    @Unique private boolean jem$overviewActive;
    @Unique private boolean jem$overviewUnsupported;
    @Unique private double jem$lastScrollX;
    @Unique private double jem$lastScrollY;

    @Unique private QuestbookLayout.Board jem$board() {
        return selectedChapter != null && selectedChapter.hasTag("jem_visual_v4")
                ? QuestbookLayout.board(selectedChapter.id) : null;
    }

    @Unique private int jem$visibleSidebar() {
        return Math.max(0, Math.min(questPanel.width,
                chapterPanel.getX() + chapterPanel.width - questPanel.getX()));
    }

    @Inject(method = "getQuestButtonSpacing", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$authoredSpacing(CallbackInfoReturnable<Double> callback) {
        // V4 uses the native spacing parameter4 instead of default8. Node/icon
        // sizes stay native, while readable zoom4 fits the authored overview.
        if (selectedChapter != null && selectedChapter.hasTag("jem_visual_v4")) {
            callback.setReturnValue((double)zoom);
        }
    }

    @Unique private int jem$fitZoom(QuestbookLayout.Board board) {
        double unitPerZoom = 1.5D + getQuestButtonSpacing() / Math.max(1, zoom);
        // Cover the complete visible viewport. Fitting the whole illustration with
        // Math.min letterboxed it and exposed pixels beyond the authored background.
        double unit = Math.max((questPanel.width - jem$visibleSidebar() + 1D) / (board.maxX() - board.minX()),
                (questPanel.height + 1D) / (board.maxY() - board.minY()));
        int fit = (int)Math.ceil(unit / unitPerZoom);
        jem$overviewUnsupported = fit > 28;
        return Math.max(4, Math.min(28, fit));
    }

    @Unique private void jem$overview(QuestbookLayout.Board board) {
        zoom = jem$fitZoom(board);
        questPanel.refreshWidgets();
        questPanel.alignWidgets();
        double unit = zoom * 1.5D + getQuestButtonSpacing();
        double x = (board.minX() + board.maxX()) / 2D - jem$visibleSidebar() / (2D * unit);
        double y = (board.minY() + board.maxY()) / 2D;
        jem$scroll(x, y);
        jem$targetFocused = false;
        jem$overviewActive = true;
        jem$lastScrollX = questPanel.getScrollX();
        jem$lastScrollY = questPanel.getScrollY();
    }

    @Unique private void jem$scroll(double x, double y) {
        questPanel.scrollTo(x, y);
        var camera = (QuestPanelCameraAccess)questPanel;
        camera.jem$setCenterX(x);
        camera.jem$setCenterY(y);
    }

    @Unique private void jem$clamp(QuestbookLayout.Board board) {
        var camera = (QuestPanelCameraAccess)questPanel;
        double unit = zoom * 1.5D + getQuestButtonSpacing();
        // Read scroll itself, because native drag updates it between draw frames.
        double x = camera.jem$getMinX() + (questPanel.getScrollX() + questPanel.width / 2D) / unit;
        double y = camera.jem$getMinY() + (questPanel.getScrollY() + questPanel.height / 2D) / unit;
        double left = board.minX() + (questPanel.width / 2D - jem$visibleSidebar()) / unit;
        double right = board.maxX() - questPanel.width / (2D * unit);
        double top = board.minY() + questPanel.height / (2D * unit);
        double bottom = board.maxY() - questPanel.height / (2D * unit);
        double nextX = left > right ? (board.minX() + board.maxX()) / 2D - jem$visibleSidebar() / (2D * unit)
                : Math.max(left, Math.min(right, x));
        double nextY = top > bottom ? (board.minY() + board.maxY()) / 2D : Math.max(top, Math.min(bottom, y));
        if (Math.abs(nextX - x) > .001 || Math.abs(nextY - y) > .001) jem$scroll(nextX, nextY);
    }

    @Inject(method = "addZoom", at = @At("TAIL"), remap = false)
    private void jem$limitZoomOut(double delta, CallbackInfo callback) {
        var board = jem$board();
        if (board == null || file.canEdit()) return;
        if (delta != 0) jem$overviewActive = false;
        int minimum = jem$fitZoom(board);
        if (zoom < minimum) {
            zoom = minimum;
            questPanel.withPreservedPos(panel -> panel.alignWidgets());
        }
        jem$clamp(board);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    private void jem$returnOverview(Key key, CallbackInfoReturnable<Boolean> callback) {
        var board = jem$board();
        if (board != null && !file.canEdit() && key.is(GLFW.GLFW_KEY_HOME)) {
            jem$overview(board);
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "addMouseOverText", at = @At("TAIL"), remap = false)
    private void jem$overviewHint(TooltipList tooltip, CallbackInfo callback) {
        if (jem$board() != null && !file.canEdit()) {
            if (jem$targetFocused) tooltip.add(Component.translatable("jem.questbook.overview.home"));
            if (jem$overviewUnsupported) tooltip.add(Component.translatable("jem.questbook.overview.gui_scale"));
        }
    }

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
            jem$targetFocused = false;
        }
    }

    @Inject(method = "onClosed", at = @At("HEAD"), remap = false)
    private void jem$rememberClose(CallbackInfo callback) {
        jem$rememberCamera();
    }

    @Inject(method = "open", at = @At("TAIL"), remap = false)
    private void jem$explicitQuestTarget(QuestObject object, boolean focus, CallbackInfo callback) {
        // Native search and #quest links have already focused their exact target.
        if ((jem$isRescueChapter() || jem$board() != null) && (object instanceof Quest || object instanceof QuestLink)) {
            jem$pendingEntry = false;
            jem$cameraReady = true;
            jem$targetFocused = true;
            jem$overviewActive = false;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void jem$enter(CallbackInfo callback) {
        var board = jem$board();
        if (board != null && !file.canEdit() && questPanel.width > 0 && questPanel.height > 0) {
            int sidebar = jem$visibleSidebar();
            boolean changed = jem$viewportWidth != questPanel.width || jem$viewportHeight != questPanel.height
                    || jem$sidebar != sidebar || jem$layoutRevision != QuestbookLayout.revision();
            if (!jem$pendingEntry && !changed && (Math.abs(questPanel.getScrollX() - jem$lastScrollX) > 1
                    || Math.abs(questPanel.getScrollY() - jem$lastScrollY) > 1)) jem$overviewActive = false;
            if (jem$pendingEntry || changed) {
                if (jem$pendingEntry || !jem$cameraReady || jem$overviewActive) {
                    jem$overview(board);
                } else {
                    zoom = Math.max(zoom, jem$fitZoom(board));
                    questPanel.withPreservedPos(panel -> panel.alignWidgets());
                }
                jem$viewportWidth = questPanel.width;
                jem$viewportHeight = questPanel.height;
                jem$sidebar = sidebar;
                jem$layoutRevision = QuestbookLayout.revision();
                jem$pendingEntry = false;
                jem$cameraReady = true;
            }
            jem$clamp(board);
            jem$lastScrollX = questPanel.getScrollX();
            jem$lastScrollY = questPanel.getScrollY();
            return;
        }
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
