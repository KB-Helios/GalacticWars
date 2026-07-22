package galacticwars.clonewars.progression;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class PlayerCampaignAttachmentStateTest {
    public static void main(String[] args) {
        ProjectionFixture fixture = fixture();
        authoritativeConversionProducesBoundedClientReadModel(fixture);
        projectionTruncationIsDeterministicAndNonAuthoritative();
        authoritativeSubjectHistoryRetainsItsNewestBoundedWindow();
        updateOperationsReplaceOnlyTheirAuthoritativeHalf(fixture);
        nestedCollectionsAreDeeplyImmutable(fixture);
        mojangCodecRoundTripsAndRejectsUnknownSchema(fixture);
        streamCodecRoundTripsDeterministicallyAndBoundsInput(fixture);
        maximumProjectionFitsSyncBudget(fixture.playerId());
        System.out.println("PlayerCampaignAttachmentStateTest passed");
    }

    private static void authoritativeConversionProducesBoundedClientReadModel(ProjectionFixture fixture) {
        PlayerCampaignAttachmentState state = PlayerCampaignAttachmentService.fromAuthoritative(
                fixture.progression(), fixture.force());
        assertEquals(PlayerCampaignAttachmentState.CURRENT_SCHEMA_VERSION,
                state.schemaVersion(), "attachment schema");
        assertEquals(fixture.playerId(), state.playerId(), "attachment player");
        assertCampaignProjection(fixture.progression(), state.campaign());
        assertForceProjection(fixture.force(), state.force());
        assertTrue(!state.campaign().eventSubjects().containsKey(ProgressionEventType.DELIVERY_COMPLETED),
                "delivery replay subjects remain server-only");
        assertThrows(IllegalArgumentException.class, () ->
                        new PlayerCampaignAttachmentState.CampaignProjection(
                                "galacticwars:republic", 0, Map.of(),
                                Map.of(ProgressionEventType.DELIVERY_COMPLETED,
                                        Set.of("courier/" + UUID.randomUUID())),
                                Set.of()),
                "client projection rejects delivery replay subjects");

        PlayerCampaignAttachmentState initial = PlayerCampaignAttachmentState.initial(fixture.playerId());
        assertEquals(fixture.playerId(), initial.playerId(), "initial attachment player");
        assertCampaignProjection(ProgressionState.create(fixture.playerId()), initial.campaign());
        assertForceProjection(ForceRuntimeState.full(), initial.force());
    }

    private static void projectionTruncationIsDeterministicAndNonAuthoritative() {
        UUID playerId = UUID.randomUUID();
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        for (int index = PlayerCampaignAttachmentState.MAX_SUBJECTS_PER_TYPE + 8;
                index >= 0; index--) {
            visited.add(String.format(Locale.ROOT, "planet_%03d", index));
        }
        LinkedHashSet<String> unlocks = new LinkedHashSet<>();
        for (int index = PlayerCampaignAttachmentState.MAX_UNLOCKS + 8; index >= 0; index--) {
            unlocks.add(String.format(Locale.ROOT, "unlock_%03d", index));
        }
        LinkedHashMap<String, Long> cooldowns = new LinkedHashMap<>();
        for (int index = PlayerCampaignAttachmentState.MAX_FORCE_COOLDOWNS + 8;
                index >= 0; index--) {
            cooldowns.put(String.format(Locale.ROOT, "ability_%03d", index), (long) index);
        }
        ProgressionState progression = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                playerId,
                "galacticwars:republic",
                0,
                Set.of(UUID.randomUUID()),
                Map.of(ProgressionEventType.PLANET_VISITED, visited.size()),
                Map.of(ProgressionEventType.PLANET_VISITED, visited),
                unlocks);
        ForceRuntimeState force = new ForceRuntimeState(
                "jedi", 1, 0, 0,
                Set.of("force_sense", "light_push", "light_leap"),
                java.util.List.of("light_push", "", "light_leap"),
                80, cooldowns, Set.of(), 0, 0L, Map.of());

        PlayerCampaignAttachmentState projected =
                PlayerCampaignAttachmentService.fromAuthoritative(progression, force);
        assertEquals(PlayerCampaignAttachmentState.MAX_SUBJECTS_PER_TYPE,
                projected.campaign().eventSubjects().get(ProgressionEventType.PLANET_VISITED).size(),
                "client subject projection is bounded");
        assertEquals(visited.stream().sorted()
                        .limit(PlayerCampaignAttachmentState.MAX_SUBJECTS_PER_TYPE).toList(),
                new ArrayList<>(projected.campaign().eventSubjects()
                        .get(ProgressionEventType.PLANET_VISITED)),
                "client subjects select the lexicographically first values");
        assertEquals(PlayerCampaignAttachmentState.MAX_UNLOCKS,
                projected.campaign().unlocks().size(), "client unlock projection is bounded");
        assertEquals(unlocks.stream().sorted()
                        .limit(PlayerCampaignAttachmentState.MAX_UNLOCKS).toList(),
                new ArrayList<>(projected.campaign().unlocks()),
                "client unlocks select the lexicographically first values");
        assertEquals(PlayerCampaignAttachmentState.MAX_FORCE_COOLDOWNS,
                projected.force().cooldownEnds().size(), "client cooldown projection is bounded");
        assertEquals(cooldowns.keySet().stream().sorted()
                        .limit(PlayerCampaignAttachmentState.MAX_FORCE_COOLDOWNS).toList(),
                new ArrayList<>(projected.force().cooldownEnds().keySet()),
                "client cooldowns select the lexicographically first values");

        assertEquals(visited.size(),
                progression.eventSubjects().get(ProgressionEventType.PLANET_VISITED).size(),
                "projection does not truncate authoritative subjects");
        assertEquals(unlocks.size(), progression.unlocks().size(),
                "projection does not truncate authoritative unlocks");
        assertEquals(cooldowns.size(), force.cooldownEnds().size(),
                "projection does not truncate authoritative cooldowns");

        ProgressionState differentlyOrderedProgression = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                playerId,
                progression.factionId(),
                progression.credits(),
                progression.processedEventIds(),
                progression.eventTotals(),
                Map.of(ProgressionEventType.PLANET_VISITED,
                        new LinkedHashSet<>(visited.stream().sorted().toList())),
                new LinkedHashSet<>(unlocks.stream().sorted().toList()));
        LinkedHashMap<String, Long> differentlyOrderedCooldowns = new LinkedHashMap<>();
        cooldowns.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> differentlyOrderedCooldowns.put(entry.getKey(), entry.getValue()));
        ForceRuntimeState differentlyOrderedForce = new ForceRuntimeState(
                force.traditionId(), force.rank(), force.masteryExperience(), force.unspentPoints(),
                force.learnedNodeIds(), force.equippedAbilityIds(), force.energy(),
                differentlyOrderedCooldowns, force.processedActivationIds(),
                force.dailyCombatExperience(), force.combatExperienceDay(), force.recentMasteryKeys());
        assertEquals(projected, PlayerCampaignAttachmentService.fromAuthoritative(
                        differentlyOrderedProgression, differentlyOrderedForce),
                "projection is independent of authoritative collection iteration order");
    }

    private static void authoritativeSubjectHistoryRetainsItsNewestBoundedWindow() {
        LinkedHashSet<String> subjects = new LinkedHashSet<>();
        for (int index = 0;
                index <= ProgressionState.MAX_AUTHORITATIVE_SUBJECTS_PER_TYPE; index++) {
            subjects.add(String.format(Locale.ROOT, "region_%04d", index));
        }
        ProgressionState state = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(),
                "galacticwars:republic",
                0,
                Set.of(),
                Map.of(ProgressionEventType.REGION_CAPTURED, subjects.size()),
                Map.of(ProgressionEventType.REGION_CAPTURED, subjects),
                Set.of());

        Set<String> retained = state.eventSubjects().get(ProgressionEventType.REGION_CAPTURED);
        assertEquals(ProgressionState.MAX_AUTHORITATIVE_SUBJECTS_PER_TYPE, retained.size(),
                "authoritative subject history remains bounded");
        assertTrue(!retained.contains("region_0000"),
                "oldest authoritative subject leaves the recent-history window");
        assertTrue(retained.contains(String.format(Locale.ROOT, "region_%04d",
                        ProgressionState.MAX_AUTHORITATIVE_SUBJECTS_PER_TYPE)),
                "newest authoritative subject remains in the recent-history window");
    }

    private static void updateOperationsReplaceOnlyTheirAuthoritativeHalf(ProjectionFixture fixture) {
        PlayerCampaignAttachmentState original = PlayerCampaignAttachmentService.fromAuthoritative(
                fixture.progression(), fixture.force());
        ProgressionState updatedProgression = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                fixture.playerId(),
                fixture.progression().factionId(),
                fixture.progression().pendingCreditRewards() + 20,
                fixture.progression().processedEventIds(),
                fixture.progression().eventTotals(),
                fixture.progression().eventSubjects(),
                Set.of("force_path", "planet_travel", "vehicle_control"));
        PlayerCampaignAttachmentState campaignUpdated =
                PlayerCampaignAttachmentService.updateProgression(original, updatedProgression);
        assertTrue(campaignUpdated != original, "changed progression creates a replacement attachment");
        assertTrue(campaignUpdated.force() == original.force(), "progression update retains Force projection");
        assertCampaignProjection(updatedProgression, campaignUpdated.campaign());
        assertTrue(PlayerCampaignAttachmentService.updateProgression(
                campaignUpdated, updatedProgression) == campaignUpdated,
                "identical progression update preserves attachment identity");

        ForceRuntimeState updatedForce = new ForceRuntimeState(
                "light", 61, Map.of("light_push", 900L, "light_leap", 1_200L),
                fixture.force().processedActivationIds());
        PlayerCampaignAttachmentState forceUpdated =
                PlayerCampaignAttachmentService.updateForce(campaignUpdated, updatedForce);
        assertTrue(forceUpdated != campaignUpdated, "changed Force state creates a replacement attachment");
        assertTrue(forceUpdated.campaign() == campaignUpdated.campaign(),
                "Force update retains campaign projection");
        assertForceProjection(updatedForce, forceUpdated.force());
        assertTrue(PlayerCampaignAttachmentService.updateForce(forceUpdated, updatedForce) == forceUpdated,
                "identical Force update preserves attachment identity");

        assertThrows(IllegalArgumentException.class, () ->
                        PlayerCampaignAttachmentService.updateProgression(
                                original, ProgressionState.create(UUID.randomUUID())),
                "cross-player progression update rejected");
    }

    private static void nestedCollectionsAreDeeplyImmutable(ProjectionFixture fixture) {
        LinkedHashMap<ProgressionEventType, Integer> totals =
                new LinkedHashMap<>(fixture.progression().eventTotals());
        LinkedHashSet<String> visited = new LinkedHashSet<>(Set.of("kamino"));
        LinkedHashMap<ProgressionEventType, Set<String>> subjects = new LinkedHashMap<>();
        subjects.put(ProgressionEventType.PLANET_VISITED, visited);
        LinkedHashSet<String> unlocks = new LinkedHashSet<>(Set.of("planet_travel"));
        LinkedHashMap<String, Long> cooldowns = new LinkedHashMap<>(fixture.force().cooldownEnds());

        PlayerCampaignAttachmentState.CampaignProjection campaign =
                new PlayerCampaignAttachmentState.CampaignProjection(
                        "galacticwars:republic", 40, totals, subjects, unlocks);
        PlayerCampaignAttachmentState.ForceProjection force =
                new PlayerCampaignAttachmentState.ForceProjection(
                        "light", 80, cooldowns);
        PlayerCampaignAttachmentState state = new PlayerCampaignAttachmentState(
                PlayerCampaignAttachmentState.CURRENT_SCHEMA_VERSION,
                fixture.playerId(), campaign, force);

        totals.clear();
        visited.add("invented_planet");
        subjects.clear();
        unlocks.add("invented_unlock");
        cooldowns.put("invented_ability", 1L);

        assertTrue(!state.campaign().eventTotals().isEmpty(), "totals copied");
        assertTrue(!state.campaign().eventSubjects().get(ProgressionEventType.PLANET_VISITED)
                .contains("invented_planet"), "nested subjects copied");
        assertTrue(!state.campaign().unlocks().contains("invented_unlock"), "unlocks copied");
        assertTrue(!state.force().cooldownEnds().containsKey("invented_ability"), "cooldowns copied");

        assertUnsupported(() -> state.campaign().eventTotals().put(ProgressionEventType.PLANET_VISITED, 9),
                "totals immutable");
        assertUnsupported(() -> state.campaign().eventSubjects()
                        .get(ProgressionEventType.PLANET_VISITED).add("other"),
                "nested subjects immutable");
        assertUnsupported(() -> state.campaign().unlocks().add("other"), "unlocks immutable");
        assertUnsupported(() -> state.force().cooldownEnds().put("other", 1L), "cooldowns immutable");
    }

    private static void mojangCodecRoundTripsAndRejectsUnknownSchema(ProjectionFixture fixture) {
        PlayerCampaignAttachmentState expected = PlayerCampaignAttachmentService.fromAuthoritative(
                fixture.progression(), fixture.force());
        Tag encoded = PlayerCampaignAttachmentState.CODEC
                .encodeStart(NbtOps.INSTANCE, expected).getOrThrow();
        PlayerCampaignAttachmentState decoded = PlayerCampaignAttachmentState.CODEC
                .parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(expected, decoded, "Mojang codec round trip");
        assertTrue(encoded instanceof CompoundTag, "attachment codec emits a structured compound");

        CompoundTag invalidSchema = ((CompoundTag) encoded).copy();
        invalidSchema.putInt("schema_version", PlayerCampaignAttachmentState.CURRENT_SCHEMA_VERSION + 1);
        assertTrue(PlayerCampaignAttachmentState.CODEC.parse(NbtOps.INSTANCE, invalidSchema)
                        .error().isPresent(),
                "Mojang codec rejects unknown schema versions");
    }

    private static void streamCodecRoundTripsDeterministicallyAndBoundsInput(ProjectionFixture fixture) {
        PlayerCampaignAttachmentState expected = PlayerCampaignAttachmentService.fromAuthoritative(
                fixture.progression(), fixture.force());
        RegistryFriendlyByteBuf first = buffer();
        RegistryFriendlyByteBuf second = buffer();
        try {
            PlayerCampaignAttachmentState.STREAM_CODEC.encode(first, expected);
            PlayerCampaignAttachmentState.STREAM_CODEC.encode(second, expected);
            assertTrue(Arrays.equals(ByteBufUtil.getBytes(first), ByteBufUtil.getBytes(second)),
                    "stream encoding is deterministic");
            PlayerCampaignAttachmentState decoded =
                    PlayerCampaignAttachmentState.STREAM_CODEC.decode(first);
            assertEquals(expected, decoded, "stream codec round trip");
            assertTrue(!first.isReadable(), "stream codec consumes exactly one attachment");
        } finally {
            first.release();
            second.release();
        }

        RegistryFriendlyByteBuf oversized = buffer();
        try {
            oversized.writeVarInt(PlayerCampaignAttachmentState.CURRENT_SCHEMA_VERSION);
            oversized.writeUUID(fixture.playerId());
            oversized.writeUtf("", PlayerCampaignAttachmentState.MAX_IDENTIFIER_LENGTH);
            oversized.writeVarInt(0);
            oversized.writeVarInt(0);
            oversized.writeVarInt(1);
            oversized.writeUtf("planet_visited", PlayerCampaignAttachmentState.MAX_IDENTIFIER_LENGTH);
            oversized.writeVarInt(PlayerCampaignAttachmentState.MAX_SUBJECTS_PER_TYPE + 1);
            assertThrows(IllegalArgumentException.class,
                    () -> PlayerCampaignAttachmentState.STREAM_CODEC.decode(oversized),
                    "stream codec rejects oversized client-subject input before allocation");
        } finally {
            oversized.release();
        }
    }

    private static void maximumProjectionFitsSyncBudget(UUID playerId) {
        LinkedHashMap<ProgressionEventType, Integer> totals = new LinkedHashMap<>();
        LinkedHashMap<ProgressionEventType, Set<String>> subjects = new LinkedHashMap<>();
        for (ProgressionEventType type : ProgressionEventType.values()) {
            totals.put(type, Integer.MAX_VALUE);
            if (type == ProgressionEventType.DELIVERY_COMPLETED) {
                continue;
            }
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (int index = 0; index < PlayerCampaignAttachmentState.MAX_SUBJECTS_PER_TYPE; index++) {
                values.add(maximumIdentifier(type.name().toLowerCase(Locale.ROOT), index));
            }
            subjects.put(type, values);
        }
        LinkedHashSet<String> unlocks = new LinkedHashSet<>();
        for (int index = 0; index < PlayerCampaignAttachmentState.MAX_UNLOCKS; index++) {
            unlocks.add(maximumIdentifier("unlock", index));
        }
        LinkedHashMap<String, Long> cooldowns = new LinkedHashMap<>();
        for (int index = 0; index < PlayerCampaignAttachmentState.MAX_FORCE_COOLDOWNS; index++) {
            cooldowns.put(maximumIdentifier("ability", index), Long.MAX_VALUE);
        }
        PlayerCampaignAttachmentState maximal = new PlayerCampaignAttachmentState(
                PlayerCampaignAttachmentState.CURRENT_SCHEMA_VERSION,
                playerId,
                new PlayerCampaignAttachmentState.CampaignProjection(
                        maximumIdentifier("faction", 0), Integer.MAX_VALUE,
                        totals, subjects, unlocks),
                new PlayerCampaignAttachmentState.ForceProjection(
                        "light", ForceRuntimeState.MAX_ENERGY, cooldowns));

        RegistryFriendlyByteBuf encoded = buffer();
        try {
            PlayerCampaignAttachmentState.STREAM_CODEC.encode(encoded, maximal);
            assertTrue(encoded.readableBytes() < PlayerCampaignAttachmentState.MAX_SYNC_PAYLOAD_BYTES,
                    "maximum legal client projection remains below the 1 MiB sync budget");
            assertEquals(maximal, PlayerCampaignAttachmentState.STREAM_CODEC.decode(encoded),
                    "maximum legal client projection round trip");
        } finally {
            encoded.release();
        }
    }

    private static String maximumIdentifier(String prefix, int index) {
        String seed = prefix + "_" + String.format(Locale.ROOT, "%03d", index) + "_";
        return seed + "\u0800".repeat(
                PlayerCampaignAttachmentState.MAX_IDENTIFIER_LENGTH - seed.length());
    }

    private static ProjectionFixture fixture() {
        UUID playerId = UUID.randomUUID();
        Set<UUID> processedEvents = new LinkedHashSet<>(Set.of(UUID.randomUUID(), UUID.randomUUID()));
        Map<ProgressionEventType, Integer> totals = new LinkedHashMap<>();
        totals.put(ProgressionEventType.FACTION_PLEDGED, 1);
        totals.put(ProgressionEventType.PLANET_VISITED, 2);
        totals.put(ProgressionEventType.QUEST_ADVANCED, 2);
        totals.put(ProgressionEventType.DELIVERY_COMPLETED, 1);
        Map<ProgressionEventType, Set<String>> subjects = new LinkedHashMap<>();
        subjects.put(ProgressionEventType.FACTION_PLEDGED, Set.of("galacticwars:republic"));
        subjects.put(ProgressionEventType.PLANET_VISITED, Set.of("kamino", "coruscant"));
        subjects.put(ProgressionEventType.QUEST_ADVANCED,
                Set.of("republic_chapter_1", "republic_chapter_2"));
        subjects.put(ProgressionEventType.DELIVERY_COMPLETED,
                Set.of("courier/" + UUID.randomUUID()));
        ProgressionState progression = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                playerId,
                "galacticwars:republic",
                110,
                processedEvents,
                totals,
                subjects,
                Set.of("intro_quest", "force_path", "planet_travel"));
        Set<UUID> processedActivations = new LinkedHashSet<>(Set.of(UUID.randomUUID(), UUID.randomUUID()));
        ForceRuntimeState force = new ForceRuntimeState(
                "light", 74, Map.of("light_push", 420L, "light_pull", 510L), processedActivations);
        return new ProjectionFixture(playerId, progression, force);
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    private static void assertCampaignProjection(
            ProgressionState authoritative,
            PlayerCampaignAttachmentState.CampaignProjection projected
    ) {
        assertEquals(authoritative.factionId(), projected.factionId(), "faction projection");
        assertEquals(authoritative.pendingCreditRewards(), projected.pendingCreditRewards(),
                "pending credits projection");
        assertEquals(authoritative.eventTotals(), projected.eventTotals(), "event total projection");
        LinkedHashMap<ProgressionEventType, Set<String>> expectedSubjects =
                new LinkedHashMap<>(authoritative.eventSubjects());
        expectedSubjects.remove(ProgressionEventType.DELIVERY_COMPLETED);
        assertEquals(expectedSubjects, projected.eventSubjects(), "client-visible subject projection");
        assertEquals(authoritative.unlocks(), projected.unlocks(), "unlock projection");
    }

    private static void assertForceProjection(
            ForceRuntimeState authoritative,
            PlayerCampaignAttachmentState.ForceProjection projected
    ) {
        assertEquals(authoritative.path(), projected.path(), "Force path projection");
        assertEquals(authoritative.energy(), projected.energy(), "Force energy projection");
        assertEquals(authoritative.cooldownEnds(), projected.cooldownEnds(),
                "Force cooldown projection");
    }

    private static void assertUnsupported(Runnable action, String label) {
        assertThrows(UnsupportedOperationException.class, action, label);
    }

    private static void assertThrows(
            Class<? extends Throwable> expected,
            Runnable action,
            String label
    ) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError(label + " threw " + actual, actual);
        }
        throw new AssertionError(label + " did not throw " + expected.getSimpleName());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private record ProjectionFixture(
            UUID playerId,
            ProgressionState progression,
            ForceRuntimeState force
    ) {
    }
}
