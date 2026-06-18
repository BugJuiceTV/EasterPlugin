package org.bug;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PickupManager {

    private final Main plugin;
    private final EggManager eggManager;
    private final LeaderboardManager leaderboardManager;
    private final Map<UUID, BukkitRunnable> activePickups = new HashMap<>();

    public PickupManager(Main plugin, EggManager eggManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.eggManager = eggManager;
        this.leaderboardManager = leaderboardManager;
    }

    public void startPickup(Player player, Egg egg) {
        if (isPickingUp(player.getUniqueId())) {
            return; // Already picking up
        }

        int totalTicks = plugin.getNormalPickupTime();
        ItemStack specialItem = plugin.getSpecialPickupItem();
        if (specialItem != null && player.getInventory().getItemInMainHand().isSimilar(specialItem)) {
            totalTicks /= plugin.getSpecialItemMultiplier();
        }

        final int finalTotalTicks = totalTicks;
        BukkitRunnable task = new BukkitRunnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.getLocation().distance(egg.getLocation()) > 3) {
                    cancelPickup(player);
                    return;
                }

                ticksPassed++;
                double progress = (double) ticksPassed / finalTotalTicks;
                sendProgressBar(player, progress);

                if (ticksPassed >= finalTotalTicks) {
                    eggManager.removeEgg(egg);
                    leaderboardManager.addScore(player);
                    
                    Sound pickupSound = plugin.getSound("egg-pickup");
                    if (pickupSound != null) {
                        player.playSound(player.getLocation(), pickupSound, 1.0f, 1.0f);
                    }
                    
                    if (plugin.shouldGivePhysicalItem()) {
                        ItemStack item = plugin.createCollectedEggItem();
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                    
                    player.sendMessage(plugin.getLangManager().get("pickup-message", "{score}", String.valueOf(leaderboardManager.getScore(player))));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("")); // Clear bar
                    cancel();
                    activePickups.remove(player.getUniqueId());
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 1L);
        activePickups.put(player.getUniqueId(), task);
    }

    public void cancelPickup(Player player) {
        if (isPickingUp(player.getUniqueId())) {
            activePickups.get(player.getUniqueId()).cancel();
            activePickups.remove(player.getUniqueId());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getLangManager().get("pickup-cancelled")));
        }
    }

    public boolean isPickingUp(UUID uuid) {
        return activePickups.containsKey(uuid);
    }

    private void sendProgressBar(Player player, double progress) {
        int barLength = 20;
        int filledLength = (int) (barLength * progress);
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("▌");
            } else {
                bar.append("§7▌");
            }
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }
}
