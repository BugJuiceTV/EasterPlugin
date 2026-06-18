# RustEaster
 
 RustEaster is a high-performance, feature-rich Easter event plugin for Minecraft servers (1.20.x - 1.21.4). It introduces an automated, proximity-based egg-hunting experience with deep support for custom 3D models, item-based progression, and competitive leaderboards.
 
 ## 🎨 Visuals & Model System
 
 RustEaster features a **Dual-Support Visual Engine**. Eggs are rendered using invisible Armor Stands, allowing for high-quality 3D models to be placed precisely on the terrain.
 
 ### 1. Modern Item Models (Minecraft 1.21.4+)
 The plugin is fully compatible with the 1.21.4+ `item_model` component.
 *   **Detection:** If a config entry contains a colon (`:`), the plugin treats it as a modern namespace (e.g., `easter:ia_auto/yellow_egg`).
 *   **Benefit:** Allows for high-definition models without hijacking `CustomModelData` IDs.
 
 ### 2. Legacy Custom Model Data
 For servers on older versions or using traditional resource packs:
 *   **Detection:** If an entry is a pure integer (e.g., `10202`), it applies that ID to the `custom-model-base-material` (Default: `PAPER`).
 
 ### 3. Precision Positioning
 *   **Equipment Slot:** Configure eggs to be placed in the `HEAD` (recommended for 3D) or `HAND` slot.
 *   **Y-Offset:** Defaulted to `-1.7`. This ensures the Armor Stand's head (the egg) sits perfectly on the block surface.
 *   **Scaling:** Supports `NORMAL` and `SMALL` armor stand sizes for different egg varieties.
 
 ---
 
 ## 🥚 Gameplay Mechanics
 
 ### Dynamic Spawning
 The plugin runs a background cycle (default: 3s) that checks for active players.
 *   **Logic:** Eggs spawn randomly within a 10–25 block radius of players.
 *   **Lifespan:** Eggs remain on the ground for 60 seconds before despawning, keeping the world clean.
 *   **Blacklist:** Spawning is automatically disabled in specified worlds (e.g., `world_the_end`, `world_nether`).
 
 ### Collection System
 To collect an egg, a player must stand near it. 
 *   **Pickup Time:** Requires 40 ticks (2 seconds) of proximity.
 *   **Sound Effects:** Features immersive audio for event starts, ends, and successful pickups.
 
 ### Tiered Progression & Conversion
 Players can upgrade their findings by **Shift-Right-Clicking** while holding a stack of 10 eggs:
 1.  **Default Egg**: Common find. (10x -> 1 Bronze)
 2.  **Bronze Egg**: Common loot. (10x -> 1 Silver)
 3.  **Silver Egg**: Rare loot. (10x -> 1 Gold)
 4.  **Gold Egg**: Ultimate rewards.
 
 ---
 
 ## 💎 Special Equipment
 
 You can define "Special Items" that provide mechanical advantages. These are assigned via administrative commands.
 
 ### **The Easter Basket**
 *   **Ability:** Provides a **2.0x Pickup Multiplier**.
 *   **Effect:** Reduces collection time from 2 seconds to 1 second.
 *   **Requirement:** Must be held in the main hand.
 
 ### **Bunny Ears**
 *   **Ability:** Provides a **2.0x Spawn Multiplier**.
 *   **Effect:** Doubles the chance of an egg spawning near the wearer during the spawn cycle.
 *   **Requirement:** Must be equipped in the **Helmet** slot (configurable to work from the inventory).
 
 ---
 
 ## 🎁 Rewards & Unboxing
 
 ### Automated Events
 The plugin features a robust `auto-start` scheduler.
 *   **Leaderboards:** At the end of the duration, the top 3 players are rewarded.
 *   **Custom Rewards:** Supports both physical items and console commands (e.g., `eco give {player} 1000`).
 
 ### Unboxing Rewards
 Right-clicking tiered eggs (Bronze+) triggers the unboxing system.
 *   **Item Rewards:** `item:MATERIAL:AMOUNT`
 *   **Command Rewards:** `cmd:your command here`
 *   **Special Rewards:** Ability to unbox the Easter Basket or Bunny Ears directly.
 
 ---
 
 ## 🛠 Configuration Guide
 
 ### Key Sections in `config.yml`:
 *   **`event-settings`**: Change the `main-command` (requires restart).
 *   **`egg-visuals`**: Define your resource pack model paths.
 *   **`auto-start`**: Configure the daily schedule and minimum player count.
 *   **`collected-egg-items`**: Customize the Name, Lore, and Item Models for the inventory items.
 
 ---
 
 ## 🎮 Commands & Permissions
 
 | Command | Description | Permission |
 |:---|:---|:---|
 | `/easter` | View leaderboard and event status. | `rusteaster.use` |
 | `/rusteaster setitem` | Set hand item as the **Easter Basket**. | `rusteaster.admin` |
 | `/rusteaster setitem spawn` | Set hand item as the **Bunny Ears**. | `rusteaster.admin` |
 | `/easter give <tier> <qty> <p>` | Manually grant eggs to players. | `rusteaster.admin` |
 | `/easter start <seconds>` | Force start an event. | `rusteaster.admin` |
 | `/easter stop` | Force stop the current event. | `rusteaster.admin` |
 
 ---
