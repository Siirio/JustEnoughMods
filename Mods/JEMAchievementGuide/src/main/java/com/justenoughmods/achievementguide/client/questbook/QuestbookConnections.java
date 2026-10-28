package com.justenoughmods.achievementguide.client.questbook;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;

/** Decoration in the native panel's already scissored, scrolled background phase. */
public final class QuestbookConnections {
    private QuestbookConnections() {}

    public static void draw(GuiGraphics graphics, QuestbookLayout.Board board, double originX, double originY,
                            double minX, double minY, double unit) {
        for (var connection : board.connections()) {
            double phase = 0;
            for (int index = 1; index < connection.points().size(); index++) {
                var a = connection.points().get(index - 1);
                var b = connection.points().get(index);
                double x = originX + (a.x() - minX) * unit;
                double y = originY + (a.y() - minY) * unit;
                double dx = (b.x() - a.x()) * unit;
                double dy = (b.y() - a.y()) * unit;
                double length = Math.hypot(dx, dy);
                if (length == 0) continue;
                double period = connection.dash() + connection.gap();
                if (period <= 0 || connection.dash() <= 0) continue;
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().mulPose(new Quaternionf().rotationZ((float)Math.atan2(dy, dx)));
                for (double start = -phase; start < length; start += period) {
                    double left = Math.max(0, start);
                    double right = Math.min(length, start + connection.dash());
                    if (right <= left) continue;
                    graphics.pose().pushPose();
                    graphics.pose().translate(left, -connection.width() / 2D, 0);
                    graphics.pose().scale((float)(right - left), connection.width(), 1);
                    graphics.fill(0, 0, 1, 1, connection.color());
                    graphics.pose().popPose();
                }
                graphics.pose().popPose();
                phase = (phase + length) % period;
            }
        }
    }
}
