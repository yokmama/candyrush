# Implementation Plan: Candy Rush - チーム対戦型ポイント収集ゲーム

**Branch**: `001-paper-plugin-gameplay` | **Date**: 2025-10-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-paper-plugin-gameplay/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Minecraft Paper プラグインによる4チーム対戦型ポイント収集ゲーム。プレイヤーは宝箱探索、襲撃イベント、ボス戦を通じてポイントを獲得し、チームの勝利を目指す。MythicMobsを使用したカスタムモンスター・NPC、自動ゲーム管理、プレイヤー状態の永続化、リアルタイムゲーム情報表示を実装する。

## Technical Context

**Language/Version**: Java 17+ (Paper API requirement)
**Primary Dependencies**:
- Paper API (NEEDS CLARIFICATION: minimum version)
- MythicMobs plugin (NEEDS CLARIFICATION: compatible version)
- NEEDS CLARIFICATION: データ永続化ライブラリ（YAML/JSON/Database）

**Storage**: NEEDS CLARIFICATION: プレイヤー状態の永続化方法（ファイルベース vs データベース）

**Testing**: NEEDS CLARIFICATION: Minecraftプラグインのテスト戦略（MockBukkit等）

**Target Platform**: Minecraft Java Edition server (Paper)
- NEEDS CLARIFICATION: 対応Minecraftバージョン範囲

**Project Type**: Single project (Minecraft server plugin)

**Performance Goals**:
- 同時接続50人以上をサポート
- 宝箱からポイント反映まで1秒以内
- スコアボード更新1秒以内
- タイトルメッセージ表示0.5秒以内

**Constraints**:
- サーバー負荷を考慮した安全なテレポート処理（全プレイヤー一括処理は避ける）
- ゲーム終了とゲーム開始時のダブルチェックによるクリーンアップ
- 同じワールドを継続使用（プレイヤー設置ブロックは保持）

**Scale/Scope**:
- 対象プレイヤー数: 最低2人〜50人以上
- 87個の機能要件（FR-001 〜 FR-087）
- 4つの固定チーム
- 11種類の宝箱タイプ
- イベントNPC、襲撃イベント、ボスシステム

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: N/A - Constitution file contains template placeholders only. No specific project principles have been defined yet.

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```
src/
├── main/
│   ├── java/
│   │   └── com/candyrush/
│   │       ├── CandyRushPlugin.java       # Main plugin class
│   │       ├── models/                     # Data models
│   │       │   ├── Player.java
│   │       │   ├── Team.java
│   │       │   ├── GameRound.java
│   │       │   ├── TreasureChest.java
│   │       │   ├── EventNPC.java
│   │       │   ├── RaidEvent.java
│   │       │   └── Boss.java
│   │       ├── managers/                   # Game logic managers
│   │       │   ├── GameManager.java        # Main game state & lifecycle
│   │       │   ├── TeamManager.java        # Team assignment & points
│   │       │   ├── TreasureManager.java    # Chest spawning & management
│   │       │   ├── EventManager.java       # Event NPC & raid events
│   │       │   ├── BossManager.java        # Boss spawning & battles
│   │       │   ├── ShopManager.java        # Shop system
│   │       │   ├── MurdererManager.java    # PvP penalty system
│   │       │   └── DisplayManager.java     # Scoreboard, titles, actionbar
│   │       ├── listeners/                  # Event listeners
│   │       │   ├── PlayerJoinListener.java
│   │       │   ├── PlayerQuitListener.java
│   │       │   ├── ChestOpenListener.java
│   │       │   ├── NPCInteractListener.java
│   │       │   ├── PlayerDamageListener.java
│   │       │   ├── PlayerDeathListener.java
│   │       │   └── ShopInteractListener.java
│   │       ├── commands/                   # Commands
│   │       │   └── StatsCommand.java
│   │       ├── storage/                    # Data persistence
│   │       │   ├── PlayerDataStorage.java
│   │       │   └── GameStateStorage.java
│   │       ├── integration/                # External plugin integration
│   │       │   └── MythicMobsIntegration.java
│   │       ├── utils/                      # Utility classes
│   │       │   ├── ConfigManager.java
│   │       │   ├── MessageUtils.java
│   │       │   └── TeleportUtils.java
│   │       └── tasks/                      # Scheduled tasks
│   │           ├── GameCountdownTask.java
│   │           ├── CooldownTask.java
│   │           ├── RaidEventTask.java
│   │           └── TimeManagementTask.java
│   └── resources/
│       ├── plugin.yml                      # Plugin metadata
│       ├── config.yml                      # Configuration file
│       └── mythicmobs/                     # MythicMobs configs
│           ├── Mobs/                       # Monster definitions
│           ├── Skills/                     # NPC skills & messages
│           └── Items/                      # Custom drops
│
└── test/
    └── java/
        └── com/candyrush/
            ├── unit/                       # Unit tests
            │   ├── TeamManagerTest.java
            │   ├── TreasureManagerTest.java
            │   └── PointCalculationTest.java
            └── integration/                # Integration tests
                └── GameFlowTest.java
```

**Structure Decision**: Standard Maven/Gradle plugin structure for Minecraft Paper plugins. Follows package-by-feature pattern with clear separation between models, managers (business logic), listeners (event handlers), and utilities.

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

