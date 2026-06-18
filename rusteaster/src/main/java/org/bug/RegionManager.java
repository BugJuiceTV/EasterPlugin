package org.bug;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration config;
    
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final List<EasterRegion> regions = new ArrayList<>();

    public RegionManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
        load();
    }

    public void setPos1(Player player, Location loc) {
        pos1.put(player.getUniqueId(), loc);
    }

    public void setPos2(Player player, Location loc) {
        pos2.put(player.getUniqueId(), loc);
    }

    public boolean hasSelection(Player player) {
        return pos1.containsKey(player.getUniqueId()) && pos2.containsKey(player.getUniqueId());
    }

    public void createRegion(Player player, String name) {
        Location l1 = pos1.get(player.getUniqueId());
        Location l2 = pos2.get(player.getUniqueId());
        
        if (l1 == null || l2 == null || !l1.getWorld().equals(l2.getWorld())) {
            player.sendMessage("§cInvalid selection!");
            return;
        }

        EasterRegion region = new EasterRegion(name, l1, l2);
        regions.add(region);
        save();
        player.sendMessage("§aRegion '" + name + "' created!");
    }

    public void deleteRegion(String name) {
        regions.removeIf(r -> r.getName().equalsIgnoreCase(name));
        save();
    }

    public List<EasterRegion> getRegions() {
        return regions;
    }

    public void load() {
        if (!file.exists()) return;
        config = YamlConfiguration.loadConfiguration(file);
        regions.clear();
        
        if (config.getConfigurationSection("regions") == null) return;
        
        for (String key : config.getConfigurationSection("regions").getKeys(false)) {
            String path = "regions." + key;
            World world = Bukkit.getWorld(config.getString(path + ".world", ""));
            if (world == null) continue;

            Location min = new Location(world, config.getDouble(path + ".min.x"), config.getDouble(path + ".min.y"), config.getDouble(path + ".min.z"));
            Location max = new Location(world, config.getDouble(path + ".max.x"), config.getDouble(path + ".max.y"), config.getDouble(path + ".max.z"));
            
            regions.add(new EasterRegion(key, min, max));
        }
    }

    public void save() {
        config = new YamlConfiguration();
        for (EasterRegion r : regions) {
            String path = "regions." + r.getName();
            config.set(path + ".world", r.getWorld().getName());
            config.set(path + ".min.x", r.getMin().getX());
            config.set(path + ".min.y", r.getMin().getY());
            config.set(path + ".min.z", r.getMin().getZ());
            config.set(path + ".max.x", r.getMax().getX());
            config.set(path + ".max.y", r.getMax().getY());
            config.set(path + ".max.z", r.getMax().getZ());
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public static class EasterRegion {
        private final String name;
        private final Location min;
        private final Location max;
        private final World world;

        public EasterRegion(String name, Location l1, Location l2) {
            this.name = name;
            this.world = l1.getWorld();
            this.min = new Location(world, Math.min(l1.getX(), l2.getX()), Math.min(l1.getY(), l2.getY()), Math.min(l1.getZ(), l2.getZ()));
            this.max = new Location(world, Math.max(l1.getX(), l2.getX()), Math.max(l1.getY(), l2.getY()), Math.max(l1.getZ(), l2.getZ()));
        }

        public String getName() { return name; }
        public Location getMin() { return min; }
        public Location getMax() { return max; }
        public World getWorld() { return world; }

        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(world)) return false;
            return loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
                   loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
                   loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ();
        }
        
        public Location getRandomLocation() {
            Random r = new Random();
            double x = min.getX() + (max.getX() - min.getX()) * r.nextDouble();
            double y = min.getY() + (max.getY() - min.getY()) * r.nextDouble();
            double z = min.getZ() + (max.getZ() - min.getZ()) * r.nextDouble();
            return new Location(world, x, y, z);
        }
    }
}
