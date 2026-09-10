package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FeedPlayerListener implements Listener {

    private final MoonCake plugin;
    private static final int DURATION_TICKS = 30 * 60 * 20; // 30m

    public FeedPlayerListener(MoonCake plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        Player feeder = event.getPlayer();

        if (!feeder.isSneaking()) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        Player receiver = (Player) event.getRightClicked();
        if (feeder.equals(receiver)) return;

        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        ItemStack item = (hand == EquipmentSlot.HAND) ?
                feeder.getInventory().getItemInMainHand() :
                feeder.getInventory().getItemInOffHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.keyMooncake, PersistentDataType.BOOLEAN)) return;

        String type = meta.getPersistentDataContainer().get(plugin.keyType, PersistentDataType.STRING);
        if (type == null) return;

        if (receiver.getFoodLevel() >= 20) {
            feeder.sendActionBar(Component.text(receiver.getName() + " 已经饱了，无法投喂！", NamedTextColor.RED));
            feeder.playSound(feeder.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        String mooncakeName;
        NamedTextColor mooncakeColor;
        switch (type) {
            case "DRAGON":
                mooncakeName = "龙骨月饼";
                mooncakeColor = NamedTextColor.GOLD;
                break;
            case "WITHER":
                mooncakeName = "凋零月饼";
                mooncakeColor = NamedTextColor.DARK_PURPLE;
                break;
            default:
                mooncakeName = "月饼";
                mooncakeColor = NamedTextColor.YELLOW;
                break;
        }

        applyMooncakeEffects(receiver, type);

        feeder.sendActionBar(Component.text("你投喂了一个", NamedTextColor.GOLD)
                .append(Component.text(mooncakeName, mooncakeColor))
                .append(Component.text("给 ", NamedTextColor.GOLD))
                .append(Component.text(receiver.getName(), NamedTextColor.GOLD)));

        receiver.sendActionBar(Component.text("你被 ", NamedTextColor.GOLD)
                .append(Component.text(feeder.getName(), NamedTextColor.GOLD))
                .append(Component.text(" 投喂了一个", NamedTextColor.GOLD))
                .append(Component.text(mooncakeName, mooncakeColor)));

        feeder.playSound(feeder.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        receiver.playSound(receiver.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        if (hand == EquipmentSlot.HAND) {
            ItemStack main = feeder.getInventory().getItemInMainHand();
            if (main.getAmount() > 1) {
                main.setAmount(main.getAmount() - 1);
            } else {
                feeder.getInventory().setItemInMainHand(null);
            }
        } else {
            ItemStack off = feeder.getInventory().getItemInOffHand();
            if (off.getAmount() > 1) {
                off.setAmount(off.getAmount() - 1);
            } else {
                feeder.getInventory().setItemInOffHand(null);
            }
        }
    }

    private void applyMooncakeEffects(Player player, String type) {
        int nutrition;
        float saturation;
        switch (type) {
            case "DRAGON":
            case "WITHER":
                nutrition = 8;
                saturation = 17;
                break;
            default:
                nutrition = 4;
                saturation = 10;
                break;
        }
        int newFood = Math.min(20, player.getFoodLevel() + nutrition);
        player.setFoodLevel(newFood);
        float newSaturation = Math.min(player.getSaturation() + saturation, newFood);
        player.setSaturation(newSaturation);

        if (type.equals("DRAGON") || type.equals("WITHER")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, DURATION_TICKS, 4, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, DURATION_TICKS, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, DURATION_TICKS, 1, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, DURATION_TICKS, 1, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, DURATION_TICKS, 1, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, DURATION_TICKS, 1, false, false, true));
            if (type.equals("WITHER")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, DURATION_TICKS, 3, false, false, true));
            }
            player.setHealth(player.getMaxHealth());
        }
    }
}