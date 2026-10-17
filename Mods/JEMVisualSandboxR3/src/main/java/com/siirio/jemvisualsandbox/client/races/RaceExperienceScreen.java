package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import com.siirio.jemvisualsandbox.workbench.SandboxNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/** A live skin in an unlit void, followed by an acknowledged, real-time 3D entry. */
public final class RaceExperienceScreen extends Screen {
    private static final String[] RACES = {"ancestors", "void_walkers", "born_in_bloom", "infected_by_sculk", "nether_hearteds"};
    private static final int[] ACCENTS = {0xE6D29D, 0xBBA6EE, 0x9ED7A1, 0x7CD6D5, 0xF1AB76};
    private static final long ENTRY_MS = 3000;
    private final boolean mandatory;
    private RaceExperience.Status status;
    private int race;
    private int requested = -1;
    private int scroll;
    private int contentHeight;
    private boolean progression;
    private boolean hiddenHud;
    private boolean hudCaptured;
    private long switchedAt = System.nanoTime();
    private final long openedAt=System.nanoTime();
    private long entryAt;
    private long requestedAt;
    private boolean rejected;
    private float turnDirection = 1;
    private Button confirm;
    private Button previous;
    private Button next;
    private Button tab;
    private Button developerTier;
    private final List<Button> bloomUnlocks = new ArrayList<>();

    RaceExperienceScreen(RaceExperience.Status status, boolean mandatory, boolean progression) {
        super(Component.translatable("jem.roles.screen.title"));
        this.status = status;
        this.mandatory = mandatory;
        this.progression = progression;
        this.race = Mth.clamp(status.race(), 0, 4);
    }

    @Override protected void init() {
        bloomUnlocks.clear();
        developerTier = null;
        if (!hudCaptured) {
            hiddenHud = minecraft.options.hideGui;
            hudCaptured = true;
            minecraft.options.hideGui = true;
            SandboxNetwork.cinematic(true);
            net.minecraft.client.KeyMapping.releaseAll();
        }
        int bottom = height - 30;
        int stage = panelLeft();
        previous = addRenderableWidget(new QuietButton(14, height / 2 - 12, 26, 24, Component.literal("‹"), b -> switchRace(-1)));
        next = addRenderableWidget(new QuietButton(stage - 39, height / 2 - 12, 26, 24, Component.literal("›"), b -> switchRace(1)));
        tab = addRenderableWidget(new QuietButton(panelLeft() + 12, 15, panelWidth() - 24, 22,
                text(progression ? "Происхождение" : "Умения и развитие", progression ? "Origin" : "Abilities & progression"), b -> {
                    progression = !progression; scroll = 0; rebuildWidgets();
                }));
        confirm = addRenderableWidget(new QuietButton(panelLeft() + 12, bottom, panelWidth() - 24, 22,
                Component.translatable("jem.roles.select"), b -> confirm()));
        if (status.developerMode()) developerTier = addRenderableWidget(new QuietButton(12, height - 28, Math.max(90, stage - 24), 20,
                text("DEV · следующий уровень", "DEV · next tier"), b -> SandboxNetwork.developerTier(Math.min(3, tier() + 1))));
        if (progression && race == 2 && status.selected() && status.race() == 2) {
            int cell = Math.max(24, (stage - 24) / 3);
            for (int i = 1; i <= 3; i++) {
                final int unlock = i;
                bloomUnlocks.add(addRenderableWidget(new QuietButton(12 + (i - 1) * cell,
                        height - (status.developerMode() ? 51 : 28), cell - 2, 20,
                        Component.literal(new String[]{"I", "II", "III"}[i - 1]), b -> SandboxNetwork.upgrade(unlock))));
            }
        }
        updateButtons();
    }

    public void receive(RaceExperience.Status value) {
        boolean developerChanged = status.developerMode() != value.developerMode();
        status = value;
        if (requested >= 0 && value.selected() && value.race() == requested) {
            entryAt = System.nanoTime();
            requested = -1;
        } else if (requested >= 0) {
            requested = -1;
            rejected = true;
        }
        if (developerChanged) rebuildWidgets();
        updateButtons();
    }

    private void confirm() {
        if (requested >= 0 || entryAt != 0) return;
        requested = race;
        requestedAt = System.nanoTime();
        rejected = false;
        SandboxNetwork.select(race);
        updateButtons();
    }

    private void switchRace(int direction) {
        if (requested >= 0 || entryAt != 0) return;
        race = Math.floorMod(race + direction, RACES.length);
        turnDirection = direction;
        switchedAt = System.nanoTime();
        scroll = 0;
        rejected = false;
        rebuildWidgets();
    }

    private void updateButtons() {
        if (confirm == null) return;
        boolean busy = requested >= 0 || entryAt != 0;
        previous.active = next.active = tab.active = !busy;
        confirm.active = !busy;
        confirm.setMessage(requested >= 0 ? text("Сервер подтверждает…", "Waiting for server…")
                : rejected ? text("Не подтверждено · повторить", "Not confirmed · retry")
                : status.selected() && status.race() == race ? text("Вернуться в мир", "Enter world")
                : Component.translatable("jem.roles.select"));
        if (developerTier != null) developerTier.active = !busy && status.selected() && status.race() == race && tier() < 3;
        for (int i = 0; i < bloomUnlocks.size(); i++) {
            int nextTier = i + 1;
            Button unlock = bloomUnlocks.get(i);
            unlock.active = !busy && nextTier == status.bloomUpgrade() + 1 && nextTier <= tier();
            unlock.setMessage(Component.literal((nextTier <= status.bloomUpgrade() ? "✓ " : "") + new String[]{"I", "II", "III"}[i]));
        }
    }

    @Override public void tick() {
        minecraft.options.hideGui = true;
        if (requested >= 0 && elapsed(requestedAt) > 10_000) {
            requestedAt = System.nanoTime();
            SandboxNetwork.sync();
        }
        if (entryAt != 0 && elapsed(entryAt) >= ENTRY_MS) {
            entryAt = 0;
            minecraft.setScreen(null);
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float entry = entryAt == 0 ? 0 : Mth.clamp(elapsed(entryAt) / (float) ENTRY_MS, 0, 1);
        float reveal = smooth((entry - .90F) / .10F);
        float opening=smooth(elapsed(openedAt)/450F);
        int alpha = (int) (255 * (entryAt==0?opening:1-reveal));
        if (alpha > 0) g.fill(0, 0, width, height, alpha << 24);
        if(opening<1&&entryAt==0)return;
        renderSkin(g, entry, reveal);
        if (entryAt != 0&&entry>=.12F) return;

        int left = panelLeft(), right = width - 16;
        int accent = ACCENTS[race];
        g.drawCenteredString(font,text("ВЫБЕРИ СВОЁ ПРОИСХОЖДЕНИЕ", "CHOOSE YOUR ORIGIN"),panelLeft()/2,20,0xD9D6CE);
        g.drawString(font, String.format("%02d / 05", race + 1), 20, 35, accent, false);
        int nameY = Math.max(56, height - 81);
        int nameWidth = Math.max(70, left - 40);
        List<FormattedCharSequence> name = font.split(tr("name"), nameWidth);
        for (int i = 0; i < name.size(); i++) g.drawCenteredString(font, name.get(i), left / 2, nameY + i * 11, accent);
        if (height > 260) g.drawCenteredString(font, tr("lore"), left / 2, nameY + name.size() * 11 + 5, 0xA9A9A9);
        g.fill(left, 49, left + 1, height - 42, 0xFF282828);

        String[] icons={"life_chorus","rift_ambush","verdant_dominion","sculk_veins","infernal_eruption"};
        g.blit(new net.minecraft.resources.ResourceLocation("jem_visual_sandbox","textures/gui/spells/"+icons[race]+".png"),left+12,52,0,0,16,16,16,16);
        g.drawString(font,tr("spell.short"),left+34,56,accent,false);
        int top = 75, bottom = height - 41;
        g.enableScissor(left + 9, top, right + 3, bottom);
        List<Line> lines = progression ? progressionLines() : originLines();
        int y = top - scroll;
        for (Line line : lines) {
            if (line.text() == null) { y += 7; continue; }
            for (FormattedCharSequence wrapped : font.split(line.text(), Math.max(30, right - left - 18))) {
                g.drawString(font, wrapped, left + 12, y, line.color(), false);
                y += 11;
            }
            y += 3;
        }
        contentHeight = y - top + scroll;
        g.disableScissor();
        int viewport = Math.max(1, bottom - top);
        int maxScroll = Math.max(0, contentHeight - viewport);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        if (maxScroll > 0) {
            int thumb = Math.max(12, viewport * viewport / contentHeight);
            int thumbY = top + (viewport - thumb) * scroll / maxScroll;
            g.fill(width - 8, top, width - 7, bottom, 0xFF242424);
            g.fill(width - 8, thumbY, width - 7, thumbY + thumb, 0xFF000000 | accent);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if(entryAt!=0){
            int fade=((int)(255*smooth(entry/.12F)))<<24;
            g.fill(panelLeft(),0,width,height,fade);g.fill(0,0,panelLeft(),50,fade);
            g.fill(0,height-86,panelLeft(),height,fade);
            for(Button button:new Button[]{previous,next})g.fill(button.getX(),button.getY(),button.getX()+button.getWidth(),button.getY()+button.getHeight(),fade);
        }
    }

    private void renderSkin(GuiGraphics g, float entry, float reveal) {
        RaceStageRenderer.render(g, width, height, panelLeft(), ACCENTS[race], entry, reveal,
                smooth(elapsed(switchedAt) / 340F), turnDirection);
    }

    private List<Line> originLines() {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(tr("lore"), ACCENTS[race]));
        lines.add(Line.gap());
        lines.add(new Line(Component.translatable("jem.roles.strengths"), 0xB5DBA4));
        for (int i = 1; i <= 5; i++) {
            lines.add(new Line(Component.literal("+ ").append(tr("buff." + i)), 0xE5E5E5));
            lines.add(new Line(tr("buff.detail." + i), 0xB2B2B2));
        }
        lines.add(Line.gap());
        lines.add(new Line(Component.translatable("jem.roles.weaknesses"), 0xE3A093));
        for (int i = 1; i <= 2; i++) {
            lines.add(new Line(Component.literal("− ").append(tr("debuff." + i)), 0xE5C0B7));
            lines.add(new Line(tr("debuff.detail." + i), 0xB2B2B2));
        }
        return lines;
    }

    private List<Line> progressionLines() {
        List<Line> lines = new ArrayList<>();
        boolean chosen = status.selected() && status.race() == race;
        lines.add(new Line(text("АКТИВНОЕ УМЕНИЕ · V", "ACTIVE ABILITY · V"), ACCENTS[race]));
        lines.add(new Line(tr("spell.short"), 0xF1F1F1));
        lines.add(new Line(tr("spell.detail"), 0xBEBEBE));
        lines.add(new Line(text("B / Shift+B — контекстные действия; см. происхождение.",
                "B / Shift+B — contextual actions; see Origin."), 0xA5A5A5));
        lines.add(Line.gap());
        lines.add(new Line(text("ПАССИВНЫЕ ЧЕРТЫ", "PASSIVE TRAITS"), ACCENTS[race]));
        lines.add(new Line(text("Все пять преимуществ и две слабости — во вкладке «Происхождение».",
                "All five strengths and two weaknesses are listed under Origin."), 0xBEBEBE));
        lines.add(Line.gap());
        lines.add(new Line(text("РАЗВИТИЕ", "PROGRESSION"), ACCENTS[race]));
        for (int i = 1; i <= 3; i++) lines.add(new Line(Component.literal(i <= tier() ? "◆ " : "◇ ")
                .append(tr("tier.short." + i)), i <= tier() ? 0xD4EAC5 : 0x767676));
        lines.add(new Line(Component.translatable("jem.roles.progress", progress(), 30), 0xDEDEDE));
        lines.add(new Line(tr("mastery.detail"), 0xB2B2B2));
        if (race == 2) {
            lines.add(Line.gap());
            lines.add(new Line(text("УЛУЧШЕНИЯ КОРНЕЙ", "ROOT UPGRADES"), ACCENTS[race]));
            lines.add(new Line(text("I · блокировка действий; II · 3 урона/сек.; III · яд.",
                    "I · action lock; II · 3 damage/sec; III · poison."), 0xDEDEDE));
            lines.add(new Line(text("Открыты: " + status.bloomUpgrade() + "/3. Кнопки I–III открывают следующий доступный этап без пропусков.",
                    "Unlocked: " + status.bloomUpgrade() + "/3. Buttons I–III unlock the next available stage in order."), 0xB2B2B2));
        }
        if (chosen) {
            lines.add(Line.gap());
            lines.add(new Line(text("ПЕРЕЗАРЯДКА", "COOLDOWNS"), ACCENTS[race]));
            lines.add(new Line(cooldown("V", status.signatureRemaining()), 0xDEDEDE));
            lines.add(new Line(cooldown("B", status.secondaryRemaining()), 0xDEDEDE));
        } else lines.add(new Line(text("Предпросмотр: эта раса сейчас не выбрана.", "Preview: this race is not currently selected."), 0xA5A5A5));
        if (status.developerMode()) {
            lines.add(Line.gap());
            lines.add(new Line(text("DEV включён явно. Кнопка уровня отправляет запрос серверу.",
                    "DEV explicitly enabled. The tier button requests a server change."), 0xE2BD88));
        }
        return lines;
    }

    private Component cooldown(String key, long ticks) {
        long remaining = RaceExperienceClient.remaining(ticks);
        return Component.literal(key + " · ").append(remaining == 0 ? text("готово", "ready")
                : Component.literal(String.format(java.util.Locale.ROOT, "%.1f s", remaining / 20.0)));
    }

    private int tier() { return Mth.clamp(status.tiers()[race], 1, 3); }
    private int progress() { return Math.max(0, status.progress()[race]); }
    private Component tr(String suffix) {
        if(!minecraft.hasSingleplayerServer())return Component.translatable("jem.roles.race."+RACES[race]+"."+suffix);
        if(race==0&&suffix.equals("spell.detail"))return text("Золотой фронт восстанавливает игрокам 3 сердца, полностью исцеляет мирных животных и текущего ездового питомца. Четыре щита уменьшают входящий урон на 25% на 5 секунд. Перезарядка: 25 секунд.","A golden front restores 3 hearts to players and fully heals peaceful animals and your living mount. Four wards reduce incoming damage by 25% for 5 seconds. Cooldown: 25 seconds.");
        // Explicit sandbox contracts differ from production only in these authored abilities.
        if (race == 3) {
            if (suffix.equals("spell.short") || suffix.equals("buff.2")) return text("Звуковая волна [V]", "Sonic Wave [V]");
            if (suffix.equals("spell.detail") || suffix.equals("buff.detail.2")) return text(
                    "Расходящаяся звуковая волна поражает враждебных существ в радиусе 8 блоков: 12 урона, один раз по каждой цели.",
                    "An expanding sonic wave strikes hostile creatures within 8 blocks: 12 damage, once per target.");
            if (suffix.equals("tier.short.1"))return text("I · 12 урона, радиус 8", "I · 12 damage, radius 8");
            if (suffix.equals("tier.short.2"))return text("II · давление отбрасывает врагов", "II · pressure knocks enemies back");
            if (suffix.equals("tier.short.3"))return text("III · эхо замедляет на 2 секунды", "III · echo slows for 2 seconds");
            if (suffix.equals("mastery.detail")) return text("Успешные применения: 10 открывают II уровень, 30 — III.",
                    "Successful uses: 10 unlock tier II, 30 unlock tier III.");
        }
        if (race == 4 && suffix.equals("buff.3")) return text("Огненные рассечения [B]", "Fiery slashes [B]");
        if(race==4&&suffix.equals("spell.detail"))return text("Пять извержений идут вперёд по поверхности; перезарядка V — 60 секунд. B — сохранённая цепочка трёх рассечений.","Five eruptions advance along the surface; V cooldown is 60 seconds. B uses the preserved three-slash combo.");
        if(race==4&&suffix.startsWith("tier.short.")){int t=suffix.charAt(suffix.length()-1)-'0';return text("Уровень "+t+": "+(4+2*t)+" урона, огонь "+(2*t)+" с", "Tier "+t+": "+(4+2*t)+" damage, "+(2*t)+"s fire");}
        if(race==1&&suffix.equals("tier.short.2"))return text("II · дальность разлома 32 блока", "II · rift reach 32 blocks");
        if (race == 4 && suffix.equals("buff.detail.3")) return text("B выполняет три последовательных огненных рассечения с рывками вперёд.",
                "B performs three successive fiery slashes with forward pulls.");
        if (race == 2) {
            if (suffix.equals("spell.detail") || suffix.equals("buff.detail.1")) return text("Корни захватывают врагов рядом. Базовое попадание: 6 урона. Доступные улучшения открываются последовательно во вкладке развития.",
                    "Roots bind nearby enemies. Initial hit: 6 damage. Available upgrades unlock in order in progression.");
            if (suffix.equals("tier.short.1")) return text("I · доступна блокировка действий", "I · action lock available");
            if (suffix.equals("tier.short.2")) return text("II · доступен урон 3/сек.", "II · 3 damage/sec available");
            if (suffix.equals("tier.short.3")) return text("III · доступен яд", "III · poison available");
        }
        return Component.translatable("jem.roles.race." + RACES[race] + "." + suffix);
    }
    private Component text(String russian, String english) {
        return Component.literal(minecraft.options.languageCode.startsWith("ru") ? russian : english);
    }
    private int panelWidth() { return Math.min(300, Math.max(144, width * 46 / 100)); }
    private int panelLeft() { return width - panelWidth(); }
    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
    private static float smooth(float value) { float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t); }

    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (entryAt != 0 || requested >= 0) return true;
        if (x >= panelLeft()) scroll = Math.max(0, scroll - (int) (delta * 30));
        else switchRace(delta < 0 ? 1 : -1);
        return true;
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (entryAt != 0 || requested >= 0) return true;
        if (key == GLFW.GLFW_KEY_LEFT) { switchRace(-1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { switchRace(1); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) { scroll += 80; return true; }
        if (key == GLFW.GLFW_KEY_PAGE_UP) { scroll = Math.max(0, scroll - 80); return true; }
        if (key == GLFW.GLFW_KEY_P) { progression = !progression; scroll = 0; rebuildWidgets(); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public boolean shouldCloseOnEsc() { return !mandatory && requested < 0 && entryAt == 0; }
    @Override public void onClose() { if (shouldCloseOnEsc()) super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() {
        if (hudCaptured) minecraft.options.hideGui = hiddenHud;
        if (minecraft.player != null && minecraft.level != null) SandboxNetwork.cinematic(false);
        net.minecraft.client.KeyMapping.releaseAll();
    }

    private record Line(Component text, int color) { static Line gap() { return new Line(null, 0); } }

    /** Text-only focus/hover states keep the stage free of default grey button plates. */
    private static final class QuietButton extends Button {
        QuietButton(int x, int y, int width, int height, Component label, OnPress action) {
            super(x, y, width, height, label, action, DEFAULT_NARRATION);
        }
        @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int color = !active ? 0x777777 : isHoveredOrFocused() ? 0xFFFFFF : 0xBFBFBF;
            if (isHoveredOrFocused()) g.fill(getX(), getY() + height - 2, getX() + width, getY() + height - 1, 0xFF707070);
            g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
        }
    }
}
