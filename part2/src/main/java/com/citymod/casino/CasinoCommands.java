package com.citymod.casino;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.util.Msg;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class CasinoCommands {
    private CasinoCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /dice <фишки> <ник|dealer>, /dice accept, /dice decline
        d.register(Commands.literal("dice")
                .then(Commands.literal("accept").executes(c -> {
                    Dice.accept(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("decline").executes(c -> {
                    Dice.decline(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.argument("chips", LongArgumentType.longArg(1))
                        .then(Commands.argument("nick", StringArgumentType.word()).executes(c -> {
                            Dice.challenge(c.getSource().getPlayerOrException(),
                                    LongArgumentType.getLong(c, "chips"), StringArgumentType.getString(c, "nick"));
                            return 1;
                        }))));

        // /roulette <ставка> <фишки>
        d.register(Commands.literal("roulette")
                .then(Commands.argument("bet", StringArgumentType.word())
                        .then(Commands.argument("chips", LongArgumentType.longArg(1)).executes(c -> {
                            Roulette.bet(c.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(c, "bet"), LongArgumentType.getLong(c, "chips"));
                            return 1;
                        }))));

        // /bj bet|hit|stand|double
        d.register(Commands.literal("bj")
                .then(Commands.literal("bet").then(Commands.argument("chips", LongArgumentType.longArg(1)).executes(c -> {
                    Blackjack.bet(c.getSource().getPlayerOrException(), LongArgumentType.getLong(c, "chips"));
                    return 1;
                })))
                .then(Commands.literal("hit").executes(c -> { Blackjack.hit(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("stand").executes(c -> { Blackjack.stand(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("double").executes(c -> { Blackjack.doubleDown(c.getSource().getPlayerOrException()); return 1; })));

        d.register(Commands.literal("wheel")
                .then(Commands.argument("chips", LongArgumentType.longArg(1)).executes(c -> {
                    Wheel.spin(c.getSource().getPlayerOrException(), LongArgumentType.getLong(c, "chips"));
                    return 1;
                })));

        d.register(Commands.literal("slots")
                .then(Commands.argument("chips", LongArgumentType.longArg(1)).executes(c -> {
                    Slots.spin(c.getSource().getPlayerOrException(), LongArgumentType.getLong(c, "chips"));
                    return 1;
                })));

        // /horses bet <1-6> <фишки>
        d.register(Commands.literal("horses")
                .then(Commands.literal("bet")
                        .then(Commands.argument("horse", IntegerArgumentType.integer(1, 6))
                                .then(Commands.argument("chips", LongArgumentType.longArg(1)).executes(c -> {
                                    Horses.bet(c.getSource().getPlayerOrException(),
                                            IntegerArgumentType.getInteger(c, "horse"), LongArgumentType.getLong(c, "chips"));
                                    return 1;
                                })))));

        // /chips buy|sell|sellall
        d.register(Commands.literal("chips")
                .then(Commands.literal("buy").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    long n = LongArgumentType.getLong(c, "amount");
                    Msg.casino(p, Economy.buyChips(p, n) ? "Куплено фишек: " + n : "Недостаточно наличных.");
                    return 1;
                })))
                .then(Commands.literal("sell").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    long n = LongArgumentType.getLong(c, "amount");
                    Msg.casino(p, Economy.sellChips(p, n) ? "Продано фишек: " + n + " (получено " + n + " $)" : "Недостаточно фишек.");
                    return 1;
                })))
                .then(Commands.literal("sellall").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    EconomyData.Account a = Economy.acc(p);
                    long n = a.chips;
                    Msg.casino(p, n > 0 && Economy.sellChips(p, n) ? "Продано фишек: " + n : "У вас нет фишек.");
                    return 1;
                })));
    }
}
