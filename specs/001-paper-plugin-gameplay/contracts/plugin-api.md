# Plugin API Contract: Candy Rush

**Date**: 2025-10-20
**Version**: 1.0.0

## Overview

このドキュメントは、Candy Rushプラグインの内部API契約を定義します。Minecraftプラグインのため、REST APIではなく、Java APIインターフェースを定義します。

---

## 1. Manager Interfaces

### GameManager

**Purpose**: ゲームライフサイクル全体の管理

```java
public interface IGameManager {
    /**
     * ゲーム開始カウントダウンを開始
     * @throws IllegalStateException 既にカウントダウン中またはゲーム進行中の場合
     */
    void startCountdown();

    /**
     * カウントダウンをキャンセル
     * @return キャンセル成功したか
     */
    boolean cancelCountdown();

    /**
     * ゲームを開始
     * @throws IllegalStateException プレイヤー数が最低人数未満の場合
     */
    void startGame();

    /**
     * ゲームを終了
     * @return 勝利チーム
     */
    TeamColor endGame();

    /**
     * クールダウン期間を開始
     */
    void startCooldown();

    /**
     * 現在のゲーム状態を取得
     * @return ゲーム状態
     */
    GameState getGameState();

    /**
     * 現在のゲームラウンドを取得
     * @return GameRound（ゲーム中でない場合null）
     */
    @Nullable
    GameRound getCurrentGameRound();

    /**
     * 最低開始人数を満たしているか
     * @return true if満たしている
     */
    boolean hasMinimumPlayers();
}
```

---

### TeamManager

**Purpose**: チーム管理とポイント計算

```java
public interface ITeamManager {
    /**
     * プレイヤーをチームに振り分け
     * @param player 対象プレイヤー
     * @return 振り分けられたチームカラー
     */
    TeamColor assignTeam(Player player);

    /**
     * プレイヤーのチームを取得
     * @param player 対象プレイヤー
     * @return チームカラー（未所属の場合null）
     */
    @Nullable
    TeamColor getPlayerTeam(Player player);

    /**
     * 個人ポイントを追加
     * @param player 対象プレイヤー
     * @param points 追加ポイント（正の数）
     * @throws IllegalArgumentException points < 0の場合
     */
    void addPersonalPoints(Player player, int points);

    /**
     * チームポイントを追加
     * @param team 対象チーム
     * @param points 追加ポイント（正の数）
     * @throws IllegalArgumentException points < 0の場合
     */
    void addTeamPoints(TeamColor team, int points);

    /**
     * 個人ポイントを使用（ショップ購入等）
     * @param player 対象プレイヤー
     * @param points 使用ポイント
     * @return 成功したか（残高不足の場合false）
     */
    boolean usePersonalPoints(Player player, int points);

    /**
     * 個人ポイントを取得
     * @param player 対象プレイヤー
     * @return ポイント数
     */
    int getPersonalPoints(Player player);

    /**
     * チームポイントを取得
     * @param team 対象チーム
     * @return ポイント数
     */
    int getTeamPoints(TeamColor team);

    /**
     * 勝利チームを判定
     * @return 最高ポイントのチーム
     */
    TeamColor getWinningTeam();

    /**
     * チーム内MVPを取得
     * @param team 対象チーム
     * @return MVPプレイヤー（チーム内に誰もいない場合null）
     */
    @Nullable
    Player getTeamMVP(TeamColor team);
}
```

---

### TreasureManager

**Purpose**: 宝箱の配置と管理

```java
public interface ITreasureManager {
    /**
     * マップ全体に宝箱を配置
     * @param centerLocation マップ中心座標
     * @param radius マップ半径
     * @return 配置された宝箱数
     */
    int spawnTreasureChests(Location centerLocation, int radius);

    /**
     * 宝箱を開けた時の処理
     * @param player 開けたプレイヤー
     * @param chest 宝箱ブロック
     * @return 処理成功したか
     */
    boolean handleChestOpen(Player player, Block chest);

    /**
     * 宝箱を再出現させる
     * @param chestId 宝箱ID
     */
    void respawnChest(UUID chestId);

    /**
     * 全ての宝箱を削除
     */
    void clearAllChests();

    /**
     * 宝箱がゲームの管理対象か確認
     * @param chest 宝箱ブロック
     * @return true if管理対象
     */
    boolean isManagedChest(Block chest);
}
```

---

### EventManager

**Purpose**: イベントNPCと襲撃イベントの管理

```java
public interface IEventManager {
    /**
     * イベントNPCを配置
     * @param centerLocation マップ中心座標
     * @param radius マップ半径
     * @return 配置されたNPC数
     */
    int spawnEventNPCs(Location centerLocation, int radius);

    /**
     * イベントNPCクリック時の処理
     * @param player クリックしたプレイヤー
     * @param npcId NPC ID
     * @return 襲撃イベントID
     */
    UUID handleNPCInteract(Player player, UUID npcId);

    /**
     * 襲撃イベントを開始
     * @param npcId 発動元NPC ID
     * @param initialParticipants 初期参加者リスト
     * @return 襲撃イベントID
     */
    UUID startRaidEvent(UUID npcId, List<Player> initialParticipants);

    /**
     * 襲撃イベント中のモンスター討伐処理
     * @param eventId イベントID
     * @param player 討伐プレイヤー
     * @param mobType モンスター種類
     */
    void handleMobKill(UUID eventId, Player player, String mobType);

    /**
     * 襲撃イベントを終了
     * @param eventId イベントID
     * @param success 成功したか
     */
    void endRaidEvent(UUID eventId, boolean success);

    /**
     * 全てのイベントNPCと襲撃イベントを削除
     */
    void clearAll();

    /**
     * イベントクリア回数を取得
     * @return クリア回数
     */
    int getEventClearedCount();
}
```

---

### BossManager

**Purpose**: ボス出現と戦闘管理

```java
public interface IBossManager {
    /**
     * ボスを出現させる
     * @param location 出現座標
     * @return ボスID
     * @throws IllegalStateException 既にボスが存在する場合
     */
    UUID spawnBoss(Location location);

    /**
     * ボス戦にプレイヤーを参加登録
     * @param bossId ボスID
     * @param player 参加プレイヤー
     */
    void addParticipant(UUID bossId, Player player);

    /**
     * ボス討伐時の処理
     * @param bossId ボスID
     */
    void handleBossDefeat(UUID bossId);

    /**
     * 現在のボスIDを取得
     * @return ボスID（存在しない場合null）
     */
    @Nullable
    UUID getCurrentBossId();

    /**
     * ボスが存在するか
     * @return true if存在する
     */
    boolean bossExists();

    /**
     * ボスを削除
     */
    void clearBoss();
}
```

---

### ShopManager

**Purpose**: ショップシステムの管理

```java
public interface IShopManager {
    /**
     * ショップGUIを開く
     * @param player 開くプレイヤー
     */
    void openShop(Player player);

    /**
     * アイテムを購入
     * @param player 購入プレイヤー
     * @param itemId アイテムID
     * @return 購入成功したか（残高不足の場合false）
     */
    boolean purchaseItem(Player player, String itemId);

    /**
     * ショップアイテム（スロット9固定）を付与
     * @param player 対象プレイヤー
     */
    void giveShopItem(Player player);

    /**
     * ブロックがショップアイテムか確認
     * @param item アイテム
     * @return true ifショップアイテム
     */
    boolean isShopItem(ItemStack item);
}
```

---

### MurdererManager

**Purpose**: PvPペナルティシステムの管理

```java
public interface IMurdererManager {
    /**
     * プレイヤーをMurderer状態にする
     * @param player 対象プレイヤー
     */
    void setMurderer(Player player);

    /**
     * Murdererフラグを解除
     * @param player 対象プレイヤー
     */
    void clearMurderer(Player player);

    /**
     * Murderer状態か確認
     * @param player 対象プレイヤー
     * @return true ifMurderer
     */
    boolean isMurderer(Player player);

    /**
     * Murdererフラグ期限切れをチェック
     */
    void checkExpiredFlags();

    /**
     * 防具装備を制限
     * @param player 対象プレイヤー
     */
    void restrictArmor(Player player);
}
```

---

### DisplayManager

**Purpose**: ゲーム情報表示の管理

```java
public interface IDisplayManager {
    /**
     * スコアボードを更新
     * @param player 対象プレイヤー
     */
    void updateScoreboard(Player player);

    /**
     * 全プレイヤーのスコアボードを更新
     */
    void updateAllScoreboards();

    /**
     * タイトルメッセージを表示
     * @param player 対象プレイヤー
     * @param title タイトル
     * @param subtitle サブタイトル
     */
    void showTitle(Player player, String title, String subtitle);

    /**
     * 全プレイヤーにタイトルメッセージを表示
     * @param title タイトル
     * @param subtitle サブタイトル
     */
    void showTitleAll(String title, String subtitle);

    /**
     * アクションバーメッセージを表示
     * @param player 対象プレイヤー
     * @param message メッセージ
     */
    void showActionBar(Player player, String message);

    /**
     * タブリストの名前色を更新
     * @param player 対象プレイヤー
     */
    void updateTabListName(Player player);

    /**
     * 統計情報を表示（/stats コマンド）
     * @param player 表示対象プレイヤー
     */
    void showStats(Player player);
}
```

---

## 2. Storage Interfaces

### PlayerDataStorage

**Purpose**: プレイヤーデータの永続化

```java
public interface IPlayerDataStorage {
    /**
     * プレイヤーデータを保存
     * @param player 対象プレイヤー
     */
    void savePlayer(PlayerData player);

    /**
     * プレイヤーデータを読み込み
     * @param uuid プレイヤーUUID
     * @return プレイヤーデータ（未登録の場合null）
     */
    @Nullable
    PlayerData loadPlayer(UUID uuid);

    /**
     * 全プレイヤーデータを保存
     */
    void saveAll();

    /**
     * プレイヤーデータを削除
     * @param uuid プレイヤーUUID
     */
    void deletePlayer(UUID uuid);
}
```

### GameStateStorage

**Purpose**: ゲーム状態の永続化

```java
public interface IGameStateStorage {
    /**
     * ゲームラウンドを保存
     * @param round ゲームラウンド
     * @return 保存されたラウンドID
     */
    long saveGameRound(GameRound round);

    /**
     * ゲームラウンドを読み込み
     * @param roundId ラウンドID
     * @return ゲームラウンド
     */
    @Nullable
    GameRound loadGameRound(long roundId);

    /**
     * チームスコアを保存
     * @param roundId ラウンドID
     * @param team チーム
     */
    void saveTeamScore(long roundId, Team team);

    /**
     * プレイヤー統計を保存
     * @param roundId ラウンドID
     * @param playerUuid プレイヤーUUID
     * @param stats 統計情報
     */
    void savePlayerStats(long roundId, UUID playerUuid, PlayerStats stats);
}
```

---

## 3. Event Contracts

### Custom Events

Bukkitのイベントシステムを拡張したカスタムイベント:

```java
/**
 * ゲーム開始イベント
 */
public class GameStartEvent extends Event implements Cancellable {
    private boolean cancelled = false;
    private final GameRound gameRound;

    // getters, setters, HandlerList
}

/**
 * ゲーム終了イベント
 */
public class GameEndEvent extends Event {
    private final GameRound gameRound;
    private final TeamColor winningTeam;

    // getters, HandlerList
}

/**
 * ポイント獲得イベント
 */
public class PointsEarnedEvent extends Event {
    private final Player player;
    private final int personalPoints;
    private final int teamPoints;
    private final PointSource source; // TREASURE, EVENT, BOSS

    // getters, HandlerList
}

/**
 * Murdererフラグイベント
 */
public class PlayerBecameMurdererEvent extends Event {
    private final Player player;
    private final Player victim;

    // getters, HandlerList
}

/**
 * ボス出現イベント
 */
public class BossSpawnedEvent extends Event {
    private final UUID bossId;
    private final Location location;

    // getters, HandlerList
}
```

---

## 4. Configuration Contract

### config.yml

```yaml
# ゲーム設定
game:
  min-players: 2                    # 最低開始人数
  countdown-seconds: 10             # カウントダウン時間
  cooldown-minutes: 5               # クールダウン時間
  duration-minutes: 20              # ゲーム制限時間
  map-radius: 250                   # マップ半径（ブロック）

# チーム設定
teams:
  colors: [RED, BLUE, GREEN, YELLOW]

# 宝箱設定
treasure:
  per-chunk: 1                      # 1チャンクあたりの宝箱数
  trapped-chest-damage: 4.0         # トラップチェストのダメージ（ハート2個）
  trapped-chest-equipment-chance: 0.7  # 装備出現確率
  respawn-delay-seconds: 60         # 再出現までの時間

# イベント設定
event:
  npc-per-chunks: 3                 # NPCの配置間隔（チャンク）
  raid-duration-seconds: 300        # 襲撃イベント継続時間
  escape-distance-chunks: 2         # 逃亡判定距離
  boss-spawn-threshold: 3           # ボス出現に必要なクリア回数

# Murderer設定
murderer:
  duration-seconds: 600             # Murdererフラグ持続時間

# 天候・時間設定
world:
  weather: CLEAR                    # 天候固定
  auto-morning: true                # 夜になったら朝にリセット

# ショップ設定
shop:
  items:
    - id: wooden_sword
      name: "木の剣"
      material: WOODEN_SWORD
      price: 10
    - id: iron_sword
      name: "鉄の剣"
      material: IRON_SWORD
      price: 50
    # ... 他のアイテム

# MythicMobs設定
mythicmobs:
  event-npc-type: "EventNPC"        # イベントNPCのMythicMobs ID
  boss-type: "CandyRushBoss"        # ボスのMythicMobs ID
  raid-mobs: ["Zombie", "Skeleton", "Creeper"]  # 襲撃モンスター種類

# データベース設定
database:
  type: sqlite                      # sqlite or mysql
  sqlite:
    file: "data.db"
  # mysql設定は将来拡張用

# デバッグ設定
debug:
  enabled: false
  verbose-logging: false
```

---

## 5. Error Handling

### Exception Types

```java
/**
 * ゲーム状態が不正な場合
 */
public class InvalidGameStateException extends IllegalStateException {
    public InvalidGameStateException(String message) {
        super(message);
    }
}

/**
 * プレイヤー数が不足している場合
 */
public class InsufficientPlayersException extends IllegalStateException {
    public InsufficientPlayersException(int required, int actual) {
        super(String.format("Required %d players, but only %d present", required, actual));
    }
}

/**
 * ポイント残高不足の場合
 */
public class InsufficientPointsException extends RuntimeException {
    public InsufficientPointsException(int required, int actual) {
        super(String.format("Required %d points, but only %d available", required, actual));
    }
}
```

---

## 6. Thread Safety

### Concurrency Model

- **メインスレッド**: 全てのBukkit API呼び出し
- **非同期スレッド**: DB I/O、ファイル読み書き
- **同期**: `synchronized` ブロックまたは `ConcurrentHashMap` 使用

```java
// 例: 非同期でDB保存、完了後メインスレッドでコールバック
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    storage.savePlayer(playerData);  // 非同期
    Bukkit.getScheduler().runTask(plugin, () -> {
        // メインスレッドで後処理
        displayManager.showActionBar(player, "データ保存完了");
    });
});
```

---

**Contract Complete**: All manager interfaces, events, and configuration defined. Ready for implementation.
