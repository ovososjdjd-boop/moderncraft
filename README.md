# moderncraft

A Fabric mod for **Minecraft 1.21.1** that turns survival into a living economy. Vanilla villager trading is gone — everything goes through phones, banks, jobs and a stock exchange.

The world starts empty. The first player to join a world gets a starter village generated near spawn, complete with working buildings and two NPCs.

## What you can do

- **Right-click the phone** in your inventory to open your in-game device. Browse the catalog (1300+ items, 20 categories) and buy anything you can afford. Deposit / withdraw from your bank.
- **Use the Pickup Point** to sell items from your inventory. Sell price is lower than buy price so the economy can't print money.
- **Work a shift at the Factory** (3 minutes). The factory produces redstone parts and pays you for each item, plus a completion bonus. Right-click the factory base to start.
- **The Cafe Courier** (an NPC at the cafe) hands you delivery orders — bring the right item, get paid 1.5× sell price. He glows and emits particles while he has an order for you.
- **The Loader** (an NPC at the loader depot) hands you heavy-block delivery orders. Bring iron blocks, gold blocks, obsidian, anvils or diamond blocks to a yellow Loader Target block on the plaza.
- **The Bank building** has the same deposit / withdraw UI as the phone, plus a 'branch lifetime deposits' stat.
- **The Stock Exchange** lets you buy and sell shares in five companies (Logistics, RedstoneCo, Pickup Express, First M$ Bank, Industrial Foundry). Prices drift every 20 minutes.

## Starter village layout

When you first join a world, a 23×23 plaza is built near spawn with 9 buildings:

- **Factory** (north-west) — fully assembled, you can start a shift immediately
- **Pickup Point** (north) — sell items here
- **Loader Depot** (north-east) — talk to the loader NPC, deliver to yellow targets
- **Cafe** (west) — talk to the courier NPC
- **Village Centre** (centre) — notice board
- **Bank** (east) — same UI as the phone's bank app, with a stat
- **House** (south-west, south, south-east) — three small player houses

If you don't like where the village spawned, run `/village here` (operator) to regenerate it at your position.

## Commands

- `/balance` — show your wallet and bank
- `/balance pay <player> <amount>` — transfer money
- `/balance deposit <amount>` / `/balance withdraw <amount>` — move money
- `/balance give <player> <amount>` — operator grant
- `/catalog` — list categories
- `/catalog list <category>` — list items in a category with prices
- `/catalog price <id>` — one-shot lookup (e.g. `/catalog price minecraft:diamond`)
- `/village status` — show where the village is
- `/village regen` / `/village here` — operator-only rebuild
- `/moderncraft` — short summary (balance, village, catalog, market)

All money is server-side and persisted in the world's PersistentState. The economy is a closed loop: do a job → sell what you made → buy upgrades with the phone.

## Build

The repo is the project root. On a machine with Java 21 and an internet connection:

```bash
gradle wrapper --gradle-version 8.10.2
./gradlew build           # builds the mod jar
./gradlew runClient       # launches a dev client
```

The wrapper jar isn't checked in (binaries aren't tracked). Run `gradle wrapper` once to generate it.

## Compatibility

- Minecraft **1.21.1**
- Fabric Loader **0.16.14**
- Fabric API **0.107.0+1.21.1**
- Java **21**

## For modders

See [DEVELOPING.md](DEVELOPING.md) for how to add new categories, prices, jobs and buildings.
