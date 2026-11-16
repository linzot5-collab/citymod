package com.citymod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Блок, у которого правый клик вызывает действие (банкомат, стол казино и т.д.). */
public class ActionBlock extends Block {
    private final BlockAction action;

    public ActionBlock(Properties props, BlockAction action) {
        super(props);
        this.action = action;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            action.use(sp, sl, pos);
        }
        return InteractionResult.CONSUME;
    }
}
