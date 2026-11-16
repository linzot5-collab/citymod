package com.citymod.city;

import com.citymod.CityMod;
import com.citymod.bank.BankQueue;
import com.citymod.casino.Blackjack;
import com.citymod.casino.Dice;
import com.citymod.casino.Horses;
import com.citymod.casino.Roulette;
import com.citymod.casino.Slots;
import com.citymod.casino.Wheel;
import com.citymod.crypto.Market;
import com.citymod.crypto.Mining;
import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.job.Jobs;
import com.citymod.util.Msg;
import com.citymod.util.Sched;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

/** Серверные события: создание города, тик, работы, защита города. */
public class CityEvents {
    private static final String WELCOME_TAG = "citymod_welcomed";
    private static boolean built = false;
    private static final Random RND = new Random();

    // ---------------------------------------------------------------- запуск / остановка

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent e) {
        Sched.server = e.getServer();
        ServerLevel level = e.getServer().overworld();
        CityData data = CityData.get(level);

        if (data.generated) {
            City.current = new City(level, data.cx, data.cz, data.by);
            built = true;
            return;
        }

        BlockPos sp = level.getSharedSpawnPos();
        int cx = sp.getX(), cz = sp.getZ();
        level.getChunk(cx >> 4, cz >> 4);
        int by = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        if (by < level.getSeaLevel() + 1) by = level.getSeaLevel() + 1;
        by = Math.min(by, 150);
        final int fby = by;

        data.cx = cx;
        data.cz = cz;
        data.by = fby;
        City city = new City(level, cx, cz, fby);
        City.current = city;

        CityMod.LOGGER.info("Строим город в {} {} {}", cx, fby, cz);
        city.build(() -> {
            level.setDefaultSpawnPos(city.spawn, 0f);
            data.generated = true;
            data.setDirty();
            built = true;
            for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) welcome(p, city);
        });
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) {
        City.current = null;
        built = false;
        BuildQueue.reset();
        Sched.reset();
        BankQueue.reset();
        Dice.reset();
        Roulette.reset();
        Blackjack.reset();
        Wheel.reset();
        Slots.reset();
        Horses.reset();
        ZoneAudio.reset();
    }

    // ---------------------------------------------------------------- игроки

    private static void welcome(ServerPlayer p, City city) {
        CompoundTag data = p.getPersistentData();
        if (data.getBoolean(WELCOME_TAG)) return;
        data.putBoolean(WELCOME_TAG, true);
        city.teleportToSpawn(p);
        net.minecraft.world.item.ItemStack phone = new net.minecraft.world.item.ItemStack(CityMod.PHONE.get());
        if (!p.getInventory().add(phone)) p.drop(phone, false);
        Msg.bar(p, "§6Добро пожаловать в город!");
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Город] §fВы дома. Вам выдан телефон (ПКМ): биржа крипты и майнинг. Стартовые вещи — в сундуке. Банк слева-сверху, казино и клуб справа. /city spawn — вернуться домой."));
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Economy.sync(sp);
        City c = City.current;
        if (c == null) return;
        if (built) welcome(sp, c);
        else Msg.bar(sp, "§eГород строится: " + BuildQueue.percent() + "%");
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        BankQueue.leave(sp.getUUID());
        Dice.onLogout(sp);
        Blackjack.onLogout(sp);
        ZoneAudio.onLogout(sp);
    }

    // ---------------------------------------------------------------- тик

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Sched.run();
        BuildQueue.tick();

        if (Sched.server != null) {
            // биржа: цены обновляются каждые 10 секунд, майнинг начисляется раз в минуту
            if (Sched.tick % 200 == 0) Market.tick(Sched.server);
            if (Sched.tick % 1200 == 0) Mining.tick(Sched.server, Sched.tick / 1200);
        }

        City city = City.current;
        if (city == null || !built || Sched.server == null) return;
        long t = Sched.tick;

        if (t % 8 == 0 && playersIn(city, "club")) city.paintDanceFloor((int) (t / 8));

        if (t % 20 == 0) {
            BankQueue.tick(Sched.server);
            ZoneAudio.tick(Sched.server, city);
            for (ServerPlayer p : Sched.server.getPlayerList().getPlayers()) {
                EconomyData.Account a = Economy.acc(p);
                // грузчик с ящиком идёт медленнее
                if (a.crates > 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, false));
            }
        }
        // новые трещины на дорогах для ремонтников
        if (t % (20 * 90) == 0) city.spawnCrack(RND);
    }

    private static boolean playersIn(City city, String zone) {
        for (ServerPlayer p : Sched.server.getPlayerList().getPlayers()) {
            if (p.level() == city.level && city.zoneAt(p.getX(), p.getY(), p.getZ()).equals(zone)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- работа дровосека и защита города

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent e) {
        City city = City.current;
        if (city == null || !(e.getPlayer() instanceof ServerPlayer p) || p.level() != city.level) return;
        BlockPos pos = e.getPos();
        boolean inCity = Math.abs(pos.getX() - city.cx) <= 64 && Math.abs(pos.getZ() - city.cz) <= 64
                && pos.getY() >= city.by - 1 && pos.getY() <= city.by + 26;
        if (!inCity) return;

        boolean admin = p.isCreative() || p.hasPermissions(2);

        if (city.inLumberZone(pos)) {
            var state = e.getState();
            if (state.is(Blocks.OAK_LOG)) {
                if (Jobs.has(p, Jobs.LUMBERJACK)) {
                    Jobs.earn(p, Jobs.LOG_PAY, "бревно");
                    city.scheduleRegrow(pos);
                } else if (!admin) {
                    Msg.bar(p, "§7Рубить лес за деньги может дровосек (биржа труда).");
                }
                return;
            }
            if (state.is(Blocks.OAK_LEAVES)) return;
        }

        if (!admin) {
            e.setCanceled(true);
            Msg.bar(p, "§cГородские здания охраняются.");
        }
    }
}
