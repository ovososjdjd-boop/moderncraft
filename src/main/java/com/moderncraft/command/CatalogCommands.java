package com.moderncraft.command;

import com.moderncraft.economy.price.PriceCatalog;
import com.moderncraft.economy.price.PriceCategory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Read-only debug commands for the price catalog. Useful while balancing the
 * economy and for players to look up prices without opening the phone.
 */
public final class CatalogCommands {

    private CatalogCommands() {}

    private static final SuggestionProvider<ServerCommandSource> CATEGORY_SUGGEST =
            (ctx, builder) -> {
                for (PriceCategory c : PriceCatalog.INSTANCE.categories()) {
                    if (c.id().startsWith(builder.getRemainingLowerCase())) builder.suggest(c.id());
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("catalog")
                        .executes(CatalogCommands::runSummary)
                        .then(literal("list")
                                .executes(CatalogCommands::runList)
                                .then(argument("category", StringArgumentType.string())
                                        .suggests(CATEGORY_SUGGEST)
                                        .executes(CatalogCommands::runListCategory)))
                        .then(literal("price")
                                .then(argument("id", StringArgumentType.string())
                                        .executes(CatalogCommands::runPrice)))
        );
    }

    private static int runSummary(CommandContext<ServerCommandSource> ctx) {
        var cats = PriceCatalog.INSTANCE.categories();
        ctx.getSource().sendMessage(() -> Text.empty()
                .append(Text.literal("Price catalog: ").formatted(Formatting.GOLD))
                .append(Text.literal(cats.size() + " categories, " + PriceCatalog.INSTANCE.totalItems() + " items.")
                        .formatted(Formatting.GRAY)));
        for (PriceCategory c : cats) {
            int n = PriceCatalog.INSTANCE.byCategory(c.id()).size();
            ctx.getSource().sendMessage(() -> Text.empty()
                    .append(Text.literal(" • ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(c.id()).formatted(Formatting.WHITE))
                    .append(Text.literal(" — " + c.displayName() + " (" + n + ")").formatted(Formatting.GRAY)));
        }
        return 1;
    }

    private static int runList(CommandContext<ServerCommandSource> ctx) {
        // Lists the first 20 items of every category (so the chat isn't spammed).
        for (PriceCategory c : PriceCatalog.INSTANCE.categories()) {
            ctx.getSource().sendMessage(() -> Text.literal(c.displayName() + ":").formatted(Formatting.GOLD));
            var items = PriceCatalog.INSTANCE.byCategory(c.id());
            int shown = Math.min(items.size(), 20);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < shown; i++) {
                if (i > 0) sb.append(", ");
                sb.append(items.get(i).displayName());
            }
            if (items.size() > shown) sb.append(", … (+").append(items.size() - shown).append(" more)");
            ctx.getSource().sendMessage(() -> Text.literal("  " + sb).formatted(Formatting.GRAY));
        }
        return 1;
    }

    private static int runListCategory(CommandContext<ServerCommandSource> ctx) {
        String cat = StringArgumentType.getString(ctx, "category");
        var items = PriceCatalog.INSTANCE.byCategory(cat);
        if (items.isEmpty()) {
            ctx.getSource().sendError(Text.literal("No such category: " + cat).formatted(Formatting.RED));
            return 0;
        }
        for (var p : items) {
            ctx.getSource().sendMessage(() -> Text.empty()
                    .append(Text.literal(" • " + p.displayName() + "  ").formatted(Formatting.WHITE))
                    .append(Text.literal("buy " + p.buy() + " / sell " + p.sell() + " M$").formatted(Formatting.GREEN)));
        }
        return 1;
    }

    private static int runPrice(CommandContext<ServerCommandSource> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        var opt = PriceCatalog.INSTANCE.lookup(id);
        if (opt == null) {
            ctx.getSource().sendError(Text.literal("No price for '" + id + "'.").formatted(Formatting.RED));
            return 0;
        }
        var p = opt;
        ctx.getSource().sendMessage(() -> Text.empty()
                .append(Text.literal(p.displayName() + " ").formatted(Formatting.WHITE))
                .append(Text.literal("[" + p.category() + "]").formatted(Formatting.DARK_GRAY))
                .append(Text.literal(" — buy " + p.buy() + " / sell " + p.sell() + " M$").formatted(Formatting.GREEN))
        );
        return 1;
    }
}
