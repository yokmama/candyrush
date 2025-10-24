# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
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

- **Food Conversion Documentation**: Clarified food-to-points conversion mechanism
  - **Primary method**: Food items auto-convert to points when clicked in treasure chests
  - **Backup method**: `/convert` command for bulk conversion of food already in inventory
  - Updated all documentation to emphasize auto-conversion as the main feature
  - Clarified point values: 1-20 points per food item (previously documented as 1-10)
  - Updated tips to reflect automatic conversion behavior

### Technical Details
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
