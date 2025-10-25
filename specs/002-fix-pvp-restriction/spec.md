# Feature Specification: PVP Restriction Bug Fix

**Feature Branch**: `002-fix-pvp-restriction`
**Created**: 2025-10-25
**Status**: Draft
**Input**: User description: "PVPの制限機能のバグ修正をしたい"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Immediate Murderer Status on PvP Attack (Priority: P1)

When a player attacks another non-murderer player during an active game, the attacking player must immediately become a "Murderer" at the moment of the attack, not when the victim dies.

**Why this priority**: This is the core bug fix - the current system only applies murderer status when a player dies, allowing attackers to continue damaging victims without penalty until death occurs. This undermines the entire PVP restriction system.

**Independent Test**: Can be fully tested by having one player attack another without killing them, and verifying that murderer status, visual indicators (red name), and penalties (armor removal) are applied immediately upon damage.

**Acceptance Scenarios**:

1. **Given** Player A and Player B are in an active game, on different teams, and neither is a murderer, **When** Player A damages Player B with any attack, **Then** Player A immediately becomes a murderer with a 3-minute penalty timer
2. **Given** Player A and Player B are on the same team, **When** Player A attempts to attack Player B, **Then** the attack is blocked and no damage occurs (team attack prevention)
3. **Given** Player A has just damaged Player B (different team) and become a murderer, **When** the murderer status is applied, **Then** Player A's name turns red (display name, tab list, and overhead nametag), their armor is removed to inventory, and a broadcast announces the attack
4. **Given** Player A is a murderer and Player B is a non-murderer on a different team, **When** Player A damages Player B again, **Then** Player A's murderer timer extends by an additional 3 minutes (capped at 60 minutes total)
5. **Given** Player A is a non-murderer and Player B is a murderer, **When** Player A damages Player B, **Then** Player A does NOT become a murderer (attacking murderers is allowed without penalty)

---

### User Story 2 - Proper Kill/Death Counting (Priority: P2)

When a player dies in PvP combat, the kill and death statistics must be recorded correctly based on whether the victim was a murderer or regular player.

**Why this priority**: Accurate statistics are important for game balance and player progression, but are secondary to the core behavior of preventing PvP abuse through immediate murderer status.

**Independent Test**: Can be tested independently by reviewing player statistics after various PvP death scenarios - murderer kills should grant kill credit, regular player kills should not.

**Acceptance Scenarios**:

1. **Given** Player A (murderer) is killed by Player B, **When** the death occurs, **Then** Player B gains 1 kill and Player A gains 1 death, and a broadcast announces "Player B killed Murderer Player A"
2. **Given** Player A (non-murderer) is killed by Player B, **When** the death occurs, **Then** Player B does NOT gain a kill credit (only Player A gains 1 death)
3. **Given** a player dies from environmental damage (fall, lava, etc.), **When** the death occurs, **Then** only the death count increases (no killer involved)

---

### User Story 3 - Murderer Status Management and Recovery (Priority: P3)

Players who become murderers must have their status tracked with proper time accumulation, and must recover to normal status when the timer expires.

**Why this priority**: Status management ensures the penalty system works over time, but the immediate application (P1) is more critical for preventing abuse.

**Independent Test**: Can be tested by tracking a player's murderer status over time, verifying timer accumulation with repeated attacks, and confirming status removal when timer expires.

**Acceptance Scenarios**:

1. **Given** Player A is a murderer with 5 minutes remaining, **When** Player A attacks another non-murderer, **Then** the timer increases to 8 minutes (5 + 3)
2. **Given** Player A is a murderer with 59 minutes remaining, **When** Player A attacks another non-murderer, **Then** the timer is capped at 60 minutes (maximum penalty)
3. **Given** Player A is a murderer and their penalty timer reaches zero, **When** the timer expires, **Then** Player A's name returns to white color, they can equip armor again, and their murderer status is cleared
4. **Given** Player A is a murderer, **When** Player A logs out and logs back in during an active game, **Then** the murderer status and remaining penalty time are preserved

---

### Edge Cases

- What happens when Player A (on Red team) attacks Player B (also on Red team)? The attack should be completely blocked - no damage, no murderer penalty, maintaining friendly fire protection.
- What happens when Player A attacks Player B multiple times rapidly (within 1 second)? The system should extend the timer each time but only broadcast the first attack to avoid spam.
- What happens when a murderer attacks another murderer? Neither player should receive additional penalty since both are already marked as PK offenders.
- What happens when game ends while a player has murderer status? The murderer status should be cleared when the game ends.
- What happens when a player tries to equip armor while being a murderer? The system should prevent armor equipping through all methods (right-click, inventory drag, shift-click).
- What happens when self-damage occurs (player damages themselves)? No murderer penalty should be applied for self-damage.
- How does the system distinguish between the same victim being attacked repeatedly vs. different victims? Repeated attacks on the same victim should extend the timer but not re-broadcast announcements, while attacks on different victims should trigger new announcements.
- What happens when a player without a team attacks another player? The system should handle teamless players appropriately (either treat as different teams or prevent all PvP for teamless players).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST detect player-vs-player damage events immediately when damage occurs, not only on death
- **FR-002**: System MUST block all damage between players on the same team (friendly fire prevention)
- **FR-003**: System MUST apply murderer status to any player who damages another non-murderer player on a different team during active gameplay
- **FR-004**: System MUST visually mark murderers by changing their name color to red in three locations: display name, tab list, and overhead nametag
- **FR-005**: System MUST remove and prevent re-equipping of all armor pieces (helmet, chestplate, leggings, boots) when a player becomes a murderer for the first time
- **FR-006**: System MUST apply a 3-minute penalty timer when a player first becomes a murderer, and add 3 minutes for each subsequent PvP attack on non-murderers
- **FR-007**: System MUST enforce a maximum murderer penalty duration of 60 minutes regardless of how many attacks occur
- **FR-008**: System MUST NOT apply murderer penalties when a player attacks another player who already has murderer status
- **FR-009**: System MUST broadcast a server-wide announcement when a player first becomes a murderer or attacks a different victim
- **FR-010**: System MUST NOT broadcast repeated announcements when the same murderer attacks the same victim multiple times
- **FR-011**: System MUST award kill credit only when a murderer is killed by another player
- **FR-012**: System MUST NOT award kill credit when a non-murderer is killed by another player
- **FR-013**: System MUST record death counts for all player deaths regardless of murderer status
- **FR-014**: System MUST preserve murderer status and remaining penalty time when players disconnect and reconnect during an active game
- **FR-015**: System MUST restore normal player status (white name, armor equipping enabled) when the murderer penalty timer expires
- **FR-016**: System MUST clear all murderer statuses when a game session ends
- **FR-017**: System MUST ignore PvP events when game is not in an active running state
- **FR-018**: System MUST ignore self-damage events (player damaging themselves)
- **FR-019**: System MUST track which victim each murderer attacked most recently to control announcement frequency
- **FR-020**: System MUST prevent murderers from equipping armor through all methods: right-click equipping, shift-click from inventory, and direct inventory slot placement
- **FR-021**: System MUST provide clear feedback to murderers when they attempt to equip armor while penalized

### Key Entities

- **Murderer Status**: A temporary state applied to players who attack non-murderers, including penalty expiration timestamp, visual markers (red name), equipment restrictions (no armor), and accumulating timer (3 minutes per offense, max 60 minutes)
- **PvP Event**: An interaction between two players where damage is inflicted, tracked with attacker UUID, victim UUID, timestamp, and victim's murderer status at time of attack
- **Player Statistics**: Persistent counters for each player including kill count (only from defeating murderers), death count (all deaths), team affiliation, and current murderer status
- **Penalty Timer**: A countdown mechanism tracking remaining murderer penalty time, supporting accumulation from multiple offenses, capping at maximum duration, and triggering status removal on expiration

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of attacks between same-team players are blocked with zero damage dealt
- **SC-002**: Players receive murderer status and visual indicators within 1 second of damaging a non-murderer player on a different team
- **SC-003**: 100% of attacks on non-murderer players on different teams result in immediate murderer penalty application, preventing further unpunalized attacks
- **SC-004**: Murderer penalty timers correctly accumulate with 3-minute increments per offense and never exceed 60-minute maximum
- **SC-005**: Murderers cannot equip any armor pieces through any equipping method while penalty is active
- **SC-006**: Players can defeat murderers and receive proper kill credit without becoming murderers themselves
- **SC-007**: Murderer status persists correctly across player disconnects and reconnects during the same game session
- **SC-008**: Server performance remains stable with no noticeable lag when processing PvP events with up to 20 concurrent players
- **SC-009**: Zero instances of players bypassing murderer penalties through rapid attacks or timing exploits
