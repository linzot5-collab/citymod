package com.citymod.client;

import com.citymod.crypto.Market;
import com.citymod.crypto.Mining;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Телефон в стиле iOS 26: градиентные обои, «жидкое стекло», динамический остров,
 * сетка приложений, док и индикатор «домой». Всё рисуется кодом, текстуры не нужны.
 */
public class PhoneScreen extends Screen {
    private enum Page { HOME, CRYPTO, COIN, MINING, BANK, WALLET }

    private static final int PW = 150, PH = 300;
    private static final int GREEN = 0xFF30D158, RED = 0xFFFF453A, WHITE = 0xFFFFFFFF, GRAY = 0xFFB8B8D0;

    private static final int[][] COIN_COLORS = {
            {0xFFF7931A, 0xFFFFB84D}, {0xFF627EEA, 0xFF9DB0FF}, {0xFF9945FF, 0xFF14F195}, {0xFFC2A633, 0xFFFFE27A}};

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    private final List<Hit> hits = new ArrayList<>();
    private Page page = Page.HOME;
    private int coin = 0;
    private int miningTab = 0;
    private float s = 1f;
    private int ox, oy;
    private double lmx, lmy;
    private int ticks = 0;

    public PhoneScreen() {
        super(Component.literal("Телефон"));
    }

    @Override
    protected void init() {
        s = Math.min(1.6f, (height - 8f) / (PH + 8));
        ox = Math.round((width - PW * s) / 2f);
        oy = Math.round((height - PH * s) / 2f);
        cmd("phone sync");
    }

    @Override
    public void tick() {
        if (++ticks % 40 == 0) cmd("phone sync");
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(c);
    }

    // ------------------------------------------------------------ рисование примитивов

    private static int lerp(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Скруглённый прямоугольник с вертикальным градиентом. */
    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int top, int bot) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < h; i++) {
            int d = i < r ? r - i : (i >= h - r ? i - (h - r) + 1 : 0);
            int inset = 0;
            if (d > 0) {
                double dy = d - 0.5;
                inset = (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
            }
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, lerp(top, bot, h <= 1 ? 0 : i / (float) (h - 1)));
        }
    }

    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        rrect(g, x, y, w, h, r, color, color);
    }

    /** «Жидкое стекло»: полупрозрачная панель со светлой кромкой и бликом. */
    private static void glass(GuiGraphics g, int x, int y, int w, int h, int r) {
        rrect(g, x, y, w, h, r, 0x30000030);
        rrect(g, x, y, w, h, r, 0x55FFFFFF, 0x28FFFFFF);
        rrect(g, x + 1, y + 1, w - 2, h - 2, r - 1, 0x22FFFFFF, 0x14000030);
        g.fill(x + r, y + 1, x + w - r, y + 2, 0x50FFFFFF);
    }

    private void txt(GuiGraphics g, String str, int x, int y, int color) {
        g.drawString(font, str, x, y, color, false);
    }

    private void txt(GuiGraphics g, String str, float x, float y, float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, str, 0, 0, color, false);
        g.pose().popPose();
    }

    private void ctxt(GuiGraphics g, String str, float cx, float y, float scale, int color) {
        txt(g, str, cx - font.width(str) * scale / 2f, y, scale, color);
    }

    private void rtxt(GuiGraphics g, String str, float rx, float y, float scale, int color) {
        txt(g, str, rx - font.width(str) * scale, y, scale, color);
    }

    private boolean hover(int x, int y, int w, int h) {
        return lmx >= x && lmx < x + w && lmy >= y && lmy < y + h;
    }

    private void hit(int x, int y, int w, int h, Runnable r) {
        hits.add(new Hit(x, y, w, h, r));
    }

    private void pill(GuiGraphics g, int x, int y, int w, int h, String label, int color, Runnable r) {
        boolean hv = hover(x, y, w, h);
        rrect(g, x, y, w, h, h / 2, lerp(color, WHITE, hv ? 0.35f : 0.2f), color);
        ctxt(g, label, x + w / 2f, y + (h - 8) / 2f + 0.5f, 1f, WHITE);
        hit(x, y, w, h, r);
    }

    private void appIcon(GuiGraphics g, int x, int y, int size, int c1, int c2, String glyph, String label, Runnable r) {
        rrect(g, x, y, size, size, 9, c1, c2);
        rrect(g, x + 1, y + 1, size - 2, size / 2, 8, 0x38FFFFFF, 0x05FFFFFF);
        ctxt(g, glyph, x + size / 2f, y + size / 2f - 7, 1.7f, WHITE);
        if (label != null) ctxt(g, label, x + size / 2f, y + size + 3, 0.72f, WHITE);
        hit(x, y, size, size + (label != null ? 10 : 0), r);
    }

    private void backButton(GuiGraphics g, String label, Page to) {
        int w = font.width(label) + 14;
        glass(g, 8, 24, w, 14, 7);
        txt(g, "< " + label, 13, 27, WHITE);
        hit(8, 24, w, 14, () -> page = to);
    }

    private void largeTitle(GuiGraphics g, String title) {
        txt(g, "§l" + title, 12, 44, 1.5f, WHITE);
    }

    private static String money(long v) { return String.format(Locale.US, "%,d", v); }

    private static double change(int c) {
        double[] h = ClientCrypto.hist[c];
        if (h.length < 2 || h[0] <= 0) return 0;
        return (h[h.length - 1] / h[0] - 1) * 100;
    }

    private static String pct(double v) { return String.format(Locale.US, "%+.2f%%", v); }

    // ------------------------------------------------------------ основной рендер

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0x88000000);
        lmx = (mouseX - ox) / s;
        lmy = (mouseY - oy) / s;
        hits.clear();

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale(s, s, 1f);

        // корпус и обои
        rrect(g, -4, -4, PW + 8, PH + 8, 24, 0xFF1C1C22);
        rrect(g, -2, -2, PW + 4, PH + 4, 22, 0xFF3A3A44);
        rrect(g, 0, 0, PW, PH, 20, 0xFF2B1F7A, 0xFFE0557C);
        g.enableScissor(0, 24, PW, PH - 20);
        rrect(g, -20, 70, 90, 90, 45, 0x55FF7AB8);
        rrect(g, 70, 150, 100, 100, 50, 0x554FA3FF);
        rrect(g, 20, 215, 80, 80, 40, 0x44FFB347);
        g.disableScissor();

        switch (page) {
            case HOME -> home(g);
            case CRYPTO -> crypto(g);
            case COIN -> coinPage(g);
            case MINING -> mining(g);
            case BANK -> bank(g);
            case WALLET -> wallet(g);
        }

        statusBar(g);
        // индикатор «домой»
        rrect(g, PW / 2 - 24, PH - 8, 48, 3, 1, 0xCCFFFFFF);
        hit(PW / 2 - 30, PH - 14, 60, 14, () -> page = Page.HOME);

        g.pose().popPose();
    }

    private void statusBar(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        long t = mc.level != null ? mc.level.getDayTime() % 24000L : 0;
        String time = String.format("%d:%02d", (int) ((t / 1000 + 6) % 24), (int) ((t % 1000) * 60 / 1000));
        txt(g, "§l" + time, 16, 8, WHITE);
        rrect(g, PW / 2 - 24, 6, 48, 13, 6, 0xFF000000);
        for (int i = 0; i < 4; i++) rrect(g, PW - 52 + i * 4, 15 - i * 2, 3, 3 + i * 2, 1, WHITE);
        rrect(g, PW - 31, 9, 17, 8, 3, 0x99FFFFFF);
        rrect(g, PW - 30, 10, 13, 6, 2, WHITE);
    }

    // ------------------------------------------------------------ главный экран

    private void home(GuiGraphics g) {
        // виджет кошелька
        glass(g, 10, 28, PW - 20, 62, 14);
        txt(g, "Кошелёк", 18, 34, GRAY);
        txt(g, "§l$" + money(ClientData.cash), 18, 45, 1.5f, WHITE);
        txt(g, "Банк $" + money(ClientData.bank), 18, 62, WHITE);
        double btc = ClientCrypto.prices[0];
        txt(g, "BTC " + Market.fmt(btc), 18, 74, GRAY);
        double ch = change(0);
        rtxt(g, pct(ch), PW - 18, 74, 1f, ch >= 0 ? GREEN : RED);

        // сетка приложений
        int y = 102;
        appIcon(g, 12, y, 28, 0xFFF7931A, 0xFFFFC857, "B", "Крипто", () -> page = Page.CRYPTO);
        appIcon(g, 46, y, 28, 0xFF2D2D3A, 0xFF5B5B78, "M", "Майнинг", () -> page = Page.MINING);
        appIcon(g, 80, y, 28, 0xFF0A84FF, 0xFF64D2FF, "$", "Банк", () -> page = Page.BANK);
        appIcon(g, 114, y, 28, 0xFF30D158, 0xFF8EF2A6, "W", "Кошелёк", () -> page = Page.WALLET);

        // виджет майнинга
        glass(g, 10, 146, PW - 20, 54, 14);
        txt(g, "Майнинг-ферма", 18, 152, GRAY);
        txt(g, String.format(Locale.US, "§l%.0f МХ/с", ClientCrypto.hash), 18, 164, 1.3f, WHITE);
        txt(g, String.format(Locale.US, "~$%.1f / мин", ClientCrypto.income), 18, 182, GREEN);
        rtxt(g, "→ " + Market.SYM[ClientCrypto.target], PW - 18, 182, 1f, GRAY);
        hit(10, 146, PW - 20, 54, () -> { page = Page.MINING; miningTab = 1; });

        // док
        glass(g, 8, PH - 56, PW - 16, 40, 16);
        appIcon(g, 22, PH - 50, 28, 0xFFF7931A, 0xFFFFC857, "B", null, () -> page = Page.CRYPTO);
        appIcon(g, 61, PH - 50, 28, 0xFF2D2D3A, 0xFF5B5B78, "M", null, () -> page = Page.MINING);
        appIcon(g, 100, PH - 50, 28, 0xFF0A84FF, 0xFF64D2FF, "$", null, () -> page = Page.BANK);
    }

    // ------------------------------------------------------------ крипта

    private void crypto(GuiGraphics g) {
        backButton(g, "Домой", Page.HOME);
        largeTitle(g, "Крипто");
        double total = 0;
        for (int c = 0; c < 4; c++) total += ClientCrypto.hold[c] * ClientCrypto.prices[c];
        txt(g, "Портфель: $" + money((long) total), 14, 62, GRAY);

        int y = 76;
        for (int c = 0; c < 4; c++) {
            final int cc = c;
            glass(g, 10, y, PW - 20, 40, 12);
            rrect(g, 17, y + 9, 22, 22, 11, COIN_COLORS[c][0], COIN_COLORS[c][1]);
            ctxt(g, "§l" + Market.SYM[c].charAt(0), 28, y + 16, 1f, WHITE);
            txt(g, "§l" + Market.NAME[c], 46, y + 9, WHITE);
            txt(g, Market.SYM[c], 46, y + 22, 0.8f, GRAY);
            rtxt(g, "$" + Market.fmt(ClientCrypto.prices[c]), PW - 17, y + 9, 1f, WHITE);
            double ch = change(c);
            rtxt(g, pct(ch), PW - 17, y + 22, 0.9f, ch >= 0 ? GREEN : RED);
            hit(10, y, PW - 20, 40, () -> { coin = cc; page = Page.COIN; });
            y += 46;
        }
        txt(g, "Комиссия сделки 1%", 14, y + 2, 0.8f, GRAY);
    }

    private void coinPage(GuiGraphics g) {
        backButton(g, "Рынок", Page.CRYPTO);
        double price = ClientCrypto.prices[coin];
        double ch = change(coin);
        txt(g, "§l" + Market.NAME[coin], 12, 44, 1.2f, WHITE);
        txt(g, "§l$" + Market.fmt(price), 12, 60, 1.6f, WHITE);
        txt(g, pct(ch) + "  (~7 мин)", 12, 78, 0.9f, ch >= 0 ? GREEN : RED);

        // график
        int cx = 10, cy = 92, cw = PW - 20, chh = 70;
        glass(g, cx, cy, cw, chh, 12);
        double[] h = ClientCrypto.hist[coin];
        if (h.length >= 2) {
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (double v : h) { min = Math.min(min, v); max = Math.max(max, v); }
            if (max - min < 1e-9) { max = min + 1; }
            int px0 = cx + 8, pw = cw - 16, py0 = cy + 8, ph = chh - 16;
            int lineColor = ch >= 0 ? GREEN : RED;
            int fillColor = ch >= 0 ? 0x2230D158 : 0x22FF453A;
            int prevY = -1;
            for (int x = 0; x < pw; x++) {
                double t = x / (double) (pw - 1) * (h.length - 1);
                int i = Math.min(h.length - 2, (int) t);
                double v = h[i] + (h[i + 1] - h[i]) * (t - i);
                int yy = py0 + ph - (int) Math.round((v - min) / (max - min) * ph);
                g.fill(px0 + x, yy, px0 + x + 1, py0 + ph, fillColor);
                int y1 = prevY < 0 ? yy : Math.min(prevY, yy);
                int y2 = prevY < 0 ? yy : Math.max(prevY, yy);
                g.fill(px0 + x, y1, px0 + x + 1, y2 + 2, lineColor);
                prevY = yy;
            }
        }

        // портфель
        double hold = ClientCrypto.hold[coin];
        glass(g, 10, 168, PW - 20, 34, 12);
        txt(g, "В портфеле", 18, 173, GRAY);
        txt(g, String.format(Locale.US, "§l%.6f %s", hold, Market.SYM[coin]), 18, 184, WHITE);
        rtxt(g, "~$" + money((long) (hold * price)), PW - 18, 184, 1f, GRAY);

        txt(g, "Купить (наличные: $" + money(ClientData.cash) + ")", 14, 210, 0.85f, GRAY);
        long[] buys = {100, 500, 2000};
        int bw = 40;
        for (int i = 0; i < 3; i++) {
            final long amt = buys[i];
            pill(g, 10 + i * (bw + 5), 222, bw, 16, "$" + amt, 0xFF28A745, () -> cmd("crypto buy " + coin + " " + amt));
        }
        txt(g, "Продать", 14, 244, 0.85f, GRAY);
        int[] sells = {25, 50, 100};
        for (int i = 0; i < 3; i++) {
            final int pc = sells[i];
            pill(g, 10 + i * (bw + 5), 256, bw, 16, pc == 100 ? "Всё" : pc + "%", 0xFFD93A3A, () -> cmd("crypto sell " + coin + " " + pc));
        }
    }

    // ------------------------------------------------------------ майнинг

    private void mining(GuiGraphics g) {
        backButton(g, "Домой", Page.HOME);
        largeTitle(g, "Майнинг");

        // сегментированный переключатель
        glass(g, 10, 62, PW - 20, 16, 8);
        int half = (PW - 20) / 2;
        rrect(g, 12 + miningTab * (half - 1), 64, half - 2, 12, 6, 0xAAFFFFFF);
        ctxt(g, "Магазин", 10 + half / 2f, 66, 1f, miningTab == 0 ? 0xFF1C1C22 : WHITE);
        ctxt(g, "Ферма", 10 + half + half / 2f, 66, 1f, miningTab == 1 ? 0xFF1C1C22 : WHITE);
        hit(10, 62, half, 16, () -> miningTab = 0);
        hit(10 + half, 62, half, 16, () -> miningTab = 1);

        if (miningTab == 0) miningShop(g); else miningFarm(g);
    }

    private int owned(String id) {
        return switch (id) {
            case "rack" -> ClientCrypto.racks;
            case "gpu0" -> ClientCrypto.gpus[0];
            case "gpu1" -> ClientCrypto.gpus[1];
            case "gpu2" -> ClientCrypto.gpus[2];
            case "cool0" -> ClientCrypto.coolant[0];
            case "cool1" -> ClientCrypto.coolant[1];
            default -> ClientCrypto.coolant[2];
        };
    }

    private void miningShop(GuiGraphics g) {
        int y = 84;
        for (Mining.Item it : Mining.SHOP) {
            glass(g, 10, y, PW - 20, 25, 9);
            txt(g, "§l" + it.name(), 16, y + 4, 0.75f, WHITE);
            String own = it.id().startsWith("cool") ? "в баке: " + owned(it.id()) : "есть: " + owned(it.id());
            txt(g, it.desc() + " · " + own, 16, y + 14, 0.62f, GRAY);
            final String id = it.id();
            pill(g, PW - 10 - 40, y + 4, 36, 17, "$" + it.price(), 0xFF0A84FF, () -> cmd("mining buy " + id));
            y += 27;
        }
    }

    private void miningFarm(GuiGraphics g) {
        int slots = ClientCrypto.racks * Mining.SLOTS_PER_RACK;
        int cards = ClientCrypto.gpus[0] + ClientCrypto.gpus[1] + ClientCrypto.gpus[2];

        glass(g, 10, 84, PW - 20, 92, 12);
        txt(g, "Стойки: §l" + ClientCrypto.racks + "§r  (слотов " + slots + ")", 18, 90, WHITE);
        txt(g, "Карты: " + ClientCrypto.gpus[0] + " / " + ClientCrypto.gpus[1] + " / " + ClientCrypto.gpus[2]
                + (cards > slots ? " §c(не хватает стоек)" : ""), 18, 102, WHITE);
        txt(g, String.format(Locale.US, "Хешрейт: §l%.0f МХ/с", ClientCrypto.hash), 18, 114, WHITE);
        String coolText = ClientCrypto.racks == 0 ? "-" : String.format(Locale.US, "x%.1f", ClientCrypto.mult);
        txt(g, "Охлаждение: §l" + coolText + (ClientCrypto.mult < 1 && ClientCrypto.racks > 0 ? " §c(нет жидкости)" : ""), 18, 126, WHITE);
        txt(g, String.format(Locale.US, "Доход: §a§l$%.1f §r§aв минуту", ClientCrypto.income), 18, 138, WHITE);
        txt(g, "Жидкость: " + ClientCrypto.coolant[0] + " / " + ClientCrypto.coolant[1] + " / " + ClientCrypto.coolant[2],
                18, 150, GRAY);
        txt(g, "1 ед. на стойку каждые 5 мин", 18, 162, 0.75f, GRAY);

        txt(g, "Майнить монету:", 14, 184, 0.9f, GRAY);
        int bw = 30;
        for (int c = 0; c < 4; c++) {
            final int cc = c;
            boolean sel = ClientCrypto.target == c;
            rrect(g, 10 + c * (bw + 3), 196, bw, 16, 8,
                    sel ? COIN_COLORS[c][1] : 0x44FFFFFF, sel ? COIN_COLORS[c][0] : 0x22FFFFFF);
            ctxt(g, Market.SYM[c], 10 + c * (bw + 3) + bw / 2f, 200, 0.85f, WHITE);
            hit(10 + c * (bw + 3), 196, bw, 16, () -> cmd("mining target " + cc));
        }

        glass(g, 10, 220, PW - 20, 46, 12);
        txt(g, "Намайнено в портфеле:", 18, 225, GRAY);
        txt(g, String.format(Locale.US, "§l%.6f %s", ClientCrypto.hold[ClientCrypto.target], Market.SYM[ClientCrypto.target]), 18, 237, WHITE);
        txt(g, "Ферма работает, пока вы в сети", 18, 253, 0.75f, GRAY);
    }

    // ------------------------------------------------------------ банк и кошелёк

    private void bank(GuiGraphics g) {
        backButton(g, "Домой", Page.HOME);
        largeTitle(g, "Банк");

        glass(g, 10, 64, PW - 20, 64, 14);
        txt(g, "На руках", 18, 70, GRAY);
        rtxt(g, "$" + money(ClientData.cash), PW - 18, 70, 1f, WHITE);
        txt(g, "На счёте", 18, 84, GRAY);
        rtxt(g, "$" + money(ClientData.bank), PW - 18, 84, 1f, 0xFF64D2FF);
        txt(g, "Кредит", 18, 98, GRAY);
        rtxt(g, "$" + money(ClientData.credit), PW - 18, 98, 1f, ClientData.credit > 0 ? RED : GRAY);
        txt(g, "Фишки казино", 18, 112, GRAY);
        rtxt(g, money(ClientData.chips), PW - 18, 112, 1f, 0xFFFF9FF3);

        String[][] btns = {
                {"Внести 100", "bank deposit 100"}, {"Внести 500", "bank deposit 500"},
                {"Снять 100", "bank withdraw 100"}, {"Снять 500", "bank withdraw 500"},
                {"Внести всё", "bank depositall"}, {"Снять всё", "bank withdrawall"},
                {"Кредит 500", "bank credit 500"}, {"Погасить 500", "bank repay 500"},
                {"Карта (50 $)", "bank card"}, {"Погасить всё", "bank repay 100000"},
        };
        for (int i = 0; i < btns.length; i++) {
            final String c = btns[i][1];
            int col = i % 2, row = i / 2;
            int color = i >= 6 ? 0xFF8E5CF6 : (col == 0 ? 0xFF28A745 : 0xFF0A84FF);
            pill(g, 10 + col * 67, 138 + row * 22, 63, 18, btns[i][0], color, () -> cmd(c));
        }
        txt(g, "Кредит до 5000 $, комиссия 10%", 14, 252, 0.75f, GRAY);
    }

    private void wallet(GuiGraphics g) {
        backButton(g, "Домой", Page.HOME);
        largeTitle(g, "Кошелёк");

        double cryptoUsd = 0;
        for (int c = 0; c < 4; c++) cryptoUsd += ClientCrypto.hold[c] * ClientCrypto.prices[c];
        long net = ClientData.cash + ClientData.bank + ClientData.chips + (long) cryptoUsd - ClientData.credit;

        glass(g, 10, 64, PW - 20, 36, 14);
        txt(g, "Чистый капитал", 18, 70, GRAY);
        txt(g, "§l$" + money(net), 18, 82, 1.4f, WHITE);

        glass(g, 10, 106, PW - 20, 74, 12);
        String[][] rows = {
                {"Наличные", "$" + money(ClientData.cash)},
                {"Банковский счёт", "$" + money(ClientData.bank)},
                {"Фишки", money(ClientData.chips)},
                {"Кредит", "-$" + money(ClientData.credit)},
                {"Заработано", "$" + money(ClientData.earned)},
        };
        for (int i = 0; i < rows.length; i++) {
            txt(g, rows[i][0], 18, 112 + i * 13, GRAY);
            rtxt(g, rows[i][1], PW - 18, 112 + i * 13, 1f, WHITE);
        }

        glass(g, 10, 186, PW - 20, 76, 12);
        txt(g, "Криптовалюта", 18, 191, GRAY);
        for (int c = 0; c < 4; c++) {
            txt(g, String.format(Locale.US, "%.5f %s", ClientCrypto.hold[c], Market.SYM[c]), 18, 203 + c * 13, WHITE);
            rtxt(g, "$" + money((long) (ClientCrypto.hold[c] * ClientCrypto.prices[c])), PW - 18, 203 + c * 13, 1f, GRAY);
        }
    }

    // ------------------------------------------------------------ ввод

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            double lx = (mx - ox) / s, ly = (my - oy) / s;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (lx >= h.x && lx < h.x + h.w && ly >= h.y && ly < h.y + h.h) {
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    h.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
