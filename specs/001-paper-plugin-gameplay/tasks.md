# Tasks: Candy Rush - チーム対戦型ポイント収集ゲーム

**Input**: Design documents from `/specs/001-paper-plugin-gameplay/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/plugin-api.md

**Tests**: テストタスクは含まれていません。テストは手動テストおよび将来的な自動化で対応します。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions
- Single Minecraft plugin project structure
- Paths: `src/main/java/com/candyrush/`, `src/main/resources/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Maven structure

- [x] T001 Create Maven project structure with pom.xml per quickstart.md
- [x] T002 Create directory structure: src/main/java/com/candyrush/{models,managers,listeners,commands,storage,integration,utils,tasks}
- [x] T003 [P] Create src/main/resources/{plugin.yml,config.yml} per quickstart.md
- [x] T004 [P] Create src/main/resources/mythicmobs/{Mobs,Skills,Items}/ directories
- [x] T005 [P] Create test directory structure: src/test/java/com/candyrush/{unit,integration}
- [x] T006 Create main plugin class src/main/java/com/candyrush/CandyRushPlugin.java with onEnable/onDisable stubs

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Create TeamColor enum in src/main/java/com/candyrush/models/TeamColor.java
- [ ] T008 Create GameState enum in src/main/java/com/candyrush/models/GameState.java (WAITING, COUNTDOWN, RUNNING, COOLDOWN)
- [ ] T009 [P] Create ChestType enum in src/main/java/com/candyrush/models/ChestType.java (11 types)
- [ ] T010 [P] Create ConfigManager utility in src/main/java/com/candyrush/utils/ConfigManager.java
- [ ] T011 [P] Create MessageUtils utility in src/main/java/com/candyrush/utils/MessageUtils.java for text formatting
- [ ] T012 Setup SQLite database schema in src/main/java/com/candyrush/storage/DatabaseInitializer.java
- [ ] T013 Create HikariCP connection pool configuration in ConfigManager
- [ ] T014 Create PlayerData model in src/main/java/com/candyrush/models/PlayerData.java
- [ ] T015 Create Team model in src/main/java/com/candyrush/models/Team.java
- [ ] T016 Create GameRound model in src/main/java/com/candyrush/models/GameRound.java
- [ ] T017 [P] Create PlayerDataStorage interface and implementation in src/main/java/com/candyrush/storage/PlayerDataStorage.java
- [ ] T018 [P] Create GameStateStorage interface and implementation in src/main/java/com/candyrush/storage/GameStateStorage.java
- [ ] T019 Create MythicMobsIntegration class in src/main/java/com/candyrush/integration/MythicMobsIntegration.java
- [ ] T020 Implement plugin dependency check for MythicMobs in CandyRushPlugin onEnable

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 10 - ゲーム初期化と後始末 (Priority: P1) 🎯 FOUNDATIONAL

**Goal**: ゲーム開始時・終了時の完全な初期化とクリーンアップを実装

**Independent Test**: ゲーム開始時に前回の残骸が全て削除され、プレイヤーが適切にリセットされて拠点にテレポートされることを確認

**Note**: この機能は他の全ユーザーストーリーの基盤となるため、P1として優先実装

### Implementation for User Story 10

- [ ] T021 [P] [US10] Create GameManager class in src/main/java/com/candyrush/managers/GameManager.java with basic structure
- [ ] T022 [US10] Implement startCountdown() method in GameManager
- [ ] T023 [US10] Implement cancelCountdown() method in GameManager
- [ ] T024 [US10] Implement startGame() method in GameManager with map initialization
- [ ] T025 [US10] Implement endGame() method in GameManager with cleanup
- [ ] T026 [US10] Implement game state management (WAITING→COUNTDOWN→RUNNING→COOLDOWN) in GameManager
- [ ] T027 [P] [US10] Create TeleportUtils in src/main/java/com/candyrush/utils/TeleportUtils.java for safe distributed teleportation
- [ ] T028 [US10] Implement player state reset (inventory, points, HP, hunger, effects, Murderer) in GameManager
- [ ] T029 [US10] Implement entity cleanup (chests, NPCs, bosses, monsters, drops) in GameManager
- [ ] T030 [US10] Implement double-check cleanup (both at game end AND game start) in GameManager
- [ ] T031 [US10] Implement structure selection (fixed or random) for map center in GameManager
- [ ] T032 [P] [US10] Create GameCountdownTask in src/main/java/com/candyrush/tasks/GameCountdownTask.java
- [ ] T033 [P] [US10] Create CooldownTask in src/main/java/com/candyrush/tasks/CooldownTask.java
- [ ] T034 [US10] Integrate GameCountdownTask with countdown display in GameManager
- [ ] T035 [US10] Integrate CooldownTask with cooldown period management in GameManager
- [ ] T036 [US10] Implement minimum player check (hasMinimumPlayers) in GameManager

**Checkpoint**: GameManager core ready - game lifecycle can now be controlled

---

## Phase 4: User Story 1 - ゲーム参加と基本プレイ (Priority: P1) 🎯 MVP

**Goal**: プレイヤー接続、チーム振り分け、カウントダウン、ゲーム開始・終了の基本フローを実装

**Independent Test**: サーバーに接続してチームに振り分けられ、カウントダウン後ゲームが開始され、時間経過で終了することを確認

### Implementation for User Story 1

- [ ] T037 [P] [US1] Create TeamManager class in src/main/java/com/candyrush/managers/TeamManager.java
- [ ] T038 [US1] Implement assignTeam() method for automatic team assignment in TeamManager
- [ ] T039 [US1] Implement addPersonalPoints() method in TeamManager
- [ ] T040 [US1] Implement addTeamPoints() method in TeamManager
- [ ] T041 [US1] Implement usePersonalPoints() method in TeamManager
- [ ] T042 [US1] Implement getWinningTeam() method in TeamManager
- [ ] T043 [US1] Implement getTeamMVP() method in TeamManager
- [ ] T044 [P] [US1] Create PlayerJoinListener in src/main/java/com/candyrush/listeners/PlayerJoinListener.java
- [ ] T045 [US1] Implement team assignment and spawn logic in PlayerJoinListener
- [ ] T046 [US1] Implement countdown trigger when minimum players reached in PlayerJoinListener
- [ ] T047 [P] [US1] Create PlayerQuitListener in src/main/java/com/candyrush/listeners/PlayerQuitListener.java
- [ ] T048 [US1] Implement countdown cancellation when players drop below minimum in PlayerQuitListener
- [ ] T049 [US1] Implement player state save on quit in PlayerQuitListener
- [ ] T050 [US1] Implement player state restoration on rejoin in PlayerJoinListener
- [ ] T051 [US1] Register PlayerJoinListener and PlayerQuitListener in CandyRushPlugin
- [ ] T052 [US1] Implement game timer and auto-end when time limit reached in GameManager

**Checkpoint**: Basic game flow complete - players can join, start game, and game ends automatically

---

## Phase 5: User Story 9 - ゲーム情報表示システム (Priority: P1)

**Goal**: スコアボード、タイトルメッセージ、アクションバー、タブリスト表示を実装

**Independent Test**: スコアボードが常に表示され、各種イベント時にタイトルメッセージやアクションバーが適切に更新されることを確認

### Implementation for User Story 9

- [ ] T053 [P] [US9] Create DisplayManager class in src/main/java/com/candyrush/managers/DisplayManager.java
- [ ] T054 [US9] Implement updateScoreboard() method for single player in DisplayManager
- [ ] T055 [US9] Implement updateAllScoreboards() method in DisplayManager
- [ ] T056 [US9] Implement showTitle() method for single player in DisplayManager
- [ ] T057 [US9] Implement showTitleAll() method for all players in DisplayManager
- [ ] T058 [US9] Implement showActionBar() method in DisplayManager
- [ ] T059 [US9] Implement updateTabListName() method for team color in DisplayManager
- [ ] T060 [P] [US9] Create StatsCommand in src/main/java/com/candyrush/commands/StatsCommand.java
- [ ] T061 [US9] Implement showStats() method to display team scores and rankings in DisplayManager
- [ ] T062 [US9] Register StatsCommand in CandyRushPlugin and plugin.yml
- [ ] T063 [US9] Integrate DisplayManager with GameManager for countdown display
- [ ] T064 [US9] Integrate DisplayManager with GameManager for game end messages
- [ ] T065 [US9] Integrate DisplayManager with TeamManager for team assignment notification
- [ ] T066 [US9] Integrate DisplayManager with TeamManager for rank change notification (1st place)
- [ ] T067 [US9] Add periodic scoreboard update task (every second) in GameManager

**Checkpoint**: Information display complete - players can see all game state and events

---

## Phase 6: User Story 2 - 宝箱探索とリスク管理 (Priority: P1)

**Goal**: 11種類の宝箱配置、食べ物→ポイント変換、トラップチェスト、宝箱再出現を実装

**Independent Test**: 通常宝箱とトラップチェストを開け、それぞれ異なる報酬とリスクが存在することを確認

### Implementation for User Story 2

- [ ] T068 [P] [US2] Create TreasureChest model in src/main/java/com/candyrush/models/TreasureChest.java
- [ ] T069 [P] [US2] Create TreasureManager class in src/main/java/com/candyrush/managers/TreasureManager.java
- [ ] T070 [US2] Implement spawnTreasureChests() method for map-wide chest placement in TreasureManager
- [ ] T071 [US2] Implement chunk-based chest spawning (1 per chunk) in TreasureManager
- [ ] T072 [US2] Implement chest type randomization (11 types) in TreasureManager
- [ ] T073 [P] [US2] Create ChestOpenListener in src/main/java/com/candyrush/listeners/ChestOpenListener.java
- [ ] T074 [US2] Implement normal chest handling (food → points) in ChestOpenListener
- [ ] T075 [US2] Implement trapped chest handling (damage + 70% equipment) in ChestOpenListener
- [ ] T076 [US2] Implement chest item auto-removal and point conversion in ChestOpenListener
- [ ] T077 [US2] Implement chest despawn when empty in TreasureManager
- [ ] T078 [US2] Implement chest respawn after delay in TreasureManager
- [ ] T079 [US2] Implement chest respawn location randomization within chunk in TreasureManager
- [ ] T080 [US2] Register ChestOpenListener in CandyRushPlugin
- [ ] T081 [US2] Integrate TreasureManager with GameManager for game start chest spawn
- [ ] T082 [US2] Integrate TreasureManager with GameManager for game end chest cleanup

**Checkpoint**: Treasure system complete - chests spawn, players get points, chests respawn

---

## Phase 7: User Story 3 - ポイントでショップ利用 (Priority: P1)

**Goal**: ショップアイテム固定配置、ショップGUI、アイテム購入を実装

**Independent Test**: ポイントを獲得し、ショップGUIを開いてアイテムを購入し、個人ポイントが減少することを確認

### Implementation for User Story 3

- [ ] T083 [P] [US3] Create ShopManager class in src/main/java/com/candyrush/managers/ShopManager.java
- [ ] T084 [US3] Implement giveShopItem() method to place shop item in slot 9 in ShopManager
- [ ] T085 [US3] Implement shop item protection (no drop, no move) in ShopManager
- [ ] T086 [US3] Implement openShop() method to display shop GUI in ShopManager
- [ ] T087 [US3] Implement shop GUI inventory creation with items from config in ShopManager
- [ ] T088 [US3] Implement purchaseItem() method with point deduction in ShopManager
- [ ] T089 [P] [US3] Create ShopInteractListener in src/main/java/com/candyrush/listeners/ShopInteractListener.java
- [ ] T090 [US3] Implement air-click detection for shop item in ShopInteractListener
- [ ] T091 [US3] Implement shop GUI click handling for purchases in ShopInteractListener
- [ ] T092 [US3] Implement shop item drop prevention in ShopInteractListener
- [ ] T093 [US3] Implement shop item move/swap prevention in ShopInteractListener
- [ ] T094 [US3] Register ShopInteractListener in CandyRushPlugin
- [ ] T095 [US3] Integrate ShopManager with GameManager to give shop item at game start
- [ ] T096 [US3] Add shop configuration section to config.yml with item list and prices

**Checkpoint**: Shop system complete - players can purchase items with personal points

---

## Phase 8: User Story 8 - ゲーム終了とリザルト (Priority: P1)

**Goal**: ゲーム終了判定、リザルト表示、クールダウン、自動再開を実装

**Independent Test**: 制限時間経過後、リザルトが表示され、全てのゲーム要素がクリーンアップされ、自動的に新しいゲームが開始されることを確認

### Implementation for User Story 8

- [ ] T097 [US8] Implement game time tracking and limit check in GameManager
- [ ] T098 [US8] Implement victory team determination in GameManager (endGame already created in US10)
- [ ] T099 [US8] Implement result message creation (team scores + MVPs) in GameManager
- [ ] T100 [US8] Integrate DisplayManager to show game end title and chat messages in GameManager
- [ ] T101 [US8] Implement result display delay before cooldown in GameManager
- [ ] T102 [US8] Integrate CooldownTask to start 5-minute cooldown in GameManager
- [ ] T103 [US8] Implement automatic game restart after cooldown if minimum players present in GameManager
- [ ] T104 [US8] Save game round history to database in GameStateStorage
- [ ] T105 [US8] Save player stats to database in GameStateStorage for MVP tracking

**Checkpoint**: Game end flow complete - results shown, cleanup done, auto-restart works

---

## Phase 9: User Story 4 - イベントNPCと襲撃イベント (Priority: P2)

**Goal**: イベントNPC配置、クリック検知、襲撃イベント開始・管理、ポイント付与を実装

**Independent Test**: イベントNPCに近づいてメッセージを確認し、クリックして襲撃イベントを開始し、モンスターを倒してポイントを獲得できることを確認

### Implementation for User Story 4

- [ ] T106 [P] [US4] Create EventNPC model in src/main/java/com/candyrush/models/EventNPC.java
- [ ] T107 [P] [US4] Create RaidEvent model in src/main/java/com/candyrush/models/RaidEvent.java
- [ ] T108 [P] [US4] Create EventManager class in src/main/java/com/candyrush/managers/EventManager.java
- [ ] T109 [US4] Implement spawnEventNPCs() method for map-wide NPC placement in EventManager
- [ ] T110 [US4] Implement NPC spawning via MythicMobsIntegration (villager appearance, invincible) in EventManager
- [ ] T111 [US4] Implement NPC proximity detection for help message in EventManager
- [ ] T112 [P] [US4] Create NPCInteractListener in src/main/java/com/candyrush/listeners/NPCInteractListener.java
- [ ] T113 [US4] Implement NPC click detection in NPCInteractListener
- [ ] T114 [US4] Implement startRaidEvent() method in EventManager
- [ ] T115 [US4] Implement participant registration (nearby players) in RaidEvent
- [ ] T116 [P] [US4] Create RaidEventTask in src/main/java/com/candyrush/tasks/RaidEventTask.java for 5-minute timer
- [ ] T117 [US4] Implement monster spawning via MythicMobs during raid in RaidEventTask
- [ ] T118 [US4] Implement handleMobKill() method to track kills and award points in EventManager
- [ ] T119 [US4] Implement escape detection (2+ chunks away) in RaidEventTask
- [ ] T120 [US4] Implement endRaidEvent() method with success/failure messages in EventManager
- [ ] T121 [US4] Implement event cleared counter increment in GameManager
- [ ] T122 [US4] Register NPCInteractListener in CandyRushPlugin
- [ ] T123 [US4] Integrate EventManager with GameManager for game start NPC spawn
- [ ] T124 [US4] Integrate EventManager with GameManager for game end NPC cleanup
- [ ] T125 [P] [US4] Create MythicMobs config for EventNPC in src/main/resources/mythicmobs/Mobs/EventNPC.yml
- [ ] T126 [P] [US4] Create MythicMobs skills config for NPC messages in src/main/resources/mythicmobs/Skills/NPCMessages.yml
- [ ] T127 [P] [US4] Create MythicMobs raid monster configs in src/main/resources/mythicmobs/Mobs/RaidMobs.yml
- [ ] T128 [US4] Integrate DisplayManager to show raid event messages (start, success, failure) in EventManager

**Checkpoint**: Raid event system complete - NPCs spawn, events start, monsters spawn, points awarded

---

## Phase 10: User Story 5 - ボス戦 (Priority: P2)

**Goal**: ボス出現条件、ボススポーン、ボスバー表示、ボス討伐報酬を実装

**Independent Test**: イベントを規定回数クリアしてボスを出現させ、ボスバーが表示され、倒すとポイントと報酬が得られることを確認

### Implementation for User Story 5

- [ ] T129 [P] [US5] Create Boss model in src/main/java/com/candyrush/models/Boss.java
- [ ] T130 [P] [US5] Create BossManager class in src/main/java/com/candyrush/managers/BossManager.java
- [ ] T131 [US5] Implement boss spawn trigger when event cleared threshold reached in EventManager
- [ ] T132 [US5] Implement spawnBoss() method via MythicMobs at last event NPC location in BossManager
- [ ] T133 [US5] Implement boss bar creation and display to all players in BossManager
- [ ] T134 [US5] Implement addParticipant() method to track combat participants in BossManager
- [ ] T135 [US5] Implement handleBossDefeat() method with item drop and point rewards in BossManager
- [ ] T136 [US5] Implement boss existence check (only 1 boss per game) in BossManager
- [ ] T137 [US5] Implement event cleared counter freeze when boss exists in EventManager
- [ ] T138 [US5] Integrate DisplayManager to show boss spawn title message in BossManager
- [ ] T139 [US5] Integrate DisplayManager to show boss HP on action bar in BossManager
- [ ] T140 [US5] Integrate BossManager with GameManager for boss cleanup at game end
- [ ] T141 [P] [US5] Create MythicMobs boss config in src/main/resources/mythicmobs/Mobs/CandyRushBoss.yml
- [ ] T142 [P] [US5] Create MythicMobs boss drops config in src/main/resources/mythicmobs/Items/BossDrops.yml

**Checkpoint**: Boss system complete - boss spawns, boss bar shows, rewards distributed

---

## Phase 11: User Story 7 - 死亡とリスポーン (Priority: P2)

**Goal**: プレイヤー死亡処理、リスポーン地点管理、装備保持/ドロップを実装

**Independent Test**: 死亡してリスポーンし、装備とポイントの状態を確認できる

### Implementation for User Story 7

- [ ] T143 [P] [US7] Create PlayerDeathListener in src/main/java/com/candyrush/listeners/PlayerDeathListener.java
- [ ] T144 [US7] Implement death handling with equipment preservation for normal players in PlayerDeathListener
- [ ] T145 [US7] Implement death handling with equipment drop for Murderer players in PlayerDeathListener
- [ ] T146 [US7] Implement Murderer flag clear on death in PlayerDeathListener
- [ ] T147 [US7] Implement point preservation on death (no point loss) in PlayerDeathListener
- [ ] T148 [US7] Implement respawn location determination (bed or team base) in PlayerDeathListener
- [ ] T149 [US7] Implement bed respawn point setting in PlayerJoinListener or separate listener
- [ ] T150 [US7] Register PlayerDeathListener in CandyRushPlugin

**Checkpoint**: Death/respawn system complete - players respawn correctly with proper state

---

## Phase 12: User Story 6 - PvPとMurdererシステム (Priority: P3)

**Goal**: チーム内PvP無効化、Murdererフラグ、ペナルティ、タイムアウトを実装

**Independent Test**: 他チームのプレイヤーを攻撃してMurdererフラグが立ち、ペナルティが適用されることを確認

### Implementation for User Story 6

- [ ] T151 [P] [US6] Create MurdererManager class in src/main/java/com/candyrush/managers/MurdererManager.java
- [ ] T152 [US6] Implement setMurderer() method with flag and expiry time in MurdererManager
- [ ] T153 [US6] Implement clearMurderer() method in MurdererManager
- [ ] T154 [US6] Implement isMurderer() check in MurdererManager
- [ ] T155 [US6] Implement checkExpiredFlags() periodic task in MurdererManager
- [ ] T156 [US6] Implement restrictArmor() method to prevent armor equip in MurdererManager
- [ ] T157 [P] [US6] Create PlayerDamageListener in src/main/java/com/candyrush/listeners/PlayerDamageListener.java
- [ ] T158 [US6] Implement team-mate damage cancellation in PlayerDamageListener
- [ ] T159 [US6] Implement Murderer flag setting when damaging other team in PlayerDamageListener
- [ ] T160 [US6] Implement armor equip prevention for Murderer players in PlayerDamageListener or separate listener
- [ ] T161 [US6] Integrate DisplayManager to show Murderer announcement in MurdererManager
- [ ] T162 [US6] Integrate DisplayManager to show player name in red for Murderer in MurdererManager
- [ ] T163 [US6] Register PlayerDamageListener in CandyRushPlugin
- [ ] T164 [US6] Integrate MurdererManager flag clear with PlayerDeathListener (already in US7)

**Checkpoint**: PvP/Murderer system complete - team PvP blocked, Murderer penalties applied

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, optimization, and production readiness

- [ ] T165 [P] Create TimeManagementTask in src/main/java/com/candyrush/tasks/TimeManagementTask.java
- [ ] T166 Implement weather control (always clear) in TimeManagementTask
- [ ] T167 Implement time cycle with auto-morning reset in TimeManagementTask
- [ ] T168 Register all managers in CandyRushPlugin with proper initialization order
- [ ] T169 Implement graceful shutdown with data save in CandyRushPlugin onDisable
- [ ] T170 Add comprehensive logging for all major operations
- [ ] T171 [P] Optimize scoreboard update frequency to minimize lag
- [ ] T172 [P] Optimize chest respawn scheduler to avoid tick lag
- [ ] T173 Add configuration validation on plugin load
- [ ] T174 Add config reload command (optional, for admin convenience)
- [ ] T175 [P] Create comprehensive config.yml with all settings and comments
- [ ] T176 [P] Add Japanese language messages to config.yml or separate messages file
- [ ] T177 Test full game cycle from countdown to game end to auto-restart
- [ ] T178 Test with 50+ players for performance validation
- [ ] T179 Review and optimize database queries for concurrent access
- [ ] T180 Final code review and cleanup of TODOs and debug code

**Checkpoint**: Plugin production-ready - all features integrated, tested, optimized

---

## Dependencies & Execution Strategy

### User Story Dependency Graph

```
Phase 1 (Setup) → Phase 2 (Foundational)
                       ↓
                  Phase 3 (US10) ← Must complete first (game lifecycle foundation)
                       ↓
        ┌──────────────┼──────────────┬──────────────┐
        ↓              ↓              ↓              ↓
    Phase 4 (US1)  Phase 5 (US9)  Phase 6 (US2)  Phase 7 (US3)
    [Basic Flow]   [Display]      [Treasures]    [Shop]
        ↓              ↓              ↓              ↓
        └──────────────┼──────────────┴──────────────┘
                       ↓
                  Phase 8 (US8) [Game End & Results]
                       ↓
        ┌──────────────┼──────────────┬──────────────┐
        ↓              ↓              ↓              ↓
    Phase 9 (US4)  Phase 10 (US5) Phase 11 (US7) Phase 12 (US6)
    [Events]       [Boss]         [Death]        [PvP/Murderer]
        ↓              ↓              ↓              ↓
        └──────────────┼──────────────┴──────────────┘
                       ↓
                  Phase 13 (Polish)
```

### Parallel Execution Opportunities

**After Phase 2 complete, these can run in parallel**:
- Phase 3 (US10) - MUST complete before others

**After Phase 3 complete, these can run in parallel**:
- Phase 4 (US1) + Phase 5 (US9) + Phase 6 (US2) + Phase 7 (US3)

**After Phases 4-7 complete, this blocks others**:
- Phase 8 (US8)

**After Phase 8 complete, these can run in parallel**:
- Phase 9 (US4) + Phase 10 (US5) + Phase 11 (US7) + Phase 12 (US6)

### MVP Scope Recommendation

**Minimum Viable Product (MVP)** should include:
- Phase 1: Setup
- Phase 2: Foundational
- Phase 3: US10 (Game initialization)
- Phase 4: US1 (Basic game flow)
- Phase 5: US9 (Display system)
- Phase 6: US2 (Treasure chests)
- Phase 8: US8 (Game end)

This MVP delivers a complete, playable game loop: players join → countdown → find treasures → earn points → game ends → results → auto-restart.

**Future Iterations** add:
- Iteration 2: US3 (Shop), US4 (Events), US7 (Death/Respawn)
- Iteration 3: US5 (Boss), US6 (PvP/Murderer)

---

## Task Summary

**Total Tasks**: 180
- Phase 1 (Setup): 6 tasks
- Phase 2 (Foundational): 14 tasks (T007-T020)
- Phase 3 (US10): 16 tasks (T021-T036)
- Phase 4 (US1): 16 tasks (T037-T052)
- Phase 5 (US9): 15 tasks (T053-T067)
- Phase 6 (US2): 15 tasks (T068-T082)
- Phase 7 (US3): 14 tasks (T083-T096)
- Phase 8 (US8): 9 tasks (T097-T105)
- Phase 9 (US4): 23 tasks (T106-T128)
- Phase 10 (US5): 14 tasks (T129-T142)
- Phase 11 (US7): 8 tasks (T143-T150)
- Phase 12 (US6): 14 tasks (T151-T164)
- Phase 13 (Polish): 16 tasks (T165-T180)

**Parallel Tasks**: 52 tasks marked with [P]

**Independent Test Criteria**:
- US10: Game starts clean, players reset properly
- US1: Players join, countdown works, game starts/ends
- US9: All displays show correctly
- US2: Chests spawn, points awarded, chests respawn
- US3: Shop opens, items purchasable
- US8: Results show, cleanup works, auto-restart
- US4: Events start, monsters spawn, points awarded
- US5: Boss spawns, boss bar shows, rewards given
- US7: Death/respawn works, equipment handled correctly
- US6: PvP blocked for teammates, Murderer penalties work

---

## Implementation Notes

1. **Start with MVP**: Focus on Phases 1-6 and 8 first for a playable game
2. **Test incrementally**: Test each phase's independent criteria before moving on
3. **MythicMobs configs**: Create YAML configs alongside Java implementation
4. **Database**: Ensure all database operations are async to avoid main thread blocking
5. **Performance**: Use TeleportUtils for safe player teleportation
6. **Cleanup**: Always use double-check cleanup (game end + game start)
7. **Config first**: Implement ConfigManager early to avoid hardcoded values
8. **Logging**: Add logging at each major operation for debugging
9. **Error handling**: Wrap all external plugin calls (MythicMobs) in try-catch

---

**Generated**: 2025-10-20
**Ready for**: `/speckit.implement` or manual implementation following task order
