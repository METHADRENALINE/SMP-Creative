package net.methadrenaline.smpcreative.maaura.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record AuraMenuHolder(AuraMenuType type) implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null;
    }
}
