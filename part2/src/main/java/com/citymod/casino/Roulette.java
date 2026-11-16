package com.citymod.casino;

import com.citymod.economy.Economy;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import com.citymod.util.Sfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Американская рулетка: 0, 00 и 1-36. Ставки собираются 15 секунд, потом крутится шарик.
 * Выплаты: число x36 (35:1), красное/чёрное/чёт/нечет/1-18/19-36 x2, дюжины x3.
 */
public final class Roulette {
    private static final class Bet {
        final UUID id;
        final String type;
        final long chips;
        Bet(UUID id, String type, long chips) { this.id = id; this.type = type; this.chips = chips; }
    }

    private static final int[] RED = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
    private static final int DOUBLE_ZERO = 37; // внутреннее обозначение "00"
    private static final int WAIT_TICKS = 300;

    private static final List<Bet> BETS = new ArrayList<>();
    private static boolean collecting = false;
    private static boolean spinning = false;

    private Roulette() {}

    private static boolean isRed(int n) {
        for (int r : RED) if (r == n) return true;
        return false;
    }

    private static String label(int n) { return n == DOUBLE_ZERO ? "00" : String.valueOf(n); }

    private static String color(int n) {
        if (n == 0 || n == DOUBLE_ZERO) return "§a";
        return isRed(n) ? "§c" : "§8";
    }

    private static boolean validType(String t) {
        switch (t) {
            case "red", "black", "even", "odd", "low", "high", "dozen1", "dozen2", "dozen3", "00": return true;
            default:
                try {
                    int n = Integer.parseInt(t);
                    return n >= 0 && n <= 36;
                } catch (NumberFormatException e) {
                    return false;
                }
        }
    }

    /** Во сколько раз ставка возвращается при выигрыше (0 — проигрыш). */
    private static int multiplier(String type, int n) {
        boolean zero = n == 0 || n == DOUBLE_ZERO;
        switch (type) {
            case "red": return !zero && isRed(n) ? 2 : 0;
            case "black": return !zero && !isRed(n) ? 2 : 0;
            case "even": return !zero && n % 2 == 0 ? 2 : 0;
            case "odd": return !zero && n % 2 == 1 ? 2 : 0;
            case "low": return !zero && n <= 18 ? 2 : 0;
            case "high": return !zero && n >= 19 ? 2 : 0;
            case "dozen1": return !zero && n <= 12 ? 3 : 0;
            case "dozen2": return !zero && n >= 13 && n <= 24 ? 3 : 0;
            case "dozen3": return !zero && n >= 25 ? 3 : 0;
            case "00": return n == DOUBLE_ZERO ? 36 : 0;
            default: return String.valueOf(n).equals(type) ? 36 : 0;
        }
    }

    public static void bet(ServerPlayer p, String type, long chips) {
        type = type.toLowerCase();
        if (spinning) { Msg.casino(p, "Шарик уже крутится, дождитесь следующего раунда."); return; }
        if (!validType(type)) {
            Msg.casino(p, "Ставки: red, black, even, odd, low, high, dozen1-3, число 0-36 или 00.");
            return;
        }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        BETS.add(new Bet(p.getUUID(), type, chips));
        Sfx.click(p);
        Msg.casino(p, "Ставка принята: " + type + " — " + chips + " фишек.");

        if (!collecting) {
            collecting = true;
            MinecraftServer server = p.getServer();
            broadcast(server, "§eРулетка: приём ставок 15 секунд! §7(/roulette <ставка> <фишки>)");
            Sched.later(WAIT_TICKS, () -> spin(server));
        }
    }

    private static void broadcast(MinecraftServer server, String text) {
        for (Bet b : BETS) {
            ServerPlayer p = server.getPlayerList().getPlayer(b.id);
            if (p != null) p.sendSystemMessage(Component.literal("§d[Казино] §f" + text));
        }
    }

    private static void spin(MinecraftServer server) {
        collecting = false;
        spinning = true;
        int result = ThreadLocalRandom.current().nextInt(0, 38); // 0..36 и 37 = "00"

        // анимация: шарик замедляется
        int[] delays = {0, 4, 8, 12, 18, 25, 33, 42, 54, 68};
        for (int d : delays) {
            Sched.later(d + 1, () -> {
                int fake = ThreadLocalRandom.current().nextInt(0, 38);
                for (Bet b : BETS) {
                    ServerPlayer p = server.getPlayerList().getPlayer(b.id);
                    if (p != null) {
                        Msg.bar(p, "§fШарик: " + color(fake) + "§l" + label(fake));
                        Sfx.click(p);
                    }
                }
            });
        }

        Sched.later(90, () -> {
            List<Bet> done = new ArrayList<>(BETS);
            BETS.clear();
            spinning = false;
            String res = color(result) + "§l" + label(result);
            for (Bet b : done) {
                int mult = multiplier(b.type, result);
                long payout = b.chips * mult;
                if (payout > 0) Economy.addChips(server, b.id, payout);
                ServerPlayer p = server.getPlayerList().getPlayer(b.id);
                if (p == null) continue;
                p.sendSystemMessage(Component.literal("§d[Казино] §fВыпало: " + res + "§f. Ваша ставка " + b.type + ": "
                        + (payout > 0 ? "§aвыигрыш " + payout : "§cпроигрыш " + b.chips)));
                if (payout > 0) Sfx.win(p); else Sfx.lose(p);
            }
        });
    }

    public static void reset() {
        BETS.clear();
        collecting = false;
        spinning = false;
    }
}
