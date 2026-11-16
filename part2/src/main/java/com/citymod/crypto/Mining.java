package com.citymod.crypto;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.util.Msg;
import com.citymod.util.Sfx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Майнинг-ферма. Стойка вмещает 6 видеокарт. Доход в минуту = хешрейт * 0.03 $ * множитель охлаждения.
 * Раз в 5 минут каждая стойка тратит 1 единицу охлаждающей жидкости (берётся лучшая из доступных).
 * Без жидкости ферма работает вполсилы. Ферма работает, пока игрок в сети.
 */
public final class Mining {
    public static final int SLOTS_PER_RACK = 6;
    public static final double[] HASH = {20, 60, 150};          // МХ/с на карту
    public static final double[] COOL_MULT = {1.0, 1.2, 1.4};   // вода / хладагент / иммерсионная жидкость
    public static final double NO_COOL_MULT = 0.5;
    public static final double USD_PER_MH_MIN = 0.03;

    public record Item(String id, String name, String desc, long price) {}

    public static final Item[] SHOP = {
            new Item("rack", "Майнинг-стойка", "6 слотов под карты", 400),
            new Item("gpu0", "GTX 1660 Super", "20 МХ/с", 250),
            new Item("gpu1", "RTX 3070", "60 МХ/с", 900),
            new Item("gpu2", "RTX 4090", "150 МХ/с", 2500),
            new Item("cool0", "Дистиллят x10", "охлаждение x1.0", 30),
            new Item("cool1", "Хладагент x10", "охлаждение x1.2", 100),
            new Item("cool2", "Иммерс. жидкость x10", "охлаждение x1.4", 250),
    };

    private Mining() {}

    public static int usedSlots(EconomyData.Account a) {
        return Math.min(a.racks * SLOTS_PER_RACK, a.gpus[0] + a.gpus[1] + a.gpus[2]);
    }

    /** Суммарный хешрейт: лучшие карты занимают слоты первыми. */
    public static double hashrate(EconomyData.Account a) {
        int slots = a.racks * SLOTS_PER_RACK;
        double h = 0;
        for (int t = 2; t >= 0 && slots > 0; t--) {
            int n = Math.min(slots, a.gpus[t]);
            h += n * HASH[t];
            slots -= n;
        }
        return h;
    }

    public static double coolMult(EconomyData.Account a) {
        if (a.racks == 0) return 0;
        for (int t = 2; t >= 0; t--) if (a.coolant[t] > 0) return COOL_MULT[t];
        return NO_COOL_MULT;
    }

    public static double incomeUsdPerMin(EconomyData.Account a) {
        return hashrate(a) * USD_PER_MH_MIN * coolMult(a);
    }

    public static void buy(ServerPlayer p, String id) {
        Item item = null;
        for (Item i : SHOP) if (i.id.equals(id)) item = i;
        if (item == null) { Msg.shop(p, "Такого товара нет."); return; }
        EconomyData.Account a = Economy.acc(p);

        if (id.startsWith("gpu") && a.gpus[0] + a.gpus[1] + a.gpus[2] >= 60) {
            Msg.shop(p, "Склад заполнен: максимум 60 видеокарт.");
            return;
        }
        if (id.equals("rack") && a.racks >= 10) { Msg.shop(p, "Максимум 10 стоек."); return; }
        if (!Economy.spendCash(p, item.price)) { Msg.shop(p, "Не хватает наличных: нужно " + item.price + " $."); return; }

        switch (id) {
            case "rack" -> a.racks++;
            case "gpu0" -> a.gpus[0]++;
            case "gpu1" -> a.gpus[1]++;
            case "gpu2" -> a.gpus[2]++;
            case "cool0" -> a.coolant[0] += 10;
            case "cool1" -> a.coolant[1] += 10;
            default -> a.coolant[2] += 10;
        }
        Economy.save(p);
        Sfx.coin(p);
        Msg.shop(p, "Куплено: " + item.name + " за " + item.price + " $.");
        if (id.startsWith("gpu") && usedSlots(a) < a.gpus[0] + a.gpus[1] + a.gpus[2]) {
            Msg.shop(p, "§eНе хватает слотов: карты вне стоек не работают. Купите ещё стойку.");
        }
        PhoneSync.send(p);
    }

    public static void setTarget(ServerPlayer p, int coin) {
        if (coin < 0 || coin > 3) return;
        Economy.acc(p).target = coin;
        Economy.save(p);
        PhoneSync.send(p);
    }

    /** Раз в минуту для каждого игрока онлайн. */
    public static void tick(MinecraftServer s, long minute) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            EconomyData.Account a = Economy.acc(p);
            if (a.racks == 0 || hashrate(a) <= 0) continue;

            double usd = incomeUsdPerMin(a);
            double price = Market.price(s, a.target);
            double coins = usd / price;
            a.crypto[a.target] += coins;

            // расход охлаждающей жидкости раз в 5 минут
            if (minute % 5 == 0) {
                int need = a.racks;
                for (int t = 2; t >= 0 && need > 0; t--) {
                    int take = Math.min(a.coolant[t], need);
                    a.coolant[t] -= take;
                    need -= take;
                }
                if (a.coolant[0] + a.coolant[1] + a.coolant[2] == 0) {
                    Msg.shop(p, "§cОхлаждающая жидкость закончилась! Ферма работает на 50%.");
                }
            }
            Economy.save(p);
            Msg.bar(p, "§7Майнинг: §a+" + String.format(Locale.US, "%.6f", coins) + " " + Market.SYM[a.target]);
            Sfx.click(p);
            PhoneSync.send(p);
        }
    }
}
