package org.bug;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class EggConversionListener implements Listener {

    private final Main plugin;
    private final EggManager eggManager;

    public EggConversionListener(Main plugin, EggManager eggManager) {
        this.plugin = plugin;
        this.eggManager = eggManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEggInteract(PlayerInteractEvent event) {
        // 1. Strict hand check to prevent double-firing from OFF_HAND
        if (event.getHand() != EquipmentSlot.HAND) return;

        // 2. Only handle Right Clicks
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        // 3. Check if the item is one of our eggs
        String tier = eggManager.getEggTier(item);
        if (tier == null) return;

        // Cancel the event immediately to prevent block placement
        event.setCancelled(true);
        Player player = event.getPlayer();

        // --- BRANCH A: UPGRADE (Shift + Right Click) ---
        if (player.isSneaking()) {
            String nextTier = null;
            switch (tier.toLowerCase()) {
                case "default": nextTier = "bronze"; break;
                case "bronze":  nextTier = "silver"; break;
                case "silver":  nextTier = "gold";   break;
            }

            if (nextTier == null) {
                player.sendMessage("§eThis is the maximum tier!");
                return; // Stop here
            }

            if (item.getAmount() < 10) {
                player.sendMessage("§cYou need 10 eggs to upgrade!");
                return; // Stop here
            }

            // Deduct 10 eggs
            item.setAmount(item.getAmount() - 10);

            // Give the new egg
            ItemStack reward = eggManager.createCollectedEggItem(nextTier);
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(reward);

            if (!leftOver.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftOver.get(0));
                player.sendMessage("§cInventory full! Dropped your " + nextTier + " egg on the ground.");
            } else {
                player.sendMessage("§a§lUPGRADE! §7Converted 10 eggs into 1 §f" + nextTier + " §7egg!");
            }

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
            return; // Exit the method after successful upgrade
        }

        // --- BRANCH B: UNBOXING (Normal Right Click) ---
        else {
            if (tier.equalsIgnoreCase("default")) {
                player.sendMessage("§eShift-Right-Click §7to upgrade 10 of these!");
                return; // Exit the method after sending the hint
            }

            // Deduct 1 egg
            item.setAmount(item.getAmount() - 1);

            // Run the unboxing method from EggManager
            eggManager.unboxEgg(player, tier);
        }
    }
}