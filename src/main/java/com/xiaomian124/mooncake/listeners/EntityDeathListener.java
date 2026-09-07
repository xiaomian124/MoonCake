package com.xiaomian124.mooncake.listeners;

import com.xiaomian124.mooncake.MoonCake;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class EntityDeathListener implements Listener {

    private final MoonCake plugin;
    private final Random random = new Random();

    public EntityDeathListener(MoonCake plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        EntityType type = entity.getType();
        Player killer = entity.getKiller();

        if (!plugin.isEventActive()) {
            return;
        }

        if (type == EntityType.ENDER_DRAGON) {
            handleSpecialDrop(entity, "DRAGON_EGG_MOONCAKE");
            return;
        } else if (type == EntityType.WITHER) {
            handleSpecialDrop(entity, "WITHER_MOONCAKE");
            return;
        }

        if (killer == null) {
            return;
        }

        String filling = getFillingByEntityType(type);
        if (filling == null) {
            return;
        }

        double chance = plugin.getConfig().getDouble("passive-mob-drop-chance", 0.2);
        if (random.nextDouble() < chance) {
            ItemStack mooncake = plugin.createMooncake("NORMAL_MOONCAKE", filling, 1);
            spawnMooncake(entity, mooncake);
        }
    }

    private void handleSpecialDrop(LivingEntity entity, String type) {
        ItemStack mooncake = plugin.createMooncake(type, null, 1);
        if (mooncake == null) {
            return;
        }

        Location dropLoc;
        if (entity.getType() == EntityType.ENDER_DRAGON) {
            dropLoc = new Location(entity.getWorld(), 0, 64.5, 0);
            dropLoc.add((random.nextDouble() - 0.5) * 3, 0, (random.nextDouble() - 0.5) * 3);
            spawnMooncakeAt(entity, mooncake, dropLoc, true); // 发光
        } else if (entity.getType() == EntityType.WITHER) {
            Location loc = entity.getLocation();
            double x = loc.getX() + (random.nextDouble() - 0.5) * 1.0;
            double y = loc.getY() + 0.5 + random.nextDouble() * 0.5;
            double z = loc.getZ() + (random.nextDouble() - 0.5) * 1.0;
            dropLoc = new Location(entity.getWorld(), x, y, z);
            spawnMooncakeAt(entity, mooncake, dropLoc, true); // 发光
        } else {
            spawnMooncake(entity, mooncake);
        }
    }

    private void spawnMooncake(LivingEntity entity, ItemStack mooncake) {
        if (mooncake == null) {
            return;
        }
        Location loc = entity.getLocation();
        World world = entity.getWorld();
        double x = loc.getX() + (random.nextDouble() - 0.5) * 1.0;
        double y = loc.getY() + 0.5 + random.nextDouble() * 0.5;
        double z = loc.getZ() + (random.nextDouble() - 0.5) * 1.0;
        Location dropLoc = new Location(world, x, y, z);
        spawnMooncakeAt(entity, mooncake, dropLoc, false); // 不发光
    }

    private void spawnMooncakeAt(LivingEntity entity, ItemStack mooncake, Location dropLoc, boolean glowing) {
        if (mooncake == null) {
            return;
        }
        World world = dropLoc.getWorld();
        if (world == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Item itemEntity = world.dropItem(dropLoc, mooncake);
            itemEntity.getPersistentDataContainer().set(plugin.keyDrop, PersistentDataType.BOOLEAN, true);

            if (glowing) {
                itemEntity.setGlowing(true);
            }
        }, 2L);
    }

    private String getFillingByEntityType(EntityType type) {
        // 怪物（五仁）
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER ||
                type == EntityType.SKELETON || type == EntityType.STRAY ||
                type == EntityType.CREEPER ||
                type == EntityType.ZOMBIE || type == EntityType.DROWNED ||
                type == EntityType.SLIME || type == EntityType.MAGMA_CUBE ||
                type == EntityType.PIGLIN || type == EntityType.BLAZE ||
                type == EntityType.ENDERMAN || type == EntityType.SHULKER) {
            return "WUREN";
        }
        // 动物（莲蓉）
        if (type == EntityType.PIG || type == EntityType.COW ||
                type == EntityType.SHEEP || type == EntityType.CHICKEN ||
                type == EntityType.HORSE || type == EntityType.MULE ||
                type == EntityType.LLAMA || type == EntityType.GOAT ||
                type == EntityType.RABBIT || type == EntityType.FOX ||
                type == EntityType.PANDA || type == EntityType.BEE) {
            return "LIANRONG";
        }
        // 水生物（水果）
        if (type == EntityType.TURTLE || type == EntityType.COD ||
                type == EntityType.SALMON || type == EntityType.PUFFERFISH ||
                type == EntityType.TROPICAL_FISH || type == EntityType.SQUID ||
                type == EntityType.GLOW_SQUID || type == EntityType.DOLPHIN) {
            return "FRUIT";
        }
        // 玩家（豆沙）
        if (type == EntityType.PLAYER) {
            return "DOUSHA";
        }
        // 蠹虫类（蛋黄）
        if (type == EntityType.SILVERFISH || type == EntityType.ENDERMITE) {
            return "DANHUANG";
        }
        return null;
    }
}