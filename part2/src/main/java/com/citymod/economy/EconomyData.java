package com.citymod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Хранит счета всех игроков в данных мира (сохраняется между перезапусками). */
public class EconomyData extends SavedData {
    public static final String NAME = "citymod_economy";

    public static class Account {
        public long cash = 500;      // наличные на руках (стартовые деньги)
        public long bank = 0;        // деньги на банковском счёте
        public long credit = 0;      // долг по кредиту
        public long earned = 0;      // всего заработано (статистика)
        public long chips = 0;       // фишки казино
        public boolean hasCard = false;
        public String job = "";      // текущая работа
        public int crates = 0;       // несёт ящик (грузчик)
        public int bag = 0;          // собрано банкоматов (инкассатор)
        public double[] crypto = new double[4]; // BTC, ETH, SOL, DOGE
        public int racks = 0;                   // майнинг-стойки
        public int[] gpus = new int[3];         // видеокарты трёх классов
        public int[] coolant = new int[3];      // запас охлаждающих жидкостей
        public int target = 0;                  // какую монету майним
    }

    private final Map<UUID, Account> accounts = new HashMap<>();

    public Account get(UUID id) {
        return accounts.computeIfAbsent(id, k -> new Account());
    }

    public static EconomyData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyData data = new EconomyData();
        ListTag list = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Account a = new Account();
            a.cash = t.getLong("cash");
            a.bank = t.getLong("bank");
            a.credit = t.getLong("credit");
            a.earned = t.getLong("earned");
            a.chips = t.getLong("chips");
            a.hasCard = t.getBoolean("hasCard");
            a.job = t.getString("job");
            a.crates = t.getInt("crates");
            a.bag = t.getInt("bag");
            for (int k = 0; k < 4; k++) a.crypto[k] = t.getDouble("crypto" + k);
            a.racks = t.getInt("racks");
            for (int k = 0; k < 3; k++) {
                a.gpus[k] = t.getInt("gpu" + k);
                a.coolant[k] = t.getInt("cool" + k);
            }
            a.target = Math.floorMod(t.getInt("target"), 4);
            data.accounts.put(t.getUUID("id"), a);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Account> e : accounts.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            Account a = e.getValue();
            t.putLong("cash", a.cash);
            t.putLong("bank", a.bank);
            t.putLong("credit", a.credit);
            t.putLong("earned", a.earned);
            t.putLong("chips", a.chips);
            t.putBoolean("hasCard", a.hasCard);
            t.putString("job", a.job);
            t.putInt("crates", a.crates);
            t.putInt("bag", a.bag);
            for (int k = 0; k < 4; k++) t.putDouble("crypto" + k, a.crypto[k]);
            t.putInt("racks", a.racks);
            for (int k = 0; k < 3; k++) {
                t.putInt("gpu" + k, a.gpus[k]);
                t.putInt("cool" + k, a.coolant[k]);
            }
            t.putInt("target", a.target);
            list.add(t);
        }
        tag.put("accounts", list);
        return tag;
    }
}
