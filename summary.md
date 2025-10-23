# CandyRush - Multiplayer Treasure Hunt Game

## What is CandyRush?

CandyRush is an exciting team-based treasure hunting game where players compete to collect the most points! Join one of four teams, explore the map, find treasure chests, defend NPCs from monsters, and battle epic bosses to become the ultimate champion!

## Game Overview

### Team-Based Competition
- **4 Teams**: Red, Blue, Green, and Yellow
- **Work Together**: Collaborate with your teammates to earn points
- **PvP Action**: Battle players from other teams
- **Time-Limited Rounds**: Each game lasts 20 minutes

### How to Earn Points

1. **Treasure Chests**
   - Find chests scattered across the map
   - Regular chests give food items and points
   - Watch out for trapped chests that can damage you!

2. **Food Collection**
   - Collect food items from chests
   - Use `/convert` to turn all your food into points
   - Different foods are worth different amounts

3. **Defense Events**
   - Help NPCs being attacked by monsters
   - Survive waves of enemies
   - Earn bonus points for successful defense
   - Complete 3 defense events to summon a boss!

4. **Boss Battles**
   - Powerful bosses appear after completing defense events
   - Defeat them for massive point rewards
   - Watch out - they're tough!

5. **PvP Combat**
   - Become a "Murderer" by getting first place
   - Steal points from other players
   - But be careful - everyone can see your red name!

## Commands

### Player Commands
- `/stats` - View your points and ranking
- `/stats top` - See the top 10 players
- `/stats teams` - View team rankings
- `/convert` - Convert all food in your inventory to points
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
- All players teleport to their team bases at the map corners
- Teams spawn on colored concrete platforms:
  - Red team: Northeast corner
  - Blue team: Northwest corner
  - Green team: Southwest corner
  - Yellow team: Southeast corner

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
- **Regular Chests**: Safe to open, contain food and items
- **Trapped Chests**: Give rewards but also deal damage
- Chests respawn after 60 seconds

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
- First place player becomes a "Murderer"
- Red name tag visible to everyone
- Can steal points by killing other players
- Lasts for 10 minutes
- High risk, high reward!

### Shop System
- Access with `/shop` command (or use the compass item)
- Buy useful items with your points
- Items include:
  - Food for quick points
  - Equipment for survival
  - Special items for advantages

## Tips for Success

1. **Stick Together**: Team up with your teammates for defense events and boss fights
2. **Convert Regularly**: Don't carry too much food - convert it to points often
3. **Watch for NPCs**: Defense events give great rewards
4. **Avoid Traps**: Look for signs of trapped chests before opening
5. **Use the Shop**: Spend points wisely on items that help your team
6. **Map Awareness**: Stay within the world border and know your surroundings
7. **First Place Risk**: Being in first place makes you a target (Murderer status)

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
| Convert food | `/convert` | Turn food into points |
| Open shop | `/shop` | Buy items with points |
| Help NPC | Right-click NPC | Start defense event |
| Use shop item | Click compass | Quick shop access |

**Point Values:**
- Treasure chests: Varies by items
- Defense event: 50-100 points
- Boss kill: Massive points
- PvP kill (Murderer): Steal opponent's points
