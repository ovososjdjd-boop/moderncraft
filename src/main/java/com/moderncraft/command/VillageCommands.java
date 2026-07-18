package com.moderncraft.command;

import com.moderncraft.economy.village.VillageGenerator;
import com.moderncraft.economy.village.VillageState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Debug/admin commands for the village.
 * <ul>
 *     <li>{@code /village status} — show center + generated flag</li>
 *     <li>{@code /village regen} — wipe and regenerate at the same center</li>
 *     <li>{@code /village here} — regenerate at the player's position</li>
 * </ul>
 */
public final class VillageCommands {

    private VillageCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("village")
                        .executes(VillageCommands::status)
                        .then(literal("status").executes(VillageCommands::status))
                        .then(literal("regen").requires(s -> s.hasPermissionLevel(2))
                                .executes(VillageCommands::regen))
                        .then(literal("here").requires(s -> s.hasPermissionLevel(2))
                                .executes(VillageCommands::here))
        );
    }

    private static int status(CommandContext<ServerCommandSource> ctx) {
        VillageState s = VillageState.get(ctx.getSource().getServer());
        if (!s.isGenerated()) {
            ctx.getSource().sendMessage(Text.literal("No village has been generated yet.").formatted(Formatting.YELLOW));
            return 1;
        }
        BlockPos c = s.getCenter();
        ctx.getSource().sendMessage(() -> Text.empty()
                .append(Text.literal("Village center: ").formatted(Formatting.GOLD))
                .append(Text.literal(c.getX() + ", " + c.getY() + ", " + c.getZ()).formatted(Formatting.WHITE)));
        return 1;
    }

    private static int regen(CommandContext<ServerCommandSource> ctx) {
        VillageState s = VillageState.get(ctx.getSource().getServer());
        if (!s.isGenerated()) {
            ctx.getSource().sendError(Text.literal("No village to regen. Use /village here."));
            return 0;
        }
        BlockPos c = s.getCenter();
        VillageGenerator.generate(ctx.getSource().getWorld(), c);
        ctx.getSource().sendMessage(Text.literal("Regenerated village at " + c.toShortString()).formatted(Formatting.GREEN));
        return 1;
    }

    private static int here(CommandContext<ServerCommandSource> ctx) {
        BlockPos pos = ctx.getSource().getPlayer().getBlockPos();
        VillageGenerator.generate(ctx.getSource().getWorld(), pos);
        VillageState.get(ctx.getSource().getServer()).setCenter(pos);
        ctx.getSource().sendMessage(Text.literal("Generated village at your position.").formatted(Formatting.GREEN));
        return 1;
    }
}
