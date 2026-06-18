package org.bug;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final Map<UUID, Integer> scores = new HashMap<>();

    public void addScore(Player player) {
        scores.put(player.getUniqueId(), scores.getOrDefault(player.getUniqueId(), 0) + 1);
    }

    public int getScore(Player player) {
        return scores.getOrDefault(player.getUniqueId(), 0);
    }

    public void clearScores() {
        scores.clear();
    }

    public Map<String, Integer> getTopPlayers(int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        entry -> {
                            Player p = Bukkit.getPlayer(entry.getKey());
                            return (p != null) ? p.getName() : Bukkit.getOfflinePlayer(entry.getKey()).getName();
                        },
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public void showLeaderboard(CommandSender sender) {
    }
}
