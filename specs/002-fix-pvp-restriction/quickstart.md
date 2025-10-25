# Quick Start: PVP Restriction Bug Fix

**Feature**: 002-fix-pvp-restriction
**Branch**: `002-fix-pvp-restriction`
**Date**: 2025-10-25

## Overview

This guide provides a quick reference for implementing the PVP restriction bug fix that applies murderer status immediately on damage rather than on death.

## What's Being Fixed

**Current Bug**: Murderer penalties only apply when a victim dies, allowing attackers to damage players repeatedly without immediate consequences.

**Solution**: Add damage event handler to detect and penalize PvP attacks immediately on first hit.

## Files to Modify

### Primary Changes

1. **`src/main/java/com/candyrush/listeners/PvpListener.java`**
   - Add `onPlayerDamage()` method (new)
   - Modify `onPlayerDeath()` method (refactor)
   - Add `setPlayerNameRed()` helper (new)
   - Add `setPlayerNameWhite()` helper (new)

2. **`src/main/java/com/candyrush/managers/PlayerManager.java`**
   - Refactor `setMurderer()` to return `boolean` instead of using array wrapper
   - Update `clearMurderer()` to use `setPlayerNameWhite()` helper
   - Update `updatePlayerNameColor()` to use red/white helpers

### Supporting Changes

3. **`src/test/java/com/candyrush/listeners/PvpListenerTest.java`** (new file)
   - Unit tests for damage handler logic
   - Test cases for team checks, murderer detection, etc.

## Implementation Checklist

### Step 1: Add Damage Event Handler

**File**: `PvpListener.java`

```java
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
public void onPlayerDamage(EntityDamageByEntityEvent event) {
    // 1. Check game is running
    if (!plugin.getGameManager().isGameRunning()) {
        return;
    }

    // 2. Verify both are players
    if (!(event.getEntity() instanceof Player)) return;
    if (!(event.getDamager() instanceof Player)) return;

    Player victim = (Player) event.getEntity();
    Player attacker = (Player) event.getDamager();

    // 3. Ignore self-damage
    if (attacker.equals(victim)) {
        return;
    }

    // 4. Check teams - cancel if same team
    PlayerData attackerData = plugin.getPlayerManager().getOrCreatePlayerData(attacker);
    PlayerData victimData = plugin.getPlayerManager().getOrCreatePlayerData(victim);

    if (attackerData.getTeamColor() != null &&
        victimData.getTeamColor() != null &&
        attackerData.getTeamColor() == victimData.getTeamColor()) {
        event.setCancelled(true);
        return;
    }

    // 5. Check if victim is murderer (attacking murderers is allowed)
    if (plugin.getPlayerManager().isMurderer(victim.getUniqueId())) {
        return;
    }

    // 6. Apply murderer penalty
    applyMurdererPenalty(attacker, victim, attackerData);
}
```

**Key Points**:
- ✅ Use `EventPriority.MONITOR` and `ignoreCancelled = false`
- ✅ Early returns for invalid cases
- ✅ Team check before murderer logic
- ✅ Cancel event for same-team attacks

---

### Step 2: Refactor Death Handler

**File**: `PvpListener.java`

Update `onPlayerDeath()` to separate kill/death counting from murderer status application:

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onPlayerDeath(PlayerDeathEvent event) {
    if (!plugin.getGameManager().isGameRunning()) {
        return;
    }

    Player victim = event.getEntity();
    Player killer = victim.getKiller();

    // Item drop logic (murderers drop items)
    boolean victimIsMurderer = plugin.getPlayerManager().isMurderer(victim.getUniqueId());
    if (!victimIsMurderer) {
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    // Non-PvP death
    if (killer == null || killer.equals(victim)) {
        handleNonPvpDeath(victim);
        return;
    }

    // PvP death - statistics only (murderer status already applied in damage handler)
    handlePvpDeath(killer, victim);
}
```

**Changes**:
- ❌ Remove murderer status application logic (now in damage handler)
- ✅ Keep kill/death counting logic
- ✅ Keep item drop logic

---

### Step 3: Refactor setMurderer Method

**File**: `PlayerManager.java`

Change return type from `void` to `boolean`:

```java
public boolean setMurderer(UUID uuid, int durationSeconds) {
    Optional<PlayerData> optData = getPlayerData(uuid);

    if (!optData.isPresent()) {
        plugin.getLogger().severe("Cannot set murderer - PlayerData not found for: " + uuid);
        return false;
    }

    PlayerData data = optData.get();
    long now = System.currentTimeMillis() / 1000;
    long maxUntil = now + 3600; // 60 minutes max
    boolean isFirstTime = false;

    if (!data.isMurdererActive()) {
        // First time
        isFirstTime = true;
        data.setMurderer(true);
        long until = now + durationSeconds;
        data.setMurdererUntil(Math.min(until, maxUntil));
    } else {
        // Extension
        long currentUntil = data.getMurdererUntil();
        long newUntil = Math.min(currentUntil + durationSeconds, maxUntil);
        data.setMurdererUntil(newUntil);
    }

    savePlayerData(data);
    return isFirstTime;
}
```

**Changes**:
- ❌ Remove `final boolean[] isFirstTime` array wrapper
- ✅ Return `boolean` directly
- ✅ Return `false` if PlayerData not found

---

### Step 4: Add Name Color Helpers

**File**: `PvpListener.java` AND `PlayerManager.java`

Add these helper methods to both classes (for redundancy and convenience):

```java
private void setPlayerNameRed(Player player) {
    // Display name and tab list
    player.setDisplayName("§c" + player.getName());
    player.setPlayerListName("§c" + player.getName());

    // Overhead nametag via Scoreboard
    Scoreboard scoreboard = player.getScoreboard();
    if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(scoreboard);
    }

    Team murdererTeam = scoreboard.getTeam("murderer");
    if (murdererTeam == null) {
        murdererTeam = scoreboard.registerNewTeam("murderer");
        murdererTeam.setColor(ChatColor.RED);
        murdererTeam.setPrefix("§c");
    }

    if (!murdererTeam.hasEntry(player.getName())) {
        murdererTeam.addEntry(player.getName());
    }
}

private void setPlayerNameWhite(Player player) {
    // Display name and tab list
    player.setDisplayName("§f" + player.getName());
    player.setPlayerListName("§f" + player.getName());

    // Remove from Scoreboard team
    Scoreboard scoreboard = player.getScoreboard();
    if (scoreboard != null) {
        Team murdererTeam = scoreboard.getTeam("murderer");
        if (murdererTeam != null && murdererTeam.hasEntry(player.getName())) {
            murdererTeam.removeEntry(player.getName());
        }
    }
}
```

**Key Points**:
- ✅ Sets color in 3 places: display name, tab list, overhead nametag
- ✅ Uses per-player scoreboard (not global)
- ✅ Creates "murderer" team dynamically

---

### Step 5: Update applyMurdererPenalty

**File**: `PvpListener.java`

Update to use new `setMurderer()` return value:

```java
private void applyMurdererPenalty(Player killer, Player victim, PlayerData killerData) {
    int durationSeconds = 3 * 60; // 3 minutes

    // Get first-time status from setMurderer
    boolean isFirstTime = plugin.getPlayerManager().setMurderer(killer.getUniqueId(), durationSeconds);

    // Check last victim
    UUID lastVictimUuid = lastVictims.get(killer.getUniqueId());
    boolean isDifferentVictim = (lastVictimUuid == null || !lastVictimUuid.equals(victim.getUniqueId()));

    // Update tracking
    lastVictims.put(killer.getUniqueId(), victim.getUniqueId());

    if (isFirstTime) {
        // First time - full penalty
        setPlayerNameRed(killer);
        removeArmor(killer);
        plugin.getBossManager().onPlayerBecomeMurderer(killer.getUniqueId());

        // Announcements (3 messages)
        Bukkit.broadcastMessage(...);
        MessageUtils.sendTitle(killer, ...);
    } else if (isDifferentVictim) {
        // Different victim - announce only
        Bukkit.broadcastMessage(...);
    }
    // Same victim - silent extension
}
```

**Changes**:
- ❌ Remove array wrapper usage: `isFirstTime[0]`
- ✅ Use direct boolean return: `boolean isFirstTime = setMurderer(...)`

---

## Testing Guide

### Manual Testing Steps

1. **Start test server**:
   ```bash
   ./gradlew runServer
   ```

2. **Create two players on different teams**:
   ```
   /team join red Player1
   /team join blue Player2
   ```

3. **Test immediate murderer status**:
   - Player1 attacks Player2 (don't kill)
   - ✅ Player1's name turns red immediately
   - ✅ Player1's armor is removed
   - ✅ Announcement broadcast

4. **Test same-team protection**:
   ```
   /team join red Player2
   ```
   - Player1 attacks Player2
   - ✅ No damage dealt
   - ✅ No murderer status

5. **Test murderer victim exception**:
   - Player1 is murderer
   - Player2 attacks Player1
   - ✅ Player2 does NOT become murderer

6. **Test kill credit**:
   - Player1 (murderer) dies to Player2
   - ✅ Player2 gets kill credit
   - Player3 (non-murderer) dies to Player2
   - ✅ Player2 does NOT get kill credit

### Unit Test Example

```java
@Test
void testDamageAppliesMurdererImmediately() {
    // Given
    Player attacker = mock(Player.class);
    Player victim = mock(Player.class);
    when(gameManager.isGameRunning()).thenReturn(true);
    when(playerManager.isMurderer(victim.getUniqueId())).thenReturn(false);

    EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
        attacker, victim, DamageCause.ENTITY_ATTACK, 5.0
    );

    // When
    listener.onPlayerDamage(event);

    // Then
    verify(playerManager).setMurderer(eq(attacker.getUniqueId()), eq(180));
}
```

## Common Pitfalls

### ❌ Don't: Apply murderer status in death handler

```java
// WRONG
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    playerManager.setMurderer(killer.getUniqueId(), 180); // ❌ Too late!
}
```

**Why**: This is the bug we're fixing - status must apply on damage, not death

---

### ❌ Don't: Forget team check

```java
// WRONG
public void onPlayerDamage(EntityDamageByEntityEvent event) {
    // Missing team check
    applyMurdererPenalty(attacker, victim, attackerData); // ❌ Allows team killing
}
```

**Why**: Friendly fire must be blocked per spec

---

### ❌ Don't: Use global scoreboard

```java
// WRONG
Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard(); // ❌
```

**Why**: Conflicts with existing team system - use per-player scoreboards

---

### ✅ Do: Check event priority

```java
// CORRECT
@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
```

**Why**: Observe after other plugins, still process cancelled events

---

## Performance Tips

1. **Early returns**: Check cheapest conditions first
   ```java
   if (!gameRunning) return;        // Cheapest
   if (!(entity instanceof Player)) return;  // Next
   if (attacker.equals(victim)) return;      // Slightly more expensive
   ```

2. **Minimize database writes**: Only write when status actually changes
   - First offense: Write
   - Timer extension: Write
   - Same victim, same murderer: Write (timer still extends)

3. **Cache PlayerData**: `PlayerManager` already caches, don't query database directly

## Deployment Checklist

- [ ] Code changes complete in `PvpListener.java`
- [ ] Code changes complete in `PlayerManager.java`
- [ ] Unit tests written and passing
- [ ] Manual testing completed (6 test scenarios above)
- [ ] No regression in existing death handling
- [ ] Performance verified (<1s for murderer application)
- [ ] Logs cleaned up (remove debug statements)

## Next Steps

After implementing this feature:
1. Run `/speckit.tasks` to generate detailed task breakdown
2. Execute tasks in priority order
3. Run full test suite: `./gradlew test`
4. Manual QA testing on test server
5. Create pull request against `001-paper-plugin-gameplay`

## Quick Reference

| Event | Priority | Purpose |
|-------|----------|---------|
| EntityDamageByEntityEvent | MONITOR | Immediate murderer status |
| PlayerDeathEvent | NORMAL | Kill/death statistics |
| PlayerRespawnEvent | NORMAL | Re-grant shop items |

| Method | Returns | Purpose |
|--------|---------|---------|
| setMurderer(UUID, int) | boolean | Apply/extend status, returns isFirstTime |
| isMurderer(UUID) | boolean | Check current status |
| clearMurderer(UUID) | void | Remove murderer status |

| Timer | Value | Purpose |
|-------|-------|---------|
| Per offense | 180s (3 min) | Murderer penalty duration |
| Maximum | 3600s (60 min) | Cap on total accumulated time |

---

**Estimated Implementation Time**: 2-4 hours

**Complexity**: Medium (event handling, state management, visual markers)

**Risk Level**: Low (well-defined scope, existing infrastructure)
