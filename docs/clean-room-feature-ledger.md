# Clean-room feature ledger

This project may study the behavior of the local Villager Recruits and Villager Workers checkouts,
but both declare `All Rights Reserved`. Their source, assets, translations, names, packet formats,
and internal architecture must not be copied. Every implementation below is original Galactic Wars
work and is verified through project-owned tests.

| Reference behavior | Galactic Wars adaptation | Project implementation | Verification | Provenance |
| --- | --- | --- | --- | --- |
| Retry-safe kingdom actions | Stable action identities for pledge, hiring, construction, travel, trade, delivery, and conquest | `KingdomActionId`, `KingdomGameplayTransactionService`, and the SavedData runtime adapter | `KingdomGameplayTransactionTest` | Clean-room behavior study; no reference source copied |
| Settlement costs continue without a menu | Bounded server simulation charges loaded Command Centers without forcing chunks | `KingdomSimulationEvents` | Kingdom runtime GameTests | Original implementation |
| Large commandable forces need bounded work | Round-robin kingdom simulation budget; vanilla navigation remains authoritative | `KingdomSimulationEvents` | Performance acceptance suite (planned) | Original implementation |
| Workers coordinate shared stock | Physical-stock demand index with expiring worker reservation leases | `SettlementSupplyLedger` | `SettlementSupplyLedgerTest` | Clean-room behavior study; no reference source copied |
| Commanded NPCs recover from blocked routes | Throttled repathing, a bounded no-progress timeout, and retry backoff without clearing the player's order | `RecruitMoveToCommandGoal` | `RecruitCompanionAiIntegrationTest` and recruit runtime GameTests | Clean-room behavior study; no reference source copied |
| Courier completion advances kingdom progress | Stable delivery event emitted only by the persisted terminal work-order transition | Recruit workforce runtime and `KingdomGameplayRuntimeService` | Workforce persistence and runtime GameTests | Original implementation |

Add a row before merging each future parity feature. Link only project-owned implementation and tests.
