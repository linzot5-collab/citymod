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
 * Скачки: 6 лошадей с разными коэффициентами. Приём ставок 30 секунд, затем забег
 * (прогресс в чате и над хотбаром). Чем выше коэффициент, тем реже лошадь побеждает.
 */
public final class Horses {
    private static final String[] NAMES = {"Молния", "Ветер", "Гром", "Искра", "Тень", "Комета"};
    private static final double[] ODDS = {2.6, 3.8, 5.5, 8.0, 12.0, 20.0};
    private static final int WAIT_TICKS = 600;
    private static final int STEPS_MIN = 22;
    private static final int TRACK = 20;

    private static final class Bet {
        final UUID id;
        final int horse;
        final long chips;
        Bet(UUID id, int horse, long chips) { this.id = id; this.horse = horse; this.chips = chips; }
    }

    private static final List<Bet> BETS = new ArrayList<>();
    private static boolean collecting = false;
    private static boolean running = false;

    private Horses() {}

    public static void bet(ServerPlayer p, int horse, long chips) {
        if (running) { Msg.casino(p, "Забег уже идёт, ставки закрыты."); return; }
        if (horse < 1 || horse > 6) { Msg.casino(p, "Номер лошади: 1-6."); return; }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        BETS.add(new Bet(p.getUUID(), horse - 1, chips));
        Sfx.click(p);
        Msg.casino(p, "Ставка: №" + horse + " " + NAMES[horse - 1] + " (x" + ODDS[horse - 1] + ") — " + chips + " фишек.");

        if (!collecting) {
            collecting = true;
            MinecraftServer server = p.getServer();
            for (ServerPlayer o : server.getPlayerList().getPlayers()) {
                o.sendSystemMessage(Component.literal("§d[Скачки] §fОткрыт приём ставок на 30 секунд! Лошади:"));
                StringBuilder sb = new StringBuilder("§7");
                for (int i = 0; i < 6; i++) sb.append("№").append(i + 1).append(' ').append(NAMES[i]).append(" x").append(ODDS[i]).append("  ");
                o.sendSystemMessage(Component.literal(sb.toString()));
            }
            Sched.later(WAIT_TICKS, () -> startRace(server));
        }
    }

    private static int pickWinner() {
        double[] w = new double[6];
        double sum = 0;
        for (int i = 0; i < 6; i++) { w[i] = 1.0 / ODDS[i]; sum += w[i]; }
        double r = ThreadLocalRandom.current().nextDouble() * sum;
        for (int i = 0; i < 6; i++) {
            r -= w[i];
            if (r <= 0) return i;
        }
        return 5;
    }

    private static String bars(double[] pos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int n = (int) Math.round(pos[i] * TRACK);
            sb.append("§e№").append(i + 1).append(" §a").append("▮".repeat(Math.max(0, n)))
                    .append("§8").append("▯".repeat(Math.max(0, TRACK - n))).append(" §7").append(NAMES[i]).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static void sendAll(MinecraftServer server, List<Bet> bets, String text, boolean actionBar) {
        List<UUID> seen = new ArrayList<>();
        for (Bet b : bets) {
            if (seen.contains(b.id)) continue;
            seen.add(b.id);
            ServerPlayer p = server.getPlayerList().getPlayer(b.id);
            if (p == null) continue;
            if (actionBar) Msg.bar(p, text); else p.sendSystemMessage(Component.literal(text));
        }
    }

    private static void startRace(MinecraftServer server) {
        collecting = false;
        List<Bet> bets = new ArrayList<>(BETS);
        BETS.clear();
        if (bets.isEmpty()) return;
        running = true;

        int winner = pickWinner();
        // шаг, на котором лошадь финиширует; у победителя он минимальный
        double[] finish = new double[6];
        double[] phase = new double[6];
        for (int i = 0; i < 6; i++) {
            finish[i] = STEPS_MIN + (i == winner ? 0 : 1.5 + ThreadLocalRandom.current().nextDouble() * 4);
            phase[i] = ThreadLocalRandom.current().nextDouble() * 6;
        }

        sendAll(server, bets, "§d[Скачки] §fЗабег начался!", false);
        int totalSteps = (int) Math.ceil(STEPS_MIN + 6);

        for (int step = 1; step <= totalSteps; step++) {
            final int s = step;
            Sched.later(s * 20, () -> {
                double[] pos = new double[6];
                int leader = 0;
                for (int i = 0; i < 6; i++) {
                    double base = Math.min(1.0, s / finish[i]);
                    double wobble = base < 1.0 ? 0.06 * Math.sin(s * 0.9 + phase[i]) : 0;
                    pos[i] = Math.max(0, Math.min(1.0, base + wobble));
                    if (pos[i] > pos[leader]) leader = i;
                }
                if (s == 8 || s == 15 || s == 21) sendAll(server, bets, bars(pos), false);
                sendAll(server, bets, "§fЛидирует: §e№" + (leader + 1) + " " + NAMES[leader], true);
                for (Bet b : bets) {
                    ServerPlayer p = server.getPlayerList().getPlayer(b.id);
                    if (p != null) Sfx.play(p, "entity.horse.step", 0.6f, 1f);
                }
            });
        }

        Sched.later((totalSteps + 1) * 20, () -> {
            running = false;
            sendAll(server, bets, "§d[Скачки] §fПобедитель: §6№" + (winner + 1) + " " + NAMES[winner] + " §f(x" + ODDS[winner] + ")", false);
            for (Bet b : bets) {
                ServerPlayer p = server.getPlayerList().getPlayer(b.id);
                if (b.horse == winner) {
                    long payout = (long) Math.floor(b.chips * ODDS[winner]);
                    Economy.addChips(server, b.id, payout);
                    if (p != null) { Msg.casino(p, "§aВаша лошадь пришла первой! Выплата: " + payout); Sfx.win(p); }
                } else if (p != null) {
                    Msg.casino(p, "§cВаша ставка не сыграла (" + b.chips + ").");
                    Sfx.lose(p);
                }
            }
        });
    }

    public static void reset() {
        BETS.clear();
        collecting = false;
        running = false;
    }
}
