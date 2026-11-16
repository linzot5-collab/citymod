package com.citymod.item;

import com.citymod.economy.Economy;
import com.citymod.economy.EconomyData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Банковская карта: правый клик показывает состояние счёта. */
public class BankCardItem extends Item {
    public BankCardItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            EconomyData.Account a = Economy.acc(sp);
            sp.sendSystemMessage(Component.literal("§6[Карта] §fСчёт: §b" + a.bank + " $§f, кредит: §c" + a.credit + " $"));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
