package com.citymod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Панель справа: баланс, банк, кредит, работа, время, статистика. */
public class HudOverlay implements IGuiOverlay {
    private static final int W = 140;
    private static final int LINE = 11;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        long t = mc.level.getDayTime() % 24000L;
        int hours = (int) ((t / 1000 + 6) % 24);
        int minutes = (int) ((t % 1000) * 60 / 1000);
        String time = String.format("%02d:%02d", hours, minutes);

        String[] lines = {
                "§6§lГОРОД",
                "§fНаличные: §a" + ClientData.cash + " $",
                "§fБанк: §b" + ClientData.bank + " $",
                "§fФишки: §d" + ClientData.chips,
                "§fКредит: " + (ClientData.credit > 0 ? "§c" : "§7") + ClientData.credit + " $",
                "§fРабота: §e" + ClientData.jobTitle(),
                "§fЗаработано: §e" + ClientData.earned + " $",
                "§fВремя: §7" + time,
        };

        int h = lines.length * LINE + 8;
        int x = screenWidth - W - 4;
        int y = 4;

        g.fill(x, y, x + W, y + h, 0x99000000);
        g.renderOutline(x, y, W, h, 0xFFD4AF37);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(mc.font, lines[i], x + 5, y + 5 + i * LINE, 0xFFFFFF, true);
        }
    }
}
