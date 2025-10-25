# CandyRush - Multiplayer Treasure Hunt Game

## What is CandyRush?

CandyRush is an exciting team-based treasure hunting game where players compete to collect the most points! Join one of three teams, explore the map, find treasure chests, defend NPCs from monsters, and battle epic bosses to become the ultimate champion!

## Game Overview

### Team-Based Competition
- **3 Teams**: Blue, Green, and Yellow (Red is reserved for Murderers)
- **Work Together**: Collaborate with your teammates to earn points
- **PvP Deterrent**: Attacking non-Murderers triggers severe Murderer penalties
- **Time-Limited Rounds**: Each game lasts 20 minutes

### How to Earn Points

1. **Treasure Chests**
   - Find 11 different chest types scattered across the map
   - **Chest/Barrel**: Food items
   - **Brewing Stand**: Potions
   - **Furnace types**: Materials
   - **Dropper/Dispenser**: Equipment
   - **Hopper**: Utility items
   - **Trapped Chest**: High-tier equipment but deals 9 hearts damage (near-death!)

2. **Food Collection**
   - **Auto-convert in chests**: Click food in treasure chests to get **personal points only**
   - Different foods are worth different amounts (1-20 points each)
   - **Note**: Team points are NOT added (personal points only)

3. **Defense Events**
   - Help NPCs being attacked by monsters
   - Survive waves of enemies
   - Earn bonus points for successful defense
   - Complete 3 defense events to summon a boss!

4. **Boss Battles**
   - Powerful bosses appear after completing defense events
   - Defeat them for massive point rewards
   - Watch out - they're tough!

5. **Murderer System (PvP Deterrent)**
   - Attacking non-Murderer players triggers severe penalties
   - Murderers move to the RED team and are excluded from their original team
   - Murderers have red names, cannot equip armor, and drop items on death
   - Killing a Murderer is penalty-free (self-defense)
   - Murderer points still contribute to their original team
   - This game discourages PvP - focus on cooperation!

## Commands

### Player Commands
- `/stats` - View your points and ranking
- `/stats top` - See the top 10 players
- `/stats teams` - View team rankings
- `/shop` - Open the item shop to buy useful items

### Admin Commands
- `/candyrush status` - Check game status
- `/candyrush start` - Force start the game
- `/candyrush stop` - Stop the current game
- `/candyrush reset` - Reset to waiting state
- `/candyrush setcenter` - Set map center to your location
- `/candyrush clearcenter` - Clear map center (random selection)

## Game Flow

### 1. Waiting Phase
- Players join the server
- Automatic team assignment
- Game starts when enough players are online

### 2. Countdown
- 10-second countdown
- Get ready to teleport!

### 3. Game Start
- All players teleport to random locations within a 50-block radius of the map center

### 4. Playing the Game (20 minutes)
- Explore the map within the world border
- Collect treasure from chests
- Help NPCs in defense events
- Battle bosses
- Fight other teams
- Use the shop to buy helpful items

### 5. Game End
- Final scores are tallied
- Winning team is announced
- 5-minute cooldown before next game

## Game Mechanics

### World Border
- The map has a visible boundary (default: 500 blocks diameter)
- Warning appears when you get close to the edge
- Taking damage if you go outside the border
- You cannot escape the play area!

### Treasure Chests
CandyRush features 11 different chest types, each with specific loot categories:
- **Chest/Barrel**: Food items (safe to open)
- **Brewing Stand**: Potion items (safe to open)
- **Furnace/Blast Furnace/Smoker**: Material items (safe to open)
- **Dropper/Dispenser**: Equipment items (safe to open)
- **Hopper**: Utility items (safe to open)
- **Trapped Chest**: High-tier equipment rewards but deals 9 hearts damage (near-death!)
- All chests respawn after 60 seconds

### Defense Events
- NPCs call for help when monsters attack
- Click the NPC to start the event
- Survive 3 waves of monsters
- 2 minutes to complete
- Don't go too far or the event will fail!

### Boss System
- Complete 3 defense events to summon a boss
- Bosses are extremely powerful
- Requires team coordination to defeat
- Huge point rewards for the entire team

### Murderer System
- **Trigger**: Attacking non-Murderer players (PK behavior)
- **Team Change**: Temporarily moves to RED team, excluded from original team
- **Red name tag**: Visible to all players
- **Armor removal**: All armor is removed and cannot be re-equipped
- **Target status**: Can be attacked by anyone (including former teammates) without penalty
- **Death penalty**: Drops all equipment
- **Duration**: 3 minutes initially, +3 minutes per attack, maximum 60 minutes
- **Self-defense**: Killing a Murderer carries NO penalty
- **Points**: Murderer points still contribute to their original team (not RED team)
- **Purpose**: Strong deterrent against PvP - this game encourages cooperation!

### Shop System
- Access with `/shop` command (or use the compass item)
- Buy useful items with your points
- Items include:
  - Food for quick points
  - Equipment for survival
  - Special items for advantages

## Tips for Success

1. **Stick Together**: Team up with your teammates for defense events and boss fights
2. **Food Points**: Clicking food in chests gives personal points only (no team points)
3. **Watch for NPCs**: Defense events give great rewards
4. **11 Chest Types**: Different chest types contain different categories of items
5. **Avoid Trapped Chests**: They give high-tier equipment but deal 9 hearts damage (near-death!)
6. **Use the Shop**: Spend points wisely on items that help your team
7. **Map Awareness**: Stay within the world border and know your surroundings
8. **Avoid PvP**: This game is designed for cooperation - don't attack other players
9. **PK Penalty**: Attacking non-Murderer players triggers severe Murderer penalties

## Language Support

CandyRush supports multiple languages! The server admin can change the language in the config:
- **Japanese** (日本語) - Default
- **English** - Available

All messages, commands, and notifications will appear in the selected language.

## Server Requirements

**For Server Admins:**
- Minecraft Version: 1.21.5
- Server Type: Paper/Spigot
- Required Plugin: MythicMobs (for bosses and NPCs)
- Recommended: 2+ players minimum

## Getting Help

If you have questions or need help:
1. Use `/stats help` for statistics commands
2. Ask your server admin about game rules
3. Check the server's Discord or support channels

---

**Have fun and may the best team win!** 🍬🏆

## Quick Reference Card

| Action | Command | Description |
|--------|---------|-------------|
| Check stats | `/stats` | View your points and rank |
| Top players | `/stats top` | See leaderboard |
| Team ranking | `/stats teams` | View team scores |
| Convert food | Click in chest | Personal points only (no team points) |
| Open shop | `/shop` | Buy items with points |
| Help NPC | Right-click NPC | Start defense event |
| Use shop item | Click compass | Quick shop access |

**Point Values:**
- Treasure chests: Varies by items
- Defense event: 50-100 points
- Boss kill: Massive points (team effort)
