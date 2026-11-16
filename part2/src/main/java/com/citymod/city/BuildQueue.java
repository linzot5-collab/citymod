package com.citymod.city;

import com.citymod.CityMod;

import java.util.ArrayDeque;
import java.util.Deque;

/** Очередь задач строительства: выполняется в пределах ~20 мс за тик, чтобы сервер не вис. */
public final class BuildQueue {
    private static final Deque<Runnable> QUEUE = new ArrayDeque<>();
    private static int total = 0;

    private BuildQueue() {}

    public static void add(Runnable r) {
        QUEUE.add(r);
        total++;
    }

    public static boolean busy() { return !QUEUE.isEmpty(); }

    public static int percent() {
        return total == 0 ? 100 : (int) (100L * (total - QUEUE.size()) / total);
    }

    public static void tick() {
        if (QUEUE.isEmpty()) return;
        long end = System.nanoTime() + 20_000_000L;
        while (!QUEUE.isEmpty() && System.nanoTime() < end) {
            Runnable r = QUEUE.poll();
            try {
                r.run();
            } catch (Exception e) {
                CityMod.LOGGER.error("Ошибка при строительстве города", e);
            }
        }
        if (QUEUE.isEmpty()) total = 0;
    }

    public static void reset() {
        QUEUE.clear();
        total = 0;
    }
}
