package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;

public class PetFeedListener implements Listener {

    private final MoonCake plugin;
    private final NamespacedKey fedKey;
    private final DecimalFormat df = new DecimalFormat("#.#");

    public PetFeedListener(MoonCake plugin) {
        this.plugin = plugin;
        this.fedKey = new NamespacedKey(plugin, "mooncake_fed");
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        Entity entity = event.getRightClicked();

        if (!(entity instanceof Tameable)) return;
        EntityType type = entity.getType();
        if (!(type == EntityType.WOLF || type == EntityType.CAT ||
                type == EntityType.PARROT || type == EntityType.HORSE ||
                type == EntityType.DONKEY || type == EntityType.MULE ||
                type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isDragonMooncake(item)) {
            item = player.getInventory().getItemInOffHand();
            if (!isDragonMooncake(item)) {
                return;
            }
        }

        Tameable tameable = (Tameable) entity;
        if (!tameable.isTamed() || tameable.getOwner() == null) {
            event.setCancelled(true);
            return;
        }

        if (!tameable.getOwner().getUniqueId().equals(player.getUniqueId())) {
            player.sendActionBar(Component.text("你不是该宠物的主人！", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        if (entity.getPersistentDataContainer().has(fedKey, PersistentDataType.BOOLEAN)) {
            player.sendActionBar(Component.text("该宠物已经吃过龙骨月饼！", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        double maxHealth = ((LivingEntity) entity).getMaxHealth();
        double bonus = 6.0; // 3颗心
        double newMax = maxHealth + bonus;

        ((LivingEntity) entity).setMaxHealth(newMax);
        ((LivingEntity) entity).setHealth(newMax);
        ((LivingEntity) entity).addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION, 40 * 20, 2, false, false, true));

        entity.getPersistentDataContainer().set(fedKey, PersistentDataType.BOOLEAN, true);

        Component nameComp = entity.customName() != null ?
                entity.customName() : Component.text(entity.getName());
        Component msg = Component.text("你的", NamedTextColor.GOLD)
                .append(nameComp)
                .append(Component.text("最大生命值已从 ", NamedTextColor.GOLD))
                .append(Component.text(df.format(maxHealth) + "❤", NamedTextColor.DARK_RED))
                .append(Component.text(" ⇨ ", NamedTextColor.GOLD))
                .append(Component.text(df.format(newMax) + "❤", NamedTextColor.DARK_RED));
        player.sendActionBar(msg);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        if (isDragonMooncake(player.getInventory().getItemInMainHand())) {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (main.getAmount() > 1) main.setAmount(main.getAmount() - 1);
            else player.getInventory().setItemInMainHand(null);
        } else {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off.getAmount() > 1) off.setAmount(off.getAmount() - 1);
            else player.getInventory().setItemInOffHand(null);
        }

        event.setCancelled(true);
    }

    private boolean isDragonMooncake(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.keyMooncake, PersistentDataType.BOOLEAN)) {
            return false;
        }
        String type = meta.getPersistentDataContainer().get(plugin.keyType, PersistentDataType.STRING);
        return "DRAGON".equals(type);
    }
}