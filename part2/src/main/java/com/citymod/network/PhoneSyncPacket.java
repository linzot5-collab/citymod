package com.citymod.network;

import com.citymod.client.ClientCrypto;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Сервер -> клиент: цены, графики, портфель и состояние майнинг-фермы для телефона. */
public record PhoneSyncPacket(double[] prices, double[][] hist, double[] hold,
                              int racks, int[] gpus, int[] coolant, int target,
                              double hash, double mult, double income) {

    public static void encode(PhoneSyncPacket p, FriendlyByteBuf b) {
        for (int c = 0; c < 4; c++) {
            b.writeDouble(p.prices[c]);
            b.writeDouble(p.hold[c]);
            b.writeVarInt(p.hist[c].length);
            for (double v : p.hist[c]) b.writeDouble(v);
        }
        b.writeVarInt(p.racks);
        for (int i = 0; i < 3; i++) {
            b.writeVarInt(p.gpus[i]);
            b.writeVarInt(p.coolant[i]);
        }
        b.writeVarInt(p.target);
        b.writeDouble(p.hash);
        b.writeDouble(p.mult);
        b.writeDouble(p.income);
    }

    public static PhoneSyncPacket decode(FriendlyByteBuf b) {
        double[] prices = new double[4], hold = new double[4];
        double[][] hist = new double[4][];
        for (int c = 0; c < 4; c++) {
            prices[c] = b.readDouble();
            hold[c] = b.readDouble();
            int n = b.readVarInt();
            hist[c] = new double[n];
            for (int i = 0; i < n; i++) hist[c][i] = b.readDouble();
        }
        int racks = b.readVarInt();
        int[] gpus = new int[3], cool = new int[3];
        for (int i = 0; i < 3; i++) {
            gpus[i] = b.readVarInt();
            cool[i] = b.readVarInt();
        }
        int target = b.readVarInt();
        return new PhoneSyncPacket(prices, hist, hold, racks, gpus, cool, target,
                b.readDouble(), b.readDouble(), b.readDouble());
    }

    public static void handle(PhoneSyncPacket p, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientCrypto.prices = p.prices;
            ClientCrypto.hist = p.hist;
            ClientCrypto.hold = p.hold;
            ClientCrypto.racks = p.racks;
            ClientCrypto.gpus = p.gpus;
            ClientCrypto.coolant = p.coolant;
            ClientCrypto.target = p.target;
            ClientCrypto.hash = p.hash;
            ClientCrypto.mult = p.mult;
            ClientCrypto.income = p.income;
            ClientCrypto.loaded = true;
        });
        ctx.setPacketHandled(true);
    }
}
