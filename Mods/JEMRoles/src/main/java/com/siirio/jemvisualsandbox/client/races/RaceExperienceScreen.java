package com.siirio.jemvisualsandbox.client.races;

import com.siirio.jemvisualsandbox.workbench.RaceExperience;
import com.siirio.jemvisualsandbox.workbench.RacePowerStages;
import com.siirio.jemvisualsandbox.workbench.BloomMechanics;
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
    private java.util.UUID requestedNonce;
    private int scroll;
    private int contentHeight;
    private boolean progression;
    private int previewSlot;
    private int previewTier;
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
    private Button bloomUnlock;

    RaceExperienceScreen(RaceExperience.Status status, boolean mandatory, boolean progression) {
        super(Component.translatable("jem.roles.screen.title"));
        this.status = status;
        this.mandatory = mandatory;
        this.progression = progression;
        this.race = Mth.clamp(status.race(), 0, 4);
        this.previewTier = tier();
        this.previewBloom = status.bloomUpgrade();
        if(race==2) this.previewTier=this.previewBloom;
    }

    private static final double FIXED_GUI_SCALE = 3.0;

    @Override protected void init() {
        width = (int)Math.ceil(minecraft.getWindow().getWidth() / FIXED_GUI_SCALE);
        height = (int)Math.ceil(minecraft.getWindow().getHeight() / FIXED_GUI_SCALE);
        bloomUnlock = null;
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
        int slotWidth = previewWidth / 2;
        addRenderableWidget(new QuietButton(panelLeft()+12, 75, slotWidth, 18, Component.literal("V"), b -> selectPreview(0)));
        addRenderableWidget(new QuietButton(panelLeft()+12+slotWidth, 75, slotWidth, 18, Component.literal("B / И"), b -> selectPreview(1)));
        previewLevel=null;
        {
        int controlsY=previewVisible()?previewBottom()+2:96;
        addRenderableWidget(new QuietButton(panelLeft()+12,controlsY,22,18,Component.literal("‹"),b->changePreviewTier(-1)));
        previewLevel=addRenderableWidget(new QuietButton(panelLeft()+36,controlsY,previewWidth-48,18,
                Component.literal(""),b->changePreviewTier(1)));
        addRenderableWidget(new QuietButton(width-36,controlsY,22,18,Component.literal("›"),b->changePreviewTier(1)));
        }
        bloomUnlock = addRenderableWidget(new QuietButton(panelLeft()+12,
                (previewVisible()?previewBottom()+2:96)+20,previewWidth,18,
                Component.empty(),b -> SandboxNetwork.upgrade(previewTier)));
        updateButtons();
    }

    public void receive(RaceExperience.Status value) {
        boolean developerChanged = status.developerMode() != value.developerMode();
        status = value;
        boolean acknowledged = requested >= 0 && !value.nonce().equals(requestedNonce);
        if (acknowledged && value.selected() && value.race() == requested) {
            entryAt = System.nanoTime();
            requested = -1;
        } else if (acknowledged) {
            requested = -1;
            rejected = true;
        }
        if (developerChanged) rebuildWidgets();
        updateButtons();
    }

    private void confirm() {
        if (requested >= 0 || entryAt != 0) return;
        requested = race;
        requestedNonce = status.nonce();
        requestedAt = System.nanoTime();
        rejected = false;
        SandboxNetwork.select(race,status.nonce());
        updateButtons();
    }

    private void switchRace(int direction) {
        if (requested >= 0 || entryAt != 0) return;
        race = Math.floorMod(race + direction, RACES.length);
        previewTier = tier(); previewBloom = race==2?status.bloomUpgrade():0; if(race==2&&previewSlot==0)previewTier=previewBloom; abilityPreview.reset();
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
        boolean waiting = status.selected() && status.race() != race && !status.testProfile()
                && RaceExperienceClient.remaining(status.reselectRemaining()) > 0L;
        confirm.active = !busy && !waiting;
        confirm.setMessage(requested >= 0 ? text("Сервер подтверждает…", "Waiting for server…")
                : waiting ? text("Смена через "+days(RaceExperienceClient.remaining(status.reselectRemaining()))+" Minecraft-дн.",
                        "Change in "+days(RaceExperienceClient.remaining(status.reselectRemaining()))+" Minecraft days")
                : rejected ? text("Не подтверждено · повторить", "Not confirmed · retry")
                : status.selected() && status.race() == race ? text("Вернуться в мир", "Enter world")
                : status.developerMode() && status.selected() ? text("Сменить расу", "Switch Race")
                : Component.translatable("jem.roles.select"));
        if (developerTier != null) {
            developerTier.active = !busy && status.selected() && status.race() == race;
            developerTier.setMessage(text("DEV · уровень " + tier(), "DEV · tier " + tier()));
        }
        if (resetProgress != null) resetProgress.active = !busy && status.selected() && status.race() == race;
        if (previewLevel != null) previewLevel.setMessage(Component.literal((previewSlot==0?"V":"B")+" · ")
                .append(stageName(previewTier))
                .append(text(previewUnlocked()?" · открыт":" · закрыт",previewUnlocked()?" · unlocked":" · locked")));
        if (bloomUnlock != null) {
            bloomUnlock.visible = progression && race==2 && previewSlot==0 && status.selected()
                    && status.race()==race && previewTier>status.bloomUpgrade();
            bloomUnlock.active = !busy && previewTier==status.bloomUpgrade()+1 && previewTier<=tier();
            bloomUnlock.setMessage(text("Открыть улучшение "+previewTier,"Unlock upgrade "+previewTier));
        }
    }

    private static String days(long ticks) {
        if(ticks<=0L)return "0";
        if(ticks<24L)return "<0.001";
        return java.math.BigDecimal.valueOf(ticks).divide(java.math.BigDecimal.valueOf(24000L),3,java.math.RoundingMode.CEILING).stripTrailingZeros().toPlainString();
    }

    @Override public void tick() {
        minecraft.options.hideGui = true;
        updateButtons();
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
        float scale = (float)(FIXED_GUI_SCALE / minecraft.getWindow().getGuiScale());
        g.pose().pushPose();
        try {
            g.pose().scale(scale, scale, 1);
            renderFixed(g, (int)(mouseX / scale), (int)(mouseY / scale), partialTick);
        } finally { g.pose().popPose(); }
    }
    private void renderFixed(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
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
        renderRaceIdentity(g,left,accent);
        g.fill(left,49,left+1,height-42,0xFF282828);

        String[] icons={"life_chorus","rift_ambush","verdant_dominion","sculk_veins","infernal_eruption"};
        if(previewSlot==0) g.blit(new net.minecraft.resources.ResourceLocation("jem_visual_sandbox","textures/gui/spell_icons/"+icons[race]+".png"),left+12,52,0,0,16,16,16,16);
        else {
            net.minecraft.world.item.Item[] utilityIcons={net.minecraft.world.item.Items.IRON_GOLEM_SPAWN_EGG,net.minecraft.world.item.Items.ENDER_PEARL,
                    net.minecraft.world.item.Items.OAK_LEAVES,net.minecraft.world.item.Items.ECHO_SHARD,net.minecraft.world.item.Items.BLAZE_POWDER};
            g.renderItem(new net.minecraft.world.item.ItemStack(utilityIcons[race]),left+12,52);
        }
        g.drawString(font,font.plainSubstrByWidth(abilityName().getString(),Math.max(24,right-left-38)),left+34,56,accent,false);
        int previewX=left+12, previewY=96, previewW=panelWidth()-24;
        if(previewVisible()) {
        g.fill(previewX,previewY,previewX+previewW,previewBottom(),0xFF08090C);
        abilityPreview.render(g,previewX,previewY,previewW,previewBottom()-previewY,
                race,previewSlot,previewTier,previewBloom,partialTick,FIXED_GUI_SCALE);
        }
        int top = (previewVisible()?previewBottom()+25:119)+(bloomUnlock!=null&&bloomUnlock.visible?20:0);
        int bottom = Math.max(top+1,height - 41);
        double scissorScale = FIXED_GUI_SCALE / minecraft.getWindow().getGuiScale();
        g.enableScissor((int)Math.floor((left + 9) * scissorScale), (int)Math.floor(top * scissorScale),
                (int)Math.ceil((right + 3) * scissorScale), (int)Math.ceil(bottom * scissorScale));
        List<Line> lines = abilityLines();
        lines.add(Line.gap());
        lines.addAll(progression ? progressionLines() : originLines());
        if(!previewVisible())lines.add(new Line(text("Уменьши масштаб GUI для 3D-просмотра", "Reduce GUI scale to show the 3D preview"),0xD8BD8D));
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

    private void renderRaceIdentity(GuiGraphics graphics,int left,int accent) {
        int center=left/2, nameY=Math.max(64,height-80), available=Math.max(70,left-40);
        float titleScale=1.35F;
        var name=font.split(tr("name").copy().withStyle(net.minecraft.ChatFormatting.BOLD),(int)(available/titleScale));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center-12,nameY-30,0);
            graphics.pose().scale(1.5F,1.5F,1);
            net.minecraft.world.item.Item[] icons={net.minecraft.world.item.Items.TOTEM_OF_UNDYING,
                    net.minecraft.world.item.Items.ENDER_PEARL,net.minecraft.world.item.Items.OAK_SAPLING,
                    net.minecraft.world.item.Items.ECHO_SHARD,net.minecraft.world.item.Items.BLAZE_POWDER};
            graphics.renderFakeItem(new net.minecraft.world.item.ItemStack(icons[race]),0,0);
        } finally { graphics.pose().popPose(); }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(center,nameY,0);
            graphics.pose().scale(titleScale,titleScale,1);
            for(int i=0;i<name.size();i++)graphics.drawCenteredString(font,name.get(i),0,i*11,accent);
        } finally { graphics.pose().popPose(); }
        if(height>260) {
            int y=nameY+(int)Math.ceil(name.size()*11*titleScale)+4;
            for(var line:font.split(tr("lore"),available)) { graphics.drawCenteredString(font,line,center,y,0xA9A9A9); y+=11; }
        }
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
        if(status.selected() && status.race()==race && status.healingPenalty())
            lines.add(new Line(text("Сейчас: получаемое лечение −25% (неблагоприятная среда)", "Active: incoming healing −25% (adverse environment)"),0xE3A093));
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
        lines.add(new Line(text("База · исходное умение без улучшений", "Base · initial ability without upgrades"), 0xD4EAC5));
        for (int i = 1; i <= 3; i++) lines.add(new Line(Component.literal(i <= tier() ? "◆ " : "◇ ")
                .append(tr("tier.short." + i)), i <= tier() ? 0xD4EAC5 : 0x767676));
        if (race == 2) {
            lines.add(Line.gap());
            lines.add(new Line(text("УЛУЧШЕНИЯ КОРНЕЙ", "ROOT UPGRADES"), ACCENTS[race]));
            lines.add(new Line(text("I · блокировка действий; II · 2,7 урона/сек.; III · яд после захвата.",
                    "I · action lock; II · 2.7 damage/sec; III · poison after release."), 0xDEDEDE));
            lines.add(new Line(text("Выбери нужное улучшение в просмотре V; кнопка под ним открывает следующий доступный этап без пропусков.",
                    "Inspect an upgrade under V; its button unlocks the next available stage in order."), 0xB2B2B2));
        }
        if (chosen) {
            lines.add(Line.gap());
            lines.add(new Line(text("ПЕРЕЗАРЯДКА", "COOLDOWNS"), ACCENTS[race]));
            lines.add(new Line(cooldown("V", status.signatureRemaining()), 0xDEDEDE));
            lines.add(new Line(cooldown("B", status.secondaryRemaining()), 0xDEDEDE));
        } else lines.add(new Line(text("Предпросмотр: эта раса сейчас не выбрана.", "Preview: this race is not currently selected."), 0xA5A5A5));
        return lines;
    }

    private boolean previewVisible() { return height >= 280; }
    private int previewBottom() { return Math.min(96 + Mth.clamp((height-144)/2, 30, 102),height-150); }
    private void selectPreview(int slot) {
        previewSlot=slot;
        if(race==2&&slot==0) previewTier=previewBloom;
        abilityPreview.reset();scroll=0;rebuildWidgets();
    }
    private void changePreviewTier(int direction) {
        previewTier=Math.floorMod(previewTier+direction,4);
        if(race==2&&previewSlot==0) previewBloom=previewTier;
        abilityPreview.reset();scroll=0;updateButtons();
    }
    private boolean previewUnlocked(){
        return status.selected() && status.race()==race &&
                (race==2&&previewSlot==0?previewTier<=status.bloomUpgrade():previewTier<=tier());
    }
    private Component stageName(int stage) {
        return stage==0?text("База", "Base"):text("Уровень "+stage,"Level "+stage);
    }
    private static String number(double value) { return String.format(java.util.Locale.ROOT,"%.1f",value); }
    private List<Line> abilityLines() {
        List<Line> lines=new ArrayList<>();
        lines.add(new Line(abilityName().copy().append(" · ").append(stageName(previewTier)),ACCENTS[race]));
        lines.add(new Line(text(previewUnlocked()?"Открыто":"Закрыто · доступен только просмотр",previewUnlocked()?"Unlocked":"Locked · inspection only"),previewUnlocked()?0xB5DBA4:0xC0A081));
        if(progression && previewTier>0 && !previewUnlocked()) {
            int required=RacePowerStages.requiredUses(previewTier);
            lines.add(new Line(text(Math.min(progress(),required)+"/"+required+" успешных использований",
                    Math.min(progress(),required)+"/"+required+" successful uses"),0xD4EAC5));
            if(race==2 && previewSlot==0 && status.selected() && status.race()==race
                    && previewTier>status.bloomUpgrade()+1)
                lines.add(new Line(text("Сначала открой улучшение "+(status.bloomUpgrade()+1),
                        "Unlock upgrade "+(status.bloomUpgrade()+1)+" first"),0xC0A081));
        }
        String ru="", en="";
        if(previewSlot==0) switch(race) {
            case 0 -> {
                ru="Радиус "+RacePowerStages.chorusRadius(previewTier)+" блоков; игрокам +6 HP; боевым союзникам до max(6, 20% max HP), не выше недостающего здоровья. Щиты: −25% входящего урона на "+days(100L)+" Minecraft-дн.";
                en="Radius "+RacePowerStages.chorusRadius(previewTier)+" blocks; players +6 HP; combat allies heal max(6, 20% max HP), capped by missing HP. Wards: −25% incoming damage for "+days(100L)+" Minecraft days.";
                if(previewTier>=2){ru+=" Очищение отрицательных эффектов.";en+=" Cleanses harmful effects.";}
                if(previewTier>=3){ru+=" При полном здоровье — поглощение.";en+=" At full health, grants absorption.";}
            }
            case 1 -> {
                ru="Дальность разлома "+RacePowerStages.riftRange(previewTier)+" блока; телепортация и настоящий удар ближнего боя.";
                en="Rift reach "+RacePowerStages.riftRange(previewTier)+" blocks; teleport and an actual melee strike.";
                if(previewTier>=2){ru+=" Замедление у места прибытия: "+days(RacePowerStages.riftSlowTicks(previewTier))+" Minecraft-дн., радиус "+RacePowerStages.riftSlowRadius(previewTier)+" блоков.";en+=" Arrival slow: "+days(RacePowerStages.riftSlowTicks(previewTier))+" Minecraft days, radius "+RacePowerStages.riftSlowRadius(previewTier)+" blocks.";}
            }
            case 2 -> {
                ru="Первое попадание: "+number(BloomMechanics.INITIAL_DAMAGE)+" HP. Захват на "+days(BloomMechanics.GRIP_TICKS)+" Minecraft-дн.: цель стоит на месте, но в базе может атаковать.";en="Initial hit: "+number(BloomMechanics.INITIAL_DAMAGE)+" HP. "+days(BloomMechanics.GRIP_TICKS)+" Minecraft days of grip: holds the target in place; base targets can still attack.";
                if(previewTier>=1){ru+=" Блокировка действий.";en+=" Action lock.";}
                if(previewTier>=2){ru+=" Сжатие: "+number(BloomMechanics.SQUEEZE_DAMAGE)+" HP/с.";en+=" Constriction: "+number(BloomMechanics.SQUEEZE_DAMAGE)+" HP/s.";}
                if(previewTier>=3){ru+=" После освобождения: яд 1 HP/с на "+days(BloomMechanics.POISON_TICKS)+" Minecraft-дн., включая невосприимчивых к обычному яду мобов; оставляет 1 HP.";en+=" After release: poison 1 HP/s for "+days(BloomMechanics.POISON_TICKS)+" Minecraft days, including normally poison-immune mobs; leaves 1 HP.";}
            }
            case 3 -> {
                ru="12 HP; радиус 8 блоков; один удар по цели.";en="12 HP; 8-block radius; one hit per target.";
                if(previewTier>=1){ru+=" Отбрасывание.";en+=" Knockback.";}
                if(previewTier>=2){ru+=" Тьма на "+days(com.siirio.jemvisualsandbox.workbench.SonicWaveAbility.DARKNESS_TICKS)+" Minecraft-дн. для игроков и мобов; дезориентация мобов без неуязвимости и заморозки.";en+=" Darkness for "+days(com.siirio.jemvisualsandbox.workbench.SonicWaveAbility.DARKNESS_TICKS)+" Minecraft days for players and mobs; mob disorientation without invulnerability or freezing.";}
                if(previewTier>=3){ru+=" Временный скалк: радиус 16, "+days(com.siirio.jemvisualsandbox.workbench.SonicWaveAbility.DOMAIN_TICKS)+" Minecraft-дн. Пока владелец внутри: размер и атака ×1,15.";en+=" Temporary sculk: radius 16, "+days(com.siirio.jemvisualsandbox.workbench.SonicWaveAbility.DOMAIN_TICKS)+" Minecraft days. While owner stays inside: size and attack ×1.15.";}
            }
            case 4 -> {
                ru="Пять извержений на каждом этапе. Урон одного: "+number(RacePowerStages.eruptionDamage(previewTier))+" HP; огонь "+days(RacePowerStages.fireSeconds(previewTier)*20L)+" Minecraft-дн.";
                en="Five eruptions at every stage. Damage per eruption: "+number(RacePowerStages.eruptionDamage(previewTier))+" HP; fire "+days(RacePowerStages.fireSeconds(previewTier)*20L)+" Minecraft days.";
            }
        } else switch(race) {
            case 0 -> {ru="Автоматический страж появляется при здоровье ниже 40%; срок жизни "+days(RacePowerStages.guardianTicks(previewTier))+" Minecraft-дн. B не является атакой.";en="Automatic guardian appears below 40% health; lifetime "+days(RacePowerStages.guardianTicks(previewTier))+" Minecraft days. B is not an attack.";}
            case 1 -> {ru="Удерживай B "+days(10L)+" Minecraft-дн.: жемчуг с настоящей траекторией. Shift+B: отрицание гравитации на "+days(RacePowerStages.gravityTicks(previewTier))+" Minecraft-дн.";en="Hold B for "+days(10L)+" Minecraft days: pearl with its real trajectory. Shift+B: gravity denial for "+days(RacePowerStages.gravityTicks(previewTier))+" Minecraft days.";}
            case 2 -> {ru="B включает/выключает временный лиственный мост: "+days(RacePowerStages.bridgeTicks(previewTier))+" Minecraft-дн., "+RacePowerStages.bridgeForward(previewTier)+" блоков вперёд.";en="B toggles a temporary leaf bridge: "+days(RacePowerStages.bridgeTicks(previewTier))+" Minecraft days, "+RacePowerStages.bridgeForward(previewTier)+" blocks forward.";}
            case 3 -> {ru="Без 25 резонанса: эхо-зрение в радиусе "+RacePowerStages.echoRadius(previewTier)+" блоков. При 25 резонанса: восстановление +"+RacePowerStages.recoveryHeal(previewTier)+" HP; весь резонанс сбрасывается в 0; поглощение на "+days(RacePowerStages.absorptionTicks(previewTier))+" Minecraft-дн. Это разные контекстные действия.";en="Below 25 resonance: echo vision within "+RacePowerStages.echoRadius(previewTier)+" blocks. At 25 resonance: recover +"+RacePowerStages.recoveryHeal(previewTier)+" HP; all resonance resets to 0; absorption for "+days(RacePowerStages.absorptionTicks(previewTier))+" Minecraft days. These are distinct contextual actions.";}
            case 4 -> {ru="Всегда три последовательных огненных рассечения. Урон каждого: "+number(RacePowerStages.slashDamage(previewTier))+" HP; огонь "+days(RacePowerStages.fireSeconds(previewTier)*20L)+" Minecraft-дн.";en="Always three successive fiery slashes. Each deals "+number(RacePowerStages.slashDamage(previewTier))+" HP; fire "+days(RacePowerStages.fireSeconds(previewTier)*20L)+" Minecraft days.";}
        }
        lines.add(new Line(text(ru,en),0xE5E5E5));

        if(previewSlot==1)lines.add(new Line(text("Длительные состояния показаны в сокращённом времени; реальная длительность указана выше.","Long states use compressed preview time; actual duration is shown above."),0xA5A5A5));
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
                : text(days(remaining)+" Minecraft-дн.", days(remaining)+" Minecraft days"));
    }

    private int tier() { return RacePowerStages.clamp(status.tiers()[race]); }
    private int progress() { return Math.max(0, status.progress()[race]); }
    private Component tr(String suffix) {
        if(race==0&&suffix.equals("spell.detail"))return text("Золотой фронт восстанавливает игрокам 3 сердца, лечит боевых союзников на max(6, 20% max HP), не выше недостающего здоровья; мирных животных вне боя исцеляет полностью. Четыре щита уменьшают входящий урон на 25% на "+days(100L)+" Minecraft-дн. Перезарядка: "+days(500L)+" Minecraft-дн.","A golden front restores 3 hearts to players and heals combat allies for max(6, 20% max HP), capped by missing health; peaceful animals outside combat heal fully. Four wards reduce incoming damage by 25% for "+days(100L)+" Minecraft days. Cooldown: "+days(500L)+" Minecraft days.");
        if(race==0&&suffix.equals("tier.short.1"))return text("I · радиус Хора 16 блоков", "I · Chorus radius 16 blocks");
        if(race==0&&suffix.equals("tier.short.2"))return text("II · очищение", "II · cleanse");
        if(race==0&&suffix.equals("tier.short.3"))return text("III · поглощение при полном здоровье", "III · absorption at full health");
        // Explicit sandbox contracts differ from production only in these authored abilities.
        if (race == 3) {
            if (suffix.equals("spell.short") || suffix.equals("buff.2")) return text("Звуковая волна [V]", "Sonic Wave [V]");
            if (suffix.equals("spell.detail") || suffix.equals("buff.detail.2")) return text(
                    "Расходящаяся звуковая волна поражает враждебных существ в радиусе 8 блоков: 12 урона, один раз по каждой цели.",
                    "An expanding sonic wave strikes hostile creatures within 8 blocks: 12 damage, once per target.");
            if (suffix.equals("tier.short.1"))return text("I · отбрасывание", "I · knockback");
            if (suffix.equals("tier.short.2"))return text("II · тьма и дезориентация", "II · darkness and disorientation");
            if (suffix.equals("tier.short.3"))return text("III · временный скалк и усиление внутри", "III · temporary sculk and empowerment inside");

        }
        if (race == 4 && suffix.equals("buff.3")) return text("Огненные рассечения [B]", "Fiery slashes [B]");
        if(race==4&&suffix.equals("spell.detail"))return text("Пять извержений идут вперёд по поверхности; перезарядка V — "+days(1200L)+" Minecraft-дн. B — сохранённая цепочка трёх рассечений.","Five eruptions advance along the surface; V cooldown is "+days(1200L)+" Minecraft days. B uses the preserved three-slash combo.");
        if(race==4&&suffix.startsWith("tier.short.")){int t=suffix.charAt(suffix.length()-1)-'0';return text("Уровень "+t+": "+number(RacePowerStages.eruptionDamage(t))+" урона, огонь "+days(RacePowerStages.fireSeconds(t)*20L)+" Minecraft-дн.", "Tier "+t+": "+number(RacePowerStages.eruptionDamage(t))+" damage, "+days(RacePowerStages.fireSeconds(t)*20L)+" Minecraft days of fire");}
        if(race==1&&suffix.equals("tier.short.1"))return text("I · дальность разлома 32 блока", "I · rift reach 32 blocks");
        if(race==1&&suffix.equals("tier.short.2"))return text("II · замедление "+days(RacePowerStages.riftSlowTicks(2))+" Minecraft-дн., радиус 4", "II · slow "+days(RacePowerStages.riftSlowTicks(2))+" Minecraft days, radius 4");
        if(race==1&&suffix.equals("tier.short.3"))return text("III · замедление "+days(RacePowerStages.riftSlowTicks(3))+" Minecraft-дн., радиус 8", "III · slow "+days(RacePowerStages.riftSlowTicks(3))+" Minecraft days, radius 8");
        if (race == 4 && suffix.equals("buff.detail.3")) return text("B выполняет три последовательных огненных рассечения с рывками вперёд.",
                "B performs three successive fiery slashes with forward pulls.");
        if (race == 2) {
            if (suffix.equals("spell.detail") || suffix.equals("buff.detail.1")) return text("Корни удерживают врагов рядом на месте "+days(BloomMechanics.GRIP_TICKS)+" Minecraft-дн. Первое попадание: "+number(BloomMechanics.INITIAL_DAMAGE)+" HP. Босса удерживает "+days(20L)+" Minecraft-дн., затем защищает от повторных корней "+days(160L)+" Minecraft-дн. В базе цель может атаковать; I блокирует действия, II добавляет сжатие, III — яд после освобождения.",
                    "Roots hold nearby enemies in place for "+days(BloomMechanics.GRIP_TICKS)+" Minecraft days. Initial hit: "+number(BloomMechanics.INITIAL_DAMAGE)+" HP. Bosses are held for "+days(20L)+" Minecraft days, followed by "+days(160L)+" Minecraft days of shared root protection. Base targets can attack; I locks actions, II adds constriction, III adds poison after release.");
            if (suffix.equals("tier.short.1")) return text("I · доступна блокировка действий", "I · action lock available");
            if (suffix.equals("tier.short.2")) return text("II · сжатие "+number(BloomMechanics.SQUEEZE_DAMAGE)+" HP/с", "II · constriction "+number(BloomMechanics.SQUEEZE_DAMAGE)+" HP/s");
            if (suffix.equals("tier.short.3")) return text("III · яд после освобождения", "III · poison after release");
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

    private double fixedCoordinate(double value) { return value * minecraft.getWindow().getGuiScale() / FIXED_GUI_SCALE; }
    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked(fixedCoordinate(x), fixedCoordinate(y), button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(fixedCoordinate(x), fixedCoordinate(y), button); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(fixedCoordinate(x), fixedCoordinate(y)); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(fixedCoordinate(x), fixedCoordinate(y), button, fixedCoordinate(dx), fixedCoordinate(dy));
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        x = fixedCoordinate(x); y = fixedCoordinate(y);
        if (entryAt != 0 || requested >= 0) return true;
        if (x >= panelLeft()+12 && x < width-12 && y>=96 && y<(previewVisible()?previewBottom()+20:114)) changePreviewTier(delta < 0 ? 1 : -1);
        else if (x >= panelLeft()) scroll = Math.max(0, scroll - (int) (delta * 30));
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
    private final class QuietButton extends Button {
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
