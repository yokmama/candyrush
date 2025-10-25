# Tasks: PVP Restriction Bug Fix

**Input**: Design documents from `/specs/002-fix-pvp-restriction/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are NOT included as they were not requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions
- **Single Minecraft plugin project**: `src/main/java/com/candyrush/`, `src/test/java/com/candyrush/`
- Paths are absolute from repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify existing project structure and dependencies

- [ ] T001 Verify Java 21 and Gradle build configuration in build.gradle
- [ ] T002 Verify Paper API 1.21.5 dependency in build.gradle
- [ ] T003 [P] Verify test dependencies (JUnit Jupiter, Mockito, MockBukkit) in build.gradle
- [ ] T004 [P] Run ./gradlew build to ensure project compiles successfully

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core refactoring that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Refactor PlayerManager.setMurderer() method to return boolean instead of using array wrapper in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T006 [P] Add setPlayerNameRed() helper method to PlayerManager in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T007 [P] Add setPlayerNameWhite() helper method to PlayerManager in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T008 Update PlayerManager.clearMurderer() to use setPlayerNameWhite() helper in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T009 Update PlayerManager.updatePlayerNameColor() to use setPlayerNameRed/White helpers in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T010 Add lastVictims map field to PvpListener class in src/main/java/com/candyrush/listeners/PvpListener.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Immediate Murderer Status on PvP Attack (Priority: P1) 🎯 MVP

**Goal**: Apply murderer status immediately when a player damages another non-murderer player, with team-based friendly fire prevention

**Independent Test**: Have one player attack another without killing them, and verify that murderer status, visual indicators (red name), and penalties (armor removal) are applied immediately upon damage. Test same-team attacks are blocked with no damage.

### Implementation for User Story 1

- [ ] T011 [US1] Add onPlayerDamage event handler method skeleton to PvpListener in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T012 [US1] Implement game state check in onPlayerDamage (verify game is running) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T013 [US1] Implement entity type validation in onPlayerDamage (both must be players) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T014 [US1] Implement self-damage check in onPlayerDamage (ignore if attacker equals victim) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T015 [US1] Implement team affiliation check in onPlayerDamage (get both players' teams) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T016 [US1] Implement same-team damage cancellation in onPlayerDamage (cancel event if same team) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T017 [US1] Implement victim murderer status check in onPlayerDamage (allow attacking murderers) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T018 [US1] Call applyMurdererPenalty when victim is non-murderer in onPlayerDamage in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T019 [US1] Add setPlayerNameRed() helper method to PvpListener (duplicate from PlayerManager for convenience) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T020 [US1] Add setPlayerNameWhite() helper method to PvpListener (duplicate from PlayerManager for convenience) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T021 [US1] Update applyMurdererPenalty to use new setMurderer return value (boolean instead of array) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T022 [US1] Update applyMurdererPenalty to track last victim in lastVictims map in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T023 [US1] Update applyMurdererPenalty to call setPlayerNameRed on first-time murderers in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T024 [US1] Update applyMurdererPenalty announcement logic to handle first-time vs repeat vs same-victim cases in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T025 [US1] Add debug logging to onPlayerDamage for troubleshooting (INFO level) in src/main/java/com/candyrush/listeners/PvpListener.java

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Players can attack each other and murderer status applies immediately. Same-team attacks are blocked.

---

## Phase 4: User Story 2 - Proper Kill/Death Counting (Priority: P2)

**Goal**: Record kill and death statistics correctly based on whether the victim was a murderer or regular player

**Independent Test**: Review player statistics after various PvP death scenarios - murderer kills should grant kill credit, regular player kills should not.

### Implementation for User Story 2

- [ ] T026 [US2] Extract handlePvpDeath method from onPlayerDeath in PvpListener in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T027 [US2] Remove murderer status application logic from onPlayerDeath (this is now in onPlayerDamage) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T028 [US2] Implement murderer victim check in handlePvpDeath (check if victim was murderer) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T029 [US2] Implement kill credit logic for murderer victims in handlePvpDeath (increment killer kills, victim deaths) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T030 [US2] Implement death-only logic for non-murderer victims in handlePvpDeath (increment victim deaths only, no kill credit) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T031 [US2] Update team statistics in handlePvpDeath (increment team kills/deaths appropriately) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T032 [US2] Update broadcast announcement for murderer kills in handlePvpDeath in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T033 [US2] Add debug logging to handlePvpDeath for statistics tracking in src/main/java/com/candyrush/listeners/PvpListener.java

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Murderer status applies on damage (US1), and statistics are counted correctly on death (US2).

---

## Phase 5: User Story 3 - Murderer Status Management and Recovery (Priority: P3)

**Goal**: Track murderer status with proper time accumulation, and restore normal status when timer expires

**Independent Test**: Track a player's murderer status over time, verify timer accumulation with repeated attacks, and confirm status removal when timer expires.

### Implementation for User Story 3

- [ ] T034 [US3] Verify murderer timer accumulation logic in PlayerManager.setMurderer() (add 3 minutes per offense) in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T035 [US3] Verify 60-minute cap logic in PlayerManager.setMurderer() (cap at maxUntil) in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T036 [US3] Verify murderer status persistence across reconnects (PlayerData already persists murdererUntil to database) in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T037 [US3] Verify existing scheduled task calls clearMurderer when timer expires (check existing timer cleanup code) in src/main/java/com/candyrush/managers/PlayerManager.java or CandyRushPlugin
- [ ] T038 [US3] Verify clearMurderer properly resets status and calls setPlayerNameWhite (already updated in T008) in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T039 [US3] Add logging for murderer status expiry in clearMurderer in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T040 [US3] Verify armor equipping prevention logic still works (existing InventoryClickEvent and PlayerInteractEvent handlers) in src/main/java/com/candyrush/listeners/PvpListener.java

**Checkpoint**: All user stories should now be independently functional. Murderer status applies immediately (US1), statistics are accurate (US2), and status management works correctly over time (US3).

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T041 [P] Remove or reduce debug logging statements added during development in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T042 [P] Remove or reduce debug logging statements added during development in src/main/java/com/candyrush/managers/PlayerManager.java
- [ ] T043 Code review: Verify all event handler priorities are correct (MONITOR for damage, NORMAL for death) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T044 Code review: Verify null-safety for team comparisons (handle null teams gracefully) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T045 Code review: Verify scoreboard team creation doesn't conflict with existing team system in src/main/java/com/candyrush/listeners/PvpListener.java and PlayerManager.java
- [ ] T046 Performance check: Verify damage event handler completes in <10ms (minimal database writes) in src/main/java/com/candyrush/listeners/PvpListener.java
- [ ] T047 Run manual testing scenarios from quickstart.md (same-team attack, murderer status, kill credit, etc.)
- [ ] T048 Run ./gradlew build to ensure all changes compile successfully
- [ ] T049 [P] Update CLAUDE.md if any new patterns or conventions emerged during implementation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Modifies same file as US1 but different methods - can work in parallel with coordination
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Primarily verification of existing functionality

### Within Each User Story

- Tasks within US1 are mostly sequential (building up the onPlayerDamage handler step by step)
- Tasks within US2 are mostly sequential (refactoring onPlayerDeath method)
- Tasks within US3 are mostly parallel verification tasks

### Parallel Opportunities

- Phase 1: T003 and T004 can run in parallel
- Phase 2: T006 and T007 can run in parallel (different helper methods)
- Phase 3 (US1): Most tasks are sequential due to building up logic in same method
- Phase 4 (US2): Most tasks are sequential due to refactoring same method
- Phase 5 (US3): T034, T035, T036, T037, T038, T040 can all run in parallel (verification tasks)
- Phase 6: T041, T042, and T049 can run in parallel

**Note**: While US2 and US3 could theoretically start in parallel with US1, they all modify PvpListener.java which creates merge conflicts. Recommended to complete sequentially in priority order (P1 → P2 → P3).

---

## Parallel Example: User Story 3

```bash
# Launch all verification tasks for User Story 3 together:
Task T034: "Verify murderer timer accumulation logic in PlayerManager.setMurderer()"
Task T035: "Verify 60-minute cap logic in PlayerManager.setMurderer()"
Task T036: "Verify murderer status persistence across reconnects"
Task T037: "Verify existing scheduled task calls clearMurderer when timer expires"
Task T038: "Verify clearMurderer properly resets status"
Task T040: "Verify armor equipping prevention logic still works"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify environment)
2. Complete Phase 2: Foundational (CRITICAL - refactor base methods)
3. Complete Phase 3: User Story 1 (immediate murderer status + team protection)
4. **STOP and VALIDATE**: Manual testing per quickstart.md scenarios
5. Test independently: same-team attacks blocked, murderer status on damage
6. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! Core bug is fixed)
3. Add User Story 2 → Test independently → Deploy/Demo (Statistics now accurate)
4. Add User Story 3 → Test independently → Deploy/Demo (Status management validated)
5. Each story adds value without breaking previous stories

### Sequential Team Strategy (Recommended)

Due to file conflicts in PvpListener.java:

1. Complete Setup + Foundational together
2. Complete User Story 1 fully (core bug fix)
3. Complete User Story 2 fully (statistics fix)
4. Complete User Story 3 fully (status management verification)
5. Run Polish phase
6. Final validation and testing

### Alternative: Careful Parallel Strategy

With careful coordination on PvpListener.java:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (onPlayerDamage method)
   - Developer B: User Story 2 (onPlayerDeath method) - coordinate on same file
   - Developer C: User Story 3 (verification tasks) - mostly in PlayerManager
3. Stories complete and integrate with merge conflict resolution

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- **File conflict warning**: PvpListener.java is modified in US1, US2, and US3 - sequential implementation recommended
- Manual testing scenarios available in quickstart.md
- All tasks follow strict checklist format: `- [ ] [ID] [P?] [Story?] Description`

---

## Task Summary

**Total Tasks**: 49
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (User Story 1 - P1): 15 tasks
- Phase 4 (User Story 2 - P2): 8 tasks
- Phase 5 (User Story 3 - P3): 7 tasks
- Phase 6 (Polish): 9 tasks

**Parallel Opportunities**: 11 tasks marked [P]

**Independent Test Criteria**:
- **US1**: Attack player without killing → murderer status applies immediately, name red, armor removed. Same-team attack → blocked, no damage.
- **US2**: Kill murderer → killer gets kill credit. Kill non-murderer → killer gets NO kill credit, only death counted.
- **US3**: Multiple attacks → timer accumulates (max 60 min). Timer expires → name white, can equip armor. Reconnect → status preserved.

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1 only) = Core bug fix with immediate murderer status application
