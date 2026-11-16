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

/** Колесо фортуны: 24 сектора, средний возврат около 92%. */
public final class Wheel {
    // множитель возврата ставки в каждом секторе
    private static final int[] SEGMENTS = new int[24];
    private static final Set<UUID> BUSY = new HashSet<>();

    static {
        // 18 x0, 2 x1, 1 x2, 1 x3, 1 x5, 1 x10 => сумма 22 из 24
        int i = 0;
        for (; i < 18; i++) SEGMENTS[i] = 0;
        SEGMENTS[i++] = 1;
        SEGMENTS[i++] = 1;
        SEGMENTS[i++] = 2;
        SEGMENTS[i++] = 3;
        SEGMENTS[i++] = 5;
        SEGMENTS[i] = 10;
    }

    private Wheel() {}

    private static String show(int mult) {
        return switch (mult) {
            case 0 -> "§7x0";
            case 1 -> "§fx1";
            case 2 -> "§ax2";
            case 3 -> "§bx3";
            case 5 -> "§dx5";
            default -> "§6§lx" + mult;
        };
    }

    public static void spin(ServerPlayer p, long chips) {
        if (BUSY.contains(p.getUUID())) { Msg.casino(p, "Колесо ещё крутится."); return; }
        if (chips < 1) { Msg.casino(p, "Минимальная ставка: 1 фишка."); return; }
        if (!Economy.takeChips(p, chips)) { Msg.casino(p, "Не хватает фишек."); return; }

        BUSY.add(p.getUUID());
        int result = SEGMENTS[ThreadLocalRandom.current().nextInt(SEGMENTS.length)];
        MinecraftServer server = p.getServer();
        UUID id = p.getUUID();

        int[] delays = {0, 3, 6, 9, 13, 18, 24, 31, 40, 52};
        for (int d : delays) {
            Sched.later(d + 1, () -> {
                ServerPlayer pl = server.getPlayerList().getPlayer(id);
                if (pl == null) return;
                int fake = SEGMENTS[ThreadLocalRandom.current().nextInt(SEGMENTS.length)];
                Msg.bar(pl, "§fКолесо: " + show(fake));
                Sfx.click(pl);
            });
        }

        Sched.later(70, () -> {
            BUSY.remove(id);
            long payout = chips * result;
            if (payout > 0) Economy.addChips(server, id, payout);
            ServerPlayer pl = server.getPlayerList().getPlayer(id);
            if (pl == null) return;
            pl.sendSystemMessage(Component.literal("§d[Казино] §fКолесо остановилось на " + show(result) + "§f. "
                    + (payout > 0 ? "§aВыплата: " + payout : "§cВы проиграли " + chips)));
            if (result >= 5) Sfx.jackpot(pl);
            else if (payout > chips) Sfx.win(pl);
            else if (payout == 0) Sfx.lose(pl);
        });
    }

    public static void reset() { BUSY.clear(); }
}
