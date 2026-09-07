package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlaceCancelListener implements Listener {

    private final MoonCake plugin;
    private final NamespacedKey key;

    public PlaceCancelListener(MoonCake plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mooncake");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) {
            event.setCancelled(true);
        }
    }
}