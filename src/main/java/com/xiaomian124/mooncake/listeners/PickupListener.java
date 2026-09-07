package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PickupListener implements Listener {

    private final MoonCake plugin;

    public PickupListener(MoonCake plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        ItemStack item = event.getItem().getItemStack();
        if (item == null || !item.hasItemMeta()) return;

        if (!event.getItem().getPersistentDataContainer().has(plugin.keyDrop, PersistentDataType.BOOLEAN)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.keyMooncake, PersistentDataType.BOOLEAN)) {
            return;
        }

        String type = meta.getPersistentDataContainer().get(plugin.keyType, PersistentDataType.STRING);
        if (type == null) return;
        if (!type.equals("DRAGON") && !type.equals("WITHER")) return;

        String displayName;
        NamedTextColor nameColor;
        ChatColor chatColor;
        if (type.equals("DRAGON")) {
            displayName = "龙骨月饼";
            nameColor = NamedTextColor.GOLD;
            chatColor = ChatColor.GOLD;
        } else {
            displayName = "凋零月饼";
            nameColor = NamedTextColor.DARK_PURPLE;
            chatColor = ChatColor.DARK_PURPLE;
        }

        String broadcast = ChatColor.GOLD + "玩家 " + player.getName() +
                " 捡到了极品" + chatColor + displayName + ChatColor.GOLD + "！";
        Bukkit.broadcastMessage(broadcast);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
        }

        final String baseMsg = "你捡到了极品" + displayName;
        final Player target = player;
        final int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int count = 0;
            private final int maxCount = 16; // 0.5s x 16 = 8
            private boolean gold = true;

            @Override
            public void run() {
                if (count >= maxCount) {
                    Bukkit.getScheduler().cancelTask(taskId[0]);
                    return;
                }
                NamedTextColor color = gold ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
                target.sendActionBar(Component.text(baseMsg, color));
                gold = !gold;
                count++;
            }
        }, 0L, 10L);
    }
}