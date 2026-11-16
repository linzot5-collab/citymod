package com.citymod.bank;

import com.citymod.network.Net;
import com.citymod.network.OpenMenuPacket;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Очередь в банк: 2 окна, каждого клиента обслуживают не дольше 2 минут. */
public final class BankQueue {
    private static final int WINDOWS = 2;
    private static final long SERVICE_TICKS = 20L * 120;

    private static final Deque<UUID> QUEUE = new ArrayDeque<>();
    private static final Map<UUID, Long> SERVING = new LinkedHashMap<>();

    private BankQueue() {}

    public static void join(ServerPlayer p) {
        UUID id = p.getUUID();
        if (SERVING.containsKey(id)) {
            Net.toPlayer(p, new OpenMenuPacket("bank"));
            return;
        }
        if (QUEUE.contains(id)) {
            Msg.bank(p, "Вы уже в очереди. Впереди вас: " + position(id));
            return;
        }
        QUEUE.add(id);
        Msg.bank(p, "Вы встали в очередь. Впереди вас: " + position(id));
        tick(p.getServer());
    }

    private static int position(UUID id) {
        int i = 0;
        for (UUID u : QUEUE) {
            if (u.equals(id)) return i;
            i++;
        }
        return 0;
    }

    public static void leave(UUID id) {
        QUEUE.remove(id);
        SERVING.remove(id);
    }

    public static String status() {
        return "У окон: " + SERVING.size() + "/" + WINDOWS + ", в очереди: " + QUEUE.size();
    }

    /** Вызывать раз в секунду. */
    public static void tick(MinecraftServer s) {
        for (Iterator<Map.Entry<UUID, Long>> it = SERVING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (Sched.tick >= e.getValue()) {
                it.remove();
                ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
                if (p != null) Msg.bank(p, "Время обслуживания вышло. Следующий!");
            }
        }
        while (SERVING.size() < WINDOWS && !QUEUE.isEmpty()) {
            UUID id = QUEUE.poll();
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) continue;
            SERVING.put(id, Sched.tick + SERVICE_TICKS);
            Msg.bank(p, "Ваша очередь! Подойдите к окну.");
            Net.toPlayer(p, new OpenMenuPacket("bank"));
        }
    }

    public static void reset() {
        QUEUE.clear();
        SERVING.clear();
    }
}
