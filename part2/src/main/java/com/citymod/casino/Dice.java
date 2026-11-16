package com.citymod.casino;

import com.citymod.economy.Economy;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import com.citymod.util.Sfx;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Кости: /dice <фишки> <ник|dealer>. Две кости, у кого сумма больше — забирает банк. */
public final class Dice {
    private static final class Challenge {
        final UUID from;
        final long chips;
        Challenge(UUID from, long chips) { this.from = from; this.chips = chips; }
    }

    private static final Map<UUID, Challenge> PENDING = new HashMap<>(); // ключ — кого вызвали
    private static final Set<UUID> BUSY = new HashSet<>();
    private static final int FEE_PERCENT = 5; // комиссия казино с банка между игроками

    private Dice() {}

    private static int roll() { return ThreadLocalRandom.current().nextInt(1, 7); }

    public static void challenge(ServerPlayer p, long chips, String nick) {
        if (BUSY.contains(p.getUUID())) { Msg.casino(p, "Вы уже в игре."); return; }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }

        boolean dealer = nick.equalsIgnoreCase("dealer") || nick.equalsIgnoreCase("npc") || nick.equalsIgnoreCase("дилер");
        if (dealer) {
            if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }
            BUSY.add(p.getUUID());
            animate(p.getServer(), p, null, chips, true);
            return;
        }

        ServerPlayer target = p.getServer().getPlayerList().getPlayerByName(nick);
        if (target == null) { Msg.casino(p, "Игрок «" + nick + "» не в сети. Для игры с дилером: /dice " + chips + " dealer"); return; }
        if (target == p) { Msg.casino(p, "Нельзя играть с самим собой."); return; }
        if (PENDING.containsKey(target.getUUID())) { Msg.casino(p, "Этому игроку уже бросили вызов."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        Challenge c = new Challenge(p.getUUID(), chips);
        PENDING.put(target.getUUID(), c);
        Msg.casino(p, "Вызов отправлен игроку " + target.getName().getString() + " (" + chips + " фишек). Ждём 30 секунд.");
        target.sendSystemMessage(Component.literal("§d[Казино] §f" + p.getName().getString() + " вызывает вас на кости на "
                + chips + " фишек. Принять: §a/dice accept§f, отказаться: §c/dice decline"));

        MinecraftServer server = p.getServer();
        UUID targetId = target.getUUID();
        Sched.later(600, () -> {
            if (PENDING.get(targetId) == c) {
                PENDING.remove(targetId);
                Economy.addChips(server, c.from, c.chips);
                ServerPlayer f = server.getPlayerList().getPlayer(c.from);
                if (f != null) Msg.casino(f, "Вызов не принят, ставка возвращена.");
            }
        });
    }

    public static void accept(ServerPlayer target) {
        Challenge c = PENDING.get(target.getUUID());
        if (c == null) { Msg.casino(target, "Вам никто не бросал вызов."); return; }
        MinecraftServer server = target.getServer();
        ServerPlayer from = server.getPlayerList().getPlayer(c.from);
        if (from == null) {
            PENDING.remove(target.getUUID());
            Economy.addChips(server, c.from, c.chips);
            Msg.casino(target, "Соперник вышел из игры.");
            return;
        }
        if (BUSY.contains(target.getUUID())) { Msg.casino(target, "Вы уже в игре."); return; }
        if (!Economy.takeChips(target, c.chips)) { Msg.casino(target, "Не хватает фишек (нужно " + c.chips + ")."); return; }
        PENDING.remove(target.getUUID());
        BUSY.add(target.getUUID());
        BUSY.add(from.getUUID());
        animate(server, from, target, c.chips, false);
    }

    public static void decline(ServerPlayer target) {
        Challenge c = PENDING.remove(target.getUUID());
        if (c == null) { Msg.casino(target, "Вам никто не бросал вызов."); return; }
        Economy.addChips(target.getServer(), c.from, c.chips);
        ServerPlayer f = target.getServer().getPlayerList().getPlayer(c.from);
        if (f != null) Msg.casino(f, target.getName().getString() + " отказался. Ставка возвращена.");
        Msg.casino(target, "Вы отказались.");
    }

    /** a — тот, кто бросил вызов; b == null означает игру против дилера. */
    private static void animate(MinecraftServer server, ServerPlayer a, ServerPlayer b, long chips, boolean vsDealer) {
        int a1 = roll(), a2 = roll(), b1 = roll(), b2 = roll();
        UUID aid = a.getUUID();
        UUID bid = b == null ? null : b.getUUID();
        String an = a.getName().getString();
        String bn = b == null ? "Дилер" : b.getName().getString();

        for (int i = 0; i < 3; i++) {
            Sched.later(i * 10, () -> {
                for (UUID id : new UUID[]{aid, bid}) {
                    if (id == null) continue;
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) {
                        Msg.bar(p, "§eКубики катятся... §f" + roll() + " " + roll());
                        Sfx.dice(p);
                    }
                }
            });
        }

        Sched.later(40, () -> {
            int sa = a1 + a2, sb = b1 + b2;
            String line = "§d[Казино] §f" + an + ": §e" + a1 + "+" + a2 + "=" + sa + " §f| " + bn + ": §e" + b1 + "+" + b2 + "=" + sb;
            long pot = chips * 2;

            if (sa > sb) {
                long win = vsDealer ? pot : pot - pot * FEE_PERCENT / 100;
                Economy.addChips(server, aid, win);
            } else if (sb > sa) {
                if (bid != null) Economy.addChips(server, bid, pot - pot * FEE_PERCENT / 100);
            } else {
                Economy.addChips(server, aid, chips);
                if (bid != null) Economy.addChips(server, bid, chips);
            }

            for (UUID id : new UUID[]{aid, bid}) {
                if (id == null) continue;
                BUSY.remove(id);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;
                p.sendSystemMessage(Component.literal(line));
                boolean isA = id.equals(aid);
                int mine = isA ? sa : sb, theirs = isA ? sb : sa;
                if (mine > theirs) { Msg.casino(p, "§aВы выиграли!"); Sfx.win(p); }
                else if (mine < theirs) { Msg.casino(p, "§cВы проиграли."); Sfx.lose(p); }
                else { Msg.casino(p, "Ничья, ставка возвращена."); }
            }
        });
    }

    public static void onLogout(ServerPlayer p) {
        PENDING.remove(p.getUUID());
    }

    public static void reset() {
        PENDING.clear();
        BUSY.clear();
    }
}
