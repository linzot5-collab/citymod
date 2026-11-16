package com.citymod.client;

import net.minecraft.client.Minecraft;

/** Вызывается только на клиенте (из пакетов). */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openMenu(String id) {
        if (id.equals("phone")) {
            Minecraft.getInstance().setScreen(new PhoneScreen());
        } else {
            Minecraft.getInstance().setScreen(new MenuScreen(id));
        }
    }
}
