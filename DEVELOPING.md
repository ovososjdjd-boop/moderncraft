# Developing moderncraft

This file is for people who want to add to the mod. The mod is structured so each subsystem owns its package under `com.moderncraft.economy` (or `com.moderncraft.client` for client-only code).

## Code layout

```
src/main/java/com/moderncraft/
├── Moderncraft.java          # main entry point, calls every *.register()
├── command/                  # Brigadier commands
│   ├── ModCommands.java      # single registration point
│   ├── BalanceCommands.java
│   ├── CatalogCommands.java
│   ├── VillageCommands.java
│   └── ModerncraftCommand.java
└── economy/
    ├── ModItems.java
    ├── ModBlocks.java
    ├── ModItemGroups.java
    ├── ModBlockEntities.java
    ├── EconomyMessages.java
    ├── PlayerJoinHandler.java
    ├── state/                # PersistentState: PlayerAccount, WorldEconomyState, EconomyService
    ├── price/               # data-pack driven catalog: PriceCatalog, ItemPrice, PriceCategory, PriceCatalogView
    ├── phone/               # PhoneItem + packets + GUI
    ├── pickup/              # PickupPoint block, BE, screen, packets
    ├── factory/             # Factory base / chimney / pipe / gear + BE + screen
    ├── bank/                # Bank block + BE + screen
    ├── stock/               # Stock market PersistentState + block + screen
    ├── jobs/                # CafeCourier + Loader + JobBoard + LoaderTarget
    └── village/             # Auto-generated starter village
```

## How to add a new price category

The catalog is **data-pack driven**. You don't need Java changes to add categories — drop a folder into the world's `datapacks/`:

```
world/datapacks/my_moderncraft/data/moderncraft/catalog/my_category.json
```

Then either:

1. Add the file to the mod's `assets/moderncraft/data/moderncraft/catalog/categories.json` (so it's bundled with the mod), or
2. Make your own data-pack and put both the `categories.json` and the per-category files in there. The reload listener picks up data-pack resources on every `/reload`.

The schema is documented in the file itself; a row looks like:

```json
{ "id": "minecraft:some_item", "buy": 4, "sell": 2, "name": "Display Name Override" }
```

- `id` is required, full namespaced. The loader normalises lowercase and adds the `minecraft:` prefix.
- `buy` is what the player pays. `sell` is what they receive when selling at a Pickup Point. `sell` must be less than `buy`.
- `name` is optional. If absent, the loader auto-derives a display name from the id (`ender_pearl` → `Ender Pearl`).

To register a new category file in the bundle, edit `src/main/resources/data/moderncraft/catalog/categories.json` and add an entry under `categories`. The order field controls the tab order in the phone's catalog screen.

## How to add a new building (a Block + BlockEntity + Screen)

1. **Create the block** in `com.moderncraft.economy.<subsystem>/<Name>Block.java`. Extend `BlockWithEntity`, override `getRenderType` to `MODEL` and `createBlockEntity` to return your BE.
2. **Create the BlockEntity** in the same package. It should extend `BlockEntity` and have a constructor `BlockEntity(BlockEntityType, BlockPos, BlockState)`. For Minecraft 1.21.1 override `writeNbt(NbtCompound, RegistryWrapper.WrapperLookup)` / `readNbt(NbtCompound, RegistryWrapper.WrapperLookup)` for persistence.
3. **Register the BlockEntityType** in `ModBlockEntities.java`. Use `FabricBlockEntityTypeBuilder.create(BE::new, yourBlock).build()`.
4. **Create the BlockItem** (if you want it in the creative tab). `ModBlocks.registerWithItem(name, block)` does this for you. Or register manually in `ModBlocks`.
5. **Add a creative tab entry** in `ModItemGroups.MOD_GROUP.entries(...)`.
6. **Add translations** in `assets/moderncraft/lang/en_us.json` and `ru_ru.json`.
7. **Add the building to the starter village** in `VillageGenerator.generate(...)` and add a private `placeX(world, origin)` method that places the structure.

## How to add a new NPC / job

A "job" in moderncraft is an entity that:
- spawns near a building (or anywhere),
- generates tasks for the player,
- has a glowing + particle highlight while a task is active,
- accepts a delivery (right-click) and pays via `EconomyService.creditWallet`.

The cafe courier (`CafeCourierEntity`) and the loader (`LoaderEntity`) are the two existing examples. They both extend `PathAwareEntity` and override `tickMovement()` to run their task lifecycle on the server.

To add a third job:
1. **Define the order record** in `com.moderncraft.economy.jobs.<JobName>Order.java`. It should carry enough info to complete the task (what to bring, where, how much).
2. **Create the entity class** in the same package. Extend `PathAwareEntity`. Override `initGoals`, `tickMovement` (server-only, generates / highlights / fulfils the order), and `interactMob` (right-click handler).
3. **Register the entity + spawn egg** in a `Entities` class (see `CourierEntities` / `LoaderEntities`).
4. **Register the renderer** in `client/com/moderncraft/client/jobs/`. The existing renderers are no-draw placeholders — replace them with a real model when you have textures.
5. **Add a spawn position in the village** by editing `VillageGenerator.generate()`.

## How to add a new persistent state

Persistent state lives in `com.moderncraft.economy.state/` or `<subsystem>/`. The pattern is:

```java
public final class FooState extends PersistentState {
    private static final PersistentStateType<FooState> TYPE =
            new PersistentStateType<>(Identifier.of(MOD_ID, "foo"),
                    FooState::new, FooState::fromNbt, null);

    public static FooState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }
    // ... fields, methods ...
}
```

The state is automatically saved when the world unloads. Override `markDirty()` whenever you change a field.

## How to add a new network packet

moderncraft uses Fabric's `CustomPayload` API. Each subsystem has its own `<Subsystem>Networking.java` that defines the record types, the codecs, and `registerCommon()` / `registerServer()` methods.

Wire it in `Moderncraft.onInitialize()`:
```java
YourNetworking.registerCommon();
YourNetworking.registerServer();
```

And in `ModerncraftClient.onInitializeClient()`:
```java
YourNetworking.registerCommon();
YourClientHandler.register();
```

`registerCommon()` must be called on **both** sides so the payload types are registered consistently.

## Style

- Java 17+ syntax is fine. We use records freely.
- No external libraries beyond Fabric API and the mod's own code.
- Each subsystem owns its package; subsystems talk to each other through `EconomyService` (for money) and the network packets.
- 4-space indentation, prefer `final` on local variables and fields that don't change.

## Testing

There are no unit tests yet. Recommended manual test cycle after a change:
1. `./gradlew build` — must compile clean.
2. `./gradlew runClient` — open a new world, verify first-join generates the village, verify the building you changed works.
3. `/moderncraft` — quick sanity check.
4. `/balance` — make sure money still persists after a relog.
