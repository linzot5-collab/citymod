package com.citymod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Универсальный экран с кнопками. Каждая кнопка отправляет команду на сервер. */
public class MenuScreen extends Screen {
    private static final long[] AMOUNTS = {10, 50, 100, 500, 1000};
    private static int amountIdx = 0; // запоминаем выбор между открытиями

    private final String id;

    public MenuScreen(String id) {
        super(Component.literal(Menus.title(id)));
        this.id = id;
    }

    private Component amountLabel() {
        return Component.literal("Сумма: " + AMOUNTS[amountIdx]);
    }

    @Override
    protected void init() {
        List<Menus.Entry> entries = Menus.entries(id);
        int bw = 150, bh = 20, gap = 4, cols = 2;
        int startX = width / 2 - (cols * bw + (cols - 1) * gap) / 2;
        int y = 44;

        if (Menus.usesAmount(id)) {
            addRenderableWidget(Button.builder(amountLabel(), b -> {
                amountIdx = (amountIdx + 1) % AMOUNTS.length;
                b.setMessage(amountLabel());
            }).bounds(width / 2 - 75, y, 150, bh).build());
            y += bh + 8;
        }

        for (int i = 0; i < entries.size(); i++) {
            Menus.Entry en = entries.get(i);
            int col = i % cols, row = i / cols;
            addRenderableWidget(Button.builder(Component.literal(en.label()), b -> send(en.cmd()))
                    .bounds(startX + col * (bw + gap), y + row * (bh + gap), bw, bh).build());
        }

        int rows = (entries.size() + cols - 1) / cols;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 50, y + rows * (bh + gap) + 8, 100, bh).build());
    }

    private void send(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.connection.sendCommand(cmd.replace("{a}", String.valueOf(AMOUNTS[amountIdx])));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFD700);
        String stats = "Наличные: " + ClientData.cash + " $   Банк: " + ClientData.bank
                + " $   Фишки: " + ClientData.chips + "   Кредит: " + ClientData.credit + " $";
        g.drawCenteredString(font, stats, width / 2, 26, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
