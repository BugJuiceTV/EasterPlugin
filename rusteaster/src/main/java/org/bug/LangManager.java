package org.bug;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LangManager {
    private final Main plugin;
    private FileConfiguration langConfig;
    private File langFile;

    public LangManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (langFile == null) {
            langFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!langFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load defaults
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
        }
    }

    public String get(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String get(String path, String... placeholders) {
        String message = get(path);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }
}