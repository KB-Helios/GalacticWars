# Authorized source intake

The project owner authorized derivative reuse and redistribution of code and assets from these
private source archives on 2026-07-12:

- `workers` (authorization evidence retained privately by the project owner)
- `recruits` (authorization evidence retained privately by the project owner)

Both source projects declare `All Rights Reserved`. This ledger is therefore required for every
import, even when the resulting file is substantially rewritten. Authorization covers use in
Galactic Wars; it does not remove third-party notices already embedded in a source file.

## Intake rules

1. Record an entry before merging each imported file or asset.
2. Port behavior into Galactic Wars domain interfaces; do not retain obsolete Forge 1.20.1
   networking, registration, global-manager, or off-thread world-access patterns.
3. Keep original copyright headers and notices when source remains substantially derived.
4. Record transformed art in `galacticwars-asset-manifest.json` and
   `galacticwars-asset-provenance.md` as well as here.
5. Validate every intake with focused tests, `build`, and runtime GameTests where world behavior
   is involved.

## Intake ledger

| Source project/path | Destination | Kind | Transformation | Substantially derived | Verification |
| --- | --- | --- | --- | --- | --- |
| `workers/src/main/java/com/talhanation/workers/entities/ai/FishermanWorkGoal.java` | `GalacticRecruitEntity` fisher controller methods | Algorithm | Replaced bobber entity and Forge-era goal state with bounded persisted work-order scanning, physical catches, tool wear, registered-storage deposits, and claim checks | Yes, behavior selectively derived and rewritten | `specialist_worker_loops` GameTest |
| `workers/src/main/java/com/talhanation/workers/entities/ai/AnimalFarmerWorkGoal.java` | `GalacticRecruitEntity` animal-farmer controller methods | Algorithm | Retained paired breeding and maximum-herd concepts; replaced work-area entities and global state with authoritative settlement worksites, bounded entity queries, physical feed, capped harvesting, and persisted orders | Yes, behavior selectively derived and rewritten | `specialist_worker_loops` GameTest |
| `workers/src/main/java/com/talhanation/workers/entities/ai/CookWorkGoal.java` | `GalacticRecruitEntity` cook controller methods | Algorithm | Replaced furnace queues and area entities with atomic registered-storage withdrawals, explicit fuel consumption, physical food output, and persisted work-order recovery | Yes, behavior selectively derived and rewritten | `specialist_worker_loops` GameTest |
| `workers/src/main/java/com/talhanation/workers/entities/ai/MerchantWorkGoal.java` | `GalacticRecruitEntity` merchant assignment loop | Algorithm | Retained persistent assigned-market and bounded-idle concepts; removed villager invitations, Forge messages, and global registries in favor of a persisted settlement work order | Yes, behavior selectively derived and rewritten | `specialist_worker_loops` GameTest |
| `recruits` commit `cff03e085d65653406a8b6ddcdd0ebff615c3e48`, `src/main/java/com/talhanation/recruits/util/FormationUtils.java` | `army/FormationPlanner` and formation harnesses | Algorithm | Ports only deterministic vector/shape coordinate math; replaces entity iteration with stable Galactic squad UUID slots, server-side facing, and terrain-aware runtime resolution | Yes, selectively adapted | formation harnesses and squad GameTests |
| `recruits` commit `cff03e085d65653406a8b6ddcdd0ebff615c3e48`, `src/main/java/com/talhanation/recruits/world/RecruitsRoute.java`, `src/main/java/com/talhanation/recruits/entities/AbstractLeaderEntity.java` | `army/ArmyPatrolPlan`, patrol planner/state, and persistence codecs | Algorithm/state machine | Retains waypoint, wait, loop, and ping-pong semantics; replaces leader-owned Forge state with `KingdomSavedData`-owned squad persistence and virtual-squad compatibility | Yes, selectively adapted | patrol harnesses and virtual-squad GameTests |
| `recruits` commit `cff03e085d65653406a8b6ddcdd0ebff615c3e48`, `src/main/java/com/talhanation/recruits/client/events/KeyEvents.java`, `src/main/java/com/talhanation/recruits/client/gui/CommandScreen.java`, command categories | Galactic field-command key mapping and screen | UI/workflow | Replaces Forge 1.20 UI/container and integer protocol with Architectury client input, bounded typed payloads, and Galactic Wars localization | Yes, selectively adapted | client smoke test and command authority tests |
| `recruits` commit `cff03e085d65653406a8b6ddcdd0ebff615c3e48`, `src/main/java/com/talhanation/recruits/CommandEvents.java`, command messages | `ArmyFieldCommandService` and secure payload handler | Command semantics | Retains player-visible order semantics while deriving actor identity from packet context, validating targets and permissions server-side, and rejecting replayed requests | Yes, selectively adapted | network/authority harnesses and two-player GameTests |
