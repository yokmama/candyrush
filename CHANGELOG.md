# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Team System Redesign**: Changed from 4-team to 3-team system with RED as Murderer-only team
  - **Game Teams**: BLUE, GREEN, YELLOW (3 teams for normal gameplay)
  - **Murderer Team**: RED (exclusive for Murderer状態 players)
  - Players are now distributed across 3 teams instead of 4
  - When a player becomes a Murderer, they temporarily move to the RED team
  - Murderers are excluded from their original team during Murderer状態
  - Murderers can attack and be attacked by their former teammates
  - Murderer points still contribute to their original team (not RED team)
  - Updated all team-related logic in ScoreboardManager, PlayerManager, TeamManager
  - Updated config.yml to list only 3 game teams (BLUE, GREEN, YELLOW)
  - Updated PvpListener to exclude Murderers from team protection

### Fixed
- **GitHub Actions Build**: Fixed build failure in CI/CD pipeline
  - Removed hardcoded Java path from `gradle.properties` that was specific to local development environment
  - Made `build.gradle` runServer task use `JAVA_HOME` environment variable instead of hardcoded path
  - Added Gradle cache to GitHub Actions workflow for faster builds
  - Added `--no-daemon` flag to prevent Gradle daemon issues in CI environment
  - Added `permissions: contents: write` to workflow to allow creating GitHub releases


- **Murderer System Implementation**: Corrected the Murderer trigger condition
  - **Previous (incorrect)**: Players became Murderers when reaching first place or team-killing
  - **Current (correct)**: Players become Murderers when attacking non-Murderer players (PK behavior)
  - Attacking a Murderer is now penalty-free (treated as self-defense)
  - Duration system: 3 minutes initially, +3 minutes per attack, maximum 60 minutes
  - Penalties include: red name tag, armor removal, cannot re-equip armor, drops all items on death

### Changed
- **Documentation Updates**: Updated all documentation to reflect correct implementation
  - Updated `spec.md`: Fixed User Story 6 (PvP and Murderer System) and Functional Requirements FR-039, FR-040, FR-040-1, FR-041, FR-042, FR-043, FR-076
  - Updated `docs/index.html`: Corrected Murderer system description for both Japanese and English versions
  - Updated `summary.md`: Fixed Murderer System section and tips
  - Updated `QUICKSTART.md`: Corrected Murderer System description
  - All documentation now accurately describes the Murderer system as a PvP deterrent rather than a reward

- **Treasure Chest System Documentation**: Corrected chest type descriptions to match implementation
  - **11 chest types**: CHEST, BARREL, BREWING_STAND, FURNACE, BLAST_FURNACE, SMOKER, DROPPER, DISPENSER, HOPPER, TRAPPED_CHEST (ChestType enum)
  - **Category-based loot**: Each chest type provides items from specific categories (ChestLootCategory enum)
    - CHEST/BARREL → Food items
    - BREWING_STAND → Potions
    - FURNACE types → Materials
    - DROPPER/DISPENSER → Equipment
    - HOPPER → Utility items
    - TRAPPED_CHEST → High-tier equipment + 18.0 damage (9 hearts, near-death!)
  - Updated `docs/index.html`, `summary.md`, `QUICKSTART.md` to accurately describe all 11 chest types
  - Previous documentation incorrectly described only "regular chests" and "trapped chests"

- **CRITICAL DOCUMENTATION FIXES**: Removed incorrect references to abolished `/convert` command
  - **`/convert` command was abolished**: ConvertCommand class does not exist in codebase
  - **Food conversion reality**: Clicking food in treasure chests converts to **personal points ONLY** (no team points)
  - Removed all `/convert` command references from `docs/index.html`, `summary.md`, `QUICKSTART.md`
  - Updated all tips and recommendations to reflect accurate point conversion behavior
  - Previous documentation incorrectly suggested `/convert` gives both personal and team points

- **Spawn System Documentation Fix**: Corrected player teleportation behavior at game start
  - **Actual implementation**: Players teleport to random locations within 50-block radius of map center (GameManager.java:600-622)
  - **Previous (incorrect) documentation**: Stated players teleport to team-specific bases in map corners
  - Updated `docs/index.html`, `summary.md`, `QUICKSTART.md` to accurately describe random center spawn
  - Removed incorrect references to "Northwestern", "Southwestern", "Southeastern" team bases

### Technical Details
- Modified `TeamColor.java`:
  - Updated comments to clarify RED is Murderer-only team
  - Regular game teams: BLUE, GREEN, YELLOW (3 teams)

- Modified `ScoreboardManager.java`:
  - `setupMainScoreboardTeams()`: Creates 4 scoreboard teams (blue, green, yellow, red) instead of 2 (murderer, normal)
  - `copyTeamsFromMainScoreboard()`: Copies all 4 team colors to player scoreboards
  - Each team has distinct color prefix for name tag display

- Modified `PlayerManager.java`:
  - `updatePlayerTeamColor()`: Assigns scoreboard team based on Murderer status and game team
  - `getScoreboardTeamName()`: Returns "red" for Murderers, otherwise returns actual team color (blue/green/yellow)
  - `updateTeamInScoreboard()`: Removes player from all 4 teams before adding to target team

- Modified `TeamManager.java`:
  - `distributePlayersEvenly()`: Distributes players across 3 game teams (BLUE, GREEN, YELLOW) only
  - `getSmallestTeam()`: Excludes RED team from balance calculations
  - Team distribution logs updated to reflect 3 teams

- Modified `config.yml`:
  - `teams.colors`: Lists only BLUE, GREEN, YELLOW (removed RED from game teams)
  - Added comments explaining RED is reserved for Murderers

- Modified `PvpListener.java`:
  - Updated team protection logic to exclude Murderers
  - Both attacker and victim must not be Murderers for team protection to apply
  - Murderers can attack and be attacked by their original teammates

- Modified `PvpListener.java`:
  - Changed `handlePvpKill()` method to check if victim is a Murderer
  - If victim is Murderer: no penalty for killer (self-defense)
  - If victim is not Murderer: killer becomes Murderer (PK behavior)
  - Updated messages from "team kill" to "PK behavior"
  - Removed team-based distinction for Murderer triggering

- Food conversion implementation (no code changes - documentation only):
  - `TreasureChestListener.java` already implements auto-conversion correctly (lines 108-129)
  - `ConvertCommand.java` provides backup bulk conversion functionality
  - Documentation updated to match existing implementation

### Notes
- The Murderer system is designed as a **PvP deterrent**, not a PvP reward system
- This game encourages **cooperative gameplay** over player-versus-player combat
- Specification documents (`spec.md`) have been updated to match the current implementation
- All player-facing documentation now consistently describes the correct behavior
