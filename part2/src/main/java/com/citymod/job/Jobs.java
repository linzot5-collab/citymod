package com.citymod.job;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.util.Msg;
import com.citymod.util.Sfx;
import net.minecraft.server.level.ServerPlayer;

/** Работы и оплата за каждое действие. */
public final class Jobs {
    public static final String LUMBERJACK = "lumberjack";
    public static final String LOADER = "loader";
    public static final String ROADWORKER = "roadworker";
    public static final String COLLECTOR = "collector";

    public static final long LOG_PAY = 4;      // за бревно в лесопосадке
    public static final long CRATE_PAY = 12;   // за доставленный ящик
    public static final long ROAD_PAY = 10;    // за починенный участок дороги
    public static final long ATM_PAY = 25;     // за каждый инкассированный банкомат
    public static final int ATM_COOLDOWN_TICKS = 20 * 300; // банкомат снова наполняется за 5 минут

    private Jobs() {}

    public static String title(String job) {
        return switch (job) {
            case LUMBERJACK -> "Дровосек";
            case LOADER -> "Грузчик";
            case ROADWORKER -> "Ремонтник дорог";
            case COLLECTOR -> "Инкассатор";
            default -> "безработный";
        };
    }

    public static boolean valid(String job) {
        return job.equals(LUMBERJACK) || job.equals(LOADER) || job.equals(ROADWORKER) || job.equals(COLLECTOR) || job.equals("none");
    }

    public static void setJob(ServerPlayer p, String job) {
        EconomyData.Account a = Economy.acc(p);
        if (job.equals("none")) {
            a.job = "";
            a.crates = 0;
            a.bag = 0;
            Economy.save(p);
            Msg.job(p, "Вы уволились.");
            return;
        }
        a.job = job;
        a.crates = 0;
        a.bag = 0;
        Economy.save(p);
        Msg.job(p, "Теперь вы: §e" + title(job) + "§f. " + hint(job));
    }

    public static String hint(String job) {
        return switch (job) {
            case LUMBERJACK -> "Рубите брёвна на лесопосадке (юго-восток города): " + LOG_PAY + " $ за бревно. Деревья отрастают.";
            case LOADER -> "Берите ящики со стеллажей на складе и несите к зоне разгрузки у входа: " + CRATE_PAY + " $ за ящик.";
            case ROADWORKER -> "Находите треснувшие участки дороги и чините (ПКМ): " + ROAD_PAY + " $ за участок.";
            case COLLECTOR -> "ПКМ по банкомату — забрать наличные (до 5 штук), затем сдайте мешок в хранилище банка: " + ATM_PAY + " $ за банкомат.";
            default -> "";
        };
    }

    public static boolean has(ServerPlayer p, String job) {
        return Economy.acc(p).job.equals(job);
    }

    public static void earn(ServerPlayer p, long amount, String what) {
        Economy.pay(p, amount);
        Msg.bar(p, "§a+" + amount + " $ §7(" + what + ")");
        Sfx.coin(p);
    }
}
