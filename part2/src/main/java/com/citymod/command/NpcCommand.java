package com.citymod.command;

import com.citymod.CityMod;
import com.citymod.entity.CityNpc;
import com.citymod.entity.NpcRole;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/** /npc spawn <роль 0-5> [скин 0-8] — поставить NPC на своём месте (для операторов). */
public final class NpcCommand {
    private NpcCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("npc").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("role", IntegerArgumentType.integer(0, NpcRole.values().length - 1))
                                .executes(c -> spawn(c.getSource(), IntegerArgumentType.getInteger(c, "role"), 0))
                                .then(Commands.argument("skin", IntegerArgumentType.integer(0, 8))
                                        .executes(c -> spawn(c.getSource(),
                                                IntegerArgumentType.getInteger(c, "role"),
                                                IntegerArgumentType.getInteger(c, "skin"))))))
                .then(Commands.literal("roles").executes(c -> {
                    StringBuilder sb = new StringBuilder();
                    for (NpcRole r : NpcRole.values()) sb.append(r.ordinal()).append("=").append(r.title).append("  ");
                    c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    return 1;
                })));
    }

    private static int spawn(CommandSourceStack src, int roleId, int skin) {
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        CityNpc npc = CityMod.CITY_NPC.get().create(level);
        if (npc == null) return 0;
        npc.moveTo(pos.x, pos.y, pos.z, src.getRotation().y, 0);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.COMMAND, null);
        npc.setRole(NpcRole.byId(roleId));
        npc.setSkin(skin);
        level.addFreshEntity(npc);
        return 1;
    }
}
