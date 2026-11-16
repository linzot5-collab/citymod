package com.citymod.network;

import com.citymod.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Сервер -> клиент: открыть меню (банк, фишки, игры, магазин, работы). */
public record OpenMenuPacket(String id) {

    public static void encode(OpenMenuPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.id);
    }

    public static OpenMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenMenuPacket(buf.readUtf());
    }

    public static void handle(OpenMenuPacket p, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientHooks.openMenu(p.id));
        ctx.setPacketHandled(true);
    }
}
