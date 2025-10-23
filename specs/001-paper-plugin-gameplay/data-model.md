# Data Model: Candy Rush

**Date**: 2025-10-20
**Feature**: Candy Rush - チーム対戦型ポイント収集ゲーム

## Core Entities

### 1. Player (プレイヤー)

**Purpose**: ゲームに参加する個人の状態を管理

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| uuid | UUID | プレイヤーの一意識別子 | Not null, unique |
| currentTeam | TeamColor | 所属チーム | Enum: RED, BLUE, GREEN, YELLOW |
| personalPoints | int | 個人ポイント（ショップ用） | >= 0 |
| teamPointsContribution | int | チームポイント貢献度 | >= 0 |
| isMurderer | boolean | Murdererフラグ | true/false |
| murdererExpiry | Long | Murdererフラグ解除時刻 | nullable, Unix timestamp |
| lastPosition | Location | 最後の座標 | nullable |
| respawnLocation | Location | リスポーン地点 | nullable |
| isInGame | boolean | ゲーム参加中フラグ | true/false |

**Relationships**:
- Belongs to one Team
- Participates in zero or one GameRound (current game)
- Has many PlayerStats (historical)

**State Transitions**:
```
[待機] --join--> [ゲーム参加中]
[ゲーム参加中] --logout--> [一時離脱] --rejoin--> [ゲーム参加中]
[ゲーム参加中] --pvp--> [Murderer状態]
[Murderer状態] --death/timeout--> [ゲーム参加中]
[ゲーム参加中] --game_end--> [待機]
```

**Validation Rules**:
- `personalPoints` は常に >= 0
- `isMurderer == true` の場合、`murdererExpiry` は not null
- `isInGame == true` の場合、`currentTeam` は not null

---

### 2. Team (チーム)

**Purpose**: 4つの固定チームの状態管理

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| color | TeamColor | チームカラー | Enum: RED, BLUE, GREEN, YELLOW |
| teamPoints | int | チームポイント（勝敗判定用） | >= 0 |
| baseLocation | Location | チーム拠点座標 | Not null |
| members | List<UUID> | 所属プレイヤーのUUID | Not null |

**Relationships**:
- Has many Players
- Belongs to one GameRound

**Validation Rules**:
- 同一GameRound内で4チーム固定
- `teamPoints`は累積のみ（減少しない）

---

### 3. GameRound (ゲームラウンド)

**Purpose**: 1回のゲームセッション情報

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| id | Long | ラウンドID | Auto-increment |
| startTime | Long | 開始時刻 | Unix timestamp |
| endTime | Long | 終了時刻 | nullable, Unix timestamp |
| duration | int | 制限時間（秒） | > 0 |
| centerStructure | Location | マップ中心構造物 | Not null |
| mapRadius | int | マップ半径（ブロック） | Default: 250 |
| state | GameState | ゲーム状態 | Enum: WAITING, COUNTDOWN, RUNNING, COOLDOWN |
| eventClearedCount | int | イベントクリア回数 | >= 0 |
| bossSpawned | boolean | ボス出現フラグ | true/false |
| winningTeam | TeamColor | 勝利チーム | nullable |

**Relationships**:
- Has four Teams
- Has many Players
- Has many TreasureChests
- Has many EventNPCs
- Has zero or one Boss

**State Transitions**:
```
[WAITING] --min_players_reached--> [COUNTDOWN] --countdown_complete--> [RUNNING]
[COUNTDOWN] --player_left--> [WAITING]
[RUNNING] --time_up--> [COOLDOWN]
[COOLDOWN] --cooldown_end + min_players--> [COUNTDOWN]
```

**Validation Rules**:
- `startTime` < `endTime` (when endTime is set)
- `state == RUNNING` の場合、全4チームが存在
- `bossSpawned == true` の場合、`eventClearedCount >= bossSpawnThreshold`

---

### 4. TreasureChest (宝箱)

**Purpose**: マップ上の宝箱配置と状態管理

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| id | UUID | 宝箱ID | Not null, unique |
| gameRoundId | Long | 所属ゲームラウンド | Foreign key |
| chestType | ChestType | 宝箱種類 | Enum: CHEST, LARGE_CHEST, BARREL, FURNACE, BLAST_FURNACE, SMOKER, BREWING_STAND, HOPPER, DROPPER, DISPENSER, TRAPPED_CHEST |
| location | Location | 設置座標 | Not null |
| chunkX | int | チャンクX座標 | - |
| chunkZ | int | チャンクZ座標 | - |
| isActive | boolean | アクティブ状態 | true/false |
| respawnTime | Long | 再出現時刻 | nullable, Unix timestamp |

**Relationships**:
- Belongs to one GameRound

**State Transitions**:
```
[配置] --opened--> [空] --wait--> [再出現準備] --respawn_time--> [配置]
[配置] --game_end--> [削除]
```

**Validation Rules**:
- 1チャンクに1個まで（`chunkX`, `chunkZ`でユニーク制約）
- `chestType == TRAPPED_CHEST` の場合、特殊処理（ダメージ + 70%装備）

---

### 5. EventNPC (イベントNPC)

**Purpose**: 襲撃イベントを発動するNPC管理

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| id | UUID | NPC ID | Not null, unique |
| gameRoundId | Long | 所属ゲームラウンド | Foreign key |
| location | Location | 配置座標 | Not null |
| mythicMobId | String | MythicMobs内部ID | Not null |
| isCleared | boolean | クリア済みフラグ | true/false |
| activeRaidEvent | UUID | アクティブな襲撃イベントID | nullable |

**Relationships**:
- Belongs to one GameRound
- Has zero or one RaidEvent (active)

**Validation Rules**:
- MythicMobsで生成されたモブと紐付け
- `isCleared == true` の場合、`activeRaidEvent == null`

---

### 6. RaidEvent (襲撃イベント)

**Purpose**: 襲撃イベントの進行状態管理

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| id | UUID | イベントID | Not null, unique |
| eventNPCId | UUID | 発動元NPC | Foreign key |
| startTime | Long | 開始時刻 | Unix timestamp |
| duration | int | 継続時間（秒） | Default: 300 (5分) |
| participants | List<UUID> | 参加プレイヤーUUID | Not null |
| defeatedCount | int | 討伐数 | >= 0 |
| status | RaidEventStatus | イベント状態 | Enum: ACTIVE, SUCCESS, FAILED |

**Relationships**:
- Belongs to one EventNPC

**State Transitions**:
```
[ACTIVE] --time_up--> [SUCCESS]
[ACTIVE] --all_escaped--> [FAILED]
```

**Validation Rules**:
- `startTime + duration` で終了時刻を計算
- 参加者が2チャンク以上離れると除外（逃亡判定）

---

### 7. Boss (ボス)

**Purpose**: ゲーム全体で1体のみ存在する強力なモンスター

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| id | UUID | ボスID | Not null, unique |
| gameRoundId | Long | 所属ゲームラウンド | Foreign key |
| mythicMobId | String | MythicMobs内部ID | Not null |
| spawnLocation | Location | 出現座標 | Not null |
| spawnTime | Long | 出現時刻 | Unix timestamp |
| participants | List<UUID> | 戦闘参加者UUID | Not null |
| isDefeated | boolean | 討伐済みフラグ | true/false |

**Relationships**:
- Belongs to one GameRound

**Validation Rules**:
- 1GameRoundに1体まで
- ボス出現中は新たなイベントクリアカウント停止

---

### 8. Shop (ショップ)

**Purpose**: アイテム販売システム（設定ベース、永続化不要）

**Fields** (Configuration):
| Item | Price (Personal Points) | Category |
|------|-------------------------|----------|
| 木の剣 | 10 | 武器 |
| 鉄の剣 | 50 | 武器 |
| 革の防具セット | 30 | 防具 |
| 鉄の防具セット | 100 | 防具 |
| パン x8 | 5 | 食べ物 |
| ステーキ x4 | 10 | 食べ物 |
| 治癒のポーション | 20 | ポーション |
| エンダーパール x2 | 50 | アイテム |
| 金のリンゴ | 100 | アイテム |

**Validation Rules**:
- 購入時、`personalPoints >= price`
- 購入後、`personalPoints -= price`（チームポイントは不変）

---

## Database Schema (SQLite)

### Table: players

```sql
CREATE TABLE players (
    uuid TEXT PRIMARY KEY NOT NULL,
    current_team TEXT,  -- RED, BLUE, GREEN, YELLOW
    personal_points INTEGER NOT NULL DEFAULT 0,
    team_points_contribution INTEGER NOT NULL DEFAULT 0,
    is_murderer BOOLEAN NOT NULL DEFAULT 0,
    murderer_expiry INTEGER,
    last_position_world TEXT,
    last_position_x REAL,
    last_position_y REAL,
    last_position_z REAL,
    last_position_yaw REAL,
    last_position_pitch REAL,
    respawn_location_world TEXT,
    respawn_location_x REAL,
    respawn_location_y REAL,
    respawn_location_z REAL,
    is_in_game BOOLEAN NOT NULL DEFAULT 0,
    last_updated INTEGER NOT NULL
);

CREATE INDEX idx_players_team ON players(current_team);
CREATE INDEX idx_players_in_game ON players(is_in_game);
```

### Table: game_rounds

```sql
CREATE TABLE game_rounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    start_time INTEGER NOT NULL,
    end_time INTEGER,
    duration INTEGER NOT NULL DEFAULT 1200,  -- 20分
    center_world TEXT NOT NULL,
    center_x REAL NOT NULL,
    center_y REAL NOT NULL,
    center_z REAL NOT NULL,
    map_radius INTEGER NOT NULL DEFAULT 250,
    state TEXT NOT NULL,  -- WAITING, COUNTDOWN, RUNNING, COOLDOWN
    event_cleared_count INTEGER NOT NULL DEFAULT 0,
    boss_spawned BOOLEAN NOT NULL DEFAULT 0,
    winning_team TEXT
);

CREATE INDEX idx_game_rounds_state ON game_rounds(state);
```

### Table: team_scores

```sql
CREATE TABLE team_scores (
    game_round_id INTEGER NOT NULL,
    team_color TEXT NOT NULL,  -- RED, BLUE, GREEN, YELLOW
    team_points INTEGER NOT NULL DEFAULT 0,
    base_world TEXT NOT NULL,
    base_x REAL NOT NULL,
    base_y REAL NOT NULL,
    base_z REAL NOT NULL,
    PRIMARY KEY (game_round_id, team_color),
    FOREIGN KEY (game_round_id) REFERENCES game_rounds(id)
);
```

### Table: player_stats (履歴)

```sql
CREATE TABLE player_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    game_round_id INTEGER NOT NULL,
    team_color TEXT NOT NULL,
    personal_points INTEGER NOT NULL,
    team_points_contribution INTEGER NOT NULL,
    was_mvp BOOLEAN NOT NULL DEFAULT 0,
    recorded_at INTEGER NOT NULL,
    FOREIGN KEY (game_round_id) REFERENCES game_rounds(id)
);

CREATE INDEX idx_player_stats_uuid ON player_stats(player_uuid);
CREATE INDEX idx_player_stats_round ON player_stats(game_round_id);
```

---

## In-Memory State Management

以下のエンティティは揮発性が高く、メモリ上でのみ管理:

- **TreasureChest**: ゲーム中のみ存在、終了時削除
- **EventNPC**: ゲーム中のみ存在、終了時削除
- **RaidEvent**: イベント中のみ存在、終了時削除
- **Boss**: ゲーム中のみ存在、終了時削除

これらは `HashMap<UUID, Entity>` でメモリ管理し、ゲーム終了時にクリア。

---

## Validation Summary

### Global Constraints

1. **ポイント整合性**: `personalPoints >= 0`, `teamPoints >= 0`
2. **チーム固定**: 4チーム固定（RED, BLUE, GREEN, YELLOW）
3. **ゲーム状態遷移**: 定義された状態遷移以外は不可
4. **エンティティ数制限**:
   - Boss: 1体/GameRound
   - TreasureChest: 1個/Chunk
   - EventNPC: 3チャンクに1体

### Data Integrity Rules

1. Foreign Key制約: `game_round_id`, `player_uuid`は参照整合性維持
2. Unique制約: `uuid`, `(chunkX, chunkZ, gameRoundId)`
3. Not Null制約: 必須フィールドは全てNot Null

---

**Data Model Complete**: Ready for contract generation (Phase 1).
