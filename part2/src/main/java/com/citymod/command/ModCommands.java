package com.citymod.command;

import com.citymod.CityMod;
import com.citymod.bank.BankQueue;
import com.citymod.city.City;
import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import com.citymod.job.Jobs;
import com.citymod.job.Shop;
import com.citymod.util.Msg;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ModCommands {
    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("balance").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            EconomyData.Account a = Economy.acc(p);
            Msg.bank(p, "Наличные: " + a.cash + " $, банк: " + a.bank + " $, фишки: " + a.chips + ", кредит: " + a.credit + " $");
            return 1;
        }));

        d.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(c -> {
                                    ServerPlayer from = c.getSource().getPlayerOrException();
                                    ServerPlayer to = EntityArgument.getPlayer(c, "player");
                                    long amount = LongArgumentType.getLong(c, "amount");
                                    if (from == to) { Msg.bank(from, "Нельзя переводить самому себе."); return 0; }
                                    if (!Economy.transferCash(from, to, amount)) { Msg.bank(from, "Недостаточно наличных."); return 0; }
                                    Msg.bank(from, "Вы передали " + amount + " $ игроку " + to.getName().getString());
                                    Msg.bank(to, from.getName().getString() + " передал вам " + amount + " $");
                                    return 1;
                                }))));

        d.register(Commands.literal("bank")
                .then(Commands.literal("card").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    EconomyData.Account a = Economy.acc(p);
                    if (a.hasCard) { Msg.bank(p, "У вас уже есть карта."); return 0; }
                    if (!Economy.spendCash(p, Economy.CARD_PRICE)) { Msg.bank(p, "Карта стоит " + Economy.CARD_PRICE + " $ наличными."); return 0; }
                    a.hasCard = true;
                    Economy.save(p);
                    ItemStack card = new ItemStack(CityMod.BANK_CARD.get());
                    if (!p.getInventory().add(card)) p.drop(card, false);
                    Msg.bank(p, "Карта оформлена!");
                    return 1;
                }))
                .then(Commands.literal("deposit").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!requireCard(p)) return 0;
                    long n = LongArgumentType.getLong(c, "amount");
                    Msg.bank(p, Economy.deposit(p, n) ? "Внесено " + n + " $" : "Недостаточно наличных.");
                    return 1;
                })))
                .then(Commands.literal("depositall").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!requireCard(p)) return 0;
                    long n = Economy.acc(p).cash;
                    Msg.bank(p, n > 0 && Economy.deposit(p, n) ? "Внесено " + n + " $" : "У вас нет наличных.");
                    return 1;
                }))
                .then(Commands.literal("withdraw").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!requireCard(p)) return 0;
                    long n = LongArgumentType.getLong(c, "amount");
                    Msg.bank(p, Economy.withdraw(p, n) ? "Снято " + n + " $" : "Недостаточно денег на счёте.");
                    return 1;
                })))
                .then(Commands.literal("withdrawall").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!requireCard(p)) return 0;
                    long n = Economy.acc(p).bank;
                    Msg.bank(p, n > 0 && Economy.withdraw(p, n) ? "Снято " + n + " $" : "На счёте пусто.");
                    return 1;
                }))
                .then(Commands.literal("credit").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String err = Economy.takeCredit(p, LongArgumentType.getLong(c, "amount"));
                    Msg.bank(p, err == null ? "Кредит одобрен, деньги на счёте (комиссия " + Economy.CREDIT_FEE_PERCENT + "%)." : err);
                    return err == null ? 1 : 0;
                })))
                .then(Commands.literal("repay").then(Commands.argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String err = Economy.repayCredit(p, LongArgumentType.getLong(c, "amount"));
                    Msg.bank(p, err == null ? "Платёж по кредиту принят." : err);
                    return err == null ? 1 : 0;
                })))
                .then(Commands.literal("queue").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BankQueue.join(p);
                    return 1;
                }))
                .then(Commands.literal("done").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BankQueue.leave(p.getUUID());
                    BankQueue.tick(p.getServer());
                    Msg.bank(p, "Окно освобождено. " + BankQueue.status());
                    return 1;
                })));

        d.register(Commands.literal("job")
                .then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String name = StringArgumentType.getString(c, "name");
                    if (!Jobs.valid(name)) {
                        Msg.job(p, "Работы: lumberjack, loader, roadworker, collector, none");
                        return 0;
                    }
                    Jobs.setJob(p, name);
                    return 1;
                }))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String job = Economy.acc(p).job;
                    Msg.job(p, job.isEmpty() ? "У вас нет работы. Биржа труда: прораб в здании слева от банка." : Jobs.title(job) + ". " + Jobs.hint(job));
                    return 1;
                }));

        d.register(Commands.literal("shop")
                .then(Commands.literal("buy").then(Commands.argument("item", StringArgumentType.word()).executes(c -> {
                    Shop.buy(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "item"));
                    return 1;
                }))));

        d.register(Commands.literal("city")
                .then(Commands.literal("spawn").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    City city = City.current;
                    if (city == null) { p.sendSystemMessage(Component.literal("§cГород ещё не создан.")); return 0; }
                    city.teleportToSpawn(p);
                    return 1;
                })));

        // Для тестирования: выдать деньги (только операторы).
        d.register(Commands.literal("givemoney")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(c -> {
                                    ServerPlayer p = EntityArgument.getPlayer(c, "player");
                                    Economy.pay(p, LongArgumentType.getLong(c, "amount"));
                                    return 1;
                                }))));
    }

    private static boolean requireCard(ServerPlayer p) {
        if (Economy.acc(p).hasCard) return true;
        Msg.bank(p, "Сначала оформите карту (меню банка или /bank card).");
        return false;
    }
}
