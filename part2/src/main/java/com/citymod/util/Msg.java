package com.citymod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Msg {
    private Msg() {}

    public static void bank(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("§6[Банк] §f" + text));
    }

    public static void casino(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("§d[Казино] §f" + text));
    }

    public static void job(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("§a[Работа] §f" + text));
    }

    public static void shop(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal("§b[Магазин] §f" + text));
    }

    /** Короткое сообщение над хотбаром. */
    public static void bar(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }
}
