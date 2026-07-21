package galacticwars.clonewars.gametest;

import galacticwars.clonewars.workforce.logistics.LogisticsAccessPolicy;
import galacticwars.clonewars.workforce.logistics.LogisticsEndpoint;
import galacticwars.clonewars.workforce.logistics.LogisticsEndpointIdentity;
import galacticwars.clonewars.workforce.logistics.LogisticsInventory;
import galacticwars.clonewars.workforce.logistics.LogisticsTransferAuthority;
import galacticwars.clonewars.workforce.logistics.LogisticsTransferRequest;
import galacticwars.clonewars.workforce.logistics.PhysicalLogisticsTransaction;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused physical-inventory tests that require the bootstrapped NeoForge GameTest runtime. */
public final class PhysicalLogisticsGameTests {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-00000000a701");
    private static final LogisticsEndpointIdentity SOURCE = new LogisticsEndpointIdentity("storage/source");
    private static final LogisticsEndpointIdentity DESTINATION = new LogisticsEndpointIdentity("worker/cargo");

    private PhysicalLogisticsGameTests() {
    }

    public static void atomicPhysicalTransfer(GameTestHelper helper) {
        try {
            exactTransferPreservesComponentsAndConservesItems();
            exactTransferDoesNotPartiallyMutate();
            partialTransferMovesOnlyDestinationCapacity();
            endpointIdentityAndPolicyAreAuthoritative();
            staleSimulationDoesNotCommit();
            mutationFailureRollsBackBothInventories();
            samePhysicalInventoryIsRejected();
            helper.succeed();
        } catch (AssertionError | RuntimeException failure) {
            helper.fail("Physical logistics transaction failed: " + failure.getMessage());
        }
    }

    private static void exactTransferPreservesComponentsAndConservesItems() {
        ItemStack marked = markedIron(1);
        SimpleContainer sourceContainer = new SimpleContainer(2);
        sourceContainer.setItem(0, marked.copyWithCount(3));
        sourceContainer.setItem(1, marked.copyWithCount(4));
        SimpleContainer destinationContainer = new SimpleContainer(2);
        destinationContainer.setItem(0, marked.copyWithCount(61));

        var result = PhysicalLogisticsTransaction.transfer(
                endpoint(SOURCE, sourceContainer, LogisticsAccessPolicy.allowAll()),
                endpoint(DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll()),
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(marked, 6,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));

        assertEquals(PhysicalLogisticsTransaction.Status.COMMITTED, result.status(), "exact status");
        assertEquals(6, result.transferredQuantity(), "exact moved quantity");
        assertEquals(68, matchingCount(sourceContainer, marked) + matchingCount(destinationContainer, marked),
                "exact transfer conserves every item");
        assertEquals(1, sourceContainer.getItem(1).getCount(), "source remainder");
        assertEquals(64, destinationContainer.getItem(0).getCount(), "existing stack filled");
        assertEquals(3, destinationContainer.getItem(1).getCount(), "overflow stack inserted");
        assertTrue(ItemStack.isSameItemSameComponents(marked, destinationContainer.getItem(0)),
                "custom components survive merge");
        assertTrue(result.transferredStacks().stream()
                .allMatch(stack -> ItemStack.isSameItemSameComponents(marked, stack)),
                "result preserves complete stack identity");
    }

    private static void exactTransferDoesNotPartiallyMutate() {
        ItemStack iron = markedIron(1);
        SimpleContainer sourceContainer = container(iron.copyWithCount(4));
        SimpleContainer destinationContainer = container(iron.copyWithCount(64));

        var result = PhysicalLogisticsTransaction.transfer(
                endpoint(SOURCE, sourceContainer, LogisticsAccessPolicy.allowAll()),
                endpoint(DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll()),
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 4,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));

        assertEquals(PhysicalLogisticsTransaction.Status.DESTINATION_UNAVAILABLE, result.status(),
                "exact full destination status");
        assertEquals(4, sourceContainer.getItem(0).getCount(), "exact failure leaves source intact");
        assertEquals(64, destinationContainer.getItem(0).getCount(),
                "exact failure leaves destination intact");
    }

    private static void partialTransferMovesOnlyDestinationCapacity() {
        ItemStack iron = markedIron(1);
        SimpleContainer sourceContainer = container(iron.copyWithCount(4));
        SimpleContainer destinationContainer = container(iron.copyWithCount(63));

        var result = PhysicalLogisticsTransaction.transfer(
                endpoint(SOURCE, sourceContainer, LogisticsAccessPolicy.allowAll()),
                endpoint(DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll()),
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 4,
                        LogisticsTransferRequest.Fulfillment.ALLOW_PARTIAL));

        assertEquals(PhysicalLogisticsTransaction.Status.COMMITTED, result.status(), "partial status");
        assertEquals(1, result.transferredQuantity(), "partial moved quantity");
        assertEquals(3, sourceContainer.getItem(0).getCount(), "partial source remainder");
        assertEquals(64, destinationContainer.getItem(0).getCount(), "partial destination fill");
        assertEquals(67, matchingCount(sourceContainer, iron) + matchingCount(destinationContainer, iron),
                "partial transfer conserves every item");
    }

    private static void endpointIdentityAndPolicyAreAuthoritative() {
        ItemStack iron = markedIron(1);
        SimpleContainer sourceContainer = container(iron.copyWithCount(2));
        SimpleContainer destinationContainer = new SimpleContainer(1);
        LogisticsEndpoint source = endpoint(SOURCE, sourceContainer, LogisticsAccessPolicy.allowAll());
        LogisticsEndpoint destination = endpoint(DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll());

        var wrongIdentity = PhysicalLogisticsTransaction.transfer(
                source, destination,
                authority(new LogisticsEndpointIdentity("storage/other"), DESTINATION),
                new LogisticsTransferRequest(iron, 1,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));
        assertEquals(PhysicalLogisticsTransaction.Status.SOURCE_IDENTITY_MISMATCH, wrongIdentity.status(),
                "caller must bind the expected source identity");

        LogisticsAccessPolicy denyExtraction = (actor, endpoint, counterpart, operation, slot, stack) ->
                operation != LogisticsAccessPolicy.Operation.EXTRACT;
        var denied = PhysicalLogisticsTransaction.transfer(
                endpoint(SOURCE, sourceContainer, denyExtraction), destination,
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 1,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));
        assertEquals(PhysicalLogisticsTransaction.Status.SOURCE_POLICY_DENIED, denied.status(),
                "endpoint policy denies extraction");
        assertEquals(2, sourceContainer.getItem(0).getCount(), "denial leaves source intact");
        assertTrue(destinationContainer.isEmpty(), "denial leaves destination intact");

        LogisticsAccessPolicy denyInsertion = (actor, endpoint, counterpart, operation, slot, stack) ->
                operation != LogisticsAccessPolicy.Operation.INSERT;
        var insertionDenied = PhysicalLogisticsTransaction.transfer(
                source, endpoint(DESTINATION, destinationContainer, denyInsertion),
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 1,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));
        assertEquals(PhysicalLogisticsTransaction.Status.DESTINATION_POLICY_DENIED, insertionDenied.status(),
                "endpoint policy denies insertion");
        assertEquals(2, sourceContainer.getItem(0).getCount(), "insertion denial leaves source intact");
        assertTrue(destinationContainer.isEmpty(), "insertion denial leaves destination intact");
    }

    private static void staleSimulationDoesNotCommit() {
        ItemStack iron = markedIron(1);
        SimpleContainer sourceContainer = container(iron.copyWithCount(4));
        SimpleContainer destinationContainer = new SimpleContainer(1);
        var simulation = PhysicalLogisticsTransaction.simulate(
                endpoint(SOURCE, sourceContainer, LogisticsAccessPolicy.allowAll()),
                endpoint(DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll()),
                authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 2,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));
        assertTrue(simulation.executable(), "preflight is executable");

        sourceContainer.setItem(0, iron.copyWithCount(3));
        var result = simulation.commit();
        assertEquals(PhysicalLogisticsTransaction.Status.STALE_STATE, result.status(), "stale plan rejected");
        assertEquals(3, sourceContainer.getItem(0).getCount(), "external source change retained");
        assertTrue(destinationContainer.isEmpty(), "stale plan never touches destination");
    }

    private static void mutationFailureRollsBackBothInventories() {
        ItemStack iron = markedIron(1);
        SimpleContainer sourceContainer = container(iron.copyWithCount(4));
        SimpleContainer destinationContainer = new SimpleContainer(1);
        FailingInventory failingSource = new FailingInventory(sourceContainer);
        LogisticsEndpoint source = new LogisticsEndpoint(
                SOURCE, failingSource, 1, LogisticsAccessPolicy.allowAll());
        LogisticsEndpoint destination = endpoint(
                DESTINATION, destinationContainer, LogisticsAccessPolicy.allowAll());

        var simulation = PhysicalLogisticsTransaction.simulate(
                source, destination, authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 2,
                        LogisticsTransferRequest.Fulfillment.REQUIRE_EXACT));
        failingSource.failNextWrite();
        var result = simulation.commit();

        assertEquals(PhysicalLogisticsTransaction.Status.MUTATION_FAILED, result.status(),
                "write failure reported");
        assertEquals(4, sourceContainer.getItem(0).getCount(), "rollback restores source");
        assertTrue(destinationContainer.isEmpty(), "rollback restores destination");
    }

    private static void samePhysicalInventoryIsRejected() {
        ItemStack iron = markedIron(1);
        SimpleContainer physical = container(iron.copyWithCount(2));
        LogisticsEndpoint source = endpoint(SOURCE, physical, LogisticsAccessPolicy.allowAll());
        LogisticsEndpoint destination = endpoint(DESTINATION, physical, LogisticsAccessPolicy.allowAll());

        var result = PhysicalLogisticsTransaction.transfer(
                source, destination, authority(SOURCE, DESTINATION),
                new LogisticsTransferRequest(iron, 1,
                        LogisticsTransferRequest.Fulfillment.ALLOW_PARTIAL));
        assertEquals(PhysicalLogisticsTransaction.Status.SAME_PHYSICAL_INVENTORY, result.status(),
                "two adapters over one inventory rejected");
        assertEquals(2, physical.getItem(0).getCount(), "same-inventory rejection conserves contents");
    }

    private static ItemStack markedIron(int count) {
        ItemStack stack = new ItemStack(Items.IRON_INGOT, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Batch Aurek"));
        return stack;
    }

    private static SimpleContainer container(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack);
        return container;
    }

    private static LogisticsEndpoint endpoint(
            LogisticsEndpointIdentity identity,
            SimpleContainer container,
            LogisticsAccessPolicy policy
    ) {
        return LogisticsEndpoint.container(identity, container, container.getContainerSize(), policy);
    }

    private static LogisticsTransferAuthority authority(
            LogisticsEndpointIdentity source,
            LogisticsEndpointIdentity destination
    ) {
        return new LogisticsTransferAuthority(ACTOR, source, destination);
    }

    private static int matchingCount(SimpleContainer container, ItemStack template) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static final class FailingInventory implements LogisticsInventory {
        private final SimpleContainer delegate;
        private boolean failNextWrite;

        private FailingInventory(SimpleContainer delegate) {
            this.delegate = delegate;
        }

        private void failNextWrite() {
            failNextWrite = true;
        }

        @Override
        public int size() {
            return delegate.getContainerSize();
        }

        @Override
        public ItemStack getStack(int slot) {
            return delegate.getItem(slot);
        }

        @Override
        public int maxStackSize(int slot, ItemStack stack) {
            return Math.min(delegate.getMaxStackSize(stack), stack.getMaxStackSize());
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (failNextWrite) {
                failNextWrite = false;
                throw new IllegalStateException("injected write failure");
            }
            delegate.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            delegate.setChanged();
        }

        @Override
        public Object transactionIdentity() {
            return delegate;
        }
    }
}
