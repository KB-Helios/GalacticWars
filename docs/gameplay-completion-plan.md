# Galactic Wars Gameplay Completion Plan

## Player promise

Galactic Wars is a first-hand battle and kingdom-building simulator. The player founds a faction base, recruits and equips people or droids, builds a working settlement, commands squads in the world, travels with them, and wins a planetary campaign. Menus explain and issue orders; they do not replace embodied play.

“Complete” means a fresh survival player can discover, perform, understand, and finish this loop without commands, test-only items, implicit target selection, or outside documentation.

## Progression spine

### 0. Arrival and discovery

- Obtain the Command Center recipe through normal survival progression.
- Placing it begins an explicit faction pledge, creates one kingdom, and teaches storage, upkeep, claims, recruitment, and the current objective.
- Empty states explain the next physical action instead of exposing disabled button walls.

Exit gate: the player has a pledged faction, an active Command Center, a visible chapter objective, and a protected starter claim.

### 1. Establish the base

- Fund the treasury with physical Credit Chips.
- Hire the faction’s first military unit and first worker.
- Assign a real worksite and storage endpoint.
- Project and construct the first blueprint using supplied blocks.

Exit gate: Chapter 1 completes through real runtime events; housing, workforce, and construction are visible in the Command Center.

### 2. Build a functioning settlement

- Complete Forward Base, Barracks, Supply Depot, Moisture Farm, Salvage Yard, and Mine projects.
- Workers farm, mine, salvage, cook, carry, trade, and build through persisted work orders.
- Blocked workers report the exact missing tool, input, route, storage, upkeep, or authority condition.

Exit gate: the settlement can feed and supply recruits, supports a commander, and unlocks planetary travel without admin intervention.

### 3. Muster and command an army

- Promote a commander and create explicitly selected squads.
- Select squad members and issue follow, hold, move, protect, attack, patrol, formation, rally, supply, split, and merge orders.
- Loaded and virtual squads share the same persisted order and resume safely across unloads, reconnects, and travel.

Exit gate: two squads can complete a patrol and combat objective without friendly fire, duplication, stale targets, or command ambiguity.

### 4. Expand to the planets

- Build and supply a Forward Base, then travel with eligible nearby squads.
- Establish an outpost on Tatooine, Geonosis, Kamino, or Coruscant.
- Planet resources, faction presence, merchants, and threats support the campaign instead of acting as disconnected dimensions.

Exit gate: travel, arrival safety, respawn, squad transfer, outpost registration, and return travel survive a save/reload cycle.

### 5. Vehicle and Force mastery

- Fabricate, deploy, fuel, board, drive, damage, repair, and destroy all five vehicles.
- Unlock a light or dark Force path through campaign choices; input, targeting, energy, cooldown, feedback, persistence, and PvP policy are server authoritative.
- Vehicle and Force unlocks appear as clear campaign rewards, not unexplained inventory additions.

Exit gate: each vehicle and each launch Force ability has a playable control path, failure feedback, persistence proof, and multiplayer GameTest coverage.

### 6. Conquest and endgame

- Capture regional objectives with present squads while respecting protected player builds.
- Control changes faction spawns, merchant stock, travel safety, rewards, and counterattacks.
- Complete the faction’s third chapter and a final planetary objective.

Exit gate: the campaign has a visible victory state, repeatable post-campaign objectives, and no reward duplication on replay or reconnect.

## Command Center product surface

The Command Center is the single truthful overview, not the sole place gameplay happens.

- **Overview:** faction, role, treasury, upkeep, population, housing, settlements, claims, commander, active chapter, and next objective.
- **Campaign:** all three faction chapters, objective completion, rewards, unlocks, and actionable failure text.
- **Construction:** blueprint requirements, placement state, assigned builders, material progress, blocked reason, pause/cancel controls, and in-world projection.
- **Squads:** explicit squad and member selection, current order, formation, lifecycle, supply, rally, patrol, and defended claim.
- **Workforce:** workers, profession, worksite, active order, carried resources, storage, and blocked reason.
- **Kingdom:** members, roles, invitations, settlements, claims, and permissions.
- **Diplomacy:** explicitly selected foreign kingdom, current relation, treaty or embargo state, proposals, and cooldowns.
- **Storage/travel:** physical inventory access and navigation remain separate embodied actions.

Every mutating action carries a replay ID and explicit target IDs. The server revalidates permissions, proximity, target membership, cost, revision, and current state before committing, then returns a fresh bounded dashboard snapshot.

## NPC quality contract

Every NPC behavior must have an observable purpose, a bounded failure policy, and persistence semantics.

- SmartBrainLib schedules local companion, worker, civilian, and combat behaviors.
- Saved-data-backed army control remains authoritative for grouped recruits.
- Explicit player commands interrupt lower-priority work safely and resume only when appropriate.
- Navigation retries are bounded; stuck NPCs report and recover rather than spin forever.
- Combat respects faction, kingdom, owner, PvP, and friendly-fire policy.
- Worker inputs and outputs are conserved through failure, death, unload, cancellation, and full storage.
- GeckoLib animation state follows real movement, attack, work, damage, death, and vehicle state.

## Library boundaries

- **GeckoLib:** entity, item, armor, vehicle, and purposeful gameplay animations.
- **SmartBrainLib:** NPC sensors, behavior scheduling, memory, and local pathing.
- **Framework:** versioned, bounded, replay-safe client/server messages and synchronized command state.
- **YACL:** client configuration and accessibility preferences only; never a required gameplay menu.

## Delivery milestones

1. Command truth: synchronized dashboard state, explicit target protocol, campaign objective view, and operation refresh.
2. Command UX: complete Command Center and recruit screens, selection flows, construction projection, feedback, and accessibility.
3. Command Center rework: interaction states, orientation/model, protected lifecycle, storage/upkeep feedback, and upgrade path.
4. NPC acceptance: fresh-world recruit, worker, commander, squad, civilian, combat, unload, and recovery playthroughs.
5. Systems completion: vehicles, Force, planets, trades, conquest, and campaign victory wired through runtime events.
6. Release proof: dedicated server, two-player permission/PvP pass, accessibility/visual QA, balance pass, and a recorded fresh survival completion.

## Verification gates

Each milestone must pass, in order:

1. Focused dependency-light harnesses for domain rules and codecs.
2. `compileJava compileTestJava` for broad source changes.
3. `runHarnesses` for the complete executable contract suite.
4. `clean build` for the published artifact lifecycle.
5. `runGameTestServer` for entity, menu, network, persistence, travel, and world interactions.
6. Manual fresh-world acceptance scenarios for client rendering, input feel, discoverability, and multi-step progression that automated tests cannot prove.

Green catalogs, source-presence tests, or compile output do not by themselves satisfy a playable milestone.
