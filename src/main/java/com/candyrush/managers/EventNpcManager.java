package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.NPCEventTier;
import com.candyrush.utils.MessageUtils;
import com.candyrush.utils.NpcNameGenerator;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * イベントNPCの生成と管理を担当
 * NPCに近づくと助けを求め、クリックすると防衛イベントが発生
 */
public class EventNpcManager {

    private final CandyRushPlugin plugin;
    private final Map<UUID, DefenseEvent> activeDefenseEvents;
    private final Map<UUID, NpcData> activeNpcs;  // Entity UUID -> NPC Data
    private final Map<UUID, Long> playerHelpMessageCooldown;  // Player UUID -> Last message time
    private final Map<UUID, Integer> playerDefenseClearCount;  // Player UUID -> Clear count (deprecated)
    private final Map<NpcRespawnData, Long> pendingNpcRespawn;  // NPC respawn location -> respawn time
    private BukkitTask proximityCheckTask;
    private BukkitTask npcRespawnTask;
    private Integer currentRoundId;
    private NpcNameGenerator nameGenerator;
    private final Random random;

    public EventNpcManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.activeDefenseEvents = new ConcurrentHashMap<>();
        this.activeNpcs = new ConcurrentHashMap<>();
        this.playerHelpMessageCooldown = new ConcurrentHashMap<>();
        this.playerDefenseClearCount = new ConcurrentHashMap<>();
        this.pendingNpcRespawn = new ConcurrentHashMap<>();
        this.currentRoundId = null;
        this.random = new Random();
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        // Initialize NPC name generator
        List<String> npcNames = plugin.getConfigManager().getNpcNames();
        if (npcNames.isEmpty()) {
            plugin.getLogger().warning("No NPC names loaded - using fallback");
            npcNames = List.of("太郎", "次郎", "さくら");
        }
        this.nameGenerator = new NpcNameGenerator(npcNames);

        startProximityCheckTask();
        startNpcRespawnTask();
        plugin.getLogger().info("EventNpcManager initialized with " + npcNames.size() + " NPC names");
    }

    /**
     * Randomly select a tier from 1-5 using weighted probability
     * Higher tier weights = more common spawns
     * @return Weighted random tier between 1-5
     */
    private NPCEventTier selectRandomTier() {
        List<NPCEventTier> tiers = plugin.getConfigManager().getEventTiers();

        if (tiers.isEmpty()) {
            plugin.getLogger().severe("No tiers configured! Cannot spawn NPC events");
            return null;
        }

        // Calculate total weight
        int totalWeight = 0;
        for (NPCEventTier tier : tiers) {
            totalWeight += tier.getSpawnWeight();
        }

        // Select random value in range [0, totalWeight)
        int randomValue = random.nextInt(totalWeight);

        // Find the tier corresponding to this random value
        int currentWeight = 0;
        for (NPCEventTier tier : tiers) {
            currentWeight += tier.getSpawnWeight();
            if (randomValue < currentWeight) {
                return tier;
            }
        }

        // Fallback to tier 1 (should never happen)
        plugin.getLogger().warning("Weighted selection failed, falling back to tier 1");
        return plugin.getConfigManager().getEventTier(1);
    }

    /**
     * ゲーム開始時にイベントNPCを配置
     * @param world ワールド
     * @param center 中心座標
     * @param radius 半径
     * @param roundId ゲームラウンドID
     */
    public void spawnEventNpcs(World world, Location center, int radius, Integer roundId) {
        this.currentRoundId = roundId;

        // MythicMobsが利用できない場合はスキップ
        if (plugin.getMythicMobsIntegration() == null) {
            plugin.getLogger().info("Skipping event NPC spawn - MythicMobs not available");
            return;
        }

        activeNpcs.clear();

        // データベースから古いラウンドのNPCを削除
        if (roundId != null) {
            deleteOldNpcsFromDatabase(roundId, world);
        }

        int npcPerChunks = plugin.getConfigManager().getEventNpcPerChunks();

        // マップ内にNPCを配置（非同期）
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        int chunkRadius = radius >> 4;

        // 非同期でNPCをスポーンするための情報を収集
        List<int[]> spawnCoordinates = new ArrayList<>();

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX += npcPerChunks) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ += npcPerChunks) {
                spawnCoordinates.add(new int[]{chunkX << 4, chunkZ << 4});
            }
        }

        plugin.getLogger().info("Starting async NPC spawn for " + spawnCoordinates.size() + " locations");

        // 非同期で座標を探索してスポーン
        spawnNpcsAsync(world, center, radius, spawnCoordinates, 0);
    }

    /**
     * NPCを非同期でスポーン（再帰的に処理）
     */
    private void spawnNpcsAsync(World world, Location center, int radius,
                                List<int[]> coordinates, int index) {
        if (index >= coordinates.size()) {
            plugin.getLogger().info("Spawned " + activeNpcs.size() + " event NPCs in the map");
            return;
        }

        int[] coord = coordinates.get(index);
        int baseX = coord[0];
        int baseZ = coord[1];

        // 座標を探索
        findSafeNpcLocationAsync(world, baseX, baseZ, center, radius, location -> {
            if (location != null) {
                // メインスレッドでスポーン
                Bukkit.getScheduler().runTask(plugin, () -> spawnNpc(location));
            }

            // 次の座標を処理（1tick後）
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                spawnNpcsAsync(world, center, radius, coordinates, index + 1), 1L);
        });
    }

    /**
     * 安全なNPC配置場所を非同期で探す
     */
    private void findSafeNpcLocationAsync(World world, int baseX, int baseZ, Location center,
                                          int radius, java.util.function.Consumer<Location> callback) {
        Random random = new Random();
        int x = baseX + random.nextInt(16);
        int z = baseZ + random.nextInt(16);

        // 中心からの距離チェック
        if (Math.sqrt(Math.pow(x - center.getX(), 2) + Math.pow(z - center.getZ(), 2)) > radius) {
            callback.accept(null);
            return;
        }

        // チャンクを非同期でロード
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (world.isChunkLoaded(chunkX, chunkZ)) {
            // 既にロード済み - 同期処理
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location loc = new Location(world, x + 0.5, y, z + 0.5);

            if (loc.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                callback.accept(loc);
            } else {
                callback.accept(null);
            }
        } else {
            // 非同期でチャンクをロード
            world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
                int y = world.getHighestBlockYAt(x, z) + 1;
                Location loc = new Location(world, x + 0.5, y, z + 0.5);

                if (loc.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                    callback.accept(loc);
                } else {
                    callback.accept(null);
                }
            });
        }
    }

    /**
     * NPCをスポーン (tier-based)
     */
    private void spawnNpc(Location location) {
        // Select random tier
        NPCEventTier selectedTier = selectRandomTier();
        if (selectedTier == null) {
            plugin.getLogger().warning("Failed to select tier for NPC at " + formatLocation(location));
            return;
        }

        String npcType = selectedTier.getNpcMobType();

        // Try to spawn the NPC
        Optional<ActiveMob> spawnResult = plugin.getMythicMobsIntegration().spawnMob(npcType, location, 1);

        // Use final variables for lambda
        NPCEventTier finalTier;
        String finalNpcType;

        if (!spawnResult.isPresent()) {
            // Fallback to tier 1 if mob type doesn't exist
            plugin.getLogger().warning("Failed to spawn " + npcType + ", falling back to tier 1");
            NPCEventTier tier1 = plugin.getConfigManager().getEventTier(1);
            if (tier1 != null) {
                finalTier = tier1;
                finalNpcType = tier1.getNpcMobType();
                spawnResult = plugin.getMythicMobsIntegration().spawnMob(finalNpcType, location, 1);
            } else {
                finalTier = selectedTier;
                finalNpcType = npcType;
            }
        } else {
            finalTier = selectedTier;
            finalNpcType = npcType;
        }

        spawnResult.ifPresent(activeMob -> {
            UUID entityUuid = activeMob.getEntity().getUniqueId();
            Entity npcEntity = activeMob.getEntity().getBukkitEntity();

            // Generate random name with tier formatting
            String formattedName = nameGenerator.getFormattedName(finalTier);

            // Set custom name and make it visible
            npcEntity.setCustomName(MessageUtils.colorize(formattedName));
            npcEntity.setCustomNameVisible(true);

            // Store NPC data with tier and name
            NpcData npcData = new NpcData(entityUuid, location, finalNpcType, finalTier, formattedName);
            activeNpcs.put(entityUuid, npcData);

            // データベースに保存（UUIDを含む）
            if (currentRoundId != null) {
                saveNpcToDatabase(location, finalNpcType, currentRoundId, entityUuid);
            }

            plugin.getLogger().fine("Spawned event NPC (Tier " + finalTier.getTier() + ") at " + formatLocation(location) +
                                  " UUID: " + entityUuid + " Name: " + formattedName);
        });
    }

    /**
     * プレイヤーがNPCをクリックした時の処理
     */
    public void onPlayerClickNpc(Player player, Entity npc) {
        UUID npcUuid = npc.getUniqueId();

        // NPCが登録されているかチェック
        if (!activeNpcs.containsKey(npcUuid)) {
            return;
        }

        // 既に防衛イベント中かチェック
        if (activeDefenseEvents.containsKey(player.getUniqueId())) {
            MessageUtils.sendMessage(player, "&c既に防衛イベント中です！");
            return;
        }

        // NPCデータ取得
        NpcData npcData = activeNpcs.get(npcUuid);

        // 防衛イベント開始
        startDefenseEvent(player, npc, npcData);
    }

    /**
     * 防衛イベントを開始
     */
    private void startDefenseEvent(Player player, Entity npc, NpcData npcData) {
        Location npcLocation = npc.getLocation();

        DefenseEvent event = new DefenseEvent(player, npc, npcLocation, npcData);
        activeDefenseEvents.put(player.getUniqueId(), event);

        // NPCを登録から削除（他のプレイヤーがクリックできないように）
        activeNpcs.remove(npc.getUniqueId());

        // プレイヤーに通知
        MessageUtils.sendMessage(player, "&e&l⚠ 防衛イベント開始！ ⚠");
        MessageUtils.sendMessage(player, "&aモンスターの波を全て倒してNPCを守りきれ！");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        // イベント開始
        event.start();

        plugin.getLogger().info("Defense event started for player: " + player.getName());
    }

    /**
     * 防衛イベントを終了
     */
    private void endDefenseEvent(UUID playerUuid, boolean success, Entity npc) {
        DefenseEvent event = activeDefenseEvents.remove(playerUuid);
        if (event == null) {
            return;
        }

        event.stop();

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        if (success) {
            // 成功 - 報酬を付与
            NPCEventTier tier = event.npcData.getTier();
            int rewardMin = tier != null ? tier.getRewardPointsMin() : plugin.getConfigManager().getRewardPointsMin();
            int rewardMax = tier != null ? tier.getRewardPointsMax() : plugin.getConfigManager().getRewardPointsMax();
            int reward = rewardMin + new Random().nextInt(Math.max(1, rewardMax - rewardMin + 1));

            int bossSpawnPoints = tier != null ? tier.getBossSpawnPoints() : 5;

            plugin.getPlayerManager().addPoints(playerUuid, reward);

            // イベント完了をプレイヤーデータに記録してチームポイント追加
            plugin.getPlayerManager().getPlayerData(playerUuid).ifPresent(data -> {
                data.incrementEventsCompleted();

                // Add boss spawn points based on tier
                data.addBossSpawnPoints(bossSpawnPoints);
                int currentBossPoints = data.getBossSpawnPoints();

                plugin.getPlayerManager().savePlayerData(data);

                // チームポイントも加算
                if (data.getTeamColor() != null) {
                    plugin.getTeamManager().addTeamPoints(data.getTeamColor(), reward);
                }

                plugin.getLogger().info("Player " + player.getName() + " earned " + bossSpawnPoints + " boss spawn points (now: " + currentBossPoints + ")");
            });

            // NPCから感謝のメッセージ
            String tierName = tier != null ? "Lv." + tier.getTier() : "";
            MessageUtils.sendMessage(player, "&a&l✓ 防衛成功！");
            MessageUtils.sendMessage(player, "&e&l[NPC] &aありがとうございます！これを受け取ってください！");
            MessageUtils.sendMessage(player, "&6&l+" + reward + "pt &7報酬を獲得しました！");
            MessageUtils.sendMessage(player, "&d&l+" + bossSpawnPoints + "pt &7ボス召喚ポイント獲得！ " + tierName);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);

            // NPCを削除してリスポーンをスケジュール
            if (npc != null && npc.isValid()) {
                Location npcLocation = npc.getLocation();
                spawnRewardEffects(npcLocation);
                npc.remove();

                // NPCのリスポーンをスケジュール
                scheduleNpcRespawn(npcLocation);
            }

            plugin.getLogger().info("Player " + player.getName() + " completed defense event (Tier " +
                                  (tier != null ? tier.getTier() : "?") + ") - reward: " + reward);

            // ボス出現判定 - Point-based system
            plugin.getPlayerManager().getPlayerData(playerUuid).ifPresent(data -> {
                if (data.canSpawnBoss()) {
                    int threshold = plugin.getConfigManager().getBossSpawnPointThreshold();
                    int currentPoints = data.getBossSpawnPoints();
                    int overflow = currentPoints - threshold;

                    // Spawn boss
                    spawnBossForPlayer(player);

                    // Reset points to overflow
                    data.resetBossSpawnPoints();
                    if (overflow > 0) {
                        data.addBossSpawnPoints(overflow);
                    }

                    plugin.getPlayerManager().savePlayerData(data);

                    MessageUtils.sendMessage(player, "&d&lボス召喚ポイントが100に到達！");
                    if (overflow > 0) {
                        MessageUtils.sendMessage(player, "&7残り " + overflow + "pt は引き継がれました");
                    }

                    plugin.getLogger().info("Boss spawned for " + player.getName() + " - overflow: " + overflow);
                }
            });
        } else {
            // 失敗
            MessageUtils.sendMessage(player, "&c&l✗ 防衛失敗...");
            MessageUtils.sendMessage(player, "&7NPCがモンスターにやられてしまった...");

            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f);

            // NPCを削除してリスポーンをスケジュール
            if (npc != null && npc.isValid()) {
                Location npcLocation = npc.getLocation();
                spawnDeathEffects(npcLocation);
                npc.remove();

                // NPCのリスポーンをスケジュール
                scheduleNpcRespawn(npcLocation);
            }

            plugin.getLogger().info("Player " + player.getName() + " failed defense event");
        }
    }

    /**
     * プレイヤーが離脱した時の処理
     */
    private void onPlayerAbandon(UUID playerUuid, Entity npc) {
        DefenseEvent event = activeDefenseEvents.remove(playerUuid);
        if (event == null) {
            return;
        }

        event.stop();

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            MessageUtils.sendMessage(player, "&c&l防衛エリアから離れすぎました！");
            MessageUtils.sendMessage(player, "&7イベントが中止されました...");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        }

        // NPCを削除してリスポーンをスケジュール
        if (npc != null && npc.isValid()) {
            Location npcLocation = npc.getLocation();
            spawnDeathEffects(npcLocation);
            npc.remove();

            // NPCのリスポーンをスケジュール
            scheduleNpcRespawn(npcLocation);
        }

        plugin.getLogger().info("Player " + (player != null ? player.getName() : playerUuid) + " abandoned defense event");
    }

    /**
     * 報酬エフェクト
     */
    private void spawnRewardEffects(Location loc) {
        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.0f);
    }

    /**
     * 死亡エフェクト
     */
    private void spawnDeathEffects(Location loc) {
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 30, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f);
    }

    /**
     * 近接チェックタスクを開始
     */
    private void startProximityCheckTask() {
        if (proximityCheckTask != null) {
            proximityCheckTask.cancel();
        }

        proximityCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            int proximityRange = plugin.getConfigManager().getProximityRange();
            long currentTime = System.currentTimeMillis();
            int cooldown = plugin.getConfigManager().getHelpMessageCooldown() * 1000;

            // 各NPCについてプレイヤーとの距離をチェック
            for (Map.Entry<UUID, NpcData> entry : activeNpcs.entrySet()) {
                UUID npcUuid = entry.getKey();
                Entity npc = Bukkit.getEntity(npcUuid);

                if (npc == null || !npc.isValid()) {
                    continue;
                }

                Location npcLoc = npc.getLocation();

                // 近くのプレイヤーを探す
                for (Player player : npcLoc.getWorld().getPlayers()) {
                    if (player.getLocation().distance(npcLoc) <= proximityRange) {
                        // クールダウンチェック
                        Long lastMessage = playerHelpMessageCooldown.get(player.getUniqueId());
                        if (lastMessage == null || (currentTime - lastMessage) >= cooldown) {
                            // 助けメッセージを表示
                            MessageUtils.sendActionBar(player, "&e&l[NPC] &c助けてください！モンスターに襲われています！");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_HURT, 0.5f, 1.0f);

                            playerHelpMessageCooldown.put(player.getUniqueId(), currentTime);
                        }
                    }
                }
            }
        }, 20L, 20L); // 1秒ごとにチェック
    }

    /**
     * NPCリスポーンタスクを開始
     */
    private void startNpcRespawnTask() {
        if (npcRespawnTask != null) {
            npcRespawnTask.cancel();
        }

        npcRespawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getGameManager().isGameRunning()) {
                return;
            }

            long currentTime = System.currentTimeMillis();
            List<NpcRespawnData> toRespawn = new ArrayList<>();

            // リスポーン時間が来たNPCをチェック
            for (Map.Entry<NpcRespawnData, Long> entry : pendingNpcRespawn.entrySet()) {
                if (currentTime >= entry.getValue()) {
                    toRespawn.add(entry.getKey());
                }
            }

            // NPCをリスポーン
            for (NpcRespawnData data : toRespawn) {
                Location loc = data.getLocation();

                // チャンクがロードされているかチェック
                int chunkX = loc.getBlockX() >> 4;
                int chunkZ = loc.getBlockZ() >> 4;

                if (loc.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    // NPCをスポーン (tier will be randomly selected)
                    spawnNpc(loc);
                    pendingNpcRespawn.remove(data);

                    plugin.getLogger().fine("Respawned NPC at " + formatLocation(loc));
                }
            }
        }, 20L, 20L); // 1秒ごとにチェック
    }

    /**
     * NPCをリスポーン待ちリストに追加
     */
    private void scheduleNpcRespawn(Location location) {
        // リスポーン遅延時間を取得（秒）
        int respawnDelay = plugin.getConfigManager().getNpcRespawnDelay();
        long respawnTime = System.currentTimeMillis() + (respawnDelay * 1000L);

        NpcRespawnData data = new NpcRespawnData(location.clone());
        pendingNpcRespawn.put(data, respawnTime);

        plugin.getLogger().fine("Scheduled NPC respawn at " + formatLocation(location) +
                              " in " + respawnDelay + " seconds");
    }

    /**
     * プレイヤーの近くにボスを召喚
     */
    private void spawnBossForPlayer(Player player) {
        Location bossLocation = player.getLocation().add(0, 0, 10); // プレイヤーの前方10ブロック

        // 全プレイヤーに通知
        Bukkit.broadcastMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() +
            "&6&l★★★ お菓子の王出現！ ★★★"));
        Bukkit.broadcastMessage(MessageUtils.colorize(
            "&e" + player.getName() + " &aが防衛イベントを" +
            plugin.getConfigManager().getBossSpawnThreshold() + "回クリアしました！"));
        Bukkit.broadcastMessage(MessageUtils.colorize(
            "&d&lお菓子の王 シュガーロード &6&lが降臨しました！"));

        // サウンド再生
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            MessageUtils.sendTitle(p, "&6&l★ &d&lお菓子の王出現！ &6&l★", "&e討伐するとポイント獲得");
        }

        // ボスを召喚
        plugin.getBossManager().spawnBoss(bossLocation);

        plugin.getLogger().info("Boss spawned for player: " + player.getName() +
                              " at " + formatLocation(bossLocation));
    }

    /**
     * 全NPCを削除
     */
    public void removeAllNpcs() {
        // すべての防衛イベントを終了
        for (UUID playerUuid : new HashSet<>(activeDefenseEvents.keySet())) {
            DefenseEvent event = activeDefenseEvents.remove(playerUuid);
            if (event != null) {
                event.stop();
            }
        }

        // 全NPCエンティティを削除
        for (UUID npcUuid : new HashSet<>(activeNpcs.keySet())) {
            Entity npc = Bukkit.getEntity(npcUuid);
            if (npc != null && npc.isValid()) {
                npc.remove();
            }
        }

        activeNpcs.clear();
        playerHelpMessageCooldown.clear();
        playerDefenseClearCount.clear();
        pendingNpcRespawn.clear();

        plugin.getLogger().info("All event NPCs removed");
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
        removeAllNpcs();

        if (proximityCheckTask != null) {
            proximityCheckTask.cancel();
            proximityCheckTask = null;
        }

        if (npcRespawnTask != null) {
            npcRespawnTask.cancel();
            npcRespawnTask = null;
        }

        plugin.getLogger().info("EventNpcManager shutdown complete");
    }

    /**
     * データベースから古いラウンドのNPCを削除
     */
    private void deleteOldNpcsFromDatabase(int newRoundId, World world) {
        try (Connection conn = plugin.getDatabaseInitializer().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, entity_uuid FROM event_npcs WHERE round_id != ? AND world = ?")) {

            stmt.setInt(1, newRoundId);
            stmt.setString(2, world.getName());

            ResultSet rs = stmt.executeQuery();
            List<Integer> dbIdsToDelete = new ArrayList<>();
            int deletedEntities = 0;

            while (rs.next()) {
                int id = rs.getInt("id");
                String uuidString = rs.getString("entity_uuid");

                // UUIDを使ってエンティティを削除
                if (uuidString != null) {
                    try {
                        UUID entityUuid = UUID.fromString(uuidString);
                        Entity entity = Bukkit.getEntity(entityUuid);

                        if (entity != null) {
                            entity.remove();
                            deletedEntities++;
                            plugin.getLogger().fine("Removed old NPC entity: " + entityUuid);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in database: " + uuidString);
                    }
                }

                dbIdsToDelete.add(id);
            }

            rs.close();

            // データベースから削除
            if (!dbIdsToDelete.isEmpty()) {
                String placeholders = String.join(",", Collections.nCopies(dbIdsToDelete.size(), "?"));
                try (PreparedStatement deleteStmt = conn.prepareStatement(
                        "DELETE FROM event_npcs WHERE id IN (" + placeholders + ")")) {

                    for (int i = 0; i < dbIdsToDelete.size(); i++) {
                        deleteStmt.setInt(i + 1, dbIdsToDelete.get(i));
                    }
                    int deleted = deleteStmt.executeUpdate();

                    plugin.getLogger().info("Deleted " + deleted + " old NPC records from database (" +
                                          deletedEntities + " entities removed)");
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete old NPCs from database", e);
        }
    }

    /**
     * NPCをデータベースに保存
     */
    private void saveNpcToDatabase(Location location, String npcType, int roundId, UUID entityUuid) {
        try (Connection conn = plugin.getDatabaseInitializer().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO event_npcs (round_id, entity_uuid, world, x, y, z, yaw, pitch, npc_type, spawned_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            stmt.setInt(1, roundId);
            stmt.setString(2, entityUuid.toString());
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setFloat(7, location.getYaw());
            stmt.setFloat(8, location.getPitch());
            stmt.setString(9, npcType);
            stmt.setLong(10, System.currentTimeMillis() / 1000);

            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save NPC to database at " + formatLocation(location), e);
        }
    }

    /**
     * NPCデータクラス
     */
    private static class NpcData {
        private final UUID entityUuid;
        private final Location spawnLocation;
        private final String npcType;
        private final NPCEventTier tier;
        private final String npcName;

        public NpcData(UUID entityUuid, Location spawnLocation, String npcType, NPCEventTier tier, String npcName) {
            this.entityUuid = entityUuid;
            this.spawnLocation = spawnLocation;
            this.npcType = npcType;
            this.tier = tier;
            this.npcName = npcName;
        }

        // Legacy constructor for backward compatibility
        public NpcData(UUID entityUuid, Location spawnLocation, String npcType) {
            this(entityUuid, spawnLocation, npcType, null, null);
        }

        public UUID getEntityUuid() {
            return entityUuid;
        }

        public Location getSpawnLocation() {
            return spawnLocation;
        }

        public String getNpcType() {
            return npcType;
        }

        public NPCEventTier getTier() {
            return tier;
        }

        public String getNpcName() {
            return npcName;
        }
    }

    /**
     * NPCリスポーンデータクラス
     */
    private static class NpcRespawnData {
        private final Location location;

        public NpcRespawnData(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NpcRespawnData that = (NpcRespawnData) o;
            return location.equals(that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }
    }

    /**
     * 防衛イベントクラス
     */
    private class DefenseEvent {
        private final Player player;
        private final Entity npc;
        private final Location npcLocation;
        private final NpcData npcData;
        private final List<UUID> spawnedMonsters;
        private final int totalWaves;
        private final int monstersPerWave;
        private final int waveInterval;
        private final int totalDuration;
        private int currentWave;
        private int elapsedSeconds;
        private BukkitTask task;
        private boolean distanceWarningShown;

        public DefenseEvent(Player player, Entity npc, Location npcLocation, NpcData npcData) {
            this.player = player;
            this.npc = npc;
            this.npcLocation = npcLocation;
            this.npcData = npcData;
            this.spawnedMonsters = new ArrayList<>();

            // Use tier-specific parameters if available, otherwise fallback to global config
            NPCEventTier tier = npcData != null ? npcData.getTier() : null;
            this.totalWaves = tier != null ? tier.getMonsterWaves() : plugin.getConfigManager().getMonsterWaves();
            this.monstersPerWave = tier != null ? tier.getMonstersPerWave() : plugin.getConfigManager().getMonstersPerWave();
            this.waveInterval = tier != null ? tier.getWaveIntervalSeconds() : plugin.getConfigManager().getWaveIntervalSeconds();
            this.totalDuration = tier != null ? tier.getDefenseDurationSeconds() : plugin.getConfigManager().getDefenseDurationSeconds();

            this.currentWave = 0;
            this.elapsedSeconds = 0;
            this.distanceWarningShown = false;
        }

        public void start() {
            // 最初の波をスポーン
            spawnWave();

            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                elapsedSeconds++;

                // プレイヤーがオフラインになった場合
                if (!player.isOnline()) {
                    onPlayerAbandon(player.getUniqueId(), npc);
                    return;
                }

                // NPCが死んだ場合
                if (npc == null || !npc.isValid()) {
                    endDefenseEvent(player.getUniqueId(), false, npc);
                    return;
                }

                // プレイヤーとNPCの距離をチェック
                // ハメ技防止: NPCの近くで戦う必要がある
                double distance = player.getLocation().distance(npcLocation);

                // 警告ゾーン（15-20ブロック）
                if (distance > 15 && distance <= 20) {
                    if (!distanceWarningShown) {
                        MessageUtils.sendMessage(player, "&e&l⚠ 警告: NPCから離れすぎています！");
                        MessageUtils.sendMessage(player, "&c20ブロック以上離れるとイベントが中止されます！");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        distanceWarningShown = true;
                    }
                } else if (distance <= 15) {
                    // プレイヤーが戻ってきた場合は警告をリセット
                    distanceWarningShown = false;
                } else if (distance > 20) {
                    // 20ブロック以上離れた場合はイベント中止
                    onPlayerAbandon(player.getUniqueId(), npc);
                    return;
                }

                // 死んだモンスターを削除
                spawnedMonsters.removeIf(uuid -> {
                    Entity entity = Bukkit.getEntity(uuid);
                    return entity == null || !entity.isValid();
                });

                // モンスターをNPC周辺に留める（16ブロック以上離れたら戻す）
                for (UUID monsterUuid : spawnedMonsters) {
                    Entity monster = Bukkit.getEntity(monsterUuid);
                    if (monster != null && monster.isValid()) {
                        double monsterDistance = monster.getLocation().distance(npcLocation);
                        if (monsterDistance > 16.0) {
                            // NPCの周辺にランダムにテレポート（5-8ブロック範囲）
                            Random random = new Random();
                            double angle = random.nextDouble() * Math.PI * 2;
                            double teleportDistance = 5 + random.nextDouble() * 3;
                            double x = npcLocation.getX() + Math.cos(angle) * teleportDistance;
                            double z = npcLocation.getZ() + Math.sin(angle) * teleportDistance;
                            int y = npcLocation.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;

                            Location teleportLoc = new Location(npcLocation.getWorld(), x, y, z);
                            monster.teleport(teleportLoc);

                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Monster teleported back to NPC - was " +
                                    String.format("%.1f", monsterDistance) + " blocks away");
                            }
                        }
                    }
                }

                // 次の波をスポーンするタイミングか
                if (elapsedSeconds % waveInterval == 0 && currentWave < totalWaves) {
                    spawnWave();
                }

                // 残り時間表示
                int remaining = totalDuration - elapsedSeconds;
                if (remaining > 0) {
                    MessageUtils.sendActionBar(player,
                        "&e&l防衛中！ &7波: &a" + currentWave + "&7/&a" + totalWaves +
                        " &7| 残り: &c" + remaining + "秒 &7| モンスター: &c" + spawnedMonsters.size());
                }

                // 全ての波が終わり、全てのモンスターを倒した場合
                if (currentWave >= totalWaves && spawnedMonsters.isEmpty()) {
                    endDefenseEvent(player.getUniqueId(), true, npc);
                    return;
                }

                // タイムアップ
                if (elapsedSeconds >= totalDuration) {
                    endDefenseEvent(player.getUniqueId(), false, npc);
                }
            }, 0L, 20L); // 1秒ごと
        }

        private void spawnWave() {
            currentWave++;

            // 最終波かどうか
            boolean isFinalWave = (currentWave == totalWaves);

            if (isFinalWave) {
                MessageUtils.sendMessage(player, "&4&l⚠ 最終波！エリートモンスターが出現！ ⚠");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
            } else {
                MessageUtils.sendMessage(player, "&c&l⚠ 第" + currentWave + "波のモンスターが出現！");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            }

            // Get monsters from tier configuration, fallback to config if tier not available
            List<String> mobTypes;
            if (npcData != null && npcData.getTier() != null) {
                mobTypes = npcData.getTier().getMonsters();
            } else {
                mobTypes = plugin.getConfigManager().getDefenseMobs();
            }

            List<String> eliteMobTypes = plugin.getConfigManager().getDefenseEliteMobs();

            if (mobTypes.isEmpty()) {
                plugin.getLogger().warning("No defense mobs configured!");
                return;
            }

            plugin.getLogger().info("Spawning wave " + currentWave + " - Configured mobs: " + mobTypes);
            plugin.getLogger().info("Monsters per wave: " + monstersPerWave);

            Random random = new Random();
            int spawnedCount = 0;

            for (int i = 0; i < monstersPerWave; i++) {
                // NPC周辺にランダムにスポーン（5-10ブロック範囲）
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 5 + random.nextDouble() * 5;
                double x = npcLocation.getX() + Math.cos(angle) * distance;
                double z = npcLocation.getZ() + Math.sin(angle) * distance;
                int y = npcLocation.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;

                Location spawnLoc = new Location(npcLocation.getWorld(), x, y, z);

                // モンスタータイプを選択（ランダム）
                String mobType;
                if (isFinalWave && !eliteMobTypes.isEmpty() && random.nextDouble() < 0.7) {
                    // 最終波は70%の確率でエリートモブ
                    mobType = eliteMobTypes.get(random.nextInt(eliteMobTypes.size()));
                    plugin.getLogger().info("Selected elite mob type: " + mobType);
                } else {
                    // 通常モブからランダム選択
                    mobType = mobTypes.get(random.nextInt(mobTypes.size()));
                    plugin.getLogger().info("Selected normal mob type: " + mobType);
                }

                plugin.getLogger().info("Attempting to spawn " + mobType + " at " +
                                      String.format("%.1f, %.1f, %.1f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));

                // MythicMobsでスポーン
                Optional<ActiveMob> spawnedMob = plugin.getMythicMobsIntegration().spawnMob(mobType, spawnLoc, 1);

                if (spawnedMob.isPresent()) {
                    ActiveMob activeMob = spawnedMob.get();
                    UUID mobUuid = activeMob.getEntity().getUniqueId();
                    spawnedMonsters.add(mobUuid);
                    spawnedCount++;

                    plugin.getLogger().info("Successfully spawned " + mobType + " with UUID: " + mobUuid);

                    // スポーンエフェクト（エリートは派手に）
                    if (mobType.startsWith("Elite")) {
                        spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 30, 0.5, 0.5, 0.5, 0.1);
                        spawnLoc.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 10, 0.3, 0.3, 0.3, 0.05);
                    } else {
                        spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 20, 0.5, 0.5, 0.5, 0.05);
                    }
                } else {
                    plugin.getLogger().warning("Failed to spawn " + mobType + " - MythicMobs returned empty");
                }
            }

            plugin.getLogger().info("Wave " + currentWave + " spawning complete - Spawned " + spawnedCount + " / " + monstersPerWave + " monsters");
        }

        public void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }

            // スポーンしたモンスターを削除
            for (UUID uuid : spawnedMonsters) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
            }
            spawnedMonsters.clear();
        }
    }
}
