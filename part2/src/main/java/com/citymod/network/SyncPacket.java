package com.citymod.network;

import com.citymod.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Сервер -> клиент: актуальные суммы для панели справа. */
public record SyncPacket(long cash, long bank, long credit, long earned, long chips, String job) {

    public static void encode(SyncPacket p, FriendlyByteBuf buf) {
        buf.writeLong(p.cash);
        buf.writeLong(p.bank);
        buf.writeLong(p.credit);
        buf.writeLong(p.earned);
        buf.writeLong(p.chips);
        buf.writeUtf(p.job);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        return new SyncPacket(buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readUtf());
    }

    public static void handle(SyncPacket p, CustomPayloadEvent.Context ctx) {
        ClientData.cash = p.cash;
        ClientData.bank = p.bank;
        ClientData.credit = p.credit;
        ClientData.earned = p.earned;
        ClientData.chips = p.chips;
        ClientData.job = p.job;
        ctx.setPacketHandled(true);
    }
}
