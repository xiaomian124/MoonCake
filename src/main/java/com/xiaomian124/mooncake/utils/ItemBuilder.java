package com.xiaomian124.mooncake.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private static final UUID FIXED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * 创建月饼头颅（无物品发光效果）
     * @param plugin 插件实例
     * @param type 月饼类型：DRAGON, WITHER, NORMAL
     * @param displayName 显示名称
     * @param color 名称颜色
     * @param textureBase64 纹理
     * @param lore Lore 列表
     * @param nutrition 饥饿值
     * @param saturation 饱和度
     * @return 构建好的 ItemStack
     */
    public static ItemStack buildCustomHead(Plugin plugin, String type, String displayName, NamedTextColor color,
                                            String textureBase64, List<Component> lore,
                                            int nutrition, float saturation) {
        try {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta == null) {
                return item;
            }

            meta.displayName(Component.text(displayName)
                    .color(color)
                    .decoration(TextDecoration.ITALIC, false));

            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }

            PlayerProfile profile = Bukkit.createProfile(FIXED_UUID, null);
            if (profile == null) {
                return item;
            }
            profile.setProperty(new ProfileProperty("textures", textureBase64));
            meta.setPlayerProfile(profile);

            NamespacedKey keyType = new NamespacedKey(plugin, "mooncake_type");
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);

            NamespacedKey keyMooncake = new NamespacedKey(plugin, "mooncake");
            meta.getPersistentDataContainer().set(keyMooncake, PersistentDataType.BOOLEAN, true);

            NamespacedKey keyNutrition = new NamespacedKey(plugin, "nutrition");
            meta.getPersistentDataContainer().set(keyNutrition, PersistentDataType.INTEGER, nutrition);

            NamespacedKey keySaturation = new NamespacedKey(plugin, "saturation");
            meta.getPersistentDataContainer().set(keySaturation, PersistentDataType.FLOAT, saturation);

            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            // 备用物品
            ItemStack fallback = new ItemStack(Material.COOKIE, 1);
            fallback.editMeta(m -> {
                m.displayName(Component.text(displayName).color(color).decoration(TextDecoration.ITALIC, false));
                if (lore != null && !lore.isEmpty()) {
                    m.lore(lore);
                }
                NamespacedKey keyType = new NamespacedKey(plugin, "mooncake_type");
                m.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);
                NamespacedKey keyMooncake = new NamespacedKey(plugin, "mooncake");
                m.getPersistentDataContainer().set(keyMooncake, PersistentDataType.BOOLEAN, true);
                NamespacedKey keyNutrition = new NamespacedKey(plugin, "nutrition");
                m.getPersistentDataContainer().set(keyNutrition, PersistentDataType.INTEGER, nutrition);
                NamespacedKey keySaturation = new NamespacedKey(plugin, "saturation");
                m.getPersistentDataContainer().set(keySaturation, PersistentDataType.FLOAT, saturation);
            });
            return fallback;
        }
    }
}