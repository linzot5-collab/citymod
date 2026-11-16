package com.citymod.economy;

import com.citymod.network.Net;
import com.citymod.network.SyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Единая точка работы с деньгами. Вся логика только на сервере. */
public final class Economy {
    public static final long CARD_PRICE = 50;
    public static final long MAX_CREDIT = 5000;
    /** Процент, который добавляется к долгу при взятии кредита. */
    public static final int CREDIT_FEE_PERCENT = 10;

    private Economy() {}

    public static EconomyData data(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EconomyData::new, EconomyData::load, null), EconomyData.NAME);
    }

    public static EconomyData.Account acc(ServerPlayer p) {
        return data(p.getServer()).get(p.getUUID());
    }

    public static EconomyData.Account acc(MinecraftServer s, UUID id) {
        return data(s).get(id);
    }

    public static void save(ServerPlayer p) {
        data(p.getServer()).setDirty();
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        EconomyData.Account a = acc(p);
        Net.toPlayer(p, new SyncPacket(a.cash, a.bank, a.credit, a.earned, a.chips, a.job));
    }

    /** Выплата за работу — наличными и в статистику заработка. */
    public static void pay(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        a.cash += amount;
        a.earned += amount;
        save(p);
    }

    public static boolean spendCash(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.cash < amount) return false;
        a.cash -= amount;
        save(p);
        return true;
    }

    public static boolean deposit(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.cash < amount) return false;
        a.cash -= amount;
        a.bank += amount;
        save(p);
        return true;
    }

    public static boolean withdraw(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.bank < amount) return false;
        a.bank -= amount;
        a.cash += amount;
        save(p);
        return true;
    }

    /** Взять кредит: деньги приходят на банковский счёт, долг = сумма + комиссия. */
    public static String takeCredit(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (!a.hasCard) return "Сначала оформите банковскую карту.";
        if (amount <= 0) return "Сумма должна быть больше нуля.";
        if (a.credit + amount > MAX_CREDIT) return "Лимит кредита: " + MAX_CREDIT + ".";
        a.bank += amount;
        a.credit += amount + amount * CREDIT_FEE_PERCENT / 100;
        save(p);
        return null;
    }

    /** Погасить кредит с банковского счёта. Возвращает сообщение об ошибке или null. */
    public static String repayCredit(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (a.credit <= 0) return "У вас нет кредита.";
        if (amount <= 0) return "Сумма должна быть больше нуля.";
        long pay = Math.min(amount, a.credit);
        if (a.bank < pay) return "Недостаточно денег на счёте.";
        a.bank -= pay;
        a.credit -= pay;
        save(p);
        return null;
    }

    public static boolean transferCash(ServerPlayer from, ServerPlayer to, long amount) {
        EconomyData.Account a = acc(from);
        if (amount <= 0 || a.cash < amount) return false;
        a.cash -= amount;
        acc(to).cash += amount;
        save(from);
        save(to);
        return true;
    }

    // ---------- фишки казино ----------

    public static boolean buyChips(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.cash < amount) return false;
        a.cash -= amount;
        a.chips += amount;
        save(p);
        return true;
    }

    public static boolean sellChips(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.chips < amount) return false;
        a.chips -= amount;
        a.cash += amount;
        save(p);
        return true;
    }

    /** Списать фишки (ставка). */
    public static boolean takeChips(ServerPlayer p, long amount) {
        EconomyData.Account a = acc(p);
        if (amount <= 0 || a.chips < amount) return false;
        a.chips -= amount;
        save(p);
        return true;
    }

    /** Начислить фишки (выигрыш или возврат). Работает и для игроков не в сети. */
    public static void addChips(MinecraftServer s, UUID id, long amount) {
        if (amount <= 0) return;
        acc(s, id).chips += amount;
        data(s).setDirty();
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        if (p != null) sync(p);
    }
}
