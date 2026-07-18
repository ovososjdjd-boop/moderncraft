package com.moderncraft.economy.price;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.moderncraft.Moderncraft;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the price catalog from {@code data/moderncraft/catalog/*} as a
 * <em>data-pack</em> resource. This means:
 * <ul>
 *     <li>Players can drop a folder {@code moderncraft} into a world's
 *         {@code datapacks/} directory and override any individual file.</li>
 *     <li>Other mods in the same modpack can ship their own data folder and
 *         contribute prices without code changes.</li>
 *     <li>Server admins can hot-reload by running {@code /reload} and see new
 *         prices live (we re-implement the reload listener for that).</li>
 * </ul>
 * <p>
 * Storage: this class itself is just a cache. The single source of truth is
 * the resource manager. We re-prepare on every (re)load.
 */
public final class PriceCatalog {

    private static final String CATALOG_DIR = "catalog";
    private static final Identifier CATEGORIES_ID = id("catalog/categories.json");
    private static final Gson GSON = new Gson();
    private static final Type LIST_OF_PRICES = new TypeToken<List<RawEntry>>() {}.getType();

    private final Map<String, ItemPrice> byId = new HashMap<>();
    private final Map<String, List<ItemPrice>> byCategory = new HashMap<>();
    private final List<PriceCategory> categories = new ArrayList<>();

    public static final PriceCatalog INSTANCE = new PriceCatalog();

    private PriceCatalog() {}

    /** Called by the resource reload listener. */
    public void apply(List<PriceCategory> categories,
                      Map<String, List<ItemPrice>> byCategory,
                      Map<String, ItemPrice> byId) {
        this.categories.clear();
        this.categories.addAll(categories);
        this.categories.sort(Comparator.comparingInt(PriceCategory::order));
        this.byCategory.clear();
        this.byCategory.putAll(byCategory);
        this.byId.clear();
        this.byId.putAll(byId);
        Moderncraft.LOGGER.info("[moderncraft] price catalog applied: {} categories, {} items.",
                this.categories.size(), this.byId.size());
    }

    public List<PriceCategory> categories() {
        return List.copyOf(categories);
    }

    public List<ItemPrice> byCategory(String categoryId) {
        return byCategory.getOrDefault(categoryId, List.of());
    }

    public ItemPrice lookup(String id) {
        if (id == null) return null;
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public int totalItems() { return byId.size(); }

    public boolean isEmpty() { return byId.isEmpty(); }

    public static Identifier id(String path) {
        return Identifier.of(Moderncraft.MOD_ID, path);
    }

    public static String normaliseId(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT);
        if (!s.contains(":")) s = "minecraft:" + s;
        return s;
    }

    // --- reload listener ----------------------------------------------------

    /**
     * Resource listener that re-prepares the catalog whenever data-packs
     * (re)load. Registered through {@code ResourceManagerHelper} on init.
     * <p>
     * Note: we can't extend {@link SinglePreparationResourceReloader} directly
     * here because the framework wants to call {@code prepare} on a worker
     * thread; instead we provide a thin anonymous class.
     */
    public static IdentifiableResourceReloadListener newReloadListener() {
        return new SinglePreparationResourceReloader<Pair<List<PriceCategory>, Map<String, List<ItemPrice>>>>() {
            @Override
            protected Pair<List<PriceCategory>, Map<String, List<ItemPrice>>> prepare(
                    ResourceManager manager, Profiler profiler) {

                Map<String, ItemPrice> flatById = new HashMap<>();
                Map<String, List<ItemPrice>> bucketByCategory = new HashMap<>();
                List<PriceCategory> loadedCategories = new ArrayList<>();

                JsonObject meta = readJson(manager, CATEGORIES_ID);
                if (meta == null || !meta.has("categories")) {
                    Moderncraft.LOGGER.warn("[moderncraft] catalog/categories.json not found — prices will be empty");
                    return new Pair<>(loadedCategories, bucketByCategory);
                }
                for (var elem : meta.getAsJsonArray("categories")) {
                    JsonObject obj = elem.getAsJsonObject();
                    List<String> files = new ArrayList<>();
                    if (obj.has("files")) {
                        obj.getAsJsonArray("files").forEach(e -> files.add(e.getAsString()));
                    }
                    PriceCategory cat = new PriceCategory(
                            obj.get("id").getAsString(),
                            obj.get("displayName").getAsString(),
                            obj.has("order") ? obj.get("order").getAsInt() : 100,
                            obj.has("icon") ? obj.get("icon").getAsString() : "minecraft:barrier",
                            files
                    );
                    loadedCategories.add(cat);

                    List<ItemPrice> bucket = new ArrayList<>();
                    for (String file : files) {
                        Identifier fileId = id(CATALOG_DIR + "/" + cat.id() + "/" + file);
                        List<RawEntry> raw = readJsonList(manager, fileId);
                        if (raw == null) {
                            Moderncraft.LOGGER.warn("[moderncraft] catalog file missing: {}", fileId);
                            continue;
                        }
                        for (RawEntry e : raw) {
                            String normId = normaliseId(e.id);
                            ItemPrice p = new ItemPrice(normId, cat.id(), e.buy, e.sell, e.name);
                            flatById.put(normId, p);
                            bucket.add(p);
                        }
                    }
                    bucket.sort(Comparator.comparing(ItemPrice::displayName));
                    bucketByCategory.put(cat.id(), bucket);
                }
                return new Pair<>(loadedCategories, bucketByCategory);
            }

            @Override
            protected void apply(Pair<List<PriceCategory>, Map<String, List<ItemPrice>>> prepared,
                                 ResourceManager manager, Profiler profiler) {
                Map<String, ItemPrice> flat = new HashMap<>();
                for (var bucket : prepared.getRight().values()) {
                    for (ItemPrice p : bucket) flat.put(p.id(), p);
                }
                INSTANCE.apply(prepared.getLeft(), prepared.getRight(), flat);
            }

            @Override
            public Identifier getFabricId() {
                return id("price_catalog");
            }
        };
    }

    // --- io helpers ---------------------------------------------------------

    private static JsonObject readJson(ResourceManager mgr, Identifier resId) {
        try {
            var opt = mgr.getResource(resId);
            if (opt.isEmpty()) return null;
            try (Reader r = opt.get().getReader()) {
                return JsonParser.parseReader(r).getAsJsonObject();
            }
        } catch (Exception ex) {
            Moderncraft.LOGGER.error("Failed to read {}: {}", resId, ex.toString());
            return null;
        }
    }

    private static List<RawEntry> readJsonList(ResourceManager mgr, Identifier resId) {
        try {
            var opt = mgr.getResource(resId);
            if (opt.isEmpty()) return null;
            try (Reader r = opt.get().getReader()) {
                return GSON.fromJson(r, LIST_OF_PRICES);
            }
        } catch (Exception ex) {
            Moderncraft.LOGGER.error("Failed to read {}: {}", resId, ex.toString());
            return null;
        }
    }

    /** Flat row from per-category JSON. {@code name} is optional. */
    public static final class RawEntry {
        public String id;
        public String name;
        public int buy;
        public int sell;
    }
}
