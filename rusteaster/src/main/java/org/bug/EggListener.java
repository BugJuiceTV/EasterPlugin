package org.bug;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class EggListener implements Listener {

    private final EggManager eggManager;
    private final PickupManager pickupManager;

    public EggListener(EggManager eggManager, PickupManager pickupManager) {
        this.eggManager = eggManager;
        this.pickupManager = pickupManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;

        ArmorStand armorStand = (ArmorStand) event.getRightClicked();
        
        // If it's an egg, we ALWAYS cancel the interaction to prevent armor stand theft
        if (!eggManager.isEgg(armorStand)) return;
        
        event.setCancelled(true);

        // If the event isn't active, remove bugged/lingering eggs on contact
        if (!eggManager.isEventActive()) {
            armorStand.remove();
            return;
        }

        Egg egg = eggManager.getEggByEntity(armorStand);
        if (egg == null) {
            // Egg tag exists but not in the active tracking list (lingering from previous session)
            armorStand.remove();
            return;
        }

        Player player = event.getPlayer();
        pickupManager.startPickup(player, egg);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEggDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) return;
        
        if (eggManager.isEgg(event.getEntity())) {
            event.setCancelled(true);
            
            // Optional: If someone punches a bugged egg when event is off, clean it up
            if (!eggManager.isEventActive()) {
                event.getEntity().remove();
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // When a chunk loads, if the event is NOT active, scan it for lingering eggs and delete them
        if (!eggManager.isEventActive()) {
            for (Entity entity : event.getChunk().getEntities()) {
                if (eggManager.isEgg(entity)) {
                    entity.remove();
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (eggManager.isEventActive()) {
            eggManager.addPlayerToBossBar(event.getPlayer());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        eggManager.removePlayerFromBossBar(event.getPlayer());
        pickupManager.cancelPickup(event.getPlayer());
    }
}
