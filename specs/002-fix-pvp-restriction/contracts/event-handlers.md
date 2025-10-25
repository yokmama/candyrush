# Event Handler Contracts

**Feature**: 002-fix-pvp-restriction
**Date**: 2025-10-25

## Overview

This document defines the contracts (interfaces) for event handlers modified or added in this feature. In Minecraft plugin development, event handlers are the primary API surface.

## PvpListener Event Handlers

### onPlayerDamage

**Purpose**: Detect player-vs-player damage immediately and apply murderer status on first hit

**Event**: `org.bukkit.event.entity.EntityDamageByEntityEvent`

**Priority**: `EventPriority.MONITOR`

**Ignore Cancelled**: `false`

**Preconditions**:
- Game must be in running state
- Both damager and entity must be Player instances
- Attacker and victim must be different players

**Input Parameters** (from event):
- `event.getEntity()` → `Player` (victim)
- `event.getDamager()` → `Player` (attacker)
- `event.getDamage()` → `double` (damage amount, for logging)
- `event.isCancelled()` → `boolean` (event cancellation status)

**Processing Logic**:

1. **Game State Check**:
   ```
   IF !gameManager.isGameRunning() THEN
       RETURN (ignore event)
   ```

2. **Entity Type Check**:
   ```
   IF entity NOT instanceof Player THEN
       RETURN (not PvP)
   IF damager NOT instanceof Player THEN
       RETURN (not player-caused)
   ```

3. **Self-Damage Check**:
   ```
   IF attacker.equals(victim) THEN
       RETURN (ignore self-damage)
   ```

4. **Team Check**:
   ```
   attackerTeam = playerManager.getPlayerData(attacker).getTeamColor()
   victimTeam = playerManager.getPlayerData(victim).getTeamColor()

   IF attackerTeam != null AND victimTeam != null AND attackerTeam == victimTeam THEN
       event.setCancelled(true)
       RETURN (friendly fire blocked)
   ```

5. **Murderer Status Check**:
   ```
   IF playerManager.isMurderer(victim.getUniqueId()) THEN
       RETURN (attacking murderer is allowed)
   ```

6. **Apply Murderer Penalty**:
   ```
   attackerData = playerManager.getPlayerData(attacker)
   applyMurdererPenalty(attacker, victim, attackerData)
   ```

**Postconditions**:
- If same team: event cancelled, no damage dealt
- If victim is murderer: no changes
- If victim is non-murderer: attacker becomes murderer (or extends timer)

**Side Effects**:
- Database write to update attacker's murderer status
- Broadcast announcement (conditional)
- Armor removal (first time only)
- Name color change to red (first time only)
- Boss manager notification (first time only)

**Error Handling**:
- If PlayerData not found: log warning, return early
- If database write fails: log error (handled by PlayerManager)

**Performance**:
- Target: <10ms per event
- Database writes: 1 per new murderer or timer extension

---

### onPlayerDeath

**Purpose**: Handle PvP death statistics and murderer kill credit

**Event**: `org.bukkit.event.entity.PlayerDeathEvent`

**Priority**: `EventPriority.NORMAL`

**Preconditions**:
- Game must be in running state

**Input Parameters** (from event):
- `event.getEntity()` → `Player` (victim)
- `event.getEntity().getKiller()` → `Player` (killer, nullable)

**Processing Logic**:

1. **Game State Check**:
   ```
   IF !gameManager.isGameRunning() THEN
       RETURN
   ```

2. **Murderer Item Drop Check**:
   ```
   IF playerManager.isMurderer(victim.getUniqueId()) THEN
       // Allow items to drop (default behavior)
   ELSE
       event.setKeepInventory(true)
       event.setKeepLevel(true)
       event.getDrops().clear()
   ```

3. **Non-PvP Death Check**:
   ```
   IF killer == null OR killer.equals(victim) THEN
       handleNonPvpDeath(victim)
       RETURN
   ```

4. **PvP Death Handling**:
   ```
   handlePvpDeath(killer, victim)
   ```

**Postconditions**:
- Victim death count incremented
- If victim was murderer: killer receives kill credit
- If victim was non-murderer: killer does NOT receive kill credit

**Side Effects**:
- Database writes for killer and victim statistics
- Team death count incremented
- Broadcast announcement if murderer was killed
- Item drops cleared for non-murderers

**Error Handling**:
- If PlayerData not found: log error, still increment death count

---

### onPlayerRespawn

**Purpose**: Re-grant shop items after respawn (existing, unmodified)

**Event**: `org.bukkit.event.player.PlayerRespawnEvent`

**Priority**: `EventPriority.NORMAL`

**Contract**: No changes to existing implementation

---

### onArmorEquip (InventoryClickEvent)

**Purpose**: Prevent murderers from equipping armor via inventory clicks (existing, unmodified)

**Event**: `org.bukkit.event.inventory.InventoryClickEvent`

**Priority**: `EventPriority.HIGHEST`

**Contract**: No changes to existing implementation

---

### onArmorEquipInteract (PlayerInteractEvent)

**Purpose**: Prevent murderers from equipping armor via right-click (existing, unmodified)

**Event**: `org.bukkit.event.player.PlayerInteractEvent`

**Priority**: `EventPriority.HIGHEST`

**Contract**: No changes to existing implementation

---

## Internal Method Contracts

### applyMurdererPenalty

**Purpose**: Apply murderer status and associated penalties to an attacking player

**Signature**:
```java
private void applyMurdererPenalty(Player killer, Player victim, PlayerData killerData)
```

**Parameters**:
- `killer: Player` - The player who attacked
- `victim: Player` - The player who was attacked
- `killerData: PlayerData` - The killer's data object

**Preconditions**:
- `killer` and `victim` must be non-null
- `killerData` must be non-null
- Caller must have verified victim is not a murderer

**Processing**:

1. Set murderer status:
   ```java
   boolean isFirstTime = playerManager.setMurderer(killer.getUniqueId(), 180);
   ```

2. Check last victim:
   ```java
   UUID lastVictimUuid = lastVictims.get(killer.getUniqueId());
   boolean isDifferentVictim = (lastVictimUuid == null || !lastVictimUuid.equals(victim.getUniqueId()));
   ```

3. Update tracking:
   ```java
   lastVictims.put(killer.getUniqueId(), victim.getUniqueId());
   ```

4. If first time:
   - Call `setPlayerNameRed(killer)`
   - Call `removeArmor(killer)`
   - Notify `bossManager.onPlayerBecomeMurderer(killer.getUniqueId())`
   - Broadcast full announcement (3 messages)
   - Send title to killer

5. Else if different victim:
   - Broadcast attack announcement (1 message)

6. Else (same victim):
   - Silent timer extension

**Postconditions**:
- Killer has murderer status (new or extended)
- Last victim tracker updated
- Announcements sent (conditional)
- Visual markers applied (first time only)

**Return**: `void`

---

### handlePvpDeath

**Purpose**: Process kill/death statistics for PvP deaths

**Signature**:
```java
private void handlePvpDeath(Player killer, Player victim)
```

**Parameters**:
- `killer: Player` - The player who caused the death
- `victim: Player` - The player who died

**Preconditions**:
- Both players must be non-null
- Caller must have verified killer is not victim

**Processing**:

1. Get player data:
   ```java
   PlayerData killerData = playerManager.getOrCreatePlayerData(killer);
   PlayerData victimData = playerManager.getOrCreatePlayerData(victim);
   ```

2. Check victim murderer status:
   ```java
   boolean victimWasMurderer = playerManager.isMurderer(victim.getUniqueId());
   ```

3. If victim was murderer:
   - `killerData.incrementKills()`
   - `victimData.incrementDeaths()`
   - Increment team kill/death counts
   - Broadcast murderer kill announcement

4. Else (victim was non-murderer):
   - Only `victimData.incrementDeaths()`
   - Increment team death count only

5. Save both player data objects

**Postconditions**:
- Statistics updated in database
- Team statistics updated
- Announcement sent (if murderer was killed)

**Return**: `void`

---

### setPlayerNameRed

**Purpose**: Apply red color to player's name in all locations

**Signature**:
```java
private void setPlayerNameRed(Player player)
```

**Parameters**:
- `player: Player` - The player whose name should turn red

**Processing**:

1. Set display name and tab list:
   ```java
   player.setDisplayName("§c" + player.getName());
   player.setPlayerListName("§c" + player.getName());
   ```

2. Get or create player scoreboard:
   ```java
   Scoreboard scoreboard = player.getScoreboard();
   if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
       scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
       player.setScoreboard(scoreboard);
   }
   ```

3. Create or get "murderer" team:
   ```java
   Team murdererTeam = scoreboard.getTeam("murderer");
   if (murdererTeam == null) {
       murdererTeam = scoreboard.registerNewTeam("murderer");
       murdererTeam.setColor(ChatColor.RED);
       murdererTeam.setPrefix("§c");
   }
   ```

4. Add player to team:
   ```java
   if (!murdererTeam.hasEntry(player.getName())) {
       murdererTeam.addEntry(player.getName());
   }
   ```

**Postconditions**:
- Player's display name is red
- Player's tab list name is red
- Player's overhead nametag is red

**Return**: `void`

---

### setPlayerNameWhite

**Purpose**: Restore player's name to default white color in all locations

**Signature**:
```java
private void setPlayerNameWhite(Player player)
```

**Parameters**:
- `player: Player` - The player whose name should return to white

**Processing**:

1. Reset display name and tab list:
   ```java
   player.setDisplayName("§f" + player.getName());
   player.setPlayerListName("§f" + player.getName());
   ```

2. Remove from murderer team:
   ```java
   Scoreboard scoreboard = player.getScoreboard();
   if (scoreboard != null) {
       Team murdererTeam = scoreboard.getTeam("murderer");
       if (murdererTeam != null && murdererTeam.hasEntry(player.getName())) {
           murdererTeam.removeEntry(player.getName());
       }
   }
   ```

**Postconditions**:
- Player's name is white in all three locations
- Player removed from scoreboard murderer team

**Return**: `void`

---

## PlayerManager API Contracts

### setMurderer (Modified)

**Purpose**: Set or extend murderer status for a player

**Signature**:
```java
public boolean setMurderer(UUID uuid, int durationSeconds)
```

**Parameters**:
- `uuid: UUID` - The player's unique identifier
- `durationSeconds: int` - Duration to add (typically 180 for 3 minutes)

**Preconditions**:
- `uuid` must correspond to a valid player with PlayerData

**Processing**:

1. Get PlayerData:
   ```java
   Optional<PlayerData> optData = getPlayerData(uuid);
   if (!optData.isPresent()) {
       logger.severe("Cannot set murderer - PlayerData not found for: " + uuid);
       return false;
   }
   PlayerData data = optData.get();
   ```

2. Check if first time or extension:
   ```java
   long now = System.currentTimeMillis() / 1000;
   boolean isFirstTime = !data.isMurdererActive();
   ```

3. Set or extend:
   ```java
   if (isFirstTime) {
       data.setMurderer(true);
       data.setMurdererUntil(now + durationSeconds);
   } else {
       long newUntil = Math.min(data.getMurdererUntil() + durationSeconds, now + 3600);
       data.setMurdererUntil(newUntil);
   }
   ```

4. Save:
   ```java
   savePlayerData(data);
   ```

**Postconditions**:
- PlayerData updated with murderer status
- Changes persisted to database

**Return**:
- `true` if this was the first time player became murderer
- `false` if player was already a murderer (timer extended)
- `false` if PlayerData not found

**Thread Safety**: Single-threaded (Bukkit main thread only)

---

## Testing Contracts

### Unit Test Interfaces

**PvpListenerTest**:

```java
@Test
void testDamageAppliesMurdererStatus() {
    // Given: Two players on different teams, neither murderer
    // When: Player A damages Player B
    // Then: Player A becomes murderer, Player B unchanged
}

@Test
void testSameTeamDamageCancelled() {
    // Given: Two players on same team
    // When: Player A damages Player B
    // Then: Event cancelled, no murderer status applied
}

@Test
void testMurdererVictimNoPenalty() {
    // Given: Player A (non-murderer), Player B (murderer)
    // When: Player A damages Player B
    // Then: No murderer status applied to Player A
}

@Test
void testFirstTimeVsRepeatOffense() {
    // Given: Player A damages Player B (first time)
    // Then: isFirstTime = true, armor removed, announcement
    // When: Player A damages Player C (second time)
    // Then: isFirstTime = false, timer extended, announcement
}

@Test
void testSameVictimNoAnnouncement() {
    // Given: Player A is murderer, last victim was Player B
    // When: Player A damages Player B again
    // Then: Timer extends, no announcement
}
```

**PlayerManagerTest**:

```java
@Test
void testSetMurdererReturnsBoolean() {
    // Given: Player with no murderer status
    // When: setMurderer() called
    // Then: Returns true (first time)
    // When: setMurderer() called again
    // Then: Returns false (already murderer)
}

@Test
void testTimerCap() {
    // Given: Player with 59 minutes murderer time
    // When: setMurderer(180) called
    // Then: Timer capped at 60 minutes
}
```

---

## Error Scenarios

| Scenario | Handler | Response |
|----------|---------|----------|
| PlayerData not found | onPlayerDamage | Log error, return early |
| Database write fails | setMurderer | Log error, status not persisted |
| Null team | Team check | Treat as different teams |
| Event already cancelled | onPlayerDamage | Still process (ignoreCancelled=false) |
| Killer is null | onPlayerDeath | Handle as non-PvP death |
| Self-damage | onPlayerDamage | Return early, no processing |

---

## Summary

This contract definition establishes:
- **2 modified event handlers**: `onPlayerDamage` (new), `onPlayerDeath` (modified logic)
- **1 modified manager method**: `setMurderer` (changed return type)
- **4 helper methods**: `applyMurdererPenalty`, `handlePvpDeath`, `setPlayerNameRed`, `setPlayerNameWhite`

All contracts are deterministic, testable, and maintain backward compatibility with existing game systems.
