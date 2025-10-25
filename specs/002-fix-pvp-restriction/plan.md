# Implementation Plan: PVP Restriction Bug Fix

**Branch**: `002-fix-pvp-restriction` | **Date**: 2025-10-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-fix-pvp-restriction/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Fix the PVP restriction system to immediately apply "Murderer" status when a player damages another non-murderer player, rather than waiting for the victim to die. The system must also prevent friendly fire between same-team players. Key changes include adding an EntityDamageByEntityEvent listener to detect damage immediately, checking team affiliation before applying penalties, and refactoring the death handler to separate kill/death counting from murderer status application.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Paper API 1.21.5, MythicMobs 5.3.5
**Storage**: SQLite with HikariCP connection pooling (existing PlayerData persistence)
**Testing**: JUnit Jupiter 5.9.3, Mockito 5.3.1, MockBukkit 3.9.0
**Target Platform**: Minecraft Paper server 1.21.5+
**Project Type**: Single Minecraft plugin project
**Performance Goals**: <1 second response time for murderer status application, handle 20 concurrent players without lag
**Constraints**: Must not interfere with existing game state, must preserve murderer status across player reconnects, event priority must not conflict with other plugins
**Scale/Scope**: Single plugin with ~10 modified/new classes in existing codebase, affects PvpListener and PlayerManager primarily

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: ✅ PASSED (No project constitution defined - using best practices)

Since no project-specific constitution exists, this feature will follow standard Minecraft plugin development best practices:
- Event-driven architecture using Bukkit event system
- Separation of concerns (listeners, managers, models)
- Existing project structure maintained
- Unit and integration testing where applicable
- No violations to justify

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
src/main/java/com/candyrush/
├── listeners/
│   └── PvpListener.java          # Modified: Add onPlayerDamage event handler
├── managers/
│   ├── PlayerManager.java        # Modified: Refactor setMurderer method
│   ├── GameManager.java          # Existing: Game state queries
│   └── TeamManager.java          # Existing: Team queries
├── models/
│   ├── PlayerData.java           # Existing: Murderer status persistence
│   └── TeamColor.java            # Existing: Team enumeration
├── utils/
│   └── MessageUtils.java         # Existing: Messaging utilities
└── CandyRushPlugin.java          # Existing: Main plugin class

src/test/java/com/candyrush/
├── listeners/
│   └── PvpListenerTest.java      # New: Unit tests for PVP logic
└── managers/
    └── PlayerManagerTest.java    # Modified: Add murderer status tests
```

**Structure Decision**: Using existing Minecraft plugin structure with standard Java package organization. This feature primarily modifies the `PvpListener` class and `PlayerManager` class to add immediate damage detection and team-aware murderer status application. All changes integrate into the existing event-driven architecture.

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

