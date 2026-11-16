package com.citymod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Звуки по строковому id ("block.note_block.bell"), чтобы не зависеть от типов в SoundEvents. */
public final class Sfx {
    private Sfx() {}

    public static void play(ServerPlayer p, String id, float vol, float pitch) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(id));
        p.playNotifySound(ev, SoundSource.PLAYERS, vol, pitch);
    }

    public static void win(ServerPlayer p) { play(p, "entity.player.levelup", 1f, 1f); }
    public static void jackpot(ServerPlayer p) { play(p, "ui.toast.challenge_complete", 1f, 1f); }
    public static void lose(ServerPlayer p) { play(p, "entity.villager.no", 0.8f, 1f); }
    public static void click(ServerPlayer p) { play(p, "block.note_block.hat", 0.8f, 1.2f); }
    public static void coin(ServerPlayer p) { play(p, "entity.experience_orb.pickup", 0.8f, 1.3f); }
    public static void dice(ServerPlayer p) { play(p, "block.wooden_button.click_on", 0.9f, 0.9f); }
}
