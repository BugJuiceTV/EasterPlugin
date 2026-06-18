package org.bug;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.NamespacedKey;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class Main extends JavaPlugin {

    private EggManager eggManager;
    private PickupManager pickupManager;
    private LeaderboardManager leaderboardManager;
    private LangManager langManager;
    private RegionManager regionManager;
    private boolean debugMode = false;
    private BukkitRunnable autoStartTask;
    private int lastAutoStartMinute = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        langManager = new LangManager(this);
        leaderboardManager = new LeaderboardManager();
        regionManager = new RegionManager(this);
        eggManager = new EggManager(this, leaderboardManager);
        pickupManager = new PickupManager(this, eggManager, leaderboardManager);

        getServer().getPluginManager().registerEvents(new EggListener(eggManager, pickupManager), this);
        getServer().getPluginManager().registerEvents(new EggConversionListener(this, eggManager), this);
        getServer().getPluginManager().registerEvents(new WandListener(regionManager), this);

        loadComponents();

        eggManager.cleanupLingeringEggs();
        startAutoStartTask();
        getLogger().info("RustEaster plugin enabled!");
    }

    private void loadComponents() {
        this.debugMode = getConfig().getBoolean("debug-mode", false);

        String mainCommandName = getConfig().getString("event-settings.main-command", "easter");
        
        EasterCommand easterCommand = new EasterCommand(this, eggManager, leaderboardManager, langManager, regionManager, mainCommandName);
        
        if (getCommand(mainCommandName) != null) {
            getCommand(mainCommandName).setExecutor(easterCommand);
            getCommand(mainCommandName).setTabCompleter(easterCommand);
        } else {
            if (getCommand("easter") != null) {
                getCommand("easter").setExecutor(easterCommand);
                getCommand("easter").setTabCompleter(easterCommand);
            }
        }
    }

    @Override
    public void onDisable() {
        if (autoStartTask != null) {
            autoStartTask.cancel();
        }
        if (eggManager != null && eggManager.isEventActive()) {
            eggManager.stopEvent();
        }
        getLogger().info("RustEaster plugin disabled!");
    }

    public void reloadPlugin() {
        if (autoStartTask != null) {
            autoStartTask.cancel();
        }
        if (eggManager != null && eggManager.isEventActive()) {
            eggManager.stopEvent();
        }
        lastAutoStartMinute = -1;
        reloadConfig();
        langManager.reload(); 
        regionManager.load();
        loadComponents();
        startAutoStartTask();
        getLogger().info("Configuration and components reloaded!");
    }
    
    public LangManager getLangManager() { return langManager; }
    public RegionManager getRegionManager() { return regionManager; }

    private void startAutoStartTask() {
        if (!getConfig().getBoolean("auto-start.enabled", false)) return;

        long checkInterval = 200L; 

        autoStartTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAutoStart();
            }
        };
        autoStartTask.runTaskTimer(this, 0L, checkInterval);
    }

    private void checkAutoStart() {
        LocalTime now = LocalTime.now();
        int currentMinuteOfDay = now.getHour() * 60 + now.getMinute();

        if (currentMinuteOfDay == lastAutoStartMinute) return;

        List<String> schedule = getConfig().getStringList("auto-start.schedule");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (String timeStr : schedule) {
            try {
                LocalTime scheduleTime = LocalTime.parse(timeStr, formatter);
                if (now.getHour() == scheduleTime.getHour() && now.getMinute() == scheduleTime.getMinute()) {

                    lastAutoStartMinute = currentMinuteOfDay;

                    if (eggManager.isEventActive()) return;

                    int minPlayers = getConfig().getInt("auto-start.min-players", 1);
                    if (getServer().getOnlinePlayers().size() < minPlayers) return;

                    int duration = getConfig().getInt("auto-start.duration", 600);
                    String region = getConfig().getString("auto-start.region", null);
                    
                    getLogger().info("Auto-starting Easter event based on schedule: " + timeStr + (region != null ? " in region " + region : ""));
                    eggManager.startEvent(duration, region);
                    return;
                }
            } catch (Exception e) {
                getLogger().warning("Invalid time format in auto-start.schedule: " + timeStr);
            }
        }
    }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        getConfig().set("debug-mode", debugMode);
        saveConfig();
    }

    // Config Getters
    public List<String> getBlacklistedWorlds() { return getConfig().getStringList("blacklisted-worlds"); }
    public List<Integer> getEggCustomModelData() { return getConfig().getIntegerList("egg-visuals.custom-model-data-list"); }
    public Material getEggBaseMaterial() {
        String mat = getConfig().getString("egg-visuals.custom-model-base-material", "PAPER");
        return Material.getMaterial(mat) != null ? Material.getMaterial(mat) : Material.PAPER;
    }
    public String getEquipmentSlot() { return getConfig().getString("egg-visuals.equipment-slot", "HEAD").toUpperCase(); }
    public String getArmorStandSize() { return getConfig().getString("egg-visuals.armor-stand-size", "NORMAL").toUpperCase(); }

    public EulerAngle getHeadPose() {
        return new EulerAngle(Math.toRadians(getConfig().getDouble("egg-visuals.head-pose.x", 0.0)),
                Math.toRadians(getConfig().getDouble("egg-visuals.head-pose.y", 0.0)),
                Math.toRadians(getConfig().getDouble("egg-visuals.head-pose.z", 0.0)));
    }

    public EulerAngle getBodyPose() {
        return new EulerAngle(Math.toRadians(getConfig().getDouble("egg-visuals.body-pose.x", 0.0)),
                Math.toRadians(getConfig().getDouble("egg-visuals.body-pose.y", 0.0)),
                Math.toRadians(getConfig().getDouble("egg-visuals.body-pose.z", 0.0)));
    }

    public int getNormalPickupTime() { return getConfig().getInt("pickup-settings.normal-pickup-time", 40); }
    public double getSpecialItemMultiplier() { return getConfig().getDouble("pickup-settings.special-item-multiplier", 2.0); }
    public int getMinSpawnRadius() { return getConfig().getInt("spawning.radius.min", 10); }
    public int getMaxSpawnRadius() { return getConfig().getInt("spawning.radius.max", 25); }
    public double getSpawnChancePerPlayer() { return getConfig().getDouble("spawning.chance-per-player", 0.5); }
    public int getCycleIntervalSeconds() { return getConfig().getInt("spawning.cycle-interval-seconds", 5); }
    public int getEggLifespanSeconds() { return getConfig().getInt("spawning.egg-lifespan-seconds", 60); }
    public double getYOffset() { return getConfig().getDouble("spawning.y-offset", -1.7); }
    public int getDefaultDuration() { return getConfig().getInt("event-settings.default-duration", 600); }


    public Sound getSound(String key) {
        String name = getConfig().getString("sounds." + key);
        try { return name != null ? Sound.valueOf(name.toUpperCase()) : null; } catch (Exception e) { return null; }
    }

    public List<String> getRewards(String place) { return getConfig().getStringList("rewards." + place); }
    public boolean shouldGivePhysicalItem() {
        return getConfig().getBoolean("collected-egg-items.give-physical-items", true);
    }
    public ItemStack getSpecialPickupItem() {
        return getConfig().getItemStack("pickup-settings.special-pickup-item");
    }

    public void setSpecialPickupItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        getConfig().set("pickup-settings.special-pickup-item", item);
        String path = "pickup-settings.special-item-info";
        getConfig().set(path + ".material", item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) getConfig().set(path + ".name", meta.getDisplayName());
            if (meta.hasItemModel()) getConfig().set(path + ".item-model", meta.getItemModel().toString());
            else getConfig().set(path + ".item-model", null);
            if (meta.hasCustomModelData()) getConfig().set(path + ".custom-model-data", meta.getCustomModelData());
            if (meta.hasLore()) getConfig().set(path + ".lore", meta.getLore());
        }
        saveConfig();
    }

    public ItemStack getSpawnBoosterItem() {
        return getConfig().getItemStack("spawning.booster-item");
    }

    public void setSpawnBoosterItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        getConfig().set("spawning.booster-item", item);
        String path = "spawning.booster-item-info";
        getConfig().set(path + ".material", item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) getConfig().set(path + ".name", meta.getDisplayName());
            if (meta.hasItemModel()) getConfig().set(path + ".item-model", meta.getItemModel().toString());
            else getConfig().set(path + ".item-model", null);
            if (meta.hasCustomModelData()) getConfig().set(path + ".custom-model-data", meta.getCustomModelData());
            if (meta.hasLore()) getConfig().set(path + ".lore", meta.getLore());
        }
        saveConfig();
    }

    public double getSpawnBoosterMultiplier() {
        return getConfig().getDouble("spawning.booster-multiplier", 2.0);
    }

    public boolean isSpawnBoosterInventoryEnabled() {
        return getConfig().getBoolean("spawning.booster-works-in-inventory", false);
    }

    public ItemStack createCollectedEggItem(String tier) {
        String path = "collected-egg-items.tiers." + tier;
        if (!getConfig().contains(path)) return null;
        String matName = getConfig().getString("collected-egg-items.material", "PAPER");
        Material material = Material.getMaterial(matName.toUpperCase());
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', getConfig().getString(path + ".name", "&fEgg")));
            List<String> lore = getConfig().getStringList(path + ".lore");
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(coloredLore);
            }
            String itemModel = getConfig().getString(path + ".item-model", null);
            if (itemModel != null && !itemModel.isEmpty()) meta.setItemModel(NamespacedKey.fromString(itemModel));
            else if (getConfig().contains(path + ".custom-model-data")) {
                double cmdValue = ((Number) getConfig().get(path + ".custom-model-data", Number.class)).floatValue();
                meta.setCustomModelData((int) cmdValue);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createCollectedEggItem() {
        return createCollectedEggItem("default");
    }

}

// --- COMMAND CLASS ---

class EasterCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EggManager eggManager;
    private final LeaderboardManager leaderboardManager;
    private final LangManager lang;
    private final RegionManager regionManager;
    private final String commandName;

    public EasterCommand(Main plugin, EggManager eggManager, LeaderboardManager leaderboardManager, LangManager langManager, RegionManager regionManager, String commandName) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.leaderboardManager = leaderboardManager;
        this.lang = langManager;
        this.regionManager = regionManager;
        this.commandName = commandName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(lang.get("command-help", "{cmd}", commandName));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                int duration = plugin.getDefaultDuration();
                String startRegion = null;
                
                if (args.length > 1) {
                    try {
                        duration = Integer.parseInt(args[1]);
                        if (args.length > 2) startRegion = args[2];
                    } catch (NumberFormatException e) {
                        startRegion = args[1];
                        if (args.length > 2) {
                            try { duration = Integer.parseInt(args[2]); } catch (Exception ignored) {}
                        }
                    }
                }
                
                eggManager.startEvent(duration, startRegion);
                sender.sendMessage(lang.get("event-started", "{time}", String.valueOf(duration)) + (startRegion != null ? " in region §f" + startRegion : ""));
                return true;

            case "stop":
                eggManager.stopEvent();
                sender.sendMessage(lang.get("event-stopped"));
                return true;

            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(lang.get("plugin-reloaded"));
                return true;

            case "give":
                if (args.length < 2) {
                    sender.sendMessage(lang.get("give-usage", "{cmd}", commandName));
                    return true;
                }
                String tier = args[1].toLowerCase();
                int amount = 1;
                Player target = null;
                if (args.length > 2) {
                    boolean isAmount = false;
                    try {
                        amount = Integer.parseInt(args[2]);
                        isAmount = true;
                    } catch (NumberFormatException ignored) {}
                    if (isAmount) {
                        if (args.length > 3) {
                            target = Bukkit.getPlayer(args[3]);
                        }
                    } else {
                        target = Bukkit.getPlayer(args[2]);
                    }
                }
                if (target == null) {
                    if (sender instanceof Player) target = (Player) sender;
                    else { sender.sendMessage("§cSpecify a player."); return true; }
                }
                ItemStack egg = plugin.createCollectedEggItem(tier);
                if (egg == null) { sender.sendMessage("§cInvalid tier."); return true; }
                egg.setAmount(amount);
                target.getInventory().addItem(egg);
                sender.sendMessage("§aGave " + amount + " " + tier + " eggs to " + target.getName());
                return true;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /" + commandName + " set <autostart|region> <value>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("autostart")) {
                    boolean val = Boolean.parseBoolean(args[2]);
                    plugin.getConfig().set("auto-start.enabled", val);
                    plugin.saveConfig();
                    sender.sendMessage("§aAuto-start set to: §f" + val);
                } else if (args[1].equalsIgnoreCase("region")) {
                    String reg = args[2].equalsIgnoreCase("none") ? null : args[2];
                    plugin.getConfig().set("auto-start.region", reg);
                    plugin.saveConfig();
                    sender.sendMessage("§aAuto-start region set to: §f" + (reg == null ? "Global" : reg));
                }
                return true;

            case "wand":
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                ItemStack wand = new ItemStack(Material.STICK);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName("§dEaster Wand");
                List<String> lore = new ArrayList<>();
                lore.add("§7Left-click block for Pos 1");
                lore.add("§7Right-click block for Pos 2");
                meta.setLore(lore);
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                p.sendMessage("§dReceived Easter Wand!");
                return true;

            case "region":
                if (!(sender instanceof Player)) return true;
                Player rp = (Player) sender;
                if (args.length < 2) {
                    rp.sendMessage("§c/easter region create <name> / delete <name>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("create") && args.length > 2) {
                    if (!regionManager.hasSelection(rp)) {
                        rp.sendMessage("§cMake a selection first!");
                        return true;
                    }
                    regionManager.createRegion(rp, args[2]);
                } else if (args[1].equalsIgnoreCase("delete") && args.length > 2) {
                    regionManager.deleteRegion(args[2]);
                    rp.sendMessage("§aRegion deleted.");
                }
                return true;

            default:
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String[] subs = {"start", "stop", "debug", "top", "reload", "give", "set", "wand", "region"};
            for (String sub : subs) if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("region")) {
                for (RegionManager.EasterRegion r : regionManager.getRegions()) {
                    if (r.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(r.getName());
                }
            } else if (args[0].equalsIgnoreCase("set")) {
                completions.add("autostart"); completions.add("region");
            }
        } else if (args.length == 3) {
             if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("autostart")) {
                 completions.add("true"); completions.add("false");
             } else if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("region")) {
                 completions.add("none");
                 for (RegionManager.EasterRegion r : regionManager.getRegions()) {
                     if (r.getName().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(r.getName());
                 }
             }
        }
        return completions;
    }
}
