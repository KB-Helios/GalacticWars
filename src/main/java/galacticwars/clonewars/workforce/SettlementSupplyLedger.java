package galacticwars.clonewars.workforce;

import galacticwars.clonewars.kingdom.StorageEndpoint;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable demand and lease index. Physical containers remain the resource authority. */
public record SettlementSupplyLedger(
        UUID settlementId,
        List<SupplyDemand> demands,
        List<SupplyReservation> reservations,
        int revision
) {
    public SettlementSupplyLedger {
        Objects.requireNonNull(settlementId, "settlementId");
        demands = uniqueDemands(demands);
        reservations = uniqueReservations(reservations);
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
    }

    public static SettlementSupplyLedger create(UUID settlementId) {
        return new SettlementSupplyLedger(settlementId, List.of(), List.of(), 0);
    }

    public Optional<SupplyDemand> nextDemand() {
        return demands.stream().filter(demand -> !demand.complete())
                .sorted(Comparator.comparingInt(SupplyDemand::priority).reversed()
                        .thenComparing(demand -> demand.id().toString()))
                .findFirst();
    }

    public SettlementSupplyLedger request(SupplyDemand demand) {
        Objects.requireNonNull(demand, "demand");
        SupplyDemand existing = demands.stream().filter(candidate -> candidate.id().equals(demand.id()))
                .findFirst().orElse(null);
        if (existing != null) {
            if (!existing.equals(demand)) {
                throw new IllegalArgumentException("supply demand id reused with different authority");
            }
            return this;
        }
        ArrayList<SupplyDemand> updated = new ArrayList<>(demands);
        updated.add(demand);
        return copy(updated, reservations);
    }

    public ReservationDecision reserve(
            UUID demandId,
            UUID workerId,
            StorageEndpoint endpoint,
            int requestedQuantity,
            int physicalStock,
            long gameTime,
            long leaseTicks
    ) {
        Objects.requireNonNull(demandId, "demandId");
        Objects.requireNonNull(workerId, "workerId");
        Objects.requireNonNull(endpoint, "endpoint");
        SettlementSupplyLedger cleaned = releaseExpired(gameTime);
        SupplyDemand demand = cleaned.demands.stream()
                .filter(candidate -> candidate.id().equals(demandId)).findFirst().orElse(null);
        if (demand == null || demand.complete()) {
            return ReservationDecision.rejected("demand_unavailable", cleaned);
        }
        if (requestedQuantity <= 0 || physicalStock < 0 || leaseTicks <= 0) {
            return ReservationDecision.rejected("invalid_reservation", cleaned);
        }
        int alreadyReserved = cleaned.reservations.stream()
                .filter(reservation -> reservation.active(gameTime))
                .filter(reservation -> reservation.endpoint().equals(endpoint))
                .filter(reservation -> cleaned.demands.stream().anyMatch(candidate ->
                        candidate.id().equals(reservation.demandId())
                                && candidate.itemId().equals(demand.itemId())))
                .mapToInt(SupplyReservation::quantity).sum();
        int available = Math.max(0, physicalStock - alreadyReserved);
        int quantity = Math.min(Math.min(requestedQuantity, demand.outstandingQuantity()), available);
        if (quantity == 0) {
            return ReservationDecision.rejected("physical_stock_unavailable", cleaned);
        }
        UUID reservationId = UUID.nameUUIDFromBytes((demandId + ":" + workerId + ":" + cleaned.revision)
                .getBytes(StandardCharsets.UTF_8));
        SupplyReservation reservation = new SupplyReservation(
                reservationId, demandId, workerId, endpoint, quantity,
                Math.addExact(gameTime, leaseTicks), SupplyReservation.State.ACTIVE);
        ArrayList<SupplyReservation> updated = new ArrayList<>(cleaned.reservations);
        updated.add(reservation);
        return ReservationDecision.accepted(reservation, cleaned.copy(cleaned.demands, updated));
    }

    public SettlementSupplyLedger complete(UUID reservationId, UUID workerId, int deliveredQuantity) {
        SupplyReservation reservation = reservation(reservationId)
                .filter(candidate -> candidate.workerId().equals(workerId))
                .filter(candidate -> candidate.state() == SupplyReservation.State.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("reservation_unavailable"));
        if (deliveredQuantity != reservation.quantity()) {
            throw new IllegalArgumentException("delivery must match the reserved quantity");
        }
        ArrayList<SupplyDemand> updatedDemands = new ArrayList<>(demands.size());
        for (SupplyDemand demand : demands) {
            updatedDemands.add(demand.id().equals(reservation.demandId())
                    ? demand.fulfill(deliveredQuantity) : demand);
        }
        ArrayList<SupplyReservation> updatedReservations = new ArrayList<>(reservations.size());
        for (SupplyReservation candidate : reservations) {
            updatedReservations.add(candidate.id().equals(reservationId) ? candidate.complete() : candidate);
        }
        return copy(updatedDemands, updatedReservations);
    }

    public SettlementSupplyLedger releaseExpired(long gameTime) {
        boolean changed = false;
        ArrayList<SupplyReservation> updated = new ArrayList<>(reservations.size());
        for (SupplyReservation reservation : reservations) {
            SupplyReservation next = reservation.state() == SupplyReservation.State.ACTIVE
                    && !reservation.active(gameTime) ? reservation.release() : reservation;
            changed |= next != reservation;
            updated.add(next);
        }
        return changed ? copy(demands, updated) : this;
    }

    public Optional<SupplyReservation> reservation(UUID reservationId) {
        return reservations.stream().filter(candidate -> candidate.id().equals(reservationId)).findFirst();
    }

    private SettlementSupplyLedger copy(List<SupplyDemand> demands, List<SupplyReservation> reservations) {
        return new SettlementSupplyLedger(settlementId, demands, reservations, revision + 1);
    }

    private static List<SupplyDemand> uniqueDemands(List<SupplyDemand> demands) {
        LinkedHashMap<UUID, SupplyDemand> unique = new LinkedHashMap<>();
        Objects.requireNonNull(demands, "demands").forEach(demand -> unique.putIfAbsent(demand.id(), demand));
        return List.copyOf(unique.values());
    }

    private static List<SupplyReservation> uniqueReservations(List<SupplyReservation> reservations) {
        LinkedHashMap<UUID, SupplyReservation> unique = new LinkedHashMap<>();
        Objects.requireNonNull(reservations, "reservations")
                .forEach(reservation -> unique.putIfAbsent(reservation.id(), reservation));
        return List.copyOf(unique.values());
    }

    public record ReservationDecision(
            boolean accepted,
            String reason,
            Optional<SupplyReservation> reservation,
            SettlementSupplyLedger ledger
    ) {
        private static ReservationDecision accepted(
                SupplyReservation reservation,
                SettlementSupplyLedger ledger
        ) {
            return new ReservationDecision(true, "accepted", Optional.of(reservation), ledger);
        }

        private static ReservationDecision rejected(String reason, SettlementSupplyLedger ledger) {
            return new ReservationDecision(false, reason, Optional.empty(), ledger);
        }
    }
}
