package com.siirio.jembuilding.client;

import com.siirio.jembuilding.construction.ConstructionPlane;
import com.siirio.jembuilding.construction.ConstructionShape;
import com.siirio.jembuilding.item.BuilderHammerItem;
import com.siirio.jembuilding.network.BuildingNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class ConstructionModeScreen extends Screen {
    private static final int ORBIT = 58;
    private static final int BUTTON_RADIUS = 22;
    private static final int COPPER = 0xFFE08B46;
    private static final int COPPER_HOVER = 0xFFFFB66D;
    private static final int SLATE = 0xE61B2028;
    private static final int SLATE_HOVER = 0xF02B3440;
    private static final List<Choice> CHOICES = List.of(
            new Choice(ConstructionShape.WALL, 0, -ORBIT, new ItemStack(Items.BRICKS)),
            new Choice(ConstructionShape.FLOOR, ORBIT, 0, new ItemStack(Items.GRASS_BLOCK)),
            new Choice(ConstructionShape.COLUMN, 0, ORBIT, new ItemStack(Items.LIGHTNING_ROD)),
            new Choice(ConstructionShape.SPHERE, -ORBIT, 0, new ItemStack(Items.SLIME_BALL)));

    private final BlockPos clicked;
    private final Direction face;
    private final BlockPos anchor;
    private final InteractionHand hand;
    private final Vec3 look;

    public ConstructionModeScreen(BlockPos clicked, Direction face, InteractionHand hand, Vec3 look) {
        super(Component.translatable("screen.jem_building.mode.title"));
        this.clicked = clicked.immutable();
        this.face = face;
        this.anchor = clicked.relative(face).immutable();
        this.hand = hand;
        this.look = look;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int centerX = width / 2;
        int centerY = height / 2;
        circle(graphics, centerX, centerY, ORBIT + 31, 0xB70B0E12);
        circle(graphics, centerX, centerY, ORBIT - 29, 0xF0181D24);
        graphics.drawCenteredString(font, title, centerX, centerY - 5, 0xFFF2E7D5);
        Choice hovered = null;
        for (Choice choice : CHOICES) {
            int x = centerX + choice.offsetX();
            int y = centerY + choice.offsetY();
            boolean over = inside(mouseX, mouseY, x, y);
            circle(graphics, x, y, BUTTON_RADIUS + 2, over ? COPPER_HOVER : COPPER);
            circle(graphics, x, y, BUTTON_RADIUS - 1, over ? SLATE_HOVER : SLATE);
            graphics.renderItem(choice.icon(), x - 8, y - 8);
            if (over) hovered = choice;
        }
        if (hovered != null) {
            graphics.drawCenteredString(font, Component.translatable("screen.jem_building.mode." + hovered.shape().name().toLowerCase()),
                    centerX, centerY + ORBIT + 38, 0xFFFFC98F);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.jem_building.mode.hint"), centerX,
                    centerY + ORBIT + 38, 0xFFB9C2CC);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int centerX = width / 2;
        int centerY = height / 2;
        for (Choice choice : CHOICES) {
            if (!inside(mouseX, mouseY, centerX + choice.offsetX(), centerY + choice.offsetY())) continue;
            choose(choice.shape());
            return true;
        }
        return true;
    }

    private void choose(ConstructionShape shape) {
        if (minecraft == null || minecraft.player == null) return;
        ConstructionPlane plane = switch (shape) {
            case WALL -> ConstructionPlane.wallFromLook(look);
            case FLOOR -> ConstructionPlane.XZ;
            case COLUMN, SPHERE -> ConstructionPlane.XY;
        };
        ItemStack hammer = minecraft.player.getItemInHand(hand);
        if (!(hammer.getItem() instanceof BuilderHammerItem)) {
            onClose();
            return;
        }
        BuilderHammerItem.setStart(hammer, anchor, shape, plane);
        BuildingNetwork.selectMode(hand, clicked, face, shape, plane);
        minecraft.setScreen(null);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y) {
        double dx = mouseX - x;
        double dy = mouseY - y;
        return dx * dx + dy * dy <= BUTTON_RADIUS * BUTTON_RADIUS;
    }

    private static void circle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int span = (int) Math.floor(Math.sqrt(radius * radius - y * y));
            graphics.fill(centerX - span, centerY + y, centerX + span + 1, centerY + y + 1, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Choice(ConstructionShape shape, int offsetX, int offsetY, ItemStack icon) { }
}
