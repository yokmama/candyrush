package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.PlayerData;
import com.candyrush.models.ShopItem;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ショップ機能を管理するクラス
 */
public class ShopManager implements Listener {

    private final CandyRushPlugin plugin;
    private final List<ShopItem> shopItems;
    private final Map<String, Inventory> openShops; // プレイヤー名 -> 開いているショップインベントリ
    private final Map<String, String> playerCurrentCategory; // プレイヤー名 -> 現在のカテゴリ
    private FileConfiguration shopConfig;
    private File shopConfigFile;

    // ショップアイテム定義
    public static final Material SHOP_ITEM_MATERIAL = Material.EMERALD;
    public static final String SHOP_ITEM_NAME = "§6§l★ ショップ ★";
    public static final int SHOP_ITEM_SLOT = 8; // スロット9番目（0-indexed）

    public ShopManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.shopItems = new ArrayList<>();
        this.openShops = new HashMap<>();
        this.playerCurrentCategory = new HashMap<>();
        loadShopConfig();
        loadShopItems();
    }

    /**
     * shop.ymlファイルをロード（存在しない場合は作成）
     */
    private void loadShopConfig() {
        shopConfigFile = new File(plugin.getDataFolder(), "shop.yml");

        if (!shopConfigFile.exists()) {
            plugin.saveResource("shop.yml", false);
            plugin.getLogger().info("Created shop.yml file");
        }

        shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
        plugin.getLogger().info("Loaded shop.yml configuration");
    }

    /**
     * shop.ymlからショップアイテムを読み込み
     */
    private void loadShopItems() {
        shopItems.clear();

        // shop.ymlのitemsリストを読み込む
        List<?> itemsList = shopConfig.getList("items");
        if (itemsList == null || itemsList.isEmpty()) {
            plugin.getLogger().warning("No shop items configured in shop.yml");
            return;
        }

        for (Object obj : itemsList) {
            if (!(obj instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) obj;

            try {
                String id = (String) itemMap.getOrDefault("id", "unknown");
                String name = (String) itemMap.getOrDefault("name", "Unknown Item");
                String materialStr = (String) itemMap.getOrDefault("material", "STONE");
                int amount = itemMap.containsKey("amount") ? (Integer) itemMap.get("amount") : 1;
                int price = itemMap.containsKey("price") ? (Integer) itemMap.get("price") : 0;
                String potionTypeStr = (String) itemMap.get("potion-type");
                String category = (String) itemMap.getOrDefault("category", "other");

                Material material = Material.getMaterial(materialStr);
                if (material == null) {
                    plugin.getLogger().warning("Invalid material for shop item: " + materialStr);
                    continue;
                }

                PotionType potionType = null;
                if (potionTypeStr != null) {
                    try {
                        potionType = PotionType.valueOf(potionTypeStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid potion type for shop item: " + potionTypeStr);
                    }
                }

                ShopItem shopItem = new ShopItem(id, name, material, amount, price, potionType, category);
                shopItems.add(shopItem);

                plugin.getLogger().info("Loaded shop item: " + name + " (" + price + "pt) [category: " + category + "]");

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load shop item: " + itemMap.get("id") + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + shopItems.size() + " shop items");
    }

    /**
     * プレイヤーにショップを開く（カテゴリメニュー）
     */
    public void openShop(Player player) {
        openCategoryMenu(player);
    }

    /**
     * カテゴリ選択メニューを開く
     */
    private void openCategoryMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "§6§lショップ - カテゴリ選択");

        // カテゴリアイテムを配置
        // 武器 (slot 10)
        ItemStack weaponCategory = new ItemStack(Material.IRON_SWORD);
        ItemMeta weaponMeta = weaponCategory.getItemMeta();
        if (weaponMeta != null) {
            weaponMeta.setDisplayName("§f§l[武器]");
            List<String> weaponLore = new ArrayList<>();
            weaponLore.add("§7剣、弓、クロスボウなど");
            weaponLore.add("");
            weaponLore.add("§eクリックして表示");
            weaponMeta.setLore(weaponLore);
            weaponCategory.setItemMeta(weaponMeta);
        }
        menu.setItem(10, weaponCategory);

        // 防具 (slot 11)
        ItemStack armorCategory = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta armorMeta = armorCategory.getItemMeta();
        if (armorMeta != null) {
            armorMeta.setDisplayName("§f§l[防具]");
            List<String> armorLore = new ArrayList<>();
            armorLore.add("§7ヘルメット、チェストプレートなど");
            armorLore.add("");
            armorLore.add("§eクリックして表示");
            armorMeta.setLore(armorLore);
            armorCategory.setItemMeta(armorMeta);
        }
        menu.setItem(11, armorCategory);

        // 食料 (slot 12)
        ItemStack foodCategory = new ItemStack(Material.COOKED_BEEF);
        ItemMeta foodMeta = foodCategory.getItemMeta();
        if (foodMeta != null) {
            foodMeta.setDisplayName("§f§l[食料]");
            List<String> foodLore = new ArrayList<>();
            foodLore.add("§7パン、肉、金のリンゴなど");
            foodLore.add("");
            foodLore.add("§eクリックして表示");
            foodMeta.setLore(foodLore);
            foodCategory.setItemMeta(foodMeta);
        }
        menu.setItem(12, foodCategory);

        // バフアイテム (slot 14)
        ItemStack buffCategory = new ItemStack(Material.POTION);
        ItemMeta buffMeta = buffCategory.getItemMeta();
        if (buffMeta != null) {
            buffMeta.setDisplayName("§f§l[バフアイテム]");
            List<String> buffLore = new ArrayList<>();
            buffLore.add("§7ポーション、エンチャント本など");
            buffLore.add("");
            buffLore.add("§eクリックして表示");
            buffMeta.setLore(buffLore);
            buffCategory.setItemMeta(buffMeta);
        }
        menu.setItem(14, buffCategory);

        // 便利ツール (slot 15)
        ItemStack toolCategory = new ItemStack(Material.ENDER_PEARL);
        ItemMeta toolMeta = toolCategory.getItemMeta();
        if (toolMeta != null) {
            toolMeta.setDisplayName("§f§l[便利ツール]");
            List<String> toolLore = new ArrayList<>();
            toolLore.add("§7矢、エンダーパール、ブロックなど");
            toolLore.add("");
            toolLore.add("§eクリックして表示");
            toolMeta.setLore(toolLore);
            toolCategory.setItemMeta(toolMeta);
        }
        menu.setItem(15, toolCategory);

        // プレイヤーの現在のポイントを表示（右下）
        PlayerData playerData = plugin.getPlayerManager().getOrCreatePlayerData(player);
        ItemStack pointsDisplay = new ItemStack(Material.GOLD_INGOT);
        ItemMeta pointsMeta = pointsDisplay.getItemMeta();
        if (pointsMeta != null) {
            pointsMeta.setDisplayName("§6§l所持ポイント: " + playerData.getPoints() + "pt");
            List<String> lore = new ArrayList<>();
            lore.add("§7ポイントでアイテムを購入できます");
            pointsMeta.setLore(lore);
            pointsDisplay.setItemMeta(pointsMeta);
        }
        menu.setItem(26, pointsDisplay);

        playerCurrentCategory.remove(player.getName());
        player.openInventory(menu);
        openShops.put(player.getName(), menu); // インベントリを開いた後にputする
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    /**
     * カテゴリ別のアイテムリストを開く
     */
    private void openCategoryItems(Player player, String category) {
        // カテゴリに属するアイテムを取得
        List<ShopItem> categoryItems = new ArrayList<>();
        for (ShopItem item : shopItems) {
            if (item.getCategory() != null && item.getCategory().equals(category)) {
                categoryItems.add(item);
            }
        }

        plugin.getLogger().info("Opening category '" + category + "' for " + player.getName() + " - Found " + categoryItems.size() + " items");
        for (ShopItem item : categoryItems) {
            plugin.getLogger().info("  - " + item.getName() + " (category: " + item.getCategory() + ")");
        }

        String categoryName = getCategoryDisplayName(category);
        Inventory itemsMenu = Bukkit.createInventory(null, 54, "§6§l" + categoryName);

        // アイテムを配置
        for (int i = 0; i < categoryItems.size() && i < 45; i++) {
            itemsMenu.setItem(i, categoryItems.get(i).createItemStack());
        }

        // 戻るボタン (slot 49)
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§e§l← カテゴリ選択に戻る");
            backButton.setItemMeta(backMeta);
        }
        itemsMenu.setItem(49, backButton);

        // プレイヤーの現在のポイントを表示
        PlayerData playerData = plugin.getPlayerManager().getOrCreatePlayerData(player);
        ItemStack pointsDisplay = new ItemStack(Material.GOLD_INGOT);
        ItemMeta pointsMeta = pointsDisplay.getItemMeta();
        if (pointsMeta != null) {
            pointsMeta.setDisplayName("§6§l所持ポイント: " + playerData.getPoints() + "pt");
            pointsDisplay.setItemMeta(pointsMeta);
        }
        itemsMenu.setItem(53, pointsDisplay);

        playerCurrentCategory.put(player.getName(), category);
        player.openInventory(itemsMenu);
        openShops.put(player.getName(), itemsMenu); // インベントリを開いた後にputする
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    /**
     * カテゴリの表示名を取得
     */
    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "weapon": return "武器";
            case "armor": return "防具";
            case "food": return "食料";
            case "buff": return "バフアイテム";
            case "tool": return "便利ツール";
            default: return "その他";
        }
    }

    /**
     * ショップでのクリックを処理
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        plugin.getLogger().info("Inventory click detected: player=" + player.getName() + ", hasOpenShop=" + openShops.containsKey(player.getName()));

        // ショップインベントリかチェック
        if (!openShops.containsKey(player.getName())) {
            return;
        }

        Inventory shopInventory = openShops.get(player.getName());
        if (clickedInventory == null || !clickedInventory.equals(shopInventory)) {
            plugin.getLogger().info("Click not in shop inventory - clickedInv=" + (clickedInventory != null ? clickedInventory.getType() : "null"));
            return;
        }

        event.setCancelled(true); // ショップ内のアイテム移動を禁止

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        String currentCategory = playerCurrentCategory.get(player.getName());

        plugin.getLogger().info("Shop click: player=" + player.getName() + ", slot=" + slot + ", category=" + currentCategory + ", item=" + clickedItem.getType());

        // カテゴリメニュー（currentCategoryがnull）の場合
        if (currentCategory == null) {
            // ポイント表示はクリック無効
            if (slot == 26) {
                return;
            }

            // カテゴリ選択処理
            switch (slot) {
                case 10: // 武器
                    plugin.getLogger().info("Opening weapon category");
                    openCategoryItems(player, "weapon");
                    break;
                case 11: // 防具
                    plugin.getLogger().info("Opening armor category");
                    openCategoryItems(player, "armor");
                    break;
                case 12: // 食料
                    plugin.getLogger().info("Opening food category");
                    openCategoryItems(player, "food");
                    break;
                case 14: // バフアイテム
                    plugin.getLogger().info("Opening buff category");
                    openCategoryItems(player, "buff");
                    break;
                case 15: // 便利ツール
                    plugin.getLogger().info("Opening tool category");
                    openCategoryItems(player, "tool");
                    break;
            }
            return;
        }

        // カテゴリ別アイテム表示の場合
        // ポイント表示はクリック無効
        if (slot == 53) {
            return;
        }

        // 戻るボタン
        if (slot == 49) {
            plugin.getLogger().info("Back button clicked - returning to category menu");
            openCategoryMenu(player);
            return;
        }

        // アイテム購入処理
        if (slot < 45) {
            // カテゴリに属するアイテムを取得
            List<ShopItem> categoryItems = new ArrayList<>();
            for (ShopItem item : shopItems) {
                if (item.getCategory() != null && item.getCategory().equals(currentCategory)) {
                    categoryItems.add(item);
                }
            }

            if (slot < categoryItems.size()) {
                ShopItem shopItem = categoryItems.get(slot);
                purchaseItem(player, shopItem);
            }
        }
    }

    /**
     * アイテムを購入
     */
    private void purchaseItem(Player player, ShopItem shopItem) {
        PlayerData playerData = plugin.getPlayerManager().getOrCreatePlayerData(player);

        // ポイント確認
        if (playerData.getPoints() < shopItem.getPrice()) {
            MessageUtils.sendMessage(player, "&cポイントが不足しています！ (&6必要: " + shopItem.getPrice() + "pt&c)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // インベントリに空きがあるか確認
        if (player.getInventory().firstEmpty() == -1) {
            MessageUtils.sendMessage(player, "&cインベントリがいっぱいです！");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 購入処理（個人ポイント減算、チームポイントも減算）
        playerData.addPoints(-shopItem.getPrice());
        plugin.getPlayerManager().savePlayerData(playerData);

        // チームポイントも減算
        if (playerData.getTeamColor() != null) {
            plugin.getTeamManager().addTeamPoints(playerData.getTeamColor(), -shopItem.getPrice());
        }

        // アイテムを渡す
        player.getInventory().addItem(shopItem.createPurchasedItem());

        // 通知
        MessageUtils.sendMessage(player, "&a&l購入成功！ &7" + shopItem.getName() + " &ax" + shopItem.getAmount());
        MessageUtils.sendMessage(player, "&7残りポイント: &6" + playerData.getPoints() + "pt");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // ショップを更新（ポイント表示を更新）
        String currentCategory = playerCurrentCategory.get(player.getName());
        if (currentCategory != null) {
            openCategoryItems(player, currentCategory);
        } else {
            openCategoryMenu(player);
        }

        plugin.getLogger().info("Player " + player.getName() + " purchased " + shopItem.getName() + " for " + shopItem.getPrice() + "pt");
    }

    /**
     * プレイヤーがインベントリを閉じた時の処理
     */
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();

            // 少し遅延させてクリア（インベントリ切り替えの場合は新しいのが登録される）
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // プレイヤーがまだショップを開いていなければクリア
                if (player.getOpenInventory().getTopInventory().equals(event.getInventory())) {
                    openShops.remove(player.getName());
                    playerCurrentCategory.remove(player.getName());
                    plugin.getLogger().info("Cleared shop data for " + player.getName());
                }
            }, 1L);
        }
    }

    /**
     * ショップアイテムを右クリック（空中のみ）でショップを開く
     */
    @EventHandler
    public void onShopItemInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 空中を右クリックした時のみ
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        // ショップアイテムかチェック
        if (!isShopItem(item)) {
            return;
        }

        // イベントキャンセル
        event.setCancelled(true);

        // ゲーム中のみショップを開く
        if (plugin.getGameManager().getCurrentState() != com.candyrush.models.GameState.RUNNING) {
            MessageUtils.sendMessage(player, "&cゲーム中のみショップを利用できます！");
            return;
        }

        // ショップを開く
        openShop(player);
    }

    /**
     * ショップアイテムのドロップを防止
     */
    @EventHandler
    public void onShopItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (isShopItem(item)) {
            event.setCancelled(true);
            MessageUtils.sendMessage(event.getPlayer(), "&cショップアイテムは捨てられません！");
        }
    }

    /**
     * ショップアイテムの移動を防止
     */
    @EventHandler
    public void onShopItemMove(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        // ショップアイテムの移動を防止
        if (isShopItem(item)) {
            // ショップインベントリ内でない場合は移動禁止
            if (!openShops.containsKey(event.getWhoClicked().getName())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * ショップアイテムを作成
     */
    public static ItemStack createShopItem() {
        ItemStack shopItem = new ItemStack(SHOP_ITEM_MATERIAL);
        ItemMeta meta = shopItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SHOP_ITEM_NAME);
            List<String> lore = new ArrayList<>();
            lore.add("§7右クリックでショップを開く");
            lore.add("§8このアイテムは捨てられません");
            meta.setLore(lore);
            shopItem.setItemMeta(meta);
        }
        return shopItem;
    }

    /**
     * アイテムがショップアイテムか判定
     */
    public static boolean isShopItem(ItemStack item) {
        if (item == null || item.getType() != SHOP_ITEM_MATERIAL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        return meta.getDisplayName().equals(SHOP_ITEM_NAME);
    }

    /**
     * プレイヤーにショップアイテムを付与
     */
    public void giveShopItem(Player player) {
        player.getInventory().setItem(SHOP_ITEM_SLOT, createShopItem());
    }

    /**
     * ショップアイテムをリロード
     */
    public void reload() {
        loadShopConfig();
        loadShopItems();
    }
}
