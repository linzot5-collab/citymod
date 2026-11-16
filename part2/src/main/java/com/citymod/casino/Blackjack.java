package com.citymod.casino;

import com.citymod.economy.Economy;
import com.citymod.util.Msg;
import com.citymod.util.Sfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Блэкджек против дилера. Блэкджек платит 3:2, дилер стоит на 17. */
public final class Blackjack {
    private static final class Game {
        long bet;
        final List<Integer> player = new ArrayList<>();
        final List<Integer> dealer = new ArrayList<>();
    }

    private static final Map<UUID, Game> GAMES = new HashMap<>();
    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};

    private Blackjack() {}

    private static int draw() { return ThreadLocalRandom.current().nextInt(1, 14); }

    private static int value(List<Integer> hand) {
        int sum = 0, aces = 0;
        for (int c : hand) {
            if (c == 1) { aces++; sum += 11; } else sum += Math.min(c, 10);
        }
        while (sum > 21 && aces > 0) { sum -= 10; aces--; }
        return sum;
    }

    private static String name(int c) {
        String s = switch (c) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(c);
        };
        return s + SUITS[ThreadLocalRandom.current().nextInt(4)];
    }

    private static String show(List<Integer> hand, boolean hideSecond) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(hideSecond && i == 1 ? "[?]" : "[" + name(hand.get(i)) + "]");
        }
        return sb.toString();
    }

    public static void bet(ServerPlayer p, long chips) {
        if (GAMES.containsKey(p.getUUID())) { Msg.casino(p, "Игра уже идёт: /bj hit, /bj stand, /bj double."); return; }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        Game g = new Game();
        g.bet = chips;
        g.player.add(draw());
        g.dealer.add(draw());
        g.player.add(draw());
        g.dealer.add(draw());
        GAMES.put(p.getUUID(), g);
        Sfx.click(p);

        Msg.casino(p, "Ваши карты: " + show(g.player, false) + " §e(" + value(g.player) + ")");
        Msg.casino(p, "Дилер: " + show(g.dealer, true));

        if (value(g.player) == 21) finish(p, g, true);
        else Msg.casino(p, "§7/bj hit — взять, /bj stand — хватит, /bj double — удвоить");
    }

    public static void hit(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g == null) { Msg.casino(p, "Сначала сделайте ставку: /bj bet <фишки>"); return; }
        g.player.add(draw());
        Sfx.click(p);
        Msg.casino(p, "Ваши карты: " + show(g.player, false) + " §e(" + value(g.player) + ")");
        if (value(g.player) > 21) finish(p, g, false);
        else if (value(g.player) == 21) stand(p);
    }

    public static void stand(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g == null) { Msg.casino(p, "Сначала сделайте ставку: /bj bet <фишки>"); return; }
        finish(p, g, false);
    }

    public static void doubleDown(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g == null) { Msg.casino(p, "Сначала сделайте ставку: /bj bet <фишки>"); return; }
        if (g.player.size() != 2) { Msg.casino(p, "Удвоить можно только на первых двух картах."); return; }
        if (!Economy.takeChips(p, g.bet)) { Msg.casino(p, "Не хватает фишек для удвоения."); return; }
        g.bet *= 2;
        g.player.add(draw());
        Msg.casino(p, "Удвоение! Ваши карты: " + show(g.player, false) + " §e(" + value(g.player) + ")");
        finish(p, g, false);
    }

    private static void finish(ServerPlayer p, Game g, boolean natural) {
        GAMES.remove(p.getUUID());
        int pv = value(g.player);

        if (pv <= 21) {
            while (value(g.dealer) < 17) g.dealer.add(draw());
        }
        int dv = value(g.dealer);
        Msg.casino(p, "Дилер: " + show(g.dealer, false) + " §e(" + dv + ")");

        long payout;
        String text;
        if (pv > 21) {
            payout = 0; text = "§cПеребор. Вы проиграли " + g.bet + ".";
        } else if (natural && !(dv == 21 && g.dealer.size() == 2)) {
            payout = g.bet * 5 / 2; text = "§6БЛЭКДЖЕК! §aВыигрыш " + payout + ".";
        } else if (dv > 21 || pv > dv) {
            payout = g.bet * 2; text = "§aВы выиграли! Выплата " + payout + ".";
        } else if (pv == dv) {
            payout = g.bet; text = "Ничья, ставка возвращена.";
        } else {
            payout = 0; text = "§cДилер выиграл. Вы проиграли " + g.bet + ".";
        }

        if (payout > 0) Economy.addChips(p.getServer(), p.getUUID(), payout);
        p.sendSystemMessage(Component.literal("§d[Казино] §f" + text));
        if (payout > g.bet) Sfx.win(p); else if (payout == 0) Sfx.lose(p);
    }

    /** Игрок вышел посреди раздачи — возвращаем ставку. */
    public static void onLogout(ServerPlayer p) {
        Game g = GAMES.remove(p.getUUID());
        if (g != null) Economy.addChips(p.getServer(), p.getUUID(), g.bet);
    }

    public static void reset() {
        GAMES.clear();
    }
}
