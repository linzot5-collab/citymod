package com.citymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface BlockAction {
    void use(ServerPlayer player, ServerLevel level, BlockPos pos);
}
