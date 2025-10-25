package com.candyrush.managers;

import com.candyrush.CandyRushPlugin;
import com.candyrush.models.GameRound;
import com.candyrush.models.GameState;
import com.candyrush.models.Team;
import com.candyrush.models.TeamColor;
import com.candyrush.utils.LanguageManager;
import com.candyrush.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * ゲーム全体のライフサイクルと状態を管理するマネージャー
 * WAITING → COUNTDOWN → RUNNING → COOLDOWN のサイクルを制御
 */
public class GameManager {

    private final CandyRushPlugin plugin;
    private final LanguageManager lang;
    private GameState currentState;
    private GameRound currentRound;
    private BukkitTask countdownTask;
    private BukkitTask gameTimerTask;
    private BukkitTask cooldownTask;
    private BukkitTask timeCheckTask;
    private int countdownSeconds;
    private int gameTimeRemaining;
    private int cooldownSecondsRemaining;

    public GameManager(CandyRushPlugin plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.currentState = GameState.WAITING;
        this.currentRound = null;
    }

    /**
     * マネージャーを初期化
     */
    public void initialize() {
        this.currentState = GameState.WAITING;

        // プラグイン起動時にワールドボーダーをリセット
        resetWorldBorder();

        // すべてのタスクをキャンセル
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }
        if (cooldownTask != null) {
            cooldownTask.cancel();
            cooldownTask = null;
        }
        if (timeCheckTask != null) {
            timeCheckTask.cancel();
            timeCheckTask = null;
        }

        plugin.getLogger().info("GameManager initialized - State: WAITING");
    }

    /**
     * 現在のゲーム状態を取得
     */
    public GameState getCurrentState() {
        return currentState;
    }

    /**
     * 現在のゲームラウンドを取得
     */
    public GameRound getCurrentRound() {
        return currentRound;
    }

    /**
     * ゲームが進行中かチェック
     */
    public boolean isGameRunning() {
        return currentState == GameState.RUNNING;
    }

    /**
     * プレイヤーが参加可能かチェック
     */
    public boolean canJoinGame() {
        return currentState.canJoin();
    }

    /**
     * ゲームを開始できるかチェック（最低人数の確認）
     */
    public boolean canStartGame() {
        if (!currentState.canStart()) {
            return false;
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int minPlayers = plugin.getConfigManager().getMinPlayers();

        return onlinePlayers >= minPlayers;
    }

    /**
     * カウントダウン開始を試行
     */
    public void tryStartCountdown() {
        plugin.getLogger().info("tryStartCountdown called - Current state: " + currentState);

        if (currentState != GameState.WAITING) {
            plugin.getLogger().info("Cannot start countdown - not in WAITING state (current: " + currentState + ")");
            return;
        }

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int minPlayers = plugin.getConfigManager().getMinPlayers();
        plugin.getLogger().info("Player check - Online: " + onlinePlayers + ", Required: " + minPlayers);

        if (!canStartGame()) {
            plugin.getLogger().info("Cannot start game - insufficient players");
            return;
        }

        plugin.getLogger().info("Starting countdown!");
        startCountdown();
    }

    /**
     * カウントダウン開始
     */
    private void startCountdown() {
        currentState = GameState.COUNTDOWN;
        countdownSeconds = plugin.getConfigManager().getCountdownSeconds();

        // 全プレイヤーに通知
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", String.valueOf(countdownSeconds));
        Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.countdown_start", placeholders));

        // カウントダウンタスク開始
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSeconds <= 0) {
                countdownTask.cancel();
                startGame();
                return;
            }

            // カウントダウン表示
            if (countdownSeconds <= 5) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Map<String, String> titlePlaceholders = new HashMap<>();
                    titlePlaceholders.put("seconds", String.valueOf(countdownSeconds));
                    MessageUtils.sendTitle(player,
                        MessageUtils.formatCountdown(countdownSeconds),
                        lang.getMessage("game.countdown_start", titlePlaceholders));
                }
            }

            if (countdownSeconds == 10 || countdownSeconds == 5 || countdownSeconds <= 3) {
                Map<String, String> msgPlaceholders = new HashMap<>();
                msgPlaceholders.put("seconds", String.valueOf(countdownSeconds));
                Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.countdown_start", msgPlaceholders));
            }

            countdownSeconds--;
        }, 0L, 20L); // 1秒ごと

        plugin.getLogger().info("Countdown started: " + countdownSeconds + " seconds");
    }

    /**
     * カウントダウンをキャンセル（人数不足の場合）
     */
    public void cancelCountdown() {
        if (currentState != GameState.COUNTDOWN) {
            return;
        }

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        currentState = GameState.WAITING;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("min", String.valueOf(plugin.getConfigManager().getMinPlayers()));
        Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.insufficient_players", placeholders));

        plugin.getLogger().info("Countdown cancelled - insufficient players");
    }

    /**
     * ゲーム開始
     */
    private void startGame() {
        currentState = GameState.RUNNING;

        int playerCount = Bukkit.getOnlinePlayers().size();
        GameRound tempRound = new GameRound(playerCount);

        // データベースに保存してIDを取得
        try {
            int roundId = plugin.getGameStateStorage().createGameRound(tempRound);
            plugin.getLogger().info("Game round created with ID: " + roundId);

            // IDを持つ新しいインスタンスを作成
            currentRound = new GameRound(
                roundId,
                tempRound.getStartedAt(),
                tempRound.getEndedAt(),
                tempRound.getWinnerTeam(),
                tempRound.getTotalPlayers(),
                tempRound.getDurationSeconds(),
                tempRound.getCreatedAt()
            );
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save game round", e);
            currentRound = tempRound; // Fallback to temp instance without ID
        }

        // プレイヤーをチームに振り分け
        plugin.getTeamManager().distributePlayersEvenly(new java.util.ArrayList<>(Bukkit.getOnlinePlayers()));

        // ワールド設定
        org.bukkit.World world = Bukkit.getWorlds().get(0); // メインワールド
        int mapRadius = plugin.getConfigManager().getMapRadius();

        // マップ中心座標を取得（設定がある場合は固定座標、なければワールドスポーン）
        org.bukkit.Location centerLocation = getMapCenterLocation(world);
        plugin.getLogger().info("Map center: X=" + centerLocation.getBlockX() + ", Z=" + centerLocation.getBlockZ());

        // チーム拠点にコンクリートを配置してテレポート（サーバー負荷を考慮して遅延処理）
        setupTeamBasesAndTeleport(world, centerLocation, mapRadius);

        // 天候と時間を設定
        setupWorldConditions(world);

        // 宝箱を配置（roundIdを渡してデータベース管理）
        plugin.getTreasureChestManager().spawnTreasureChests(world, centerLocation, mapRadius, currentRound.getId());

        // イベントNPCを配置（roundIdを渡してデータベース管理）
        plugin.getEventNpcManager().spawnEventNpcs(world, centerLocation, mapRadius, currentRound.getId());

        // 全プレイヤーに通知とショップアイテムを付与
        Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.game_start"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendTitle(player, lang.getMessage("game.game_start"), "&e");

            // プレイヤーの状態を完全リセット
            player.setHealth(player.getMaxHealth()); // 体力を最大に
            player.setFoodLevel(20); // 空腹度を最大に
            player.setSaturation(20.0f); // 満腹度も最大に

            // インベントリを完全クリア
            player.getInventory().clear();

            // 装備を完全クリア
            player.getInventory().setHelmet(null);
            player.getInventory().setChestplate(null);
            player.getInventory().setLeggings(null);
            player.getInventory().setBoots(null);

            // ポイントをリセット
            plugin.getPlayerManager().getPlayerData(player.getUniqueId()).ifPresent(data -> {
                data.setPoints(0);
                plugin.getPlayerManager().savePlayerData(data);
            });

            // Murdererステータスをクリア（これでnormalチームに戻る）
            plugin.getPlayerManager().clearMurderer(player.getUniqueId());

            // ショップアイテムを付与（スロット9番目）
            plugin.getShopManager().giveShopItem(player);

            // ネームタグを常に表示
            player.setCanPickupItems(true);
        }

        // すべてのプレイヤーのネームタグを常に表示に設定
        setupNameTagVisibility();

        // ゲームタイマー開始
        int gameDuration = plugin.getConfigManager().getGameDurationMinutes() * 60;
        gameTimeRemaining = gameDuration;

        gameTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            gameTimeRemaining--;

            // 残り時間の通知（特定のタイミング）
            if (gameTimeRemaining == 600 || gameTimeRemaining == 300 ||
                gameTimeRemaining == 60 || gameTimeRemaining == 30 || gameTimeRemaining == 10) {

                Bukkit.broadcastMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() +
                    "&e残り時間: &c" + MessageUtils.formatTime(gameTimeRemaining)));
            }

            // アクションバーで常に表示
            if (gameTimeRemaining % 10 == 0 || gameTimeRemaining <= 10) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    MessageUtils.sendActionBar(player,
                        "&e残り時間: &c" + MessageUtils.formatTime(gameTimeRemaining));
                }
            }

            // 時間切れ
            if (gameTimeRemaining <= 0) {
                gameTimerTask.cancel();
                endGame();
            }
        }, 20L, 20L); // 1秒ごと

        plugin.getLogger().info("Game started with " + playerCount + " players");
    }

    /**
     * ゲーム終了
     */
    public void endGame() {
        if (currentState != GameState.RUNNING) {
            return;
        }

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        // 時間チェックタスクを停止
        stopTimeCheckTask();

        // 勝者チーム判定（TeamManagerが実装されたら追加）
        TeamColor winnerTeam = determineWinner();

        if (currentRound != null) {
            // ゲーム終了情報を設定した新しいインスタンスを作成
            long now = System.currentTimeMillis() / 1000;
            int duration = (int) (now - currentRound.getStartedAt());

            GameRound endedRound = new GameRound(
                currentRound.getId(),
                currentRound.getStartedAt(),
                now,  // endedAt
                winnerTeam,
                currentRound.getTotalPlayers(),
                duration,
                currentRound.getCreatedAt()
            );

            // データベース更新
            try {
                plugin.getGameStateStorage().updateGameRound(endedRound);
                plugin.getLogger().info("Game round ended and saved");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update game round", e);
            }

            currentRound = endedRound;
        }

        // 結果発表
        announceResults(winnerTeam);

        // クリーンアップ
        cleanupGame();

        // クールダウン開始
        startCooldown();

        plugin.getLogger().info("Game ended - Winner: " + (winnerTeam != null ? winnerTeam : "None"));
    }

    /**
     * ゲーム終了時のクリーンアップ
     */
    private void cleanupGame() {
        // 宝箱を削除
        plugin.getTreasureChestManager().removeAllChests();

        // イベントNPCを削除
        plugin.getEventNpcManager().removeAllNpcs();

        // ボスを削除
        plugin.getBossManager().removeAllBosses();

        // チームをリセット
        plugin.getTeamManager().resetAllTeams();

        plugin.getLogger().info("Game cleanup completed");
    }

    /**
     * 勝者チームを判定
     */
    private TeamColor determineWinner() {
        TeamManager teamManager = plugin.getTeamManager();
        if (teamManager == null) {
            return null;
        }

        // 引き分けチェック
        if (teamManager.isTie()) {
            return null;
        }

        // 最高ポイントのチームを返す
        return teamManager.getWinningTeam().orElse(null);
    }

    /**
     * ゲーム結果を発表
     */
    private void announceResults(TeamColor winnerTeam) {
        Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.game_end"));

        if (winnerTeam != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("team", winnerTeam.getFormattedName());
            Bukkit.broadcastMessage(lang.getMessageWithPrefix("game.first_place", placeholders));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (winnerTeam != null) {
                MessageUtils.sendTitle(player,
                    lang.getMessage("game.game_end"),
                    winnerTeam.getFormattedName());
            } else {
                MessageUtils.sendTitle(player, lang.getMessage("game.game_end"), "");
            }
        }
    }

    /**
     * クールダウン開始
     */
    private void startCooldown() {
        currentState = GameState.COOLDOWN;
        cooldownSecondsRemaining = plugin.getConfigManager().getCooldownMinutes() * 60;

        Bukkit.broadcastMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() +
            "&e次のゲームまで &c" + plugin.getConfigManager().getCooldownMinutes() + "分 &eお待ちください"));

        cooldownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cooldownSecondsRemaining--;

            // クールダウン終了
            if (cooldownSecondsRemaining <= 0) {
                cooldownTask.cancel();
                endCooldown();
            }
        }, 20L, 20L);

        plugin.getLogger().info("Cooldown started: " + plugin.getConfigManager().getCooldownMinutes() + " minutes");
    }

    /**
     * クールダウン終了
     */
    private void endCooldown() {
        currentState = GameState.WAITING;
        currentRound = null;

        Bukkit.broadcastMessage(MessageUtils.colorize(
            plugin.getConfigManager().getPrefix() +
            "&aクールダウンが終了しました！"));

        // 最低人数がいれば自動的にカウントダウン開始
        tryStartCountdown();

        plugin.getLogger().info("Cooldown ended - State: WAITING");
    }

    /**
     * 残りゲーム時間を取得（秒）
     */
    public int getGameTimeRemaining() {
        return gameTimeRemaining;
    }

    /**
     * 残りクールダウン時間を取得（秒）
     */
    public int getCooldownSecondsRemaining() {
        return cooldownSecondsRemaining;
    }

    /**
     * ワールドの天候と時間を設定
     */
    /**
     * マップの中心座標を取得
     * 設定で固定されている場合はその座標、なければランダムに構造物を選択
     */
    private org.bukkit.Location getMapCenterLocation(org.bukkit.World world) {
        Integer centerX = plugin.getConfigManager().getMapCenterX();
        Integer centerZ = plugin.getConfigManager().getMapCenterZ();

        if (centerX != null && centerZ != null) {
            // 設定で固定座標が指定されている場合
            int y = world.getHighestBlockYAt(centerX, centerZ);
            plugin.getLogger().info("Using configured map center: X=" + centerX + ", Z=" + centerZ);
            return new org.bukkit.Location(world, centerX, y, centerZ);
        } else {
            // ランダムに地上の構造物を選択
            plugin.getLogger().info("Searching for nearby structures to use as map center...");
            Location structureLocation = findRandomStructure(world);

            if (structureLocation != null) {
                plugin.getLogger().info("Using random structure as map center: X=" + structureLocation.getBlockX() + ", Z=" + structureLocation.getBlockZ());
                return structureLocation;
            } else {
                // 構造物が見つからない場合はワールドスポーン
                org.bukkit.Location spawn = world.getSpawnLocation();
                plugin.getLogger().warning("No structures found nearby, using world spawn as map center: X=" + spawn.getBlockX() + ", Z=" + spawn.getBlockZ());
                return spawn;
            }
        }
    }

    /**
     * ランダムに地上の構造物を検索して座標を返す
     * 近い場所から複数見つけてランダムに選択
     */
    private Location findRandomStructure(World world) {
        Location spawnLocation = world.getSpawnLocation();
        int searchRadius = 2000; // 2000ブロック範囲で検索（サーバー負荷を考慮）

        // 検索対象の構造物タイプ（地上の構造物のみ）
        List<StructureType> targetStructures = new ArrayList<>();

        // Minecraft 1.21で利用可能な地上構造物
        targetStructures.add(StructureType.VILLAGE);
        targetStructures.add(StructureType.PILLAGER_OUTPOST);
        targetStructures.add(StructureType.DESERT_PYRAMID);
        targetStructures.add(StructureType.JUNGLE_PYRAMID);
        targetStructures.add(StructureType.SWAMP_HUT);
        targetStructures.add(StructureType.IGLOO);

        if (targetStructures.isEmpty()) {
            plugin.getLogger().warning("No structure types available for search");
            return null;
        }

        // 複数の構造物を見つける
        List<Location> foundStructures = new ArrayList<>();

        for (StructureType structureType : targetStructures) {
            try {
                Location found = world.locateNearestStructure(spawnLocation, structureType, searchRadius, false);
                if (found != null) {
                    // 地上のY座標を取得
                    int x = found.getBlockX();
                    int z = found.getBlockZ();
                    int y = world.getHighestBlockYAt(x, z);

                    Location surfaceLocation = new Location(world, x, y, z);
                    foundStructures.add(surfaceLocation);

                    plugin.getLogger().info("Found structure: " + structureType.getName() + " at X=" + x + ", Z=" + z);
                }
            } catch (Exception e) {
                plugin.getLogger().fine("Could not find structure " + structureType.getName() + ": " + e.getMessage());
            }
        }

        if (foundStructures.isEmpty()) {
            plugin.getLogger().warning("No structures found within " + searchRadius + " blocks");
            return null;
        }

        // 見つかった構造物からランダムに1つ選択
        Collections.shuffle(foundStructures);
        Location selected = foundStructures.get(0);

        plugin.getLogger().info("Randomly selected structure from " + foundStructures.size() + " candidates");
        return selected;
    }

    /**
     * チーム拠点にコンクリートを配置してプレイヤーをテレポート
     */
    private void setupTeamBasesAndTeleport(org.bukkit.World world, org.bukkit.Location center, int mapRadius) {
        // 全プレイヤーを中央のランダムな位置にテレポート
        long delay = 5L; // 0.25秒後から開始

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                // 中央から半径50ブロック以内のランダムな位置を生成
                int randomRadius = 50;
                double angle = Math.random() * 2 * Math.PI;
                double distance = Math.random() * randomRadius;

                int x = (int) (center.getBlockX() + distance * Math.cos(angle));
                int z = (int) (center.getBlockZ() + distance * Math.sin(angle));

                // 安全な地上の位置を見つける（窒息防止）
                int y = world.getHighestBlockYAt(x, z) + 1;

                org.bukkit.Location spawnLocation = new org.bukkit.Location(world, x + 0.5, y, z + 0.5);
                spawnLocation.setYaw((float) (Math.random() * 360));

                player.teleport(spawnLocation);
                plugin.getLogger().info("Teleported " + player.getName() + " to random center location (X=" + x + ", Y=" + y + ", Z=" + z + ")");
            }
        }, delay);

        // 全プレイヤーのテレポート完了後にワールドボーダーを設定
        long borderDelay = delay + 10L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setupMapBarrier(world, center, mapRadius);
            plugin.getLogger().info("World border activated after all players teleported");
        }, borderDelay);
    }

    /**
     * チームカラーのコンクリートを配置
     */
    private void placeTeamConcrete(org.bukkit.Location center, TeamColor teamColor) {
        org.bukkit.Material concreteType = getConcreteTypeForTeam(teamColor);
        org.bukkit.World world = center.getWorld();

        // 5x5の範囲にコンクリートを配置
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int blockX = center.getBlockX() + x;
                int blockZ = center.getBlockZ() + z;

                // 地面のY座標を探す（コンクリートを除外して本来の地形を見つける）
                int groundY = findNaturalGroundLevel(world, blockX, blockZ, center.getBlockY());

                // 地面の上（groundY）にコンクリートを配置
                org.bukkit.Location concreteLocation = new org.bukkit.Location(world, blockX, groundY, blockZ);
                concreteLocation.getBlock().setType(concreteType);
            }
        }

        plugin.getLogger().info("Placed " + teamColor + " concrete at team base");
    }

    /**
     * 自然な地面レベルを探す（コンクリートや人工物を除外）
     * centerY付近から下に探索して、最初の固体ブロックを見つける
     */
    private int findNaturalGroundLevel(org.bukkit.World world, int x, int z, int startY) {
        // startYから下に探索（最大10ブロック下まで）
        for (int y = startY - 1; y >= startY - 10; y--) {
            org.bukkit.block.Block block = world.getBlockAt(x, y, z);
            org.bukkit.Material material = block.getType();

            // コンクリートやバリアブロックは無視
            if (material == org.bukkit.Material.RED_CONCRETE ||
                material == org.bukkit.Material.BLUE_CONCRETE ||
                material == org.bukkit.Material.GREEN_CONCRETE ||
                material == org.bukkit.Material.YELLOW_CONCRETE ||
                material == org.bukkit.Material.WHITE_CONCRETE ||
                material == org.bukkit.Material.BARRIER) {
                continue;
            }

            // 固体ブロックを見つけたらその上
            if (material.isSolid() && material != org.bukkit.Material.AIR) {
                return y + 1;
            }
        }

        // 見つからなければstartYを使用
        return startY;
    }

    /**
     * チームカラーに対応するコンクリートを取得
     */
    private org.bukkit.Material getConcreteTypeForTeam(TeamColor teamColor) {
        switch (teamColor) {
            case RED:
                return org.bukkit.Material.RED_CONCRETE;
            case BLUE:
                return org.bukkit.Material.BLUE_CONCRETE;
            case GREEN:
                return org.bukkit.Material.GREEN_CONCRETE;
            case YELLOW:
                return org.bukkit.Material.YELLOW_CONCRETE;
            default:
                return org.bukkit.Material.WHITE_CONCRETE;
        }
    }

    /**
     * マップ境界にワールドボーダーを設定（バリアブロックの代わり）
     */
    private void setupMapBarrier(org.bukkit.World world, org.bukkit.Location center, int mapRadius) {
        // ワールドボーダーを使用（サーバー負荷が低い）
        org.bukkit.WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(mapRadius * 2.0); // 直径 = 半径 * 2
        border.setWarningDistance(10); // 境界10ブロック手前で警告
        border.setDamageAmount(0.2); // 境界外でのダメージ
        border.setDamageBuffer(5); // ダメージ開始までの距離

        plugin.getLogger().info("World border set at center X=" + center.getBlockX() + ", Z=" + center.getBlockZ() + ", radius=" + mapRadius);
    }

    private void setupWorldConditions(org.bukkit.World world) {
        // 天候設定
        String weatherConfig = plugin.getConfigManager().getWeather();
        switch (weatherConfig.toUpperCase()) {
            case "CLEAR":
                world.setStorm(false);
                world.setThundering(false);
                break;
            case "RAIN":
                world.setStorm(true);
                world.setThundering(false);
                break;
            case "THUNDER":
                world.setStorm(true);
                world.setThundering(true);
                break;
        }

        // 時間を朝に設定（0 = 朝6時）
        world.setTime(0);

        plugin.getLogger().info("World conditions set - Weather: " + weatherConfig + ", Time: Morning");

        // 自動朝リセット機能を開始
        if (plugin.getConfigManager().isAutoMorning()) {
            startTimeCheckTask(world);
        }
    }

    /**
     * 時間チェックタスクを開始（夜になったら朝に戻す）
     */
    private void startTimeCheckTask(org.bukkit.World world) {
        if (timeCheckTask != null) {
            timeCheckTask.cancel();
        }

        timeCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // ゲーム中でない場合はチェックしない
            if (currentState != GameState.RUNNING) {
                return;
            }

            long time = world.getTime();

            // 夜（13000 = 夜7時）になったら朝（0）に戻す
            if (time >= 13000) {
                world.setTime(0);
                plugin.getLogger().fine("Time reset to morning (night detected)");
            }
        }, 200L, 200L); // 10秒ごとにチェック（200 ticks = 10秒）

        plugin.getLogger().info("Time check task started - Auto-morning enabled");
    }

    /**
     * 時間チェックタスクを停止
     */
    private void stopTimeCheckTask() {
        if (timeCheckTask != null) {
            timeCheckTask.cancel();
            timeCheckTask = null;
            plugin.getLogger().info("Time check task stopped");
        }
    }

    /**
     * すべてのプレイヤーのネームタグを常に表示に設定
     */
    private void setupNameTagVisibility() {
        // ScoreboardManagerがすでにチーム色設定を各プレイヤーのScoreboardに適用しているため
        // ここでは全プレイヤーのScoreboardを更新するだけ
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardManager().setupScoreboard(player);
            plugin.getPlayerManager().updatePlayerTeamColor(player);
        }

        plugin.getLogger().info("Setup team colors and name tag visibility for all players");
    }

    /**
     * クリーンアップ（プラグイン無効化時）
     */
    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        if (gameTimerTask != null) {
            gameTimerTask.cancel();
        }
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }
        if (timeCheckTask != null) {
            timeCheckTask.cancel();
        }

        // ワールドボーダーをリセット
        resetWorldBorder();

        // ゲーム中のクリーンアップ
        if (isGameRunning()) {
            cleanupGame();
        }

        plugin.getLogger().info("GameManager shutdown complete");
    }

    /**
     * ワールドボーダーをリセット（デフォルトの巨大サイズに戻す）
     */
    private void resetWorldBorder() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            org.bukkit.WorldBorder border = world.getWorldBorder();
            // デフォルトの巨大サイズに戻す（29999984ブロック、約3000万ブロック）
            border.setSize(59999968.0);
            border.setCenter(0, 0);
            border.setWarningDistance(5);
            border.setDamageAmount(0.2);
            border.setDamageBuffer(5);

            plugin.getLogger().info("Reset world border for world: " + world.getName());
        }
    }
}
