package com.citymod.crypto;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.util.Msg;
import com.citymod.util.Sfx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Random;

/** Биржа: 4 монеты, цены меняются каждые 10 секунд (случайное блуждание с возвратом к базе). */
public final class Market {
    public static final String[] SYM = {"BTC", "ETH", "SOL", "DOGE"};
    public static final String[] NAME = {"Bitcoin", "Ethereum", "Solana", "Dogecoin"};
    public static final double[] BASE = {42000, 2400, 95, 0.12};
    public static final double[] VOL = {0.008, 0.012, 0.020, 0.030};
    public static final int HIST = 40;
    public static final double FEE = 0.01;

    private static final Random RND = new Random();

    private Market() {}

    public static void tick(MinecraftServer s) {
        CryptoData d = CryptoData.get(s);
        for (int c = 0; c < 4; c++) {
            double p = d.prices[c];
            double pull = Math.log(BASE[c] / p) * 0.01;            // тянет к базовой цене
            double step = RND.nextGaussian() * VOL[c] + pull;
            if (RND.nextInt(120) == 0) step += (RND.nextBoolean() ? 1 : -1) * VOL[c] * 6; // редкий резкий скачок
            p = p * Math.exp(step);
            p = Math.max(BASE[c] * 0.2, Math.min(BASE[c] * 5, p));
            d.prices[c] = p;
            System.arraycopy(d.hist[c], 1, d.hist[c], 0, HIST - 1);
            d.hist[c][HIST - 1] = p;
        }
        d.setDirty();
    }

    public static double price(MinecraftServer s, int coin) {
        return CryptoData.get(s).prices[coin];
    }

    public static String fmt(double v) {
        if (v >= 1000) return String.format(Locale.US, "%,.0f", v);
        if (v >= 1) return String.format(Locale.US, "%.2f", v);
        return String.format(Locale.US, "%.4f", v);
    }

    public static void buy(ServerPlayer p, int coin, long usd) {
        if (coin < 0 || coin > 3 || usd < 1) return;
        if (!Economy.spendCash(p, usd)) { Msg.bank(p, "Недостаточно наличных: нужно " + usd + " $."); return; }
        double price = price(p.getServer(), coin);
        double amount = usd * (1 - FEE) / price;
        EconomyData.Account a = Economy.acc(p);
        a.crypto[coin] += amount;
        Economy.save(p);
        Sfx.coin(p);
        Msg.bar(p, "§aКуплено " + String.format(Locale.US, "%.6f", amount) + " " + SYM[coin]);
        PhoneSync.send(p);
    }

    public static void sell(ServerPlayer p, int coin, int percent) {
        if (coin < 0 || coin > 3 || percent < 1) return;
        EconomyData.Account a = Economy.acc(p);
        double hold = a.crypto[coin];
        if (hold <= 0) { Msg.bank(p, "У вас нет " + SYM[coin] + "."); return; }
        double amount = percent >= 100 ? hold : hold * percent / 100.0;
        long usd = (long) Math.floor(amount * price(p.getServer(), coin) * (1 - FEE));
        if (usd < 1) { Msg.bank(p, "Слишком маленькая сумма для продажи."); return; }
        a.crypto[coin] = percent >= 100 ? 0 : hold - amount;
        a.cash += usd;
        Economy.save(p);
        Sfx.coin(p);
        Msg.bar(p, "§aПродано " + String.format(Locale.US, "%.6f", amount) + " " + SYM[coin] + " за " + usd + " $");
        PhoneSync.send(p);
    }
}
