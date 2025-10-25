# Research: PVP Restriction Bug Fix

**Feature**: 002-fix-pvp-restriction
**Date**: 2025-10-25
**Status**: Complete

## Overview

This document consolidates research findings for implementing immediate murderer status application on PvP damage, with team-based friendly fire prevention.

## Key Decisions

### 1. Event Detection Strategy

**Decision**: Use `EntityDamageByEntityEvent` with `EventPriority.MONITOR` and `ignoreCancelled = false`

**Rationale**:
- `EntityDamageByEntityEvent` fires immediately when any entity damages another, before death occurs
- `MONITOR` priority ensures we observe the event after other plugins have processed it
- `ignoreCancelled = false` allows us to see all damage attempts, including those blocked by other systems
- This enables immediate murderer status application on first hit

**Alternatives Considered**:
- `PlayerDeathEvent` only - **Rejected**: Only fires on death, misses non-fatal attacks (the bug we're fixing)
- `EntityDamageEvent` - **Rejected**: Doesn't provide attacker information directly
- `HIGHEST` priority - **Rejected**: Could interfere with other protection plugins

**Implementation Notes**:
- Must check `event.getEntity() instanceof Player` and `event.getDamager() instanceof Player`
- Must verify game is running before processing
- Must handle projectile damage (arrows, snowballs) by checking damager type

### 2. Team-Based Friendly Fire Prevention

**Decision**: Check team affiliation in damage event handler and cancel event if same team

**Rationale**:
- Prevents damage before it's applied, ensuring zero damage to teammates
- Uses existing `PlayerData.getTeamColor()` and `TeamManager` infrastructure
- Clean separation: friendly fire check happens before murderer logic
- Matches existing game team system (Red, Blue, etc.)

**Alternatives Considered**:
- Allow damage but don't apply murderer penalty - **Rejected**: Spec requires zero damage between teammates
- Use Minecraft Scoreboard teams for detection - **Rejected**: Project uses custom TeamColor enum system
- Check teams in death handler only - **Rejected**: Doesn't prevent non-fatal friendly fire

**Implementation Notes**:
- Must handle `null` team cases (teamless players)
- Early return pattern: check teams → cancel if same → check murderer status → apply penalty
- Event cancellation prevents damage application entirely

### 3. Murderer Status Application Timing

**Decision**: Separate murderer status application (damage event) from kill/death counting (death event)

**Rationale**:
- Murderer status must apply immediately on damage (damage event handler)
- Kill/death statistics only make sense when a player dies (death event handler)
- This separation fixes the core bug: murderer penalty applies on first hit, not on kill
- Prevents duplicate penalty application

**Alternatives Considered**:
- Apply murderer status in death handler only - **Rejected**: This is the current bug
- Apply both status and statistics in damage handler - **Rejected**: Statistics need actual death to be meaningful
- Combine damage and death handlers into one - **Rejected**: Violates single responsibility principle

**Implementation Notes**:
- Damage handler: checks victim murderer status → applies penalty to attacker if victim is non-murderer
- Death handler: checks victim murderer status → awards kill credit only if victim was murderer
- Both handlers must use same `isMurderer()` check for consistency

### 4. Murderer Status Persistence

**Decision**: Refactor `PlayerManager.setMurderer()` to return `boolean` instead of using array wrapper

**Rationale**:
- Current implementation uses `final boolean[] isFirstTime` array wrapper - awkward Java pattern
- Direct return of boolean is cleaner and more readable
- Supports existing functionality: tracking first-time vs. repeat offenses
- Enables different announcement behavior (broadcast on first offense, quiet on repeats)

**Alternatives Considered**:
- Keep array wrapper - **Rejected**: Unnecessarily complex for simple boolean return
- Remove first-time tracking - **Rejected**: Spec requires different behavior for first vs. subsequent offenses
- Use AtomicBoolean - **Rejected**: No concurrency concerns in single-threaded event handling

**Implementation Notes**:
- Method signature: `public boolean setMurderer(UUID uuid, int durationSeconds)`
- Returns `true` if this is player's first time becoming murderer
- Returns `false` if player was already a murderer (time extension only)
- Must handle `Optional<PlayerData>` properly, return `false` if player data not found

### 5. Visual Marker Implementation

**Decision**: Use Minecraft Scoreboard Team system for overhead name color (in addition to display name and tab list)

**Rationale**:
- Display name and tab list can be set directly via `Player.setDisplayName()` and `Player.setPlayerListName()`
- Overhead nametag color requires Scoreboard Team with color/prefix settings
- Creates a "murderer" team dynamically per player's scoreboard
- Ensures visual consistency across all three name display locations

**Alternatives Considered**:
- Display name and tab list only - **Rejected**: Spec requires overhead nametag to also be red
- Use global scoreboard team - **Rejected**: Could conflict with existing team system
- Use packets to change name color - **Rejected**: Over-engineered, scoreboard teams are standard

**Implementation Notes**:
- Create per-player scoreboard if player has none or has main scoreboard
- Register "murderer" team on player's scoreboard with `ChatColor.RED` and prefix "§c"
- Add player to murderer team: `murdererTeam.addEntry(player.getName())`
- Remove from team when murderer status expires
- Helper methods: `setPlayerNameRed(Player)` and `setPlayerNameWhite(Player)`

### 6. Same-Victim Attack Tracking

**Decision**: Use `Map<UUID, UUID>` to track last victim per murderer for announcement control

**Rationale**:
- Spec requires announcements for first attack and different-victim attacks
- No announcement spam when same murderer attacks same victim repeatedly
- Simple map lookup: `lastVictims.get(attackerUUID)` → compare with current victim UUID
- Enables smart announcement logic

**Alternatives Considered**:
- Announce every attack - **Rejected**: Spec explicitly requires spam prevention
- Track timestamp instead of victim - **Rejected**: Doesn't help identify same vs. different victim
- Store in PlayerData - **Rejected**: Transient data, doesn't need persistence

**Implementation Notes**:
- Declare in `PvpListener`: `private final Map<UUID, UUID> lastVictims`
- Update on every murderer penalty application: `lastVictims.put(killerUUID, victimUUID)`
- Check before announcing: `boolean isDifferentVictim = !Objects.equals(lastVictimUUID, currentVictimUUID)`
- Announce if `isFirstTime || isDifferentVictim`

## Technical Patterns

### Event Flow

```
1. Player A attacks Player B
   ↓
2. EntityDamageByEntityEvent fires
   ↓
3. PvpListener.onPlayerDamage() checks:
   - Is game running? → No: return
   - Is victim a player? → No: return
   - Is attacker a player? → No: return
   - Is self-damage? → Yes: return
   - Same team? → Yes: cancel event, return
   - Is victim murderer? → Yes: return (no penalty)
   ↓
4. Apply murderer penalty to attacker
   - Call PlayerManager.setMurderer() → returns isFirstTime
   - Check last victim → determine if different victim
   - If first time: remove armor, set name red, announce
   - If different victim: announce
   - Update last victim tracker
   ↓
5. (Later) Player B dies
   ↓
6. PlayerDeathEvent fires
   ↓
7. PvpListener.onPlayerDeath() checks:
   - Was victim murderer? → Yes: award kill credit
   - Was victim murderer? → No: only count death, no kill credit
```

### Code Organization

**PvpListener.java**:
- `onPlayerDamage()` - New method, handles immediate murderer application
- `onPlayerDeath()` - Modified method, handles kill/death counting only
- `applyMurdererPenalty()` - Existing helper, called from damage handler
- `setPlayerNameRed()` - New helper, applies red name via scoreboard
- `setPlayerNameWhite()` - New helper, removes red name

**PlayerManager.java**:
- `setMurderer()` - Refactored to return boolean directly
- `clearMurderer()` - Modified to use `setPlayerNameWhite()` helper
- `updatePlayerNameColor()` - Modified to use red/white helpers

## Integration Points

### Existing Systems

1. **GameManager**: Query `isGameRunning()` to check game state
2. **TeamManager**: Query team information for friendly fire checks
3. **PlayerManager**: Manage murderer status and persistence
4. **BossManager**: Existing integration for boss summoning on 3rd murderer
5. **MessageUtils**: Existing utilities for broadcasts and titles

### Event Priorities

- `onPlayerDamage`: `EventPriority.MONITOR, ignoreCancelled = false`
- `onPlayerDeath`: `EventPriority.NORMAL` (existing)
- `onPlayerRespawn`: `EventPriority.NORMAL` (existing, unmodified)

### Data Persistence

- Murderer status already persisted in SQLite via `PlayerData`
- Timer (murdererUntil) already stored as Unix timestamp
- No new database fields required
- `lastVictims` map is transient (in-memory only, resets on server restart)

## Testing Strategy

### Unit Tests

1. **PvpListenerTest**:
   - Test damage event → murderer status applied
   - Test same team → damage cancelled
   - Test murderer victim → no penalty
   - Test first-time vs. repeat offense
   - Test same-victim vs. different-victim announcements

2. **PlayerManagerTest**:
   - Test setMurderer() returns correct boolean
   - Test timer accumulation
   - Test 60-minute cap
   - Test status persistence

### Integration Tests

1. Two players, different teams → damage → verify murderer status applied
2. Two players, same team → damage → verify damage cancelled
3. Murderer attacks non-murderer → verify timer extends
4. Non-murderer kills murderer → verify kill credit awarded
5. Player disconnects as murderer → reconnects → verify status preserved

### Manual Testing Checklist

- [ ] Attack teammate → no damage, no murderer status
- [ ] Attack different team → immediate murderer status (red name, armor removed)
- [ ] Attack same player 5 times → only one announcement
- [ ] Attack different player → new announcement
- [ ] Kill murderer → kill credit awarded, announcement
- [ ] Kill non-murderer → no kill credit
- [ ] Murderer timer reaches zero → name returns to white, can equip armor

## Performance Considerations

### Event Handler Performance

- Damage events are frequent (every hit in combat)
- Keep handler lightweight: quick checks, early returns
- Team lookup is O(1) via `PlayerData.getTeamColor()`
- Murderer status check is O(1) via in-memory map
- Database writes only occur on status change, not every damage event

### Scalability

- 20 concurrent players (target): minimal impact
- Each damage event: ~5 conditional checks + 1 map lookup
- Database write only on new murderer or timer extension
- Scoreboard operations are client-side, negligible server load

## Risk Analysis

### Potential Issues

1. **Event cancellation conflicts**: Other plugins may also cancel damage events
   - **Mitigation**: Use `MONITOR` priority, observe after other handlers

2. **Scoreboard conflicts**: Other systems may manage scoreboards
   - **Mitigation**: Use per-player scoreboards, not global main scoreboard

3. **Rapid attack spam**: Could trigger many murderer timer extensions
   - **Mitigation**: Timer extensions are capped at 60 minutes total

4. **Null team handling**: Players without teams
   - **Mitigation**: Null-safe team comparison, define behavior for teamless players

### Edge Cases

- Self-damage (fall while being attacked): Handled by `attacker.equals(victim)` check
- Projectile damage: Handled by checking damager type
- Environmental death after being damaged: Only death handler runs (no murderer penalty)
- Server restart during murderer status: Status persisted in database, reloaded on join

## Conclusion

This research establishes a clear implementation path:
1. Add damage event handler for immediate murderer detection
2. Implement team-based friendly fire prevention
3. Separate murderer status logic from kill/death counting
4. Use scoreboard teams for comprehensive name coloring
5. Track last victim for smart announcement control

All technical unknowns have been resolved. Implementation can proceed to Phase 1 (data modeling and contracts).
