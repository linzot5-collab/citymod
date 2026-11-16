package com.citymod.network;

import com.citymod.CityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class Net {
    private static final int PROTOCOL = 3;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(CityMod.MODID, "main"))
            .networkProtocolVersion(PROTOCOL)
            .acceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();

    private Net() {}

    public static void init() {
        CHANNEL.play()
                .clientbound()
                .add(SyncPacket.class,
                        StreamCodec.<RegistryFriendlyByteBuf, SyncPacket>of(
                                (buf, p) -> SyncPacket.encode(p, buf), SyncPacket::decode),
                        SyncPacket::handle)
                .add(PhoneSyncPacket.class,
                        StreamCodec.<RegistryFriendlyByteBuf, PhoneSyncPacket>of(
                                (buf, p) -> PhoneSyncPacket.encode(p, buf), PhoneSyncPacket::decode),
                        PhoneSyncPacket::handle)
                .add(OpenMenuPacket.class,
                        StreamCodec.<RegistryFriendlyByteBuf, OpenMenuPacket>of(
                                (buf, p) -> OpenMenuPacket.encode(p, buf), OpenMenuPacket::decode),
                        OpenMenuPacket::handle);
    }

    public static void toPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }
}
