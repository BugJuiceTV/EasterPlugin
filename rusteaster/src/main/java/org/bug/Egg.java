package org.bug;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class Egg {

    private final Location location;
    private final ArmorStand armorStand;

    public Egg(Location location, ArmorStand armorStand) {
        this.location = location;
        this.armorStand = armorStand;
    }

    public Location getLocation() {
        return location;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }
}
