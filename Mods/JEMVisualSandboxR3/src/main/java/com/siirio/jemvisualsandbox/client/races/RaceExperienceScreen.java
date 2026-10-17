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
    private boolean abilityDetails;
    private int previewSlot;
    private int previewTier = 1;
    private int previewBloom;
    private final AbilityPreviewRenderer abilityPreview = new AbilityPreviewRenderer();
    private Button previewLevel;
    private Button resetProgress;
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
        this.previewTier = tier();
        this.previewBloom = status.bloomUpgrade();
    }

    @Override protected void init() {
        bloomUnlocks.clear();
        developerTier = null;
        resetProgress = null;
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
        int previewWidth = panelWidth() - 24;
        int slotWidth = Math.max(22, previewWidth / 5);
        addRenderableWidget(new QuietButton(panelLeft()+12, 75, slotWidth, 18, Component.literal("V"), b -> selectPreview(0)));
        addRenderableWidget(new QuietButton(panelLeft()+12+slotWidth, 75, slotWidth, 18, Component.literal("B / И"), b -> selectPreview(1)));
        addRenderableWidget(new QuietButton(panelLeft()+12+slotWidth*2, 75, Math.max(28,previewWidth-slotWidth*2), 18,
                text(abilityDetails ? "Назад" : "Подробнее", abilityDetails ? "Back" : "Details"), b -> {abilityDetails=!abilityDetails;scroll=0;rebuildWidgets();}));
        previewLevel=null;
        if(height>=216) {
        int controlsY=previewBottom()+2;
        addRenderableWidget(new QuietButton(panelLeft()+12,controlsY,22,18,Component.literal("‹"),b->changePreviewTier(-1)));
        previewLevel=addRenderableWidget(new QuietButton(panelLeft()+36,controlsY,previewWidth-48,18,
                Component.literal(""),b->changePreviewTier(1)));
        addRenderableWidget(new QuietButton(width-36,controlsY,22,18,Component.literal("›"),b->changePreviewTier(1)));
        }
        if (status.developerMode()) {
            int half=Math.max(44,(stage-28)/2);
            developerTier=addRenderableWidget(new QuietButton(12,height-28,half,20,
                    Component.literal(""),b->SandboxNetwork.developerTier(tier()%3+1)));
            resetProgress=addRenderableWidget(new QuietButton(16+half,height-28,half,20,
                    text("DEV · сброс", "DEV · reset"),b->SandboxNetwork.developerReset()));
        }
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
        previewTier = tier(); previewBloom = race==2?status.bloomUpgrade():0; abilityPreview.reset();
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
                : status.developerMode() && status.selected() ? text("Сменить расу", "Switch Race")
                : Component.translatable("jem.roles.select"));
        if (developerTier != null) {
            developerTier.active = !busy && status.selected() && status.race() == race;
            developerTier.setMessage(text("DEV · уровень " + tier(), "DEV · tier " + tier()));
        }
        if (resetProgress != null) resetProgress.active = !busy && status.selected() && status.race() == race;
        if (previewLevel != null) previewLevel.setMessage(Component.literal((previewSlot==0?"V":"B")+" · "+(race==2&&previewSlot==0?text("Корни ","Roots ").getString()+previewBloom:"I".repeat(previewTier)))
                .append(text(previewUnlocked()?" · открыт":" · закрыт",previewUnlocked()?" · unlocked":" · locked")));
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
        if(previewSlot==0) g.blit(new net.minecraft.resources.ResourceLocation("jem_visual_sandbox","textures/gui/spells/"+icons[race]+".png"),left+12,52,0,0,16,16,16,16);
        else {
            net.minecraft.world.item.Item[] utilityIcons={net.minecraft.world.item.Items.IRON_GOLEM_SPAWN_EGG,net.minecraft.world.item.Items.ENDER_PEARL,
                    net.minecraft.world.item.Items.OAK_LEAVES,net.minecraft.world.item.Items.ECHO_SHARD,net.minecraft.world.item.Items.BLAZE_POWDER};
            g.renderItem(new net.minecraft.world.item.ItemStack(utilityIcons[race]),left+12,52);
        }
        g.drawString(font,font.plainSubstrByWidth(abilityName().getString(),Math.max(24,right-left-38)),left+34,56,accent,false);
        int previewX=left+12, previewY=96, previewW=panelWidth()-24;
        if(height>=216) {
        g.fill(previewX,previewY,previewX+previewW,previewBottom(),0xFF08090C);
        if (minecraft.hasSingleplayerServer()) abilityPreview.render(g,previewX,previewY,previewW,previewBottom()-previewY,
                race,previewSlot,previewTier,previewBloom,partialTick);
        else g.drawCenteredString(font,text("Нет совместимого VFX-предпросмотра", "No compatible VFX preview"),previewX+previewW/2,previewY+12,0xBFBFBF);
        }
        int top = height>=216?previewBottom()+25:98, bottom = Math.max(top+1,height - 41);
        g.enableScissor(left + 9, top, right + 3, bottom);
        List<Line> lines = abilityDetails ? abilityLines() : progression ? progressionLines() : originLines();
        if(height<216)lines.add(0,new Line(text("Уменьши масштаб GUI для 3D-просмотра", "Reduce GUI scale to show the 3D preview"),0xD8BD8D));
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
            lines.add(new Line(text("SANDBOX · без перезарядки; смена расы без лимита. DEV меняет фактический уровень; стрелки под анимацией — только просмотр.",
                    "SANDBOX · no cooldowns; unlimited race switching. DEV changes the real tier; preview arrows only inspect it."), 0xE2BD88));
        }
        return lines;
    }

    private int previewBottom() { return 96 + Mth.clamp((height-144)/2, 30, 102); }
    private void selectPreview(int slot) { previewSlot=slot;abilityPreview.reset();scroll=0;rebuildWidgets(); }
    private void changePreviewTier(int direction) {
        if(race==2&&previewSlot==0) { previewBloom=Math.floorMod(previewBloom+direction,4);previewTier=Math.max(1,previewBloom); }
        else previewTier=Math.floorMod(previewTier-1+direction,3)+1;
        abilityPreview.reset();scroll=0;updateButtons();
    }
    private boolean previewUnlocked(){return race==2&&previewSlot==0?previewBloom<=status.bloomUpgrade():previewTier<=tier();}
    private List<Line> abilityLines() {
        List<Line> lines=new ArrayList<>();
        lines.add(new Line(abilityName(),ACCENTS[race]));
        lines.add(new Line(text("Только визуальный просмотр · мир не изменяется", "Visual inspection only · world unchanged"),0xA5A5A5));
        lines.add(new Line(text(previewUnlocked()?"Открыто":"Закрыто · доступен просмотр",previewUnlocked()?"Unlocked":"Locked · inspection available"),previewUnlocked()?0xB5DBA4:0xC0A081));
        if(previewSlot==0) {
            lines.add(new Line(tr("spell.detail"),0xDEDEDE));
            String ru="",en="";
            switch(race) {
                case 0 -> {ru="Радиус "+(previewTier>=2?16:12)+" блоков; игрокам +6 HP; щиты −25% урона, 5 с";en="Radius "+(previewTier>=2?16:12)+" blocks; players +6 HP; wards −25% damage, 5s";
                    if(previewTier>=2){ru+="; очищение";en+="; cleanse";} if(previewTier>=3){ru+="; поглощение при полном здоровье";en+="; absorption at full health";}}
                case 1 -> {ru="Разлом: дальность "+(previewTier>=2?32:24)+" блока; настоящий удар ближнего боя";en="Rift: "+(previewTier>=2?32:24)+" block reach; actual melee strike";if(previewTier>=3){ru+="; замедление рядом с прибытием";en+="; slow around arrival";}}
                case 2 -> {ru="Базовое попадание: 6 HP; корни: "+previewBloom+"/3";en="Initial hit: 6 HP; roots: "+previewBloom+"/3";if(previewBloom>=1){ru+="; блокировка действий";en+="; action lock";}if(previewBloom>=2){ru+="; сжатие 3 HP/с";en+="; constriction 3 HP/s";}if(previewBloom>=3){ru+="; яд";en+="; poison";}}
                case 3 -> {ru="12 HP; радиус 8 блоков; один удар по цели";en="12 HP; 8 block radius; one hit per target";if(previewTier>=2){ru+="; отбрасывание";en+="; knockback";}if(previewTier>=3){ru+="; замедление 2 с";en+="; 2s slow";}}
                case 4 -> {ru="Пять извержений; "+(4+2*previewTier)+" HP, огонь "+(2*previewTier)+" с";en="Five eruptions; "+(4+2*previewTier)+" HP, "+(2*previewTier)+"s fire";}
            }
            lines.add(new Line(text(ru,en),0xE5E5E5));
        } else {
            String[] ru={"Автоматический страж при здоровье ниже 40%; B не является атакой. V — Хор жизни.","Удерживай B 10 тиков: жемчуг с настоящей траекторией. Shift+B — отрицание гравитации на 5 с.","B включает и выключает временный Лиственный мост. Уровень не меняет его внешний вид.","B: при 25 резонанса — лечение +6 HP и поглощение; иначе Эхо-зрение. Это не удар по криперу.","B: настоящая цепочка трёх огненных рассечений. Уровень не добавляет новых ударов."};
            String[] en={"Automatic guardian below 40% health; B is not an attack. V is Life Chorus.","Hold B for 10 ticks: pearl with its real trajectory. Shift+B denies gravity for 5 seconds.","B toggles the temporary Leaf Bridge. Tier does not change its appearance.","B: with 25 resonance, heal +6 HP and absorption; otherwise Echo Vision. This does not hit the Creeper.","B: the actual three fiery slashes. Tier adds no new combo hits."};
            lines.add(new Line(text(ru[race],en[race]),0xDEDEDE));
            lines.add(new Line(text("I–III используют ту же визуальную форму, если эффект не меняется.","I–III retain the same visual form when the effect does not change."),0xA5A5A5));
        }
        if(minecraft.hasSingleplayerServer()) lines.add(new Line(text("Базовая перезарядка V: "+RaceExperience.signatureCooldown(race)/20+" с; sandbox-переопределение не меняет определение.","Base V cooldown: "+RaceExperience.signatureCooldown(race)/20+"s; sandbox override preserves the definition."),0xA5A5A5));
        return lines;
    }
    private Component abilityName(){
        if(previewSlot==0)return Component.literal("V · ").append(tr("spell.short"));
        String[] ru={"Автоматический страж", "Жемчуг / гравитация", "Лиственный мост", "Эхо / восстановление", "Огненные рассечения"};
        String[] en={"Automatic guardian", "Pearl / gravity", "Leaf Bridge", "Echo / recovery", "Fiery slashes"};
        return Component.literal("B · ").append(text(ru[race],en[race]));
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
        if (key == GLFW.GLFW_KEY_V) { selectPreview(0); return true; }
        if (key == GLFW.GLFW_KEY_B) { selectPreview(1); return true; }
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
        abilityPreview.dispose();
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
