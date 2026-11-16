package com.citymod.crypto;

import com.citymod.CityMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CryptoCommands {
    private CryptoCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("crypto")
                .then(Commands.literal("buy")
                        .then(Commands.argument("coin", IntegerArgumentType.integer(0, 3))
                                .then(Commands.argument("usd", LongArgumentType.longArg(1)).executes(c -> {
                                    Market.buy(c.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(c, "coin"), LongArgumentType.getLong(c, "usd"));
                                    return 1;
                                }))))
                .then(Commands.literal("sell")
                        .then(Commands.argument("coin", IntegerArgumentType.integer(0, 3))
                                .then(Commands.argument("percent", IntegerArgumentType.integer(1, 100)).executes(c -> {
                                    Market.sell(c.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(c, "coin"), IntegerArgumentType.getInteger(c, "percent"));
                                    return 1;
                                })))));

        d.register(Commands.literal("mining")
                .then(Commands.literal("buy").then(Commands.argument("item", StringArgumentType.word()).executes(c -> {
                    Mining.buy(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "item"));
                    return 1;
                })))
                .then(Commands.literal("target").then(Commands.argument("coin", IntegerArgumentType.integer(0, 3)).executes(c -> {
                    Mining.setTarget(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "coin"));
                    return 1;
                }))));

        // /phone — выдать телефон, /phone sync — обновить данные в открытом телефоне
        d.register(Commands.literal("phone")
                .then(Commands.literal("sync").executes(c -> {
                    PhoneSync.send(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (ItemStack s : p.getInventory().items) {
                        if (s.is(CityMod.PHONE.get())) return 0;
                    }
                    ItemStack phone = new ItemStack(CityMod.PHONE.get());
                    if (!p.getInventory().add(phone)) p.drop(phone, false);
                    return 1;
                }));
    }
}
