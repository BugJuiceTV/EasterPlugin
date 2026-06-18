package org.bug;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class WandListener implements Listener {

    private final RegionManager regionManager;

    public WandListener(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.getDisplayName().equals("§dEaster Wand")) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            regionManager.setPos1(event.getPlayer(), event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage("§aPosition 1 set!");
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            regionManager.setPos2(event.getPlayer(), event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage("§aPosition 2 set!");
        }
    }
}
