# Data Model: PVP Restriction Bug Fix

**Feature**: 002-fix-pvp-restriction
**Date**: 2025-10-25

## Overview

This feature primarily leverages existing data models with minimal modifications. The core entities (`PlayerData`, `TeamColor`) already support the required functionality. New transient state tracking is added for last-victim tracking.

## Entities

### PlayerData (Existing - No Schema Changes)

Represents persistent player game state including murderer status.

**Fields**:
- `uuid: UUID` - Player unique identifier
- `username: String` - Player name
- `teamColor: TeamColor` - Player's assigned team (nullable)
- `isMurderer: boolean` - Whether player is currently a murderer
- `murdererUntil: long` - Unix timestamp when murderer status expires
- `kills: int` - Total kill count (only from defeating murderers)
- `deaths: int` - Total death count
- `candies: int` - Player currency
- `treasureChestsOpened: int` - Treasure interaction count

**Validation Rules**:
- `murdererUntil` must be >= current timestamp when `isMurderer` is true
- `murdererUntil` capped at current time + 3600 seconds (60 minutes max)
- `kills` and `deaths` must be >= 0

**State Transitions**:
```
Normal Player → Murderer:
  - Trigger: Damage non-murderer player on different team
  - Changes: isMurderer = true, murdererUntil = now + 180 (3 minutes)
  - Side effects: Name turns red, armor removed

Murderer → Extended Penalty:
  - Trigger: Damage another non-murderer while already murderer
  - Changes: murdererUntil += 180 (capped at now + 3600)
  - Side effects: Timer extends, possible announcement

Murderer → Normal Player:
  - Trigger: murdererUntil timestamp reached
  - Changes: isMurderer = false, murdererUntil = 0
  - Side effects: Name returns to white, can equip armor
```

**Persistence**: SQLite via HikariCP, managed by `PlayerManager`

---

### TeamColor (Existing - No Changes)

Enum representing player team affiliations.

**Values**:
- `RED` - Red team
- `BLUE` - Blue team
- (Additional team colors as defined in existing system)

**Usage**:
- Determines friendly fire prevention (same team = no damage)
- Used for team statistics tracking
- Displayed in player UI and broadcasts

**Nullable**: Yes - players may not have a team assigned

---

### MurdererPenalty (Conceptual - Not Persisted)

Represents the temporary state applied when a player becomes a murderer. This is not a stored entity but a conceptual model of the penalty effects.

**Components**:
- **Visual Marker**: Red name in display name, tab list, overhead nametag
- **Equipment Restriction**: Cannot equip helmet, chestplate, leggings, boots
- **Timer**: Duration in seconds, accumulates with each offense (max 60 minutes)
- **Announcement**: Broadcast message on first offense or different victim

**Implementation**:
- Visual marker: Scoreboard team "murderer" with `ChatColor.RED` and prefix "§c"
- Equipment restriction: Event cancellation in `InventoryClickEvent` and `PlayerInteractEvent`
- Timer: Stored as `murdererUntil` timestamp in `PlayerData`
- Announcement: Controlled by `lastVictims` map in `PvpListener`

---

### LastVictimTracker (New - Transient)

In-memory map tracking the last player each murderer attacked to control announcement spam.

**Structure**: `Map<UUID, UUID>`
- Key: Attacker (murderer) UUID
- Value: Last victim UUID

**Lifecycle**:
- **Created**: When `PvpListener` is instantiated
- **Updated**: Every time murderer damages a non-murderer
- **Cleared**: Server restart (not persisted)

**Purpose**: Enables different announcement behavior:
- First attack: Always announce
- Same victim repeated attacks: Silent (timer extends, no announcement)
- Different victim attack: Announce

**Example**:
```java
// Player A attacks Player B
lastVictims.put(playerA_UUID, playerB_UUID);

// Player A attacks Player B again → same victim
UUID lastVictim = lastVictims.get(playerA_UUID);
boolean isDifferent = !lastVictim.equals(playerB_UUID); // false → no announcement

// Player A attacks Player C → different victim
UUID lastVictim = lastVictims.get(playerA_UUID);
boolean isDifferent = !lastVictim.equals(playerC_UUID); // true → announce
lastVictims.put(playerA_UUID, playerC_UUID); // update tracking
```

---

## Relationships

```
Player (Bukkit) 1:1 PlayerData
  - Player identified by UUID
  - PlayerData persisted in database
  - Bidirectional: Player → UUID → PlayerData lookup

PlayerData N:1 TeamColor
  - Many players can be on same team
  - Nullable: player may have no team
  - Used for friendly fire checks

PvpListener 1:1 LastVictimTracker
  - PvpListener owns the map
  - Map lifetime tied to listener instance
  - Not shared with other components
```

## Data Flow

### Damage Event → Murderer Status

1. `EntityDamageByEntityEvent` fires
2. `PvpListener.onPlayerDamage()` extracts:
   - Attacker UUID
   - Victim UUID
3. Query `PlayerManager`:
   - Get attacker `PlayerData`
   - Get victim `PlayerData`
4. Check teams: `attackerData.getTeamColor()` vs `victimData.getTeamColor()`
5. Check murderer status: `PlayerManager.isMurderer(victimUUID)`
6. Apply penalty: `PlayerManager.setMurderer(attackerUUID, 180)` → returns `isFirstTime`
7. Update tracker: `lastVictims.put(attackerUUID, victimUUID)`
8. Save: `PlayerManager.savePlayerData(attackerData)`

### Murderer Expiry → Normal Status

1. Scheduled task checks `murdererUntil` timestamps
2. For expired murderers: `PlayerManager.clearMurderer(uuid)`
3. Update player state:
   - Set `isMurderer = false`
   - Set `murdererUntil = 0`
   - Call `setPlayerNameWhite(player)` if online
4. Save: `PlayerManager.savePlayerData()`

### Death Event → Statistics

1. `PlayerDeathEvent` fires
2. `PvpListener.onPlayerDeath()` extracts:
   - Victim UUID
   - Killer UUID (nullable)
3. Check victim murderer status: `PlayerManager.isMurderer(victimUUID)`
4. If victim was murderer:
   - Increment killer kills: `killerData.incrementKills()`
   - Increment victim deaths: `victimData.incrementDeaths()`
5. If victim was non-murderer:
   - Only increment victim deaths (no kill credit for killer)
6. Save both player data objects

## Schema Impact

**Database Changes**: None

All required fields already exist in the `PlayerData` table:
- `is_murderer` (boolean)
- `murderer_until` (long/integer)
- `kills`, `deaths`, `team_color`, etc.

**Migration**: Not required

## Validation Logic

### Team Comparison

```java
boolean isSameTeam(PlayerData attacker, PlayerData victim) {
    TeamColor attackerTeam = attacker.getTeamColor();
    TeamColor victimTeam = victim.getTeamColor();

    // Null-safe comparison
    if (attackerTeam == null || victimTeam == null) {
        return false; // Treat teamless as different teams
    }

    return attackerTeam == victimTeam;
}
```

### Murderer Timer Cap

```java
long calculateNewUntil(long currentUntil, int durationSeconds) {
    long now = System.currentTimeMillis() / 1000;
    long maxUntil = now + 3600; // 60 minutes
    long newUntil = currentUntil + durationSeconds;

    return Math.min(newUntil, maxUntil);
}
```

### First-Time Detection

```java
boolean setMurderer(UUID uuid, int durationSeconds) {
    PlayerData data = getPlayerData(uuid).orElseThrow();
    long now = System.currentTimeMillis() / 1000;

    if (!data.isMurdererActive()) {
        // First time
        data.setMurderer(true);
        data.setMurdererUntil(now + durationSeconds);
        savePlayerData(data);
        return true;
    } else {
        // Already murderer, extend time
        long newUntil = calculateNewUntil(data.getMurdererUntil(), durationSeconds);
        data.setMurdererUntil(newUntil);
        savePlayerData(data);
        return false;
    }
}
```

## Indexes and Performance

**Existing Indexes**:
- Primary key on `uuid` (PlayerData table)
- Queries are by UUID only (no scans)

**Query Patterns**:
- Lookup by UUID: O(1) with index
- In-memory murderer check: O(1) map lookup
- Team comparison: O(1) field access

**Performance Characteristics**:
- Database writes: Only on murderer status change (~1-5 times per PvP encounter)
- Database reads: Cached in memory (`PlayerManager` manages cache)
- Event handler: <1ms per damage event (quick conditional checks)

## Testing Data

### Test Scenarios

1. **Normal player attacks non-murderer (different team)**:
   - Before: `isMurderer=false, murdererUntil=0`
   - After: `isMurderer=true, murdererUntil=now+180`

2. **Murderer attacks another non-murderer**:
   - Before: `isMurderer=true, murdererUntil=now+180`
   - After: `isMurderer=true, murdererUntil=now+360`

3. **Timer reaches cap**:
   - Before: `murdererUntil=now+3540` (59 minutes)
   - Attack: +180 seconds
   - After: `murdererUntil=now+3600` (capped at 60 minutes)

4. **Same team attack**:
   - Event cancelled before any data changes
   - PlayerData unchanged

5. **Attack on murderer**:
   - No changes to attacker PlayerData
   - Only victim death counted if killed

## Summary

This feature requires **no schema changes**. All functionality is achieved through:
- Existing `PlayerData` entity with `isMurderer` and `murdererUntil` fields
- Existing `TeamColor` enum for team affiliation
- New transient `lastVictims` map for announcement control
- Minecraft Scoreboard teams for visual markers (not persisted)

The data model is minimal, efficient, and fully compatible with the existing codebase.
