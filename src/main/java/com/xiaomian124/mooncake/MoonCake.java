package com.xiaomian124.mooncake;

import com.xiaomian124.mooncake.listeners.EntityDeathListener;
import com.xiaomian124.mooncake.listeners.PickupListener;
import com.xiaomian124.mooncake.listeners.PlaceCancelListener;
import com.xiaomian124.mooncake.listeners.PlayerInteractListener;
import com.xiaomian124.mooncake.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public final class MoonCake extends JavaPlugin implements CommandExecutor, TabCompleter {

    private double passiveDropChance;
    private String eventTimeStr;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isEventActive = false;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    private int startTaskId = -1;
    private int endTaskId = -1;
    private int startCountdownTaskId = -1;
    private int endCountdownTaskId = -1;

    public final NamespacedKey keyMooncake = new NamespacedKey(this, "mooncake");
    public final NamespacedKey keyType = new NamespacedKey(this, "mooncake_type");
    public final NamespacedKey keyDrop = new NamespacedKey(this, "mooncake_drop");
    public final NamespacedKey keyNutrition = new NamespacedKey(this, "nutrition");
    public final NamespacedKey keySaturation = new NamespacedKey(this, "saturation");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.M.d.HH:mm");
    private static final String MOON_CAKE_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjQ4NGM0MzVhYTBkY2E4MmMyOWViZjRhNTM1NGMzMjk0ODA2MWE3NmM3ZWNiMDUzNWY0ZTliYTI0MDNkMGMzNiJ9fX0=";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlaceCancelListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PickupListener(this), this);

        registerRecipes();

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerJoin(PlayerJoinEvent event) {
                for (NamespacedKey key : recipeKeys) {
                    event.getPlayer().discoverRecipe(key);
                }
            }
        }, this);

        Objects.requireNonNull(getCommand("mooncake")).setExecutor(this);
        Objects.requireNonNull(getCommand("mooncake")).setTabCompleter(this);

        reloadPluginConfig();

        getLogger().info("插件已启用，已应用配置");
    }

    @Override
    public void onDisable() {
        cancelAllTasks();
        getLogger().info("插件已禁用");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        passiveDropChance = getConfig().getDouble("passive-mob-drop-chance", 0.2);
        eventTimeStr = getConfig().getString("event-time", "2026.9.25.00:00-2026.9.27.23:59");
        parseTimeRange();
        cancelAllTasks();
        scheduleEventTasks();
        getLogger().info("插件配置已重新加载");
    }

    private void parseTimeRange() {
        String[] parts = eventTimeStr.split("-");
        if (parts.length != 2) {
            getLogger().warning("无效的事件时间格式（开始时间）！将使用默认值。");
            startTime = LocalDateTime.parse("2026-09-25T00:00");
            endTime = LocalDateTime.parse("2026-09-27T23:59");
            return;
        }
        try {
            startTime = LocalDateTime.parse(parts[0], formatter);
            endTime = LocalDateTime.parse(parts[1], formatter);
        } catch (Exception e) {
            getLogger().warning("无效的事件时间格式（结束时间）！将使用默认值。");
            startTime = LocalDateTime.parse("2026-09-25T00:00");
            endTime = LocalDateTime.parse("2026-09-27T23:59");
        }
    }

    private void scheduleEventTasks() {
        parseTimeRange();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(startTime) && now.isBefore(endTime)) {
            isEventActive = true;
            long endDelay = ChronoUnit.MILLIS.between(now, endTime);
            if (endDelay > 0) {
                endTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::onEventEnd, endDelay / 50);
                if (endDelay <= 10000) {
                    startEndCountdown((int)(endDelay / 1000));
                } else {
                    long countdownStart = endDelay - 10000;
                    if (countdownStart > 0) {
                        endCountdownTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> startEndCountdown(10), countdownStart / 50);
                    }
                }
            }
        } else if (now.isBefore(startTime)) {
            isEventActive = false;
            long startDelay = ChronoUnit.MILLIS.between(now, startTime);
            if (startDelay > 0) {
                startTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::onEventStart, startDelay / 50);
                if (startDelay <= 10000) {
                    startStartCountdown((int)(startDelay / 1000));
                } else {
                    long countdownStart = startDelay - 10000;
                    if (countdownStart > 0) {
                        startCountdownTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> startStartCountdown(10), countdownStart / 50);
                    }
                }
            }
        } else {
            isEventActive = false;
        }
    }

    private void cancelAllTasks() {
        if (startTaskId != -1) Bukkit.getScheduler().cancelTask(startTaskId);
        if (endTaskId != -1) Bukkit.getScheduler().cancelTask(endTaskId);
        if (startCountdownTaskId != -1) Bukkit.getScheduler().cancelTask(startCountdownTaskId);
        if (endCountdownTaskId != -1) Bukkit.getScheduler().cancelTask(endCountdownTaskId);
        startTaskId = endTaskId = startCountdownTaskId = endCountdownTaskId = -1;
    }

    private void onEventStart() {
        isEventActive = true;
        String msg = ChatColor.GOLD + "中秋节开始了，现在你可以击败生物获取月饼！\n" +
                ChatColor.GRAY + "- 输入 " + ChatColor.UNDERLINE + "/mooncake news" + ChatColor.RESET + ChatColor.GRAY + " 即可查看详细消息";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        long endDelay = ChronoUnit.MILLIS.between(LocalDateTime.now(), endTime);
        if (endDelay > 0) {
            endTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, this::onEventEnd, endDelay / 50);
            if (endDelay > 10000) {
                long countdownStart = endDelay - 10000;
                endCountdownTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> startEndCountdown(10), countdownStart / 50);
            } else {
                startEndCountdown((int)(endDelay / 1000));
            }
        }
    }

    private void onEventEnd() {
        isEventActive = false;
        String msg = ChatColor.GOLD + "中秋节结束了，期待下一次中秋节！\n" +
                ChatColor.GRAY + "- 输入 " + ChatColor.UNDERLINE + "/mooncake ranking" + ChatColor.RESET + ChatColor.GRAY + " 即可查看月饼排行榜";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            p.playSound(p.getLocation(), Sound.ENTITY_WOLF_WHINE, 1.0f, 1.0f);
        }
    }

    private void startStartCountdown(int seconds) {
        if (seconds <= 0) seconds = 10;
        int finalSeconds = seconds;
        startCountdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            int count = finalSeconds;
            @Override
            public void run() {
                if (count <= 0) {
                    Bukkit.getScheduler().cancelTask(startCountdownTaskId);
                    startCountdownTaskId = -1;
                    return;
                }
                String msg = "中秋节还有 " + count + " 秒开始";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(Component.text(msg, NamedTextColor.GOLD));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
                count--;
            }
        }, 0L, 20L);
    }

    private void startEndCountdown(int seconds) {
        if (seconds <= 0) seconds = 10;
        int finalSeconds = seconds;
        endCountdownTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            int count = finalSeconds;
            @Override
            public void run() {
                if (count <= 0) {
                    Bukkit.getScheduler().cancelTask(endCountdownTaskId);
                    endCountdownTaskId = -1;
                    return;
                }
                String msg = "中秋节还有 " + count + " 秒结束";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(Component.text(msg, NamedTextColor.GOLD));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
                count--;
            }
        }, 0L, 20L);
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public ItemStack createMooncake(String type, String filling, int amount) {
        String baseType;
        String fillingName = null;
        if (type.startsWith("NORMAL_MOONCAKE")) {
            baseType = "NORMAL_MOONCAKE";
            if (type.contains("[") && type.endsWith("]")) {
                int start = type.indexOf('[') + 1;
                int end = type.indexOf(']');
                if (start < end) {
                    fillingName = type.substring(start, end).toUpperCase();
                }
            }
            if (fillingName == null && filling != null) {
                fillingName = filling.toUpperCase();
            }
            if (fillingName == null) {
                return null;
            }
            if (getFillingChinese(fillingName) == null) {
                return null;
            }
        } else {
            baseType = type.toUpperCase();
            if (!baseType.equals("DRAGON_EGG_MOONCAKE") && !baseType.equals("WITHER_MOONCAKE")) {
                return null;
            }
            fillingName = null;
        }

        List<Component> lore = new ArrayList<>();
        NamedTextColor color;
        String displayName;
        String typeId;
        int nutrition;
        float saturation;

        switch (baseType) {
            case "DRAGON_EGG_MOONCAKE":
                displayName = "龙骨月饼";
                color = NamedTextColor.GOLD;
                lore.add(Component.text("中秋节时期从末影龙身上爆出来的极品月饼")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                nutrition = 8;
                saturation = 17;
                typeId = "DRAGON";
                break;
            case "WITHER_MOONCAKE":
                displayName = "凋零月饼";
                color = NamedTextColor.DARK_PURPLE;
                lore.add(Component.text("中秋节时期从凋零身上爆出来的极品月饼")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                nutrition = 8;
                saturation = 17;
                typeId = "WITHER";
                break;
            case "NORMAL_MOONCAKE":
                displayName = "月饼";
                color = NamedTextColor.YELLOW;
                lore.add(Component.text("中秋节时期人人都会吃的月饼")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                String chinese = getFillingChinese(fillingName);
                lore.add(Component.text("月饼馅：" + chinese)
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                nutrition = 4;
                saturation = 10;
                typeId = "NORMAL";
                break;
            default:
                return null;
        }
        ItemStack item = ItemBuilder.buildCustomHead(this, typeId, displayName, color, MOON_CAKE_TEXTURE, lore, nutrition, saturation);
        if (item == null) return null;

        item.setAmount(amount);
        return item;
    }

    private void registerRecipes() {
        registerNormalMooncakeRecipe("蛋黄", "NORMAL_MOONCAKE[DANHUANG]", Material.EGG, null);
        registerNormalMooncakeRecipe("五仁", "NORMAL_MOONCAKE[WUREN]", Material.PUMPKIN_SEEDS, Material.MELON_SEEDS);
        registerNormalMooncakeRecipe("莲蓉", "NORMAL_MOONCAKE[LIANRONG]", Material.LILY_PAD, null);
        registerNormalMooncakeRecipe("水果", "NORMAL_MOONCAKE[FRUIT]", Material.APPLE, null);
        registerNormalMooncakeRecipe("豆沙", "NORMAL_MOONCAKE[DOUSHA]", Material.COCOA_BEANS, null);
    }

    /**
     * 注册单个月饼配方
     * @param name 馅料名称（用于日志）
     * @param typeStr 月饼类型字符串（如 NORMAL_MOONCAKE[DANHUANG]）
     * @param mainIngredient 主要馅料材料
     * @param alternativeIngredient 替代材料（如西瓜种子，可为 null）
     */
    private void registerNormalMooncakeRecipe(String name, String typeStr, Material mainIngredient, Material alternativeIngredient) {
        NamespacedKey key1 = new NamespacedKey(this, "mooncake_" + typeStr.toLowerCase().replace("[", "_").replace("]", "_"));
        recipeKeys.add(key1);
        ShapedRecipe recipe1 = createRecipe(key1, typeStr, mainIngredient);
        if (recipe1 != null) {
            Bukkit.addRecipe(recipe1);
        }

        if (alternativeIngredient != null) {
            NamespacedKey key2 = new NamespacedKey(this, "mooncake_" + typeStr.toLowerCase().replace("[", "_").replace("]", "_") + "_alt");
            recipeKeys.add(key2);
            ShapedRecipe recipe2 = createRecipe(key2, typeStr, alternativeIngredient);
            if (recipe2 != null) {
                Bukkit.addRecipe(recipe2);
            }
        }
    }

    private ShapedRecipe createRecipe(NamespacedKey key, String typeStr, Material ingredient) {
        try {
            ItemStack result = createMooncake(typeStr, null, 1);
            if (result == null) {
                return null;
            }

            ShapedRecipe recipe = new ShapedRecipe(key, result);
            // 顶部 - 小麦 小麦 小麦
            // 中间 - 小麦种子 馅料 小麦种子
            // 底部 - 小麦种子 小麦种子 小麦种子
            recipe.shape("WWW", "SIS", "SSS");
            recipe.setIngredient('W', Material.WHEAT);
            recipe.setIngredient('S', Material.WHEAT_SEEDS);
            recipe.setIngredient('I', ingredient);

            return recipe;
        } catch (Exception e) {
            return null;
        }
    }

    // 弃用
    public int getPlayerMooncakeCount(Player player, String type) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (!meta.getPersistentDataContainer().has(keyMooncake, PersistentDataType.BOOLEAN)) continue;
            String t = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
            if (t != null && t.equals(type)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    // 弃用
    public int getPlayerSpecialTotal(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (!meta.getPersistentDataContainer().has(keyMooncake, PersistentDataType.BOOLEAN)) continue;
            String t = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
            if (t != null && (t.equals("DRAGON") || t.equals("WITHER"))) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private String getFillingChinese(String filling) {
        switch (filling.toUpperCase()) {
            case "WUREN": return "五仁";
            case "LIANRONG": return "莲蓉";
            case "FRUIT": return "水果";
            case "DOUSHA": return "豆沙";
            case "DANHUANG": return "蛋黄";
            default: return null;
        }
    }

    private static class PlayerStats {
        String name;
        int dragon;
        int wither;
        int normal;
        int total;
        PlayerStats(String name, int dragon, int wither, int normal) {
            this.name = name;
            this.dragon = dragon;
            this.wither = wither;
            this.normal = normal;
            this.total = dragon + wither + normal;
        }
    }

    private void showRanking(Player player) {
        List<PlayerStats> stats = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            int dragon = 0, wither = 0, normal = 0;
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (!meta.getPersistentDataContainer().has(keyMooncake, PersistentDataType.BOOLEAN)) continue;
                String type = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
                if (type == null) continue;
                int amount = item.getAmount();
                switch (type) {
                    case "DRAGON": dragon += amount; break;
                    case "WITHER": wither += amount; break;
                    case "NORMAL": normal += amount; break;
                }
            }
            int total = dragon + wither + normal;
            if (total > 0) {
                stats.add(new PlayerStats(p.getName(), dragon, wither, normal));
            }
        }
        stats.sort((a, b) -> {
            if (b.total != a.total) return b.total - a.total;
            if (b.dragon != a.dragon) return b.dragon - a.dragon;
            return b.wither - a.wither;
        });
        List<PlayerStats> top = stats.stream().limit(10).collect(Collectors.toList());

        player.sendMessage(ChatColor.GRAY + "==== " + ChatColor.GOLD + "中秋月饼排行榜" + ChatColor.GRAY + " ====");
        if (top.isEmpty()) {
            player.sendMessage(ChatColor.RED + "- 暂无玩家拥有月饼");
        } else {
            int rank = 1;
            for (PlayerStats s : top) {
                String line = ChatColor.GRAY + "No." + rank + " " + s.name + " " + ChatColor.GRAY + "- " +
                        ChatColor.GOLD + s.dragon + ChatColor.GRAY + " - " +
                        ChatColor.DARK_PURPLE + s.wither + ChatColor.GRAY + " - " +
                        ChatColor.YELLOW + s.normal;
                player.sendMessage(line);
                rank++;
            }
        }
        player.sendMessage(ChatColor.GRAY + "=====================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        if (!sender.hasPermission("mooncake.use")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mooncake.admin")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return true;
            }
            reloadPluginConfig();
            sender.sendMessage(ChatColor.GREEN + "MoonCake插件配置文件已重载！");
            return true;
        }

        if (args[0].equalsIgnoreCase("ranking")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("此命令仅玩家可用。");
                return true;
            }
            showRanking((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("news")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("此命令仅玩家可用。");
                return true;
            }
            if (!isEventActive()) {
                sender.sendMessage(ChatColor.RED + "现在还不是中秋节，你不能使用此指令！");
                return true;
            }
            openNewsBook((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("mooncake.admin")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                return true;
            }
            if (args.length != 4) {
                sender.sendMessage(ChatColor.RED + "用法：/mooncake give <玩家名> <类型> <数量>");
                sender.sendMessage(ChatColor.RED + "类型：DRAGON_EGG_MOONCAKE, WITHER_MOONCAKE, NORMAL_MOONCAKE[馅料]");
                return true;
            }
            handleGiveCommand(sender, args[1], args[2], args[3]);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "未知子命令。可用：help, reload, ranking, news, give");
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "==== " + ChatColor.GOLD + "月饼插件帮助列表 " + ChatColor.GRAY + "====");
        sender.sendMessage(ChatColor.GOLD + "/mooncake give <player> <type> <amount>");
        sender.sendMessage(ChatColor.GRAY + "- 给予玩家月饼 (仅管理员可用)");
        sender.sendMessage(ChatColor.GOLD + "/mooncake ranking");
        sender.sendMessage(ChatColor.GRAY + "- 查看月饼排行榜");
        sender.sendMessage(ChatColor.GOLD + "/mooncake news");
        sender.sendMessage(ChatColor.GRAY + "- 查看活动详细信息（仅活动开启可用）");
        sender.sendMessage(ChatColor.GOLD + "/mooncake reload");
        sender.sendMessage(ChatColor.GRAY + "- 重新加载插件配置文件(仅管理员可用)");
        sender.sendMessage(ChatColor.GOLD + "/mooncake help");
        sender.sendMessage(ChatColor.GRAY + "- 显示此帮助信息");
        sender.sendMessage(ChatColor.GRAY + "=====================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("help", "ranking", "news"));
            if (sender.hasPermission("mooncake.admin")) {
                subCommands.add("reload");
                subCommands.add("give");
            }
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("mooncake.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String name = p.getName();
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("mooncake.admin")) {
                List<String> types = new ArrayList<>();
                types.add("DRAGON_EGG_MOONCAKE");
                types.add("WITHER_MOONCAKE");
                String[] fillings = {"WUREN", "LIANRONG", "FRUIT", "DOUSHA", "DANHUANG"};
                for (String f : fillings) {
                    types.add("NORMAL_MOONCAKE[" + f + "]");
                }
                for (String type : types) {
                    if (type.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(type);
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("mooncake.admin")) {
                completions.add("1");
                completions.add("5");
                completions.add("10");
                completions.add("64");
            }
        }
        return completions;
    }

    private void handleGiveCommand(CommandSender sender, String playerName, String typeStr, String amountStr) {
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                sender.sendMessage(ChatColor.RED + "数量必须为正整数！");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数量必须为整数！");
            return;
        }

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + playerName + " 不在线或不存在！");
            return;
        }

        ItemStack mooncake = createMooncake(typeStr, null, amount);
        if (mooncake == null) {
            sender.sendMessage(ChatColor.RED + "无效的月饼类型！支持：DRAGON_EGG_MOONCAKE, WITHER_MOONCAKE, NORMAL_MOONCAKE[馅料]");
            sender.sendMessage(ChatColor.RED + "馅料：WUREN, LIANRONG, FRUIT, DOUSHA, DANHUANG");
            return;
        }

        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(mooncake);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                target.getWorld().dropItem(target.getLocation(), item);
            }
            sender.sendMessage(ChatColor.YELLOW + "目标玩家背包已满，部分物品已掉落在地上。");
        }

        String displayName = mooncake.getItemMeta().getDisplayName();
        sender.sendMessage(ChatColor.GOLD + "已将 " + amount + "个 [" + displayName + ChatColor.GOLD + "] 给予 " + target.getName());
        target.sendMessage(ChatColor.GREEN + "你收到了 " + amount + " 个 [" + displayName + ChatColor.GREEN + "] ！");
    }

    private void openNewsBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;

        meta.setTitle("中秋节特刊");
        meta.setAuthor("MoonCakePlugin");

        String page1 = "       【中秋节由来】\n" +
                "是中国的传统节日之一，\n" +
                "时间为农历八月十五。\n" +
                "中秋节起源于古代对月的崇拜，至今已历史悠久。\n" +
                "自古便有祭月、赏月、拜月、吃月饼、赏桂花、饮桂花\n" +
                "酒等习俗，\n" +
                "流传至今，经久不息。\n" +
                "中秋节以月之圆兆人团圆，\n" +
                "寄托思念故乡，思念亲人之情，\n" +
                "祈盼丰收、幸福。\n" +
                "是中国四大传统节日之一。";

        String page2Raw = "       &6【中秋节活动】\n" +
                "&f收集尽可能多的月饼！\n" +
                "&8活动时间：" + eventTimeStr + "\n\n" +
                "&6龙骨月饼 &0- 击败末影龙可掉落\n\n" +
                "&5凋零月饼 &0- 击败凋零可掉落\n\n" +
                "&e月饼 &0- 击败生物（如：猪、僵尸、鲑鱼等）有" +
                String.format("%.0f%%", passiveDropChance * 100) + "的几率掉落";

        String page3Raw = "       &3【月饼类型】\n" +
                "&0有7种类型的月饼：\n" +
                "&0极品月饼：龙骨月饼、凋零月饼\n" +
                "&0普通月饼（馅料）：怪物（五仁）、动物（莲蓉）、水生物（水果）、玩家（豆沙）、蠹虫（蛋黄）\n\n" +
                "&0拥有的月饼将会记录到插件排行榜！比比谁的月饼更多吧！\n" +
                "&0输入/mooncake ranking\n&0即可查看月饼排行榜";

        Component page1Comp = Component.text(page1, NamedTextColor.BLACK);
        Component page2Comp = translateColorCodes(page2Raw);
        Component page3Comp = translateColorCodes(page3Raw);

        meta.pages(page1Comp, page2Comp, page3Comp);
        book.setItemMeta(meta);

        player.openBook(book);
    }

    private Component translateColorCodes(String text) {
        String[] lines = text.split("\n");
        Component result = Component.empty();
        for (String line : lines) {
            if (!result.equals(Component.empty())) {
                result = result.append(Component.newline());
            }
            String[] parts = line.split("&");
            Component lineComp = Component.empty();
            NamedTextColor currentColor = NamedTextColor.WHITE;
            for (int i = 0; i < parts.length; i++) {
                if (i == 0) {
                    lineComp = lineComp.append(Component.text(parts[i], currentColor));
                } else {
                    if (parts[i].length() > 0) {
                        char colorChar = parts[i].charAt(0);
                        NamedTextColor color = getColorFromChar(colorChar);
                        if (color != null) currentColor = color;
                        String textPart = parts[i].substring(1);
                        lineComp = lineComp.append(Component.text(textPart, currentColor));
                    }
                }
            }
            result = result.append(lineComp);
        }
        return result;
    }

    private NamedTextColor getColorFromChar(char c) {
        switch (c) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            case 'f': return NamedTextColor.WHITE;
            default: return null;
        }
    }
}