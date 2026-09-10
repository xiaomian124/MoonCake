package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerInteractListener implements Listener {

    private final MoonCake plugin;
    private static final int DURATION_TICKS = 30 * 60 * 20; // 30m
    private static final int COOLDOWN_TICKS = 5 * 20; // 5s

    public PlayerInteractListener(MoonCake plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (player.isSneaking()) return;
        if (item == null || !item.hasItemMeta()) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.keyMooncake, PersistentDataType.BOOLEAN)) {
            return;
        }

        if (player.getCooldown(Material.PLAYER_HEAD) > 0) {
            event.setCancelled(true);
            return;
        }

        if (player.getFoodLevel() >= 20) {
            player.sendActionBar(Component.text("你现在吃不下月饼！", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Integer nutrition = meta.getPersistentDataContainer().get(plugin.keyNutrition, PersistentDataType.INTEGER);
        Float saturation = meta.getPersistentDataContainer().get(plugin.keySaturation, PersistentDataType.FLOAT);
        if (nutrition == null || saturation == null) {
            return;
        }

        String type = meta.getPersistentDataContainer().get(plugin.keyType, PersistentDataType.STRING);
        if (type == null) type = "NORMAL";

        NamedTextColor actionBarColor;
        Color fireworkColor;
        switch (type) {
            case "DRAGON":
                actionBarColor = NamedTextColor.GOLD;
                fireworkColor = Color.fromRGB(0xFFD700);
                break;
            case "WITHER":
                actionBarColor = NamedTextColor.DARK_PURPLE;
                fireworkColor = Color.fromRGB(0x800080);
                break;
            default:
                actionBarColor = NamedTextColor.YELLOW;
                fireworkColor = Color.fromRGB(0xFFFF00);
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

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendActionBar(Component.text("中秋快乐！", actionBarColor));
        spawnFirework(player, fireworkColor);
        player.setCooldown(Material.PLAYER_HEAD, COOLDOWN_TICKS);
        EquipmentSlot hand = event.getHand();
        if (hand == null) hand = EquipmentSlot.HAND;

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(item);
            } else {
                player.getInventory().setItemInOffHand(item);
            }
        } else {
            if (hand == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    private void spawnFirework(Player player, Color color) {
        try {
            Location loc = player.getLocation().add(0, 5.0, 0);
            Firework firework = player.getWorld().spawn(loc, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(color)
                    .with(FireworkEffect.Type.BALL)
                    .withFlicker()
                    .withTrail()
                    .build();
            meta.addEffect(effect);
            meta.setPower(1);
            firework.setFireworkMeta(meta);
            firework.detonate();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}