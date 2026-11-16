package com.citymod.item;

import com.citymod.crypto.PhoneSync;
import com.citymod.network.Net;
import com.citymod.network.OpenMenuPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Телефон: ПКМ открывает экран с приложениями. */
public class PhoneItem extends Item {
    public PhoneItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PhoneSync.send(sp);
            Net.toPlayer(sp, new OpenMenuPacket("phone"));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
