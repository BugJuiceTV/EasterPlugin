package org.bug;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class EggManager {

    private final Main plugin;
    private final LeaderboardManager leaderboardManager;
    private final Set<Egg> activeEggs = Collections.synchronizedSet(new HashSet<>());
    private final Random random = new Random();
    private boolean isEventActive = false;

    private BukkitTask spawnTask;
    private BukkitTask timerTask;
    private BossBar bossBar;
    private long eventEndTime;
    private String activeRegionName = null;

    // Use a NamespacedKey for the Persistent Data Container (Modern way to tag entities)
    private final NamespacedKey EGG_PERSISTENT_KEY;

    private static final Set<Material> INVALID_SPAWN_BLOCKS = EnumSet.of(
            Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
            Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.DEAD_BUSH, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
            Material.BAMBOO, Material.BAMBOO_SAPLING, Material.SUGAR_CANE, Material.VINE, Material.LADDER,
            Material.SNOW, Material.WATER, Material.LAVA
    );

    public EggManager(Main plugin, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.leaderboardManager = leaderboardManager;
        this.EGG_PERSISTENT_KEY = new NamespacedKey(plugin, "rust_easter_egg");
    }

    public void startEvent(int durationSeconds) {
        startEvent(durationSeconds, null);
    }

    public void startEvent(int durationSeconds, String regionName) {
        if (isEventActive) return;
        isEventActive = true;
        this.activeRegionName = regionName;
        leaderboardManager.clearScores();
        plugin.getLogger().info("Starting Rust Easter Event!" + (regionName != null ? " (Region: " + regionName + ")" : ""));

        playSoundToAll(plugin.getSound("event-start"));

        eventEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        bossBar = Bukkit.createBossBar("Easter Event", BarColor.PINK, BarStyle.SOLID);
        bossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                long remainingMillis = eventEndTime - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    stopEvent();
                    return;
                }
                double progress = (double) remainingMillis / (durationSeconds * 1000L);
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                bossBar.setTitle(plugin.getLangManager().get("bossbar-title", "{time}", formatTime(remainingMillis)));
            }
        }.runTaskTimer(plugin, 0L, 20L);

        int spawnInterval = plugin.getCycleIntervalSeconds() * 20;
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnEggsAroundPlayers();
            }
        }.runTaskTimer(plugin, 0L, (long) spawnInterval);
    }

    public void stopEvent() {
        if (!isEventActive) return;
        isEventActive = false;
        activeRegionName = null;

        if (timerTask != null) timerTask.cancel();
        if (spawnTask != null) spawnTask.cancel();

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }

        playSoundToAll(plugin.getSound("event-end"));
        announceWinnersAndGiveRewards();
        despawnAllEggs();
        plugin.getLogger().info("Rust Easter Event Ended!");
    }

    private void announceWinnersAndGiveRewards() {
        Bukkit.broadcastMessage(plugin.getLangManager().get("event-over-header"));
        Map<String, Integer> topPlayers = leaderboardManager.getTopPlayers(3);
        if (topPlayers.isEmpty()) {
            Bukkit.broadcastMessage(plugin.getLangManager().get("no-winners"));
        } else {
            int rank = 1;
            for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
                String playerName = entry.getKey();
                Bukkit.broadcastMessage(plugin.getLangManager().get("winner-entry", "{rank}", String.valueOf(rank), "{player}", playerName, "{score}", String.valueOf(entry.getValue())));

                String rewardPath = switch (rank) {
                    case 1 -> "first-place";
                    case 2 -> "second-place";
                    case 3 -> "third-place";
                    default -> "";
                };

                List<String> commands = plugin.getRewards(rewardPath);
                for (String command : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", playerName));
                }
                rank++;
            }
        }
    }

    private void spawnEggsAroundPlayers() {
        List<String> blacklistedWorlds = plugin.getBlacklistedWorlds();
        double baseSpawnChance = plugin.getSpawnChancePerPlayer();
        ItemStack spawnBoosterItem = plugin.getSpawnBoosterItem();
        double multiplier = plugin.getSpawnBoosterMultiplier();
        boolean checkInventory = plugin.isSpawnBoosterInventoryEnabled();
        
        List<RegionManager.EasterRegion> regions = plugin.getRegionManager().getRegions();

        // 1. If we have an active region restriction or global regions
        if (activeRegionName != null || !regions.isEmpty()) {
            List<RegionManager.EasterRegion> targets = new ArrayList<>();
            if (activeRegionName != null) {
                for (RegionManager.EasterRegion r : regions) if (r.getName().equalsIgnoreCase(activeRegionName)) targets.add(r);
            } else {
                targets.addAll(regions);
            }

            if (!targets.isEmpty()) {
                // REDUCED SPAWN COUNT FOR REGIONS
                int eggsToSpawn = (int) Math.ceil(Bukkit.getOnlinePlayers().size() * baseSpawnChance * 3);
                for (int i = 0; i < eggsToSpawn; i++) {
                    RegionManager.EasterRegion region = targets.get(random.nextInt(targets.size()));
                    Location randomLoc = region.getRandomLocation();
                    Location groundLoc = findGround(randomLoc);
                    if (groundLoc != null && region.contains(groundLoc)) {
                        spawnEggAt(groundLoc.add(0, 1, 0));
                    }
                }
                
                // If a specific region is set, we ONLY spawn in that region
                if (activeRegionName != null) return;
            }
        }
        
        // 2. Default spawning around players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (blacklistedWorlds.contains(player.getWorld().getName())) continue;

            double chance = baseSpawnChance;
            if (spawnBoosterItem != null) {
                boolean hasBooster = false;
                if (checkInventory) {
                    if (player.getInventory().containsAtLeast(spawnBoosterItem, 1)) {
                        hasBooster = true;
                    }
                } else {
                    ItemStack helmet = player.getInventory().getHelmet();
                    if (helmet != null && helmet.isSimilar(spawnBoosterItem)) {
                        hasBooster = true;
                    }
                }
                
                if (hasBooster) {
                    chance *= multiplier;
                }
            }

            if (random.nextDouble() < chance) spawnEggNear(player);
        }
    }

    private void spawnEggNear(Player player) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return;

        int minRadius = plugin.getMinSpawnRadius();
        int maxRadius = plugin.getMaxSpawnRadius();

        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
            Location randomLoc = playerLoc.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            Location groundLoc = findGround(randomLoc);

            if (groundLoc != null) {
                Location spawnLoc = groundLoc.add(0, 1, 0);
                if (isValidSpawnLocation(spawnLoc)) {
                    spawnEggAt(spawnLoc);
                    return;
                }
            }
        }
    }

    private Location findGround(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;
        for (int y = world.getMaxHeight() - 1; y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (block.getType().isSolid() && !INVALID_SPAWN_BLOCKS.contains(block.getType())) {
                return block.getLocation();
            }
        }
        return null;
    }

    private boolean isValidSpawnLocation(Location loc) {
        Block block = loc.getBlock();
        return block.getType() == Material.AIR && !block.isLiquid();
    }

    private void spawnEggAt(Location loc) {
        double yOffset = plugin.getYOffset();
        Location spawnLoc = loc.clone().add(0.5, yOffset, 0.5);

        ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

        // Tag with PersistentData so it survives restarts
        armorStand.getPersistentDataContainer().set(EGG_PERSISTENT_KEY, PersistentDataType.BYTE, (byte) 1);

        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCustomName("Easter Egg");
        armorStand.setCustomNameVisible(true);
        armorStand.setCanPickupItems(false);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);

        // Disable all slots to prevent manual removal of the head/hand item
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            armorStand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
            armorStand.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
        }

        if (plugin.getArmorStandSize().equals("SMALL")) armorStand.setSmall(true);
        armorStand.setHeadPose(plugin.getHeadPose());
        armorStand.setBodyPose(plugin.getBodyPose());

        Material baseMat = plugin.getEggBaseMaterial();
        if (baseMat == null) baseMat = Material.PAPER;
        ItemStack eggItem = new ItemStack(baseMat);
        ItemMeta meta = eggItem.getItemMeta();

        if (meta != null) {
            List<String> modelList = plugin.getConfig().getStringList("egg-visuals.custom-model-data-list");
            if (modelList != null && !modelList.isEmpty()) {
                String selected = modelList.get(random.nextInt(modelList.size()));
                if (selected.contains(":")) {
                    meta.setItemModel(NamespacedKey.fromString(selected));
                } else {
                    try {
                        double cmdValue = Double.parseDouble(selected);
                        meta.setCustomModelData((int) cmdValue);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid egg visual in config: " + selected);
                    }
                }
            }
            eggItem.setItemMeta(meta);
        }

        String slot = plugin.getEquipmentSlot();
        if (slot.equals("HAND")) Objects.requireNonNull(armorStand.getEquipment()).setItemInMainHand(eggItem);
        else Objects.requireNonNull(armorStand.getEquipment()).setHelmet(eggItem);

        Egg egg = new Egg(loc, armorStand);
        activeEggs.add(egg);

        int lifespan = plugin.getEggLifespanSeconds();
        if (lifespan > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    removeEgg(egg);
                }
            }.runTaskLater(plugin, lifespan * 20L);
        }
    }

    public void despawnAllEggs() {
        // Create a copy to avoid ConcurrentModificationException
        synchronized (activeEggs) {
            Iterator<Egg> iterator = activeEggs.iterator();
            while (iterator.hasNext()) {
                Egg egg = iterator.next();
                if (egg.getArmorStand() != null) {
                    egg.getArmorStand().remove();
                }
                iterator.remove();
            }
        }
        
        // Safety: Double check worlds for any lingering ones with the key
        cleanupLingeringEggs();
    }

    public void cleanupLingeringEggs() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand as : world.getEntitiesByClass(ArmorStand.class)) {
                if (isEgg(as)) {
                    as.remove();
                    count++;
                }
            }
        }
        activeEggs.clear();
        if (count > 0) plugin.getLogger().info("Removed " + count + " lingering eggs.");
    }

    public boolean isEgg(Entity entity) {
        if (!(entity instanceof ArmorStand)) return false;
        return entity.getPersistentDataContainer().has(EGG_PERSISTENT_KEY, PersistentDataType.BYTE) ||
                entity.hasMetadata("RustEasterEgg");
    }

    public Egg getEggByEntity(ArmorStand armorStand) {
        if (!isEgg(armorStand)) return null;

        synchronized (activeEggs) {
            return activeEggs.stream()
                    .filter(e -> e.getArmorStand().equals(armorStand))
                    .findFirst()
                    .orElse(null);
        }
    }

    public void removeEgg(Egg egg) {
        if (egg != null) {
            if (egg.getArmorStand() != null) {
                egg.getArmorStand().remove();
            }
            activeEggs.remove(egg);
        }
    }

    public boolean isEventActive() { return isEventActive; }

    public void addPlayerToBossBar(Player player) {
        if (isEventActive && bossBar != null) bossBar.addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player) {
        if (isEventActive && bossBar != null) bossBar.removePlayer(player);
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void playSoundToAll(Sound sound) {
        if (sound == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public void giveCollectedEgg(Player player) {
        if (!plugin.getConfig().getBoolean("collected-egg-items.give-physical-items", true)) return;
        ItemStack item = createCollectedEggItem("default");
        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
        if (!leftOver.isEmpty()) player.getWorld().dropItemNaturally(player.getLocation(), leftOver.get(0));

        Sound sound = plugin.getSound("egg-pickup");
        if (sound != null) player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public ItemStack createCollectedEggItem(String tier) {
        return plugin.createCollectedEggItem(tier);
    }

    public void unboxEgg(Player player, String tier) {
        List<String> lootList = plugin.getConfig().getStringList("collected-egg-items.tiers." + tier + ".unboxing");
        if (lootList == null || lootList.isEmpty()) return;

        String entry = lootList.get(random.nextInt(lootList.size()));
        String[] parts = entry.split(":", 2);
        String type = parts[0].toLowerCase();

        if (entry.equalsIgnoreCase("item:{special_item}")) {
            ItemStack special = plugin.getSpecialPickupItem();
            if (special == null || special.getType() == Material.AIR) return;
            ItemStack reward = special.clone();
            reward.setAmount(1);
            player.getInventory().addItem(reward);
            player.sendMessage(plugin.getLangManager().get("unbox-special"));
        }
        else if (entry.equalsIgnoreCase("item:{spawn_item}")) {
            ItemStack spawnItem = plugin.getSpawnBoosterItem();
            if (spawnItem == null || spawnItem.getType() == Material.AIR) return;
            ItemStack reward = spawnItem.clone();
            reward.setAmount(1);
            player.getInventory().addItem(reward);
            player.sendMessage(plugin.getLangManager().get("unbox-spawn"));
        }
        else if (type.equals("item")) {
            try {
                String[] itemParts = parts[1].split(":");
                Material mat = Material.getMaterial(itemParts[0].toUpperCase());
                if (mat != null) {
                    int amount = (itemParts.length > 1) ? Integer.parseInt(itemParts[1]) : 1;
                    ItemStack item = new ItemStack(mat, amount);
                    if (itemParts.length > 2) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setCustomModelData(Integer.parseInt(itemParts[2]));
                            item.setItemMeta(meta);
                        }
                    }
                    player.getInventory().addItem(item);
                    player.sendMessage(plugin.getLangManager().get("unbox-item", "{amount}", String.valueOf(amount), "{item}", mat.name()));
                }
            } catch (Exception ignored) {}
        }
        else if (type.equals("cmd")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[1].replace("{player}", player.getName()));
            player.sendMessage(plugin.getLangManager().get("unbox-reward"));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1.2f);
    }

    public String getEggTier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String basePath = "collected-egg-items.tiers";
        if (plugin.getConfig().getConfigurationSection(basePath) == null) return null;

        if (meta.hasItemModel()) {
            NamespacedKey modelKey = meta.getItemModel();
            if (modelKey != null) {
                String modelString = modelKey.toString();
                for (String key : plugin.getConfig().getConfigurationSection(basePath).getKeys(false)) {
                    String configModel = plugin.getConfig().getString(basePath + "." + key + ".item-model");
                    if (configModel != null && configModel.equalsIgnoreCase(modelString)) return key;
                }
            }
        }

        if (meta.hasCustomModelData()) {
            int modelData = meta.getCustomModelData();
            for (String key : plugin.getConfig().getConfigurationSection(basePath).getKeys(false)) {
                if (plugin.getConfig().contains(basePath + "." + key + ".custom-model-data")) {
                    if (modelData == plugin.getConfig().getInt(basePath + "." + key + ".custom-model-data")) return key;
                }
            }
        }
        return null;
    }
}