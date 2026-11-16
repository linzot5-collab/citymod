package com.citymod.util;

import com.citymod.CityMod;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Простой планировщик отложенных задач (в тиках сервера). */
public final class Sched {
    private static final class Task {
        final long at;
        final Runnable run;
        Task(long at, Runnable run) { this.at = at; this.run = run; }
    }

    public static MinecraftServer server;
    public static long tick;
    private static final List<Task> PENDING = new ArrayList<>();

    private Sched() {}

    public static void later(int ticks, Runnable r) {
        PENDING.add(new Task(tick + Math.max(1, ticks), r));
    }

    /** Вызывается каждый тик сервера. */
    public static void run() {
        tick++;
        if (PENDING.isEmpty()) return;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = PENDING.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.at <= tick) {
                due.add(t);
                it.remove();
            }
        }
        for (Task t : due) {
            try {
                t.run.run();
            } catch (Exception e) {
                CityMod.LOGGER.error("Ошибка в отложенной задаче", e);
            }
        }
    }

    public static void reset() {
        PENDING.clear();
        server = null;
    }
}
