package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.ChestType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 宝箱の生成・管理・リスポーンを担当するマネージャー
 * ゲーム開始時にマップ内にランダムに宝箱を配置
 */
public class TreasureChestManager {

    private final CandyRushPlugin plugin;
    private final Map<Location, ChestData> activeChests;
    private final Set<Location> pendingRespawn;
    private BukkitTask respawnTask;
    private Integer currentRoundId;  // 現在のゲームラウンドID

    public TreasureChestManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.activeChests = new ConcurrentHashMap<>();
        this.pendingRespawn = ConcurrentHashMap.newKeySet();
        this.currentRoundId = null;
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        plugin.getLogger().info("TreasureChestManager initialized");
    }

    /**
     * ゲーム開始時に宝箱を配置（非同期）
     * @param world ワールド
     * @param center 中心座標
     * @param radius 半径
     * @param roundId ゲームラウンドID
     */
    public void spawnTreasureChests(World world, Location center, int radius, Integer roundId) {
        this.currentRoundId = roundId;

        // 前回の宝箱を削除（activeChestsに記録されているもの）
        removeAllChests();

        // データベースから古いラウンドの宝箱を削除
        if (roundId != null) {
            deleteOldChestsFromDatabase(roundId, world);
        }

        int chestsPerChunk = plugin.getConfigManager().getTreasurePerChunk();

        // 中心座標からradiusブロック以内のチャンクをスキャン
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        int chunkRadius = radius >> 4;

        plugin.getLogger().info("Attempting to spawn chests in radius " + chunkRadius + " chunks (" + radius + " blocks)");
        plugin.getLogger().info("Center chunk: " + centerChunkX + ", " + centerChunkZ);
        plugin.getLogger().info("Chests per chunk: " + chestsPerChunk);

        // 非同期で宝箱を配置するための座標リストを作成
        java.util.List<int[]> chunkCoordinates = new java.util.ArrayList<>();
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                chunkCoordinates.add(new int[]{chunkX, chunkZ});
            }
        }

        java.util.concurrent.atomic.AtomicInteger totalChests = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger attemptedChunks = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failedLocations = new java.util.concurrent.atomic.AtomicInteger(0);

        // 非同期で宝箱をスポーン
        spawnChestsAsync(world, center, radius, chestsPerChunk, chunkCoordinates, 0,
                        totalChests, attemptedChunks, failedLocations);

        // リスポーンタスク開始
        startRespawnTask();
    }

    /**
     * 宝箱を非同期でスポーン（再帰的に処理）
     */
    private void spawnChestsAsync(World world, Location center, int radius, int chestsPerChunk,
                                  java.util.List<int[]> coordinates, int index,
                                  java.util.concurrent.atomic.AtomicInteger totalChests,
                                  java.util.concurrent.atomic.AtomicInteger attemptedChunks,
                                  java.util.concurrent.atomic.AtomicInteger failedLocations) {
        if (index >= coordinates.size()) {
            plugin.getLogger().info("Spawned " + totalChests.get() + " treasure chests in the map");
            plugin.getLogger().info("Attempted chunks: " + attemptedChunks.get() + ", Failed locations: " + failedLocations.get());
            return;
        }

        int[] coord = coordinates.get(index);
        int chunkX = coord[0];
        int chunkZ = coord[1];

        // チャンクを非同期でロード
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            attemptedChunks.incrementAndGet();

            // チャンクごとに宝箱を配置
            for (int i = 0; i < chestsPerChunk; i++) {
                Location chestLoc = findSafeChestLocationSync(chunk, center, radius);
                if (chestLoc != null) {
                    // メインスレッドでスポーン
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        spawnChest(chestLoc);
                        totalChests.incrementAndGet();
                    });
                } else {
                    failedLocations.incrementAndGet();
                }
            }

            // 次のチャンクを処理（1tick後）
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                spawnChestsAsync(world, center, radius, chestsPerChunk, coordinates, index + 1,
                               totalChests, attemptedChunks, failedLocations), 1L);
        });
    }

    /**
     * チャンク内で安全な宝箱配置場所を探す（同期版 - チャンクが既にロード済み前提）
     */
    private Location findSafeChestLocationSync(Chunk chunk, Location center, int radius) {
        Random random = new Random();
        int distanceRejects = 0;
        int groundRejects = 0;

        // 最大10回試行
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = (chunk.getX() << 4) + random.nextInt(16);
            int z = (chunk.getZ() << 4) + random.nextInt(16);

            // 中心からの距離チェック
            double distance = Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2));
            if (distance > radius) {
                distanceRejects++;
                continue;
            }

            // 適切な高さを探す（地表を探索）
            // チャンクは既にロード済みなのでgetHighestBlockYAtは安全
            int groundY = chunk.getWorld().getHighestBlockYAt(x, z);
            int chestY = groundY + 1;

            // 地表が安全かチェック
            Block ground = chunk.getWorld().getBlockAt(x, groundY, z);
            Block chest = chunk.getWorld().getBlockAt(x, chestY, z);

            if (isSafeGroundBlock(ground) && chest.getType() == Material.AIR) {
                return new Location(chunk.getWorld(), x, chestY, z);
            } else {
                groundRejects++;
            }
        }

        return null;
    }

    /**
     * 安全な地面ブロックかチェック
     */
    private boolean isSafeGroundBlock(Block block) {
        Material type = block.getType();
        return type.isSolid() &&
               type != Material.LAVA &&
               type != Material.WATER &&
               type != Material.CACTUS &&
               type != Material.MAGMA_BLOCK;
    }

    /**
     * 宝箱を配置
     */
    private void spawnChest(Location location) {
        ChestType chestType = ChestType.random();
        Block block = location.getBlock();

        block.setType(chestType.getMaterial());

        // チェストデータを記録
        ChestData data = new ChestData(location, chestType, System.currentTimeMillis());
        activeChests.put(location, data);

        // データベースに保存
        if (currentRoundId != null) {
            saveChestToDatabase(location, chestType, currentRoundId);
        }

        // 宝箱の場合、中身を生成（ブロック設置後すぐに直接インベントリにアクセス）
        if (chestType.isContainer()) {
            fillChestWithLootDirect(location, chestType);
        }

        plugin.getLogger().fine("Spawned " + chestType + " at " + formatLocation(location));
    }

    /**
     * 宝箱に戦利品を入れる（直接インベントリアクセス）
     */
    private void fillChestWithLootDirect(Location location, ChestType chestType) {
        // 次のtickで実行してブロックの初期化を待つ
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block block = location.getBlock();

            // ブロックタイプを確認
            if (!chestType.getMaterial().equals(block.getType())) {
                plugin.getLogger().warning("Block type mismatch at " + formatLocation(location) +
                    " - Expected: " + chestType.getMaterial() + ", Actual: " + block.getType());
                return;
            }

            // BlockStateからインベントリを取得
            org.bukkit.block.BlockState state = block.getState();
            if (!(state instanceof org.bukkit.inventory.InventoryHolder)) {
                plugin.getLogger().warning("Block is not an InventoryHolder at " + formatLocation(location));
                return;
            }

            Inventory inv = ((org.bukkit.inventory.InventoryHolder) state).getInventory();
            inv.clear();

            Random random = new Random();
            int itemCount = Math.min(random.nextInt(4) + 2, inv.getSize()); // 2-5個、ただしインベントリサイズを超えない

            java.util.List<String> itemsAdded = new java.util.ArrayList<>();
            java.util.Set<Integer> usedSlots = new java.util.HashSet<>(); // 使用済みスロットを記録

            // チェストタイプのカテゴリーに応じてアイテムを生成
            switch (chestType.getLootCategory()) {
                case FOOD:
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack food = getRandomFood(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, food);
                            itemsAdded.add(food.getType().name() + "x" + food.getAmount());
                        }
                    }
                    break;

                case POTION:
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack potion = getRandomPotion(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, potion);
                            itemsAdded.add(potion.getType().name() + "x" + potion.getAmount());
                        }
                    }
                    break;

                case EQUIPMENT:
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack equipment = getRandomEquipment(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, equipment);
                            itemsAdded.add(equipment.getType().name() + "x" + equipment.getAmount());
                        }
                    }
                    break;

                case MATERIAL:
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack material = getRandomMaterial(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, material);
                            itemsAdded.add(material.getType().name() + "x" + material.getAmount());
                        }
                    }
                    break;

                case UTILITY:
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack utility = getRandomUtility(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, utility);
                            itemsAdded.add(utility.getType().name() + "x" + utility.getAmount());
                        }
                    }
                    break;

                case TRAP_REWARD:
                    // トラップチェストは高性能装備
                    for (int i = 0; i < itemCount; i++) {
                        ItemStack trapReward = getRandomTrapReward(random);
                        int slot = getAvailableSlot(inv, usedSlots, random);
                        if (slot >= 0) {
                            inv.setItem(slot, trapReward);
                            itemsAdded.add(trapReward.getType().name() + "x" + trapReward.getAmount());
                        }
                    }
                    break;
            }

            // plugin.getLogger().info("Filled " + chestType + " at " + formatLocation(location) +
            //     " with " + itemCount + " items: " + String.join(", ", itemsAdded));

            // 検証
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Block checkBlock = location.getBlock();
                if (checkBlock.getState() instanceof org.bukkit.inventory.InventoryHolder) {
                    Inventory checkInv = ((org.bukkit.inventory.InventoryHolder) checkBlock.getState()).getInventory();
                    int actualItems = 0;
                    for (ItemStack item : checkInv.getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            actualItems++;
                        }
                    }
                    if (actualItems == 0) {
                        plugin.getLogger().severe("VERIFICATION FAILED: Chest at " + formatLocation(location) +
                            " is EMPTY after filling! Expected " + itemCount + " items.");
                    }
                }
            }, 5L);
        });
    }

    /**
     * 宝箱に戦利品を入れる（古いメソッド - 使用されていない）
     */
    private void fillChestWithLoot(Location location, ChestType chestType) {
        Block block = location.getBlock();

        if (!(block.getState() instanceof org.bukkit.block.Container)) {
            plugin.getLogger().warning("Block at " + formatLocation(location) +
                " is not a container: " + block.getType() + " (expected container for " + chestType + ")");
            return;
        }

        // BlockStateを取得
        org.bukkit.block.BlockState state = block.getState();
        if (!(state instanceof org.bukkit.block.Container)) {
            plugin.getLogger().warning("BlockState is not a container at " + formatLocation(location));
            return;
        }

        org.bukkit.block.Container container = (org.bukkit.block.Container) state;
        Inventory inv = container.getInventory();
        inv.clear();

        Random random = new Random();

        // ランダムに食料アイテムを配置
        int itemCount = random.nextInt(4) + 2; // 2-5個
        java.util.List<ItemStack> items = new java.util.ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            ItemStack food = getRandomFood(random);
            items.add(food);
            int slot = random.nextInt(inv.getSize());
            inv.setItem(slot, food);
        }

        // トラップチェストの場合、装備品を追加
        if (chestType.isTrapped()) {
            double equipmentChance = plugin.getConfigManager().getTrappedChestEquipmentChance();
            if (random.nextDouble() < equipmentChance) {
                ItemStack equipment = getRandomEquipment(random);
                int slot = random.nextInt(inv.getSize());
                inv.setItem(slot, equipment);
            }
        }

        // BlockStateを更新して保存
        boolean updated = container.update(true, false);

        if (updated) {
            // plugin.getLogger().info("Filled " + chestType + " at " + formatLocation(location) +
            //     " with " + itemCount + " items: " + items.stream()
            //         .map(item -> item.getType().name() + "x" + item.getAmount())
            //         .collect(java.util.stream.Collectors.joining(", ")));
        } else {
            plugin.getLogger().warning("Failed to update container at " + formatLocation(location));
        }

        // 検証：実際に入っているか確認
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block checkBlock = location.getBlock();
            if (checkBlock.getState() instanceof org.bukkit.block.Container) {
                org.bukkit.block.Container checkContainer = (org.bukkit.block.Container) checkBlock.getState();
                int actualItems = 0;
                for (ItemStack item : checkContainer.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        actualItems++;
                    }
                }
                if (actualItems == 0) {
                    plugin.getLogger().severe("VERIFICATION FAILED: Chest at " + formatLocation(location) +
                        " is EMPTY after filling! Expected " + itemCount + " items.");
                } else {
                    plugin.getLogger().fine("Verified: " + actualItems + " items in chest at " + formatLocation(location));
                }
            }
        }, 5L);
    }

    /**
     * 利用可能なスロットを取得（重複を避ける）
     */
    private int getAvailableSlot(Inventory inv, java.util.Set<Integer> usedSlots, Random random) {
        int invSize = inv.getSize();

        // 全スロットが使用済みの場合
        if (usedSlots.size() >= invSize) {
            return -1;
        }

        // 最大10回試行
        for (int attempt = 0; attempt < 10; attempt++) {
            int slot = random.nextInt(invSize);
            if (!usedSlots.contains(slot)) {
                usedSlots.add(slot);
                return slot;
            }
        }

        // 10回試行しても見つからない場合、未使用スロットを探す
        for (int slot = 0; slot < invSize; slot++) {
            if (!usedSlots.contains(slot)) {
                usedSlots.add(slot);
                return slot;
            }
        }

        return -1; // 利用可能なスロットなし
    }

    /**
     * ランダムな食料アイテムを取得
     * お菓子系を多めに配置
     */
    private ItemStack getRandomFood(Random random) {
        // お菓子系を重視（70%）
        double candyChance = random.nextDouble();

        Material food;
        if (candyChance < 0.7) {
            // お菓子系（70%の確率）
            Material[] candies = {
                Material.COOKIE, Material.COOKIE, Material.COOKIE,  // クッキー高確率
                Material.PUMPKIN_PIE, Material.CAKE,
                Material.SWEET_BERRIES, Material.GLOW_BERRIES,
                Material.GOLDEN_APPLE, Material.GOLDEN_CARROT,
                Material.MELON_SLICE, Material.APPLE
            };
            food = candies[random.nextInt(candies.length)];
        } else {
            // 通常の食料（30%の確率）
            Material[] normalFoods = {
                Material.BREAD, Material.COOKED_BEEF,
                Material.COOKED_CHICKEN, Material.COOKED_MUTTON,
                Material.COOKED_PORKCHOP, Material.COOKED_SALMON,
                Material.BAKED_POTATO, Material.CARROT
            };
            food = normalFoods[random.nextInt(normalFoods.length)];
        }

        int amount = random.nextInt(8) + 1; // 1-8個
        return new ItemStack(food, amount);
    }

    /**
     * ランダムな装備品を取得（鉄・ダイヤ）
     */
    private ItemStack getRandomEquipment(Random random) {
        Material[] equipment = {
            Material.IRON_HELMET, Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.IRON_SWORD, Material.BOW, Material.SHIELD,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.DIAMOND_SWORD
        };

        Material item = equipment[random.nextInt(equipment.length)];
        return new ItemStack(item, 1);
    }

    /**
     * ランダムなポーション・薬系アイテムを取得
     */
    private ItemStack getRandomPotion(Random random) {
        Material[] potions = {
            Material.POTION,           // 通常ポーション
            Material.SPLASH_POTION,    // スプラッシュポーション
            Material.LINGERING_POTION, // 残留ポーション
            Material.HONEY_BOTTLE,     // ハチミツ
            Material.MILK_BUCKET,      // ミルク
            Material.SUSPICIOUS_STEW   // 怪しげなシチュー
        };

        Material item = potions[random.nextInt(potions.length)];
        int amount = 1;

        // ハチミツとシチューは複数個
        if (item == Material.HONEY_BOTTLE || item == Material.SUSPICIOUS_STEW) {
            amount = random.nextInt(3) + 1; // 1-3個
        }

        ItemStack potion = new ItemStack(item, amount);

        // ポーションの場合、効果を追加
        if (item == Material.POTION || item == Material.SPLASH_POTION || item == Material.LINGERING_POTION) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
            if (meta != null) {
                // ランダムなポーション効果
                org.bukkit.potion.PotionType[] types = {
                    org.bukkit.potion.PotionType.HEALING,
                    org.bukkit.potion.PotionType.REGENERATION,
                    org.bukkit.potion.PotionType.SWIFTNESS,
                    org.bukkit.potion.PotionType.STRENGTH,
                    org.bukkit.potion.PotionType.FIRE_RESISTANCE,
                    org.bukkit.potion.PotionType.WATER_BREATHING
                };
                meta.setBasePotionType(types[random.nextInt(types.length)]);
                potion.setItemMeta(meta);
            }
        }

        return potion;
    }

    /**
     * ランダムな素材・燃料系アイテムを取得
     */
    private ItemStack getRandomMaterial(Random random) {
        Material[] materials = {
            Material.COAL, Material.CHARCOAL,
            Material.IRON_INGOT, Material.GOLD_INGOT,
            Material.STICK, Material.STRING,
            Material.FEATHER, Material.LEATHER,
            Material.BONE, Material.GUNPOWDER,
            Material.REDSTONE, Material.GLOWSTONE_DUST,
            Material.BLAZE_POWDER, Material.MAGMA_CREAM
        };

        Material item = materials[random.nextInt(materials.length)];
        int amount = random.nextInt(8) + 4; // 4-11個
        return new ItemStack(item, amount);
    }

    /**
     * ランダムなユーティリティアイテムを取得
     */
    private ItemStack getRandomUtility(Random random) {
        Material[] utilities = {
            Material.ARROW,
            Material.ENDER_PEARL,
            Material.SNOWBALL,
            Material.EGG,
            Material.FISHING_ROD,
            Material.BUCKET,
            Material.WATER_BUCKET,
            Material.TORCH,
            Material.LADDER,
            Material.COMPASS
        };

        Material item = utilities[random.nextInt(utilities.length)];
        int amount = 1;

        // 消耗品は複数個
        if (item == Material.ARROW) {
            amount = random.nextInt(16) + 8; // 8-23本
        } else if (item == Material.SNOWBALL || item == Material.EGG) {
            amount = random.nextInt(16) + 4; // 4-19個
        } else if (item == Material.ENDER_PEARL) {
            amount = random.nextInt(3) + 1; // 1-3個
        } else if (item == Material.TORCH || item == Material.LADDER) {
            amount = random.nextInt(16) + 8; // 8-23個
        }

        return new ItemStack(item, amount);
    }

    /**
     * トラップチェスト用の高性能報酬を取得
     */
    private ItemStack getRandomTrapReward(Random random) {
        Material[] rewards = {
            // ダイヤ装備（高確率）
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
            Material.DIAMOND_AXE,
            // ネザライト装備（低確率）
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.NETHERITE_SWORD,
            // 特殊装備
            Material.ELYTRA, Material.TRIDENT,
            Material.ENCHANTED_GOLDEN_APPLE
        };

        Material item = rewards[random.nextInt(rewards.length)];
        int amount = 1;

        // エンチャントされた金のリンゴは複数個
        if (item == Material.ENCHANTED_GOLDEN_APPLE) {
            amount = random.nextInt(2) + 1; // 1-2個
        }

        ItemStack reward = new ItemStack(item, amount);

        // 装備品にランダムエンチャント追加（50%の確率）
        if (random.nextDouble() < 0.5 && (item.toString().contains("HELMET") ||
            item.toString().contains("CHESTPLATE") || item.toString().contains("LEGGINGS") ||
            item.toString().contains("BOOTS") || item.toString().contains("SWORD") ||
            item.toString().contains("PICKAXE") || item.toString().contains("AXE") ||
            item.toString().contains("TRIDENT"))) {

            org.bukkit.enchantments.Enchantment[] enchantments = {
                org.bukkit.enchantments.Enchantment.PROTECTION,
                org.bukkit.enchantments.Enchantment.SHARPNESS,
                org.bukkit.enchantments.Enchantment.UNBREAKING
            };

            org.bukkit.enchantments.Enchantment ench = enchantments[random.nextInt(enchantments.length)];
            int level = random.nextInt(3) + 1; // レベル1-3

            try {
                reward.addUnsafeEnchantment(ench, level);
            } catch (IllegalArgumentException e) {
                // エンチャント追加失敗は無視
            }
        }

        return reward;
    }

    /**
     * 宝箱が開けられた際の処理
     */
    public void onChestOpened(Location location) {
        ChestData data = activeChests.get(location);
        if (data == null) {
            return;
        }

        // トラップチェストの場合、ダメージを与える処理は別のリスナーで実装

        // リスポーン待ちリストに追加
        pendingRespawn.add(location);
        activeChests.remove(location);

        plugin.getLogger().fine("Chest opened at " + formatLocation(location));
    }

    /**
     * 指定された位置のチェストがゲーム生成チェストかどうかをチェック
     * @param location チェストの位置
     * @return ゲーム生成チェストの場合true、プレイヤー設置の場合false
     */
    public boolean isGameChest(Location location) {
        return activeChests.containsKey(location);
    }

    /**
     * 宝箱リスポーンタスク開始
     */
    private void startRespawnTask() {
        if (respawnTask != null) {
            respawnTask.cancel();
        }

        int respawnDelay = plugin.getConfigManager().getTreasureRespawnDelay();

        respawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Location> toRespawn = new ArrayList<>(pendingRespawn);

            for (Location loc : toRespawn) {
                // 既に別のブロックがある場合はスキップ
                if (loc.getBlock().getType() != Material.AIR) {
                    pendingRespawn.remove(loc);
                    continue;
                }

                // リスポーン
                spawnChest(loc);
                pendingRespawn.remove(loc);

                plugin.getLogger().fine("Respawned chest at " + formatLocation(loc));
            }
        }, respawnDelay * 20L, respawnDelay * 20L);
    }

    /**
     * 既存の宝箱タイプのブロックをクリーンアップ（範囲内）
     * サーバー再起動後にactiveChestsが空でも、物理的なブロックを削除する
     */
    private void cleanupExistingChests(World world, Location center, int radius) {
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        int chunkRadius = radius >> 4;

        int removedCount = 0;

        // ロード済みチャンクのみスキャン
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                // チャンク内の宝箱タイプのブロックを検索
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            Material type = block.getType();

                            // 宝箱タイプのブロックかチェック
                            if (isChestType(type)) {
                                // 中心からの距離チェック
                                Location blockLoc = block.getLocation();
                                double distance = blockLoc.distance(center);
                                if (distance <= radius) {
                                    block.setType(Material.AIR);
                                    removedCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " existing chest blocks in the map");
        }
    }

    /**
     * ブロックが宝箱タイプかチェック
     */
    private boolean isChestType(Material type) {
        return type == Material.CHEST ||
               type == Material.TRAPPED_CHEST ||
               type == Material.BARREL ||
               type == Material.FURNACE ||
               type == Material.BLAST_FURNACE ||
               type == Material.SMOKER ||
               type == Material.BREWING_STAND ||
               type == Material.HOPPER ||
               type == Material.DROPPER ||
               type == Material.DISPENSER;
    }

    /**
     * 全宝箱を削除
     */
    public void removeAllChests() {
        for (Location loc : activeChests.keySet()) {
            Block block = loc.getBlock();
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR);
            }
        }

        activeChests.clear();
        pendingRespawn.clear();

        if (respawnTask != null) {
            respawnTask.cancel();
            respawnTask = null;
        }

        plugin.getLogger().info("All treasure chests removed");
    }

    /**
     * 座標フォーマット
     */
    private String formatLocation(Location loc) {
        return String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * クリーンアップ
     */
    public void shutdown() {
        removeAllChests();
        plugin.getLogger().info("TreasureChestManager shutdown complete");
    }

    /**
     * データベースから古いラウンドの宝箱を削除
     * 新しいゲームが開始されたら、前回のゲームの宝箱を削除する
     * @param newRoundId 新しいゲームラウンドID
     * @param world ワールド
     */
    private void deleteOldChestsFromDatabase(int newRoundId, World world) {
        try (Connection conn = plugin.getDatabaseInitializer().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, world, x, y, z, chest_type FROM treasure_chests WHERE round_id != ? AND world = ?")) {

            stmt.setInt(1, newRoundId);
            stmt.setString(2, world.getName());

            ResultSet rs = stmt.executeQuery();
            List<Location> chestsToDelete = new ArrayList<>();
            List<Integer> dbIdsToDelete = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");

                Location loc = new Location(world, x, y, z);
                chestsToDelete.add(loc);
                dbIdsToDelete.add(id);
            }

            rs.close();

            // 物理ブロックを削除（ロード済みチャンクのみ）
            int deletedBlocks = 0;
            for (Location loc : chestsToDelete) {
                int chunkX = loc.getBlockX() >> 4;
                int chunkZ = loc.getBlockZ() >> 4;

                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    Block block = loc.getBlock();
                    if (isChestType(block.getType())) {
                        block.setType(Material.AIR);
                        deletedBlocks++;
                    }
                }
            }

            // データベースから削除
            if (!dbIdsToDelete.isEmpty()) {
                String placeholders = String.join(",", Collections.nCopies(dbIdsToDelete.size(), "?"));
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM treasure_chests WHERE id IN (" + placeholders + ")")) {

                    for (int i = 0; i < dbIdsToDelete.size(); i++) {
                        deleteStmt.setInt(i + 1, dbIdsToDelete.get(i));
                    }
                    int deleted = deleteStmt.executeUpdate();

                    plugin.getLogger().info("Deleted " + deleted + " old chest records from database (" +
                                          deletedBlocks + " physical blocks removed, " +
                                          (deleted - deletedBlocks) + " in unloaded chunks)");
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete old chests from database", e);
        }
    }

    /**
     * 宝箱をデータベースに保存
     * @param location 宝箱の位置
     * @param chestType 宝箱の種類
     * @param roundId ゲームラウンドID
     */
    private void saveChestToDatabase(Location location, ChestType chestType, int roundId) {
        try (Connection conn = plugin.getDatabaseInitializer().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO treasure_chests (round_id, world, x, y, z, chest_type, spawned_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {

            stmt.setInt(1, roundId);
            stmt.setString(2, location.getWorld().getName());
            stmt.setInt(3, location.getBlockX());
            stmt.setInt(4, location.getBlockY());
            stmt.setInt(5, location.getBlockZ());
            stmt.setString(6, chestType.name());
            stmt.setLong(7, System.currentTimeMillis() / 1000);

            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save chest to database at " + formatLocation(location), e);
        }
    }

    /**
     * チャンクがロードされたときに古い宝箱をクリーンアップ
     * サーバー再起動後、チャンクがロードされたタイミングで前回のゲームの宝箱を削除
     * @param world ワールド
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     */
    public void cleanupChestOnChunkLoad(World world, int chunkX, int chunkZ) {
        if (currentRoundId == null) {
            return; // ゲームが開始されていない
        }

        try (Connection conn = plugin.getDatabaseInitializer().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, x, y, z FROM treasure_chests WHERE round_id != ? AND world = ? AND x >= ? AND x < ? AND z >= ? AND z < ?")) {

            int minX = chunkX << 4;
            int maxX = minX + 16;
            int minZ = chunkZ << 4;
            int maxZ = minZ + 16;

            stmt.setInt(1, currentRoundId);
            stmt.setString(2, world.getName());
            stmt.setInt(3, minX);
            stmt.setInt(4, maxX);
            stmt.setInt(5, minZ);
            stmt.setInt(6, maxZ);

            ResultSet rs = stmt.executeQuery();
            List<Integer> idsToDelete = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");

                Location loc = new Location(world, x, y, z);
                Block block = loc.getBlock();

                // 物理ブロックを削除
                if (isChestType(block.getType())) {
                    block.setType(Material.AIR);
                    plugin.getLogger().fine("Cleaned up old chest at " + formatLocation(loc) + " on chunk load");
                }

                idsToDelete.add(id);
            }

            rs.close();

            // データベースから削除
            if (!idsToDelete.isEmpty()) {
                String placeholders = String.join(",", Collections.nCopies(idsToDelete.size(), "?"));
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM treasure_chests WHERE id IN (" + placeholders + ")")) {

                    for (int i = 0; i < idsToDelete.size(); i++) {
                        deleteStmt.setInt(i + 1, idsToDelete.get(i));
                    }
                    deleteStmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to cleanup chests on chunk load", e);
        }
    }

    /**
     * 宝箱データクラス
     */
    private static class ChestData {
        private final Location location;
        private final ChestType type;
        private final long spawnTime;

        public ChestData(Location location, ChestType type, long spawnTime) {
            this.location = location;
            this.type = type;
            this.spawnTime = spawnTime;
        }

        public Location getLocation() {
            return location;
        }

        public ChestType getType() {
            return type;
        }

        public long getSpawnTime() {
            return spawnTime;
        }
    }
}
