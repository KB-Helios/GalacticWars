package galacticwars.clonewars.workforce.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/**
 * Optimistic, failure-atomic transfer of complete item stacks between two physical inventories.
 * Callers must invoke it on the thread that owns both inventories (normally the server thread).
 */
public final class PhysicalLogisticsTransaction {
    private PhysicalLogisticsTransaction() {
    }

    public static Result transfer(
            LogisticsEndpoint source,
            LogisticsEndpoint destination,
            LogisticsTransferAuthority authority,
            LogisticsTransferRequest request
    ) {
        return simulate(source, destination, authority, request).commit();
    }

    public static Simulation simulate(
            LogisticsEndpoint source,
            LogisticsEndpoint destination,
            LogisticsTransferAuthority authority,
            LogisticsTransferRequest request
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(authority, "authority");
        Objects.requireNonNull(request, "request");

        if (!source.identity().equals(authority.expectedSource())) {
            return Simulation.rejected(Status.SOURCE_IDENTITY_MISMATCH, request.quantity());
        }
        if (!destination.identity().equals(authority.expectedDestination())) {
            return Simulation.rejected(Status.DESTINATION_IDENTITY_MISMATCH, request.quantity());
        }
        if (source.identity().equals(destination.identity())) {
            return Simulation.rejected(Status.SAME_ENDPOINT, request.quantity());
        }
        if (source.inventory().transactionIdentity() == destination.inventory().transactionIdentity()) {
            return Simulation.rejected(Status.SAME_PHYSICAL_INVENTORY, request.quantity());
        }
        if (!slotBoundaryValid(source) || !slotBoundaryValid(destination)) {
            return Simulation.rejected(Status.ENDPOINT_CHANGED, request.quantity());
        }

        List<ItemStack> sourceBefore = snapshot(source);
        List<ItemStack> destinationBefore = snapshot(destination);
        ItemStack template = request.itemTemplate();
        SourceScan sourceScan = scanSource(source, destination, authority, template, request.quantity(), sourceBefore);
        int quantityFromSource = Math.min(request.quantity(), sourceScan.availableQuantity());
        if (request.fulfillment() == LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT
                && quantityFromSource < request.quantity()) {
            Status status = sourceScan.policyRejected() && sourceScan.matchingQuantity() >= request.quantity()
                    ? Status.SOURCE_POLICY_DENIED : Status.SOURCE_UNAVAILABLE;
            return Simulation.rejected(status, request.quantity());
        }
        if (quantityFromSource == 0) {
            return Simulation.rejected(sourceScan.policyRejected()
                    ? Status.SOURCE_POLICY_DENIED : Status.SOURCE_UNAVAILABLE, request.quantity());
        }

        InsertionSimulation insertion = simulateInsertion(
                source, destination, authority, template, quantityFromSource, destinationBefore);
        if (request.fulfillment() == LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT
                && insertion.insertedQuantity() < request.quantity()) {
            return Simulation.rejected(insertion.policyRejected()
                    ? Status.DESTINATION_POLICY_DENIED : Status.DESTINATION_UNAVAILABLE, request.quantity());
        }
        int transferred = insertion.insertedQuantity();
        if (transferred == 0) {
            return Simulation.rejected(insertion.policyRejected()
                    ? Status.DESTINATION_POLICY_DENIED : Status.DESTINATION_UNAVAILABLE, request.quantity());
        }

        ExtractionSimulation extraction = simulateExtraction(sourceBefore, sourceScan.slots(), transferred);
        return Simulation.ready(
                source, destination, authority, request.quantity(), transferred,
                sourceBefore, extraction.after(), destinationBefore, insertion.after(),
                extraction.mutations(), insertion.mutations(), extraction.movedStacks());
    }

    private static SourceScan scanSource(
            LogisticsEndpoint source,
            LogisticsEndpoint destination,
            LogisticsTransferAuthority authority,
            ItemStack template,
            int requested,
            List<ItemStack> before
    ) {
        ArrayList<Integer> slots = new ArrayList<>();
        int available = 0;
        int matching = 0;
        boolean policyRejected = false;
        for (int slot = 0; slot < before.size(); slot++) {
            ItemStack stack = before.get(slot);
            if (!sameItem(stack, template)) {
                continue;
            }
            matching = saturatingAdd(matching, stack.getCount());
            boolean inventoryAllowed = source.inventory().canExtract(slot, stack.copy());
            boolean policyAllowed = policyAllows(
                    source, destination, authority, LogisticsAccessPolicy.Operation.EXTRACT, slot, stack);
            policyRejected |= !policyAllowed;
            if (inventoryAllowed && policyAllowed) {
                slots.add(slot);
                available = saturatingAdd(available, stack.getCount());
                if (available >= requested) {
                    break;
                }
            }
        }
        return new SourceScan(Math.min(available, requested), matching, List.copyOf(slots), policyRejected);
    }

    private static ExtractionSimulation simulateExtraction(
            List<ItemStack> before,
            List<Integer> slots,
            int requested
    ) {
        List<ItemStack> after = copyStacks(before);
        ArrayList<SlotMutation> mutations = new ArrayList<>();
        ArrayList<ItemStack> movedStacks = new ArrayList<>();
        int remaining = requested;
        for (int slot : slots) {
            if (remaining == 0) {
                break;
            }
            ItemStack stored = after.get(slot);
            int moved = Math.min(remaining, stored.getCount());
            ItemStack movedStack = stored.copyWithCount(moved);
            ItemStack replacement = stored.copy();
            replacement.shrink(moved);
            after.set(slot, replacement.isEmpty() ? ItemStack.EMPTY : replacement);
            mutations.add(new SlotMutation(slot, movedStack));
            movedStacks.add(movedStack.copy());
            remaining -= moved;
        }
        if (remaining != 0) {
            throw new IllegalStateException("source simulation did not satisfy its planned quantity");
        }
        return new ExtractionSimulation(after, List.copyOf(mutations), copyStacks(movedStacks));
    }

    private static InsertionSimulation simulateInsertion(
            LogisticsEndpoint source,
            LogisticsEndpoint destination,
            LogisticsTransferAuthority authority,
            ItemStack template,
            int requested,
            List<ItemStack> before
    ) {
        List<ItemStack> after = copyStacks(before);
        ArrayList<SlotMutation> mutations = new ArrayList<>();
        int remaining = requested;
        boolean policyRejected = false;

        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < after.size() && remaining > 0; slot++) {
                ItemStack stored = after.get(slot);
                boolean mergePass = pass == 0;
                if (mergePass && (stored.isEmpty() || !sameItem(stored, template))) {
                    continue;
                }
                if (!mergePass && !stored.isEmpty()) {
                    continue;
                }
                int maximum = Math.max(0, Math.min(
                        template.getMaxStackSize(), destination.inventory().maxStackSize(slot, template.copy())));
                int capacity = stored.isEmpty() ? maximum : Math.max(0, maximum - stored.getCount());
                int moved = Math.min(remaining, capacity);
                if (moved == 0) {
                    continue;
                }
                ItemStack candidate = template.copyWithCount(moved);
                boolean inventoryAllowed = destination.inventory().canInsert(slot, candidate.copy());
                boolean policyAllowed = policyAllows(
                        destination, source, authority, LogisticsAccessPolicy.Operation.INSERT, slot, candidate);
                policyRejected |= !policyAllowed;
                if (!inventoryAllowed || !policyAllowed) {
                    continue;
                }
                if (stored.isEmpty()) {
                    after.set(slot, candidate.copy());
                } else {
                    ItemStack replacement = stored.copy();
                    replacement.grow(moved);
                    after.set(slot, replacement);
                }
                mutations.add(new SlotMutation(slot, candidate));
                remaining -= moved;
            }
        }
        return new InsertionSimulation(
                after, requested - remaining, List.copyOf(mutations), policyRejected);
    }

    private static boolean policyAllows(
            LogisticsEndpoint endpoint,
            LogisticsEndpoint counterpart,
            LogisticsTransferAuthority authority,
            LogisticsAccessPolicy.Operation operation,
            int slot,
            ItemStack stack
    ) {
        return endpoint.policy().allows(
                authority.actorId(), endpoint.identity(), counterpart.identity(), operation, slot, stack.copy());
    }

    private static boolean slotBoundaryValid(LogisticsEndpoint endpoint) {
        return endpoint.authorizedSlotCount() <= endpoint.inventory().size();
    }

    private static List<ItemStack> snapshot(LogisticsEndpoint endpoint) {
        ArrayList<ItemStack> copy = new ArrayList<>(endpoint.authorizedSlotCount());
        for (int slot = 0; slot < endpoint.authorizedSlotCount(); slot++) {
            copy.add(endpoint.inventory().getStack(slot).copy());
        }
        return copy;
    }

    private static List<ItemStack> copyStacks(List<ItemStack> stacks) {
        ArrayList<ItemStack> copy = new ArrayList<>(stacks.size());
        stacks.forEach(stack -> copy.add(stack.copy()));
        return copy;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return first.getCount() == second.getCount() && sameItem(first, second);
    }

    private static int saturatingAdd(int first, int second) {
        long total = (long) first + second;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public enum Status {
        READY,
        COMMITTED,
        SOURCE_IDENTITY_MISMATCH,
        DESTINATION_IDENTITY_MISMATCH,
        SAME_ENDPOINT,
        SAME_PHYSICAL_INVENTORY,
        ENDPOINT_CHANGED,
        SOURCE_UNAVAILABLE,
        SOURCE_POLICY_DENIED,
        DESTINATION_UNAVAILABLE,
        DESTINATION_POLICY_DENIED,
        POLICY_CHANGED,
        STALE_STATE,
        MUTATION_FAILED
    }

    public static final class Result {
        private final Status status;
        private final int requestedQuantity;
        private final int transferredQuantity;
        private final List<ItemStack> transferredStacks;

        private Result(
                Status status,
                int requestedQuantity,
                int transferredQuantity,
                List<ItemStack> transferredStacks
        ) {
            this.status = status;
            this.requestedQuantity = requestedQuantity;
            this.transferredQuantity = transferredQuantity;
            this.transferredStacks = copyStacks(transferredStacks);
        }

        public Status status() {
            return status;
        }

        public boolean committed() {
            return status == Status.COMMITTED;
        }

        public int requestedQuantity() {
            return requestedQuantity;
        }

        public int transferredQuantity() {
            return transferredQuantity;
        }

        public List<ItemStack> transferredStacks() {
            return copyStacks(transferredStacks);
        }
    }

    public static final class Simulation {
        private final Status status;
        private final LogisticsEndpoint source;
        private final LogisticsEndpoint destination;
        private final LogisticsTransferAuthority authority;
        private final int requestedQuantity;
        private final int transferredQuantity;
        private final List<ItemStack> sourceBefore;
        private final List<ItemStack> sourceAfter;
        private final List<ItemStack> destinationBefore;
        private final List<ItemStack> destinationAfter;
        private final List<SlotMutation> sourceMutations;
        private final List<SlotMutation> destinationMutations;
        private final List<ItemStack> transferredStacks;
        private Result resolved;

        private Simulation(
                Status status,
                LogisticsEndpoint source,
                LogisticsEndpoint destination,
                LogisticsTransferAuthority authority,
                int requestedQuantity,
                int transferredQuantity,
                List<ItemStack> sourceBefore,
                List<ItemStack> sourceAfter,
                List<ItemStack> destinationBefore,
                List<ItemStack> destinationAfter,
                List<SlotMutation> sourceMutations,
                List<SlotMutation> destinationMutations,
                List<ItemStack> transferredStacks
        ) {
            this.status = status;
            this.source = source;
            this.destination = destination;
            this.authority = authority;
            this.requestedQuantity = requestedQuantity;
            this.transferredQuantity = transferredQuantity;
            this.sourceBefore = copyStacks(sourceBefore);
            this.sourceAfter = copyStacks(sourceAfter);
            this.destinationBefore = copyStacks(destinationBefore);
            this.destinationAfter = copyStacks(destinationAfter);
            this.sourceMutations = List.copyOf(sourceMutations);
            this.destinationMutations = List.copyOf(destinationMutations);
            this.transferredStacks = copyStacks(transferredStacks);
        }

        private static Simulation rejected(Status status, int requestedQuantity) {
            return new Simulation(
                    status, null, null, null, requestedQuantity, 0,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        private static Simulation ready(
                LogisticsEndpoint source,
                LogisticsEndpoint destination,
                LogisticsTransferAuthority authority,
                int requestedQuantity,
                int transferredQuantity,
                List<ItemStack> sourceBefore,
                List<ItemStack> sourceAfter,
                List<ItemStack> destinationBefore,
                List<ItemStack> destinationAfter,
                List<SlotMutation> sourceMutations,
                List<SlotMutation> destinationMutations,
                List<ItemStack> transferredStacks
        ) {
            return new Simulation(
                    Status.READY, source, destination, authority, requestedQuantity, transferredQuantity,
                    sourceBefore, sourceAfter, destinationBefore, destinationAfter,
                    sourceMutations, destinationMutations, transferredStacks);
        }

        public Status status() {
            return status;
        }

        public boolean executable() {
            return status == Status.READY;
        }

        public int transferredQuantity() {
            return transferredQuantity;
        }

        public List<ItemStack> transferredStacks() {
            return copyStacks(transferredStacks);
        }

        public synchronized Result commit() {
            if (resolved != null) {
                return resolved;
            }
            if (!executable()) {
                resolved = new Result(status, requestedQuantity, 0, List.of());
                return resolved;
            }
            try {
                if (!slotBoundaryValid(source) || !slotBoundaryValid(destination)) {
                    resolved = new Result(Status.ENDPOINT_CHANGED, requestedQuantity, 0, List.of());
                    return resolved;
                }
                if (!matchesCurrent(source, sourceBefore) || !matchesCurrent(destination, destinationBefore)) {
                    resolved = new Result(Status.STALE_STATE, requestedQuantity, 0, List.of());
                    return resolved;
                }
                if (!stillAuthorized()) {
                    resolved = new Result(Status.POLICY_CHANGED, requestedQuantity, 0, List.of());
                    return resolved;
                }
            } catch (RuntimeException failure) {
                resolved = new Result(Status.MUTATION_FAILED, requestedQuantity, 0, List.of());
                return resolved;
            }

            try {
                writeChanges(destination, destinationBefore, destinationAfter);
                writeChanges(source, sourceBefore, sourceAfter);
                destination.inventory().setChanged();
                source.inventory().setChanged();
                resolved = new Result(
                        Status.COMMITTED, requestedQuantity, transferredQuantity, transferredStacks);
                return resolved;
            } catch (RuntimeException failure) {
                try {
                    restore(source, sourceBefore);
                    restore(destination, destinationBefore);
                    source.inventory().setChanged();
                    destination.inventory().setChanged();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                    resolved = new Result(Status.MUTATION_FAILED, requestedQuantity, 0, List.of());
                    throw new IllegalStateException("physical logistics rollback failed", failure);
                }
                resolved = new Result(Status.MUTATION_FAILED, requestedQuantity, 0, List.of());
                return resolved;
            }
        }

        private boolean stillAuthorized() {
            for (SlotMutation mutation : sourceMutations) {
                ItemStack moved = mutation.stack().copy();
                if (!source.inventory().canExtract(mutation.slot(), moved.copy())
                        || !policyAllows(source, destination, authority,
                                LogisticsAccessPolicy.Operation.EXTRACT, mutation.slot(), moved)) {
                    return false;
                }
            }
            for (SlotMutation mutation : destinationMutations) {
                ItemStack moved = mutation.stack().copy();
                if (!destination.inventory().canInsert(mutation.slot(), moved.copy())
                        || !policyAllows(destination, source, authority,
                                LogisticsAccessPolicy.Operation.INSERT, mutation.slot(), moved)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean matchesCurrent(LogisticsEndpoint endpoint, List<ItemStack> expected) {
            for (int slot = 0; slot < expected.size(); slot++) {
                if (!sameStack(endpoint.inventory().getStack(slot), expected.get(slot))) {
                    return false;
                }
            }
            return true;
        }

        private static void writeChanges(
                LogisticsEndpoint endpoint,
                List<ItemStack> before,
                List<ItemStack> after
        ) {
            for (int slot = 0; slot < after.size(); slot++) {
                if (!sameStack(before.get(slot), after.get(slot))) {
                    endpoint.inventory().setStack(slot, after.get(slot).copy());
                }
            }
        }

        private static void restore(LogisticsEndpoint endpoint, List<ItemStack> before) {
            for (int slot = 0; slot < before.size(); slot++) {
                endpoint.inventory().setStack(slot, before.get(slot).copy());
            }
        }
    }

    private record SourceScan(
            int availableQuantity,
            int matchingQuantity,
            List<Integer> slots,
            boolean policyRejected
    ) {
    }

    private record ExtractionSimulation(
            List<ItemStack> after,
            List<SlotMutation> mutations,
            List<ItemStack> movedStacks
    ) {
    }

    private record InsertionSimulation(
            List<ItemStack> after,
            int insertedQuantity,
            List<SlotMutation> mutations,
            boolean policyRejected
    ) {
    }

    private record SlotMutation(int slot, ItemStack stack) {
        private SlotMutation {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
