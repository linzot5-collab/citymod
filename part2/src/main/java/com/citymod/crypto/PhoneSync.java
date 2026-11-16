package com.citymod.crypto;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.network.Net;
import com.citymod.network.PhoneSyncPacket;
import net.minecraft.server.level.ServerPlayer;

public final class PhoneSync {
    private PhoneSync() {}

    public static void send(ServerPlayer p) {
        CryptoData d = CryptoData.get(p.getServer());
        EconomyData.Account a = Economy.acc(p);
        Net.toPlayer(p, new PhoneSyncPacket(
                d.prices.clone(), copy(d.hist), a.crypto.clone(),
                a.racks, a.gpus.clone(), a.coolant.clone(), a.target,
                Mining.hashrate(a), Mining.coolMult(a), Mining.incomeUsdPerMin(a)));
    }

    private static double[][] copy(double[][] src) {
        double[][] r = new double[src.length][];
        for (int i = 0; i < src.length; i++) r[i] = src[i].clone();
        return r;
    }
}
