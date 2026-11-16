package com.citymod.casino;

import com.citymod.economy.Economy;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import com.citymod.util.Sfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Игровой автомат: три барабана. Три одинаковых платят по таблице, две вишни возвращают ставку. */
public final class Slots {
    // 8 позиций на барабане (повторы = выше шанс)
    private static final String[] REEL = {"SEVEN", "STAR", "BELL", "BELL", "CHERRY", "CHERRY", "LEMON", "LEMON"};
    private static final Set<UUID> BUSY = new HashSet<>();

    private Slots() {}

    private static String icon(String s) {
        return switch (s) {
            case "SEVEN" -> "§c§l7";
            case "STAR" -> "§e★";
            case "BELL" -> "§6♪";
            case "CHERRY" -> "§4●";
            default -> "§a◆";
        };
    }

    private static int tripleMultiplier(String s) {
        return switch (s) {
            case "SEVEN" -> 100;
            case "STAR" -> 50;
            case "BELL" -> 12;
            case "CHERRY" -> 8;
            default -> 5;
        };
    }

    private static String spinOne() { return REEL[ThreadLocalRandom.current().nextInt(REEL.length)]; }

    public static void spin(ServerPlayer p, long chips) {
        if (BUSY.contains(p.getUUID())) { Msg.casino(p, "Барабаны ещё крутятся."); return; }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        BUSY.add(p.getUUID());
        String a = spinOne(), b = spinOne(), c = spinOne();
        MinecraftServer server = p.getServer();
        UUID id = p.getUUID();

        // барабаны останавливаются по очереди
        for (int step = 0; step < 3; step++) {
            final int s = step;
            Sched.later(8 + s * 14, () -> {
                ServerPlayer pl = server.getPlayerList().getPlayer(id);
                if (pl == null) return;
                String l = s >= 1 ? icon(a) : icon(spinOne());
                String m = s >= 2 ? icon(b) : icon(spinOne());
                String r = icon(spinOne());
                if (s == 2) r = icon(c);
                Msg.bar(pl, "§f[ " + (s >= 0 ? icon(a) : l) + " §f| " + (s >= 1 ? icon(b) : m) + " §f| " + (s >= 2 ? icon(c) : r) + " §f]");
                Sfx.click(pl);
            });
        }

        Sched.later(50, () -> {
            BUSY.remove(id);
            long payout = 0;
            if (a.equals(b) && b.equals(c)) {
                payout = chips * tripleMultiplier(a);
            } else {
                int cherries = (a.equals("CHERRY") ? 1 : 0) + (b.equals("CHERRY") ? 1 : 0) + (c.equals("CHERRY") ? 1 : 0);
                if (cherries == 2) payout = chips;
            }
            if (payout > 0) Economy.addChips(server, id, payout);
            ServerPlayer pl = server.getPlayerList().getPlayer(id);
            if (pl == null) return;
            pl.sendSystemMessage(Component.literal("§d[Казино] §f[ " + icon(a) + " §f| " + icon(b) + " §f| " + icon(c) + " §f] "
                    + (payout > chips ? "§aВыигрыш " + payout + "!" : payout == chips ? "Ставка возвращена." : "§cНе повезло.")));
            if (payout >= chips * 12) Sfx.jackpot(pl);
            else if (payout > chips) Sfx.win(pl);
            else if (payout == 0) Sfx.lose(pl);
        });
    }

    public static void reset() { BUSY.clear(); }
}
