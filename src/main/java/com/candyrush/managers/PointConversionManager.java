package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.LanguageManager;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 食料アイテムをポイントに変換するマネージャー
 * プレイヤーが食料を消費すると自動的にポイントに変換
 */
public class PointConversionManager {

    private final CandyRushPlugin plugin;
    private final LanguageManager lang;
    private final Map<Material, Integer> foodPointValues;

    public PointConversionManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.foodPointValues = new HashMap<>();
        initializeFoodValues();
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        plugin.getLogger().info("PointConversionManager initialized with " +
                              foodPointValues.size() + " food types");
    }

    /**
     * 食料アイテムのポイント値を初期化
     */
    private void initializeFoodValues() {
        // 基本的な食料
        foodPointValues.put(Material.APPLE, 5);
        foodPointValues.put(Material.BREAD, 10);
        foodPointValues.put(Material.CARROT, 3);
        foodPointValues.put(Material.POTATO, 3);
        foodPointValues.put(Material.BAKED_POTATO, 8);
        foodPointValues.put(Material.BEETROOT, 3);
        foodPointValues.put(Material.MELON_SLICE, 2);
        foodPointValues.put(Material.SWEET_BERRIES, 2);

        // 肉類
        foodPointValues.put(Material.COOKED_BEEF, 20);
        foodPointValues.put(Material.COOKED_PORKCHOP, 20);
        foodPointValues.put(Material.COOKED_CHICKEN, 15);
        foodPointValues.put(Material.COOKED_MUTTON, 15);
        foodPointValues.put(Material.COOKED_RABBIT, 12);

        // 魚類
        foodPointValues.put(Material.COOKED_COD, 12);
        foodPointValues.put(Material.COOKED_SALMON, 15);

        // お菓子類
        foodPointValues.put(Material.COOKIE, 5);
        foodPointValues.put(Material.PUMPKIN_PIE, 15);
        foodPointValues.put(Material.CAKE, 25);

        // 特別な食料
        foodPointValues.put(Material.GOLDEN_APPLE, 50);
        foodPointValues.put(Material.GOLDEN_CARROT, 30);
        foodPointValues.put(Material.ENCHANTED_GOLDEN_APPLE, 100);

        // その他
        foodPointValues.put(Material.MUSHROOM_STEW, 15);
        foodPointValues.put(Material.RABBIT_STEW, 20);
        foodPointValues.put(Material.BEETROOT_SOUP, 15);
        foodPointValues.put(Material.SUSPICIOUS_STEW, 15);
    }

    /**
     * プレイヤーが食料を食べた時の処理
     */
    public void onPlayerEatFood(Player player, Material food) {
        // ゲームが進行中でない場合は無視
        if (!plugin.getGameManager().isGameRunning()) {
            return;
        }

        // 食料がポイント対象かチェック
        if (!foodPointValues.containsKey(food)) {
            return;
        }

        int points = foodPointValues.get(food);

        // プレイヤーデータを取得
        PlayerData playerData = plugin.getPlayerManager().getOrCreatePlayerData(player);
        TeamColor teamColor = playerData.getTeamColor();

        if (teamColor == null) {
            // チームに所属していない場合は変換しない
            return;
        }

        // プレイヤーにポイント追加
        playerData.addPoints(points);
        plugin.getPlayerManager().savePlayerData(playerData);

        // チームにポイント追加
        plugin.getTeamManager().addTeamPoints(teamColor, points);

        // プレイヤーに通知
        MessageUtils.sendActionBar(player,
            "&e+" + points + "pt &7(" + getFoodName(food) + ") &6合計: " +
            MessageUtils.formatPoints(playerData.getPoints()) + "pt");

        plugin.getLogger().fine("Player " + player.getName() + " earned " + points +
                              " points from " + food);
    }

    /**
     * インベントリ内の全食料を一括変換
     */
    public int convertAllFood(Player player) {
        // ゲームが進行中でない場合は変換しない
        if (!plugin.getGameManager().isGameRunning()) {
            MessageUtils.sendMessage(player, lang.getMessage("game.not_running"));
            return 0;
        }

        PlayerData playerData = plugin.getPlayerManager().getOrCreatePlayerData(player);
        TeamColor teamColor = playerData.getTeamColor();

        if (teamColor == null) {
            MessageUtils.sendMessage(player, lang.getMessage("game.not_running"));
            return 0;
        }

        int totalPoints = 0;
        int totalItems = 0;
        Map<Material, Integer> convertedItems = new HashMap<>();

        // インベントリをスキャン
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Material material = item.getType();
            if (!foodPointValues.containsKey(material)) {
                continue;
            }

            int itemPoints = foodPointValues.get(material);
            int amount = item.getAmount();
            int points = itemPoints * amount;

            totalPoints += points;
            totalItems += amount;
            convertedItems.put(material, convertedItems.getOrDefault(material, 0) + amount);

            // アイテムを削除
            item.setAmount(0);
        }

        if (totalPoints == 0) {
            MessageUtils.sendMessage(player, lang.getMessage("convert.nothing_to_convert"));
            return 0;
        }

        // ポイント加算
        playerData.addPoints(totalPoints);
        plugin.getPlayerManager().savePlayerData(playerData);
        plugin.getTeamManager().addTeamPoints(teamColor, totalPoints);

        // 結果を通知
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("items", String.valueOf(totalItems));
        placeholders.put("points", MessageUtils.formatPoints(totalPoints));
        MessageUtils.sendMessage(player, lang.getMessage("convert.success"));
        MessageUtils.sendMessage(player, lang.getMessage("convert.converted", placeholders));

        placeholders.clear();
        placeholders.put("points", MessageUtils.formatPoints(playerData.getPoints()));
        MessageUtils.sendMessage(player, lang.getMessage("convert.total_points", placeholders));

        MessageUtils.sendTitle(player,
            "&6&l+" + MessageUtils.formatPoints(totalPoints) + "pt",
            lang.getMessage("convert.success"));

        plugin.getLogger().info("Player " + player.getName() + " converted food for " +
                              totalPoints + " points");

        return totalPoints;
    }

    /**
     * アイテムが食料かチェック
     */
    public boolean isFood(Material material) {
        return foodPointValues.containsKey(material);
    }

    /**
     * 食料のポイント値を取得
     */
    public int getFoodPoints(Material material) {
        return foodPointValues.getOrDefault(material, 0);
    }

    /**
     * 食料の日本語名を取得（簡易版）
     */
    private String getFoodName(Material material) {
        switch (material) {
            case APPLE: return "リンゴ";
            case BREAD: return "パン";
            case COOKED_BEEF: return "焼き牛肉";
            case COOKED_CHICKEN: return "焼き鳥";
            case COOKED_PORKCHOP: return "焼き豚肉";
            case GOLDEN_APPLE: return "金のリンゴ";
            case GOLDEN_CARROT: return "金のニンジン";
            case ENCHANTED_GOLDEN_APPLE: return "エンチャント金リンゴ";
            default: return material.name();
        }
    }

    /**
     * クリーンアップ
     */
    public void shutdown() {
        plugin.getLogger().info("PointConversionManager shutdown complete");
    }
}
