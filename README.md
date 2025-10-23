# CandyRush - Minecraft Team Battle Plugin

4チーム対戦型の宝探しゲームプラグイン

## 🎮 ゲーム概要

- **4チーム制**: 赤・青・緑・黄の4チームで対戦
- **宝箱システム**: マップ内に配置された11種類の宝箱から食料を獲得
- **ポイント変換**: 食料を食べてポイントに変換
- **イベントNPC**: NPCに接触してレイドイベント発生
- **ボスバトル**: 討伐で500ptボーナス
- **PvPシステム**: チームキルでMurdererペナルティ
- **自動進行**: 最低人数で自動カウントダウン→ゲーム開始→クールダウン→再スタート

## 📋 必要要件

- **Minecraft**: 1.21.4
- **サーバー**: Paper 1.21.4+
- **依存プラグイン**: MythicMobs 5.3.5+
- **Java**: 21以降

## 🚀 クイックスタート

### ビルド

```bash
./gradlew build
```

生成されるファイル: `build/libs/CandyRush-1.0.0.jar`

### 開発用テストサーバー起動

```bash
./gradlew runServer
```

このコマンドで以下を自動実行：
1. Paper 1.21.4サーバーのダウンロード
2. プラグインのビルドとコピー
3. サーバーの起動

**初回起動時:**
- Paper JARが自動ダウンロードされます
- `run/` ディレクトリに開発サーバーが作成されます
- EULA自動承諾、オフラインモード有効

**MythicMobsのセットアップ（必須）:**

MythicMobsは自動ダウンロードできないため、手動でダウンロードが必要です：

1. https://www.spigotmc.org/resources/mythicmobs.5702/ からダウンロード
2. `run/plugins/` フォルダに配置
3. ファイル名を `MythicMobs-5.3.5.jar` に変更（または任意の名前でOK）

**または、MythicMobsなしでテスト:**
- MythicMobsなしでもプラグインは起動します
- ただし、イベントNPCとボス機能は動作しません

**サーバー停止:**
```
stop
```

### 本番サーバーへのデプロイ

1. プラグインをビルド
```bash
./gradlew build
```

2. JARファイルをコピー
```bash
cp build/libs/CandyRush-1.0.0.jar /path/to/server/plugins/
```

3. MythicMobsをインストール（未導入の場合）
   - https://mythiccraft.io/

4. サーバーを再起動

## ⚙️ 設定ファイル

`plugins/CandyRush/config.yml`

```yaml
game:
  min-players: 2              # 最低プレイヤー数
  countdown-seconds: 10       # カウントダウン秒数
  cooldown-minutes: 5         # クールダウン時間
  duration-minutes: 20        # ゲーム時間
  map-radius: 250            # マップ半径

treasure:
  per-chunk: 1               # チャンクあたりの宝箱数
  trapped-chest-damage: 4.0  # トラップダメージ
  respawn-delay-seconds: 60  # リスポーン時間

event:
  npc-per-chunks: 3          # NPC配置間隔
  raid-duration-seconds: 300 # レイド時間
  escape-distance-chunks: 2  # 脱出距離

murderer:
  duration-seconds: 600      # Murderer持続時間

mythicmobs:
  event-npc-type: EventNPC   # イベントNPC名
  boss-type: CandyRushBoss   # ボス名
```

## 🎯 コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/stats` | 個人統計表示 | candyrush.stats |
| `/stats top` | トッププレイヤーTOP10 | candyrush.stats |
| `/stats teams` | チームランキング | candyrush.stats |
| `/convert` | インベントリ内全食料を一括変換 | candyrush.convert |

## 🏗️ プロジェクト構造

```
src/main/java/com/candyrush/
├── CandyRushPlugin.java          # メインクラス
├── commands/                      # コマンド
│   ├── StatsCommand.java
│   └── ConvertCommand.java
├── listeners/                     # イベントリスナー
│   ├── PlayerConnectionListener.java
│   ├── TreasureChestListener.java
│   ├── FoodConsumeListener.java
│   ├── EventNpcListener.java
│   ├── BossDeathListener.java
│   └── PvpListener.java
├── managers/                      # ゲームマネージャー
│   ├── GameManager.java
│   ├── TeamManager.java
│   ├── PlayerManager.java
│   ├── TreasureChestManager.java
│   ├── PointConversionManager.java
│   ├── EventNpcManager.java
│   └── BossManager.java
├── models/                        # データモデル
│   ├── ChestType.java
│   ├── GameState.java
│   ├── GameRound.java
│   ├── PlayerData.java
│   ├── Team.java
│   └── TeamColor.java
├── storage/                       # データベース
│   ├── DatabaseInitializer.java
│   ├── PlayerDataStorage.java
│   └── GameStateStorage.java
├── integration/                   # 外部連携
│   └── MythicMobsIntegration.java
└── utils/                         # ユーティリティ
    ├── ConfigManager.java
    └── MessageUtils.java
```

## 🎨 MythicMobs設定

以下のMobを作成する必要があります：

### EventNPC（イベントNPC）
- タイプ: VILLAGER推奨
- 不死身（Invincible: true）
- 右クリックでレイドイベント発生

### CandyRushBoss（ボス）
- 討伐で500pt獲得
- 強力な攻撃力と体力

設定例は `plugins/MythicMobs/Mobs/` に配置

## 📊 データベース

- **タイプ**: SQLite
- **場所**: `plugins/CandyRush/data.db`
- **テーブル**: players, game_rounds, team_scores, player_stats

## 🔧 開発

### ビルドコマンド

```bash
# クリーンビルド
./gradlew clean build

# テストサーバー起動（自動ビルド含む）
./gradlew runServer

# プラグインのみ再ビルド・コピー
./gradlew copyPlugin
```

### 開発サーバーディレクトリ

```
run/
├── paper.jar              # Paper サーバー
├── plugins/
│   ├── CandyRush.jar     # 自動コピーされたプラグイン
│   └── MythicMobs-5.3.5.jar
├── eula.txt              # 自動承諾済み
└── server.properties     # オフラインモード設定済み
```

## 📝 ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 🙏 クレジット

- Paper - https://papermc.io/
- MythicMobs - https://mythiccraft.io/
- HikariCP - https://github.com/brettwooldridge/HikariCP
- SQLite - https://www.sqlite.org/
