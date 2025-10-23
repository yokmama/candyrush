# Research: Candy Rush Technical Decisions

**Date**: 2025-10-20
**Feature**: Candy Rush - チーム対戦型ポイント収集ゲーム

## Research Items

### 1. Paper API Minimum Version

**Decision**: Paper API 1.19.4+ (Minecraft 1.19.4+)

**Rationale**:
- Paper 1.19.4以降でパフォーマンスと安定性が大幅に向上
- 現代的なAPI機能（Adventure APIの完全サポート）
- MythicMobs 5.x系との互換性が良好
- 広く使用されており、コミュニティサポートが充実

**Alternatives Considered**:
- Paper 1.20.x: より新しいが、MythicMobsの完全サポート待ち
- Paper 1.18.x: 古く、必要なAPI機能が不足
- Spigot: Paperの方が高性能で最適化されている

**Implementation Notes**:
- `plugin.yml`で`api-version: 1.19`を指定
- Paper専用APIを活用（Folia対応は将来検討）

---

### 2. MythicMobs Compatible Version

**Decision**: MythicMobs 5.3.x+

**Rationale**:
- Paper 1.19.4+と完全互換
- `PreventSunburn: true`設定をサポート
- `onInteract`, `onPlayerNear`トリガーの安定動作
- `message`スキルによるプレイヤー通知機能
- カスタムドロップの柔軟な設定

**Alternatives Considered**:
- MythicMobs 4.x: 古く、必要な機能が不足
- EliteMobs: カスタマイズ性がMythicMobsに劣る
- 自作モンスターシステム: 開発コスト高、MythicMobsの実績を活用

**Implementation Notes**:
- `depend`として`plugin.yml`に記載
- MythicMobs APIを通じて動的スポーン
- 設定ファイルは`resources/mythicmobs/`配下に配置

---

### 3. データ永続化ライブラリ・方法

**Decision**: SQLite + HikariCP (組み込みデータベース)

**Rationale**:
- **ファイルベース（YAML/JSON）の問題点**:
  - 50人以上の同時アクセスで競合・破損リスク
  - リアルタイム更新の性能不足
  - トランザクション制御が困難

- **SQLiteの利点**:
  - サーバーレス、設定不要（外部DBサーバー不要）
  - トランザクション対応、データ整合性保証
  - 高速な読み書き、インデックス活用
  - HikariCPによるコネクションプーリング

- **PostgreSQL/MySQLと比較**:
  - 小〜中規模サーバーでは設定の手間が不要
  - バックアップが単一ファイルで簡単
  - 将来的にMySQLへの移行も可能（JDBCインターフェース統一）

**Alternatives Considered**:
- YAML (Bukkit Configuration API): 性能・信頼性不足
- JSON (Gson/Jackson): トランザクション未サポート
- MySQL/PostgreSQL: 小規模サーバーでは過剰、設定負荷大
- H2 Database: SQLiteより実績少

**Implementation Notes**:
- Maven依存: `org.xerial:sqlite-jdbc` + `com.zaxxer:HikariCP`
- データベースファイル: `plugins/CandyRush/data.db`
- テーブル設計:
  - `players`: プレイヤー状態（UUID, team, points, murderer_flag, last_position）
  - `game_rounds`: ゲームラウンド履歴
  - `stats`: 統計情報

---

### 4. 対応Minecraftバージョン範囲

**Decision**: Minecraft 1.19.4 〜 1.20.6

**Rationale**:
- Paper 1.19.4+の安定版範囲
- MythicMobs 5.3.xが対応する範囲
- 1.19.4: 最小対応バージョン（広く普及）
- 1.20.6: 現行最新の安定版
- この範囲内であればAPIの互換性が保たれる

**Alternatives Considered**:
- 1.18以前: Paper/MythicMobsの新機能未対応
- 1.21以降のみ: ユーザーベースが限定的

**Implementation Notes**:
- `plugin.yml`で`api-version: 1.19`を指定
- マイナーバージョン差異は実行時チェックで吸収

---

### 5. Minecraftプラグインのテスト戦略

**Decision**: MockBukkit + JUnit 5

**Rationale**:
- **MockBukkit**:
  - Bukkitサーバーのモック環境を提供
  - プラグインロード、イベント発火、コマンド実行をテスト可能
  - Paper APIのモックも部分的にサポート

- **JUnit 5**:
  - 標準的なJavaテストフレームワーク
  - `@ParameterizedTest`, `@Nested`などの豊富な機能
  - Maven/Gradleとの統合が容易

**Testing Layers**:
1. **Unit Tests**: ビジネスロジック（ポイント計算、チーム振り分けなど）
2. **Integration Tests**: MockBukkit使用、プラグイン全体の動作確認
3. **Manual Tests**: テストサーバーでの実機テスト（MythicMobs連携など）

**Alternatives Considered**:
- Mockito単体: Bukkit固有の複雑なモックが困難
- テストサーバー手動テストのみ: 自動化不可、CI/CD不可

**Implementation Notes**:
- Maven依存: `com.github.seeseemelk:MockBukkit-v1.19`
- CIパイプライン: GitHub Actions で自動テスト実行

---

## Best Practices

### Paper Plugin Development

**Researched Patterns**:
1. **非同期処理の活用**:
   - `Bukkit.getScheduler().runTaskAsynchronously()` でDB I/O実行
   - メインスレッドはブロックしない
   - ただし、Bukkit APIの呼び出しは必ずメインスレッドで

2. **イベントリスナーの最適化**:
   - `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)`
   - 不要なイベントは登録解除

3. **安全なテレポート**:
   - 全プレイヤーを一度にテレポート → サーバー過負荷
   - 推奨: 1tickごとに数人ずつ処理
   ```java
   BukkitRunnable task = new BukkitRunnable() {
       int index = 0;
       @Override
       public void run() {
           if (index >= players.size()) {
               cancel();
               return;
           }
           teleportPlayer(players.get(index));
           index++;
       }
   };
   task.runTaskTimer(plugin, 0L, 1L);
   ```

4. **エンティティタグによる識別**:
   - スポーンしたエンティティに`PersistentDataContainer`でタグ付け
   - クリーンアップ時に確実に削除可能

### MythicMobs Integration

**Researched Patterns**:
1. **動的スポーン**:
   ```java
   MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("EventNPC");
   ActiveMob activeMob = mob.spawn(location, level);
   ```

2. **イベント検知**:
   - `MythicMobDeathEvent`: ボス/モンスター討伐
   - `MythicMobSpawnEvent`: スポーン時の追加処理

3. **設定ファイル構成**:
   ```yaml
   # Mobs/EventNPC.yml
   EventNPC:
     Type: VILLAGER
     Display: '&a助けて！'
     Health: 100
     Damage: 0
     Options:
       PreventSunburn: true
       PreventItemPickup: true
       Invincible: true
     Skills:
       - message{m="助けて！"} @PlayersInRadius{r=10} ~onPlayerNear
       - message{m="ありがとう！"} @trigger ~onInteract
   ```

---

## Technology Stack Summary

| Component | Choice | Version |
|-----------|--------|---------|
| Language | Java | 17+ |
| Server | Paper | 1.19.4+ |
| Minecraft | Java Edition | 1.19.4 - 1.20.6 |
| Monster Plugin | MythicMobs | 5.3.x+ |
| Database | SQLite | 3.x |
| Connection Pool | HikariCP | 5.x |
| Testing | MockBukkit + JUnit | Latest |
| Build Tool | Maven or Gradle | (NEEDS DECISION) |

---

## Open Questions

1. **Build Tool選択**: Maven vs Gradle
   - Maven: Paper公式テンプレートが存在、設定シンプル
   - Gradle: ビルド高速、Kotlin DSL対応
   - **推奨**: Maven（Paperプラグインのデファクトスタンダード）

2. **設定ファイル形式**: YAML vs HOCON
   - YAML: Bukkit標準、コミュニティ慣習
   - HOCON: より柔軟だが学習コスト
   - **推奨**: YAML（Bukkit Configuration API使用）

3. **ロギングフレームワーク**: SLF4J vs java.util.logging
   - Paper内蔵のjava.util.loggingで十分
   - **推奨**: `plugin.getLogger()` 使用

---

**Research Complete**: All NEEDS CLARIFICATION items resolved. Ready for Phase 1 (Design & Contracts).
