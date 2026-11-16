package com.citymod.city;

import com.citymod.util.Msg;
import com.citymod.util.Sfx;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Музыка и звуки внутри казино и клуба. Вызывается раз в секунду. */
public final class ZoneAudio {
    private record Track(String id, int seconds) {}

    // Музыкальные диски ваниллы. Длительность приблизительная — трек просто сменяется по таймеру.
    private static final Track[] CASINO = {new Track("music_disc.mall", 197), new Track("music_disc.cat", 185), new Track("music_disc.13", 178)};
    private static final Track[] CLUB = {new Track("music_disc.pigstep", 149), new Track("music_disc.otherside", 195), new Track("music_disc.relic", 218)};

    private static final class State {
        String zone = "";
        int trackIdx = 0;
        int secondsLeft = 0;
        String playing = null;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private ZoneAudio() {}

    private static Track[] tracks(String zone) { return zone.equals("casino") ? CASINO : CLUB; }

    private static void stop(ServerPlayer p, State s) {
        if (s.playing != null) {
            p.connection.send(new ClientboundStopSoundPacket(ResourceLocation.withDefaultNamespace(s.playing), SoundSource.RECORDS));
            s.playing = null;
        }
    }

    private static void start(ServerPlayer p, State s) {
        Track[] list = tracks(s.zone);
        Track t = list[s.trackIdx % list.length];
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(t.id));
        p.playNotifySound(ev, SoundSource.RECORDS, 0.7f, 1f);
        s.playing = t.id;
        s.secondsLeft = t.seconds;
    }

    public static void tick(MinecraftServer server, City city) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level() != city.level) continue;
            State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
            String zone = city.zoneAt(p.getX(), p.getY(), p.getZ());

            if (!zone.equals(s.zone)) {
                stop(p, s);
                s.zone = zone;
                s.trackIdx = ThreadLocalRandom.current().nextInt(3);
                if (!zone.isEmpty()) {
                    start(p, s);
                    Msg.bar(p, zone.equals("casino") ? "§6★ Добро пожаловать в Royal Casino ★" : "§5♪ Ночной клуб NEON ♪");
                }
                continue;
            }
            if (zone.isEmpty()) continue;

            if (--s.secondsLeft <= 0) {
                s.trackIdx++;
                start(p, s);
            }

            ThreadLocalRandom r = ThreadLocalRandom.current();
            if (zone.equals("casino")) {
                // звон монет и щелчки автоматов
                if (r.nextInt(4) == 0) Sfx.play(p, "entity.experience_orb.pickup", 0.35f, 1.4f + r.nextFloat() * 0.5f);
                if (r.nextInt(7) == 0) Sfx.play(p, "block.note_block.chime", 0.3f, 0.8f + r.nextFloat() * 0.8f);
                if (r.nextInt(12) == 0) Sfx.play(p, "block.amethyst_block.chime", 0.4f, 1f);
            } else {
                // бит клуба: бас-барабан каждую секунду
                Sfx.play(p, "block.note_block.basedrum", 0.5f, 0.9f);
                if (r.nextInt(2) == 0) Sfx.play(p, "block.note_block.hat", 0.3f, 1.4f);
            }
        }
    }

    public static void onLogout(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }

    public static void reset() {
        STATES.clear();
    }
}
