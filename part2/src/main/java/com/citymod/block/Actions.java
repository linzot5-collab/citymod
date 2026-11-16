package com.citymod.block;

import com.citymod.bank.BankQueue;
import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.job.Jobs;
import com.citymod.city.City;
import com.citymod.network.Net;
import com.citymod.network.OpenMenuPacket;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import com.citymod.util.Sfx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/** Поведение функциональных блоков города. */
public final class Actions {
    private static final Map<Long, Long> ATM_READY = new HashMap<>(); // позиция банкомата -> тик, когда снова полон

    private Actions() {}

    private static void menu(ServerPlayer p, String id) {
        Net.toPlayer(p, new OpenMenuPacket(id));
    }

    // ---- банк ----

    public static void atm(ServerPlayer p, ServerLevel l, BlockPos pos) {
        EconomyData.Account a = Economy.acc(p);

        // инкассатор забирает наличные; с Shift можно пользоваться банкоматом как обычно
        if (a.job.equals(Jobs.COLLECTOR) && !p.isShiftKeyDown()) {
            if (a.bag >= 5) { Msg.job(p, "Мешок полон (5/5). Сдайте его в хранилище банка."); return; }
            long ready = ATM_READY.getOrDefault(pos.asLong(), 0L);
            if (Sched.tick < ready) {
                long left = (ready - Sched.tick) / 20;
                Msg.job(p, "Банкомат пуст. Наполнится через " + left + " с.");
                return;
            }
            ATM_READY.put(pos.asLong(), Sched.tick + Jobs.ATM_COOLDOWN_TICKS);
            a.bag++;
            Economy.save(p);
            Sfx.coin(p);
            Msg.job(p, "Наличные собраны. В мешке банкоматов: " + a.bag + "/5. (Shift+ПКМ — пользоваться банкоматом)");
            return;
        }

        if (!a.hasCard) { Msg.bank(p, "Для банкомата нужна карта. Оформите её у окна банка."); return; }
        menu(p, "bank");
    }

    public static void bankDesk(ServerPlayer p, ServerLevel l, BlockPos pos) {
        BankQueue.join(p);
    }

    public static void bankVault(ServerPlayer p, ServerLevel l, BlockPos pos) {
        EconomyData.Account a = Economy.acc(p);
        if (!a.job.equals(Jobs.COLLECTOR)) { Msg.bank(p, "Хранилище. Вход только для инкассаторов."); return; }
        if (a.bag <= 0) { Msg.job(p, "Мешок пуст. Соберите наличные из банкоматов."); return; }
        long pay = a.bag * Jobs.ATM_PAY;
        int n = a.bag;
        a.bag = 0;
        Jobs.earn(p, pay, "инкассация, банкоматов: " + n);
    }

    // ---- работы ----

    public static void jobBoard(ServerPlayer p, ServerLevel l, BlockPos pos) {
        menu(p, "jobs");
    }

    public static void cratePile(ServerPlayer p, ServerLevel l, BlockPos pos) {
        EconomyData.Account a = Economy.acc(p);
        if (!a.job.equals(Jobs.LOADER)) { Msg.job(p, "Грузчики берут ящики отсюда. Устроиться: биржа труда."); return; }
        if (a.crates > 0) { Msg.job(p, "Вы уже несёте ящик. Отнесите его к зоне разгрузки."); return; }
        a.crates = 1;
        Economy.save(p);
        Sfx.click(p);
        Msg.job(p, "Вы взяли ящик (вы замедлены). Несите его к зоне разгрузки у входа.");
    }

    public static void deliveryPoint(ServerPlayer p, ServerLevel l, BlockPos pos) {
        EconomyData.Account a = Economy.acc(p);
        if (!a.job.equals(Jobs.LOADER)) { Msg.job(p, "Зона разгрузки склада."); return; }
        if (a.crates <= 0) { Msg.job(p, "У вас нет ящика. Возьмите его со стеллажа."); return; }
        a.crates = 0;
        Jobs.earn(p, Jobs.CRATE_PAY, "ящик доставлен");
    }

    public static void crackedRoad(ServerPlayer p, ServerLevel l, BlockPos pos) {
        if (!Jobs.has(p, Jobs.ROADWORKER)) { Msg.job(p, "Дорога разбита. Чинить её могут дорожные рабочие (биржа труда)."); return; }
        l.setBlock(pos, Blocks.GRAY_CONCRETE.defaultBlockState(), 3);
        City.crackRepaired();
        Jobs.earn(p, Jobs.ROAD_PAY, "ремонт дороги");
    }

    // ---- казино ----

    public static void chipBooth(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "chips"); }
    public static void rouletteTable(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "roulette"); }
    public static void blackjackTable(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "blackjack"); }
    public static void diceTable(ServerPlayer p, ServerLevel l, BlockPos pos) {
        menu(p, "dice");
        Msg.casino(p, "Против игрока: /dice <фишки> <ник>");
    }
    public static void wheel(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "wheel"); }
    public static void horseBoard(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "horses"); }
    public static void slotMachine(ServerPlayer p, ServerLevel l, BlockPos pos) { menu(p, "slots"); }
}
