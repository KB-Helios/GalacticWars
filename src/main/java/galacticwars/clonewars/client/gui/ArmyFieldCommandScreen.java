package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.army.FieldCommandAction;
import galacticwars.clonewars.army.ArmyPatrolPlan;
import galacticwars.clonewars.client.FieldCommandClientState;
import galacticwars.clonewars.network.FieldCommandRequestPayload;
import galacticwars.clonewars.network.FieldCommandStatePayload;
import galacticwars.clonewars.network.GalacticNetwork;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lightweight, in-world squad command panel. It shows only a server-owned projection and never
 * sends target coordinates or entity identities from the client.
 */
public final class ArmyFieldCommandScreen extends Screen {
    private static final int MAX_SELECTED_SQUADS = FieldCommandRequestPayload.MAX_GROUPS;
    private static final int SQUADS_PER_PAGE = 8;
    private static final int BUTTON_WIDTH = 132;
    private static final int BUTTON_HEIGHT = 18;
    private static final int GAP = 3;
    private static final int COMPACT_BUTTON_WIDTH = (BUTTON_WIDTH - GAP) / 2;
    private static final int TEXT_COLOR = 0xE6EDF3;
    private static final int MUTED_COLOR = 0x9BA8B5;
    private static final int FEEDBACK_COLOR = 0xFFD27A;

    private final Set<UUID> selectedGroupIds = new LinkedHashSet<>();
    private Category category = Category.MOVEMENT;
    private int groupPage;
    private int patrolWaypointIndex;
    private long observedRevision = -1L;
    private boolean refreshRequested;
    private String patrolRouteNameDraft = "";
    private String patrolWaypointWaitDraft = "0";
    private EditBox patrolRouteNameInput;
    private EditBox patrolWaypointWaitInput;
    private Component feedback = Component.translatable("screen.galacticwars.field_command.syncing");

    public ArmyFieldCommandScreen() {
        super(Component.translatable("screen.galacticwars.field_command.title"));
    }

    @Override
    protected void init() {
        super.init();
        syncState();
        buildWidgets();
        if (!refreshRequested) {
            refreshRequested = true;
            GalacticNetwork.CHANNEL.sendToServer(new FieldCommandRequestPayload(
                    UUID.randomUUID(), FieldCommandAction.REFRESH, List.of()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (FieldCommandClientState.revision() != observedRevision) {
            syncState();
            this.rebuildWidgets();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FieldCommandStatePayload state = FieldCommandClientState.snapshot();
        int left = Math.max(12, (this.width - (BUTTON_WIDTH * 2 + 24)) / 2);
        int right = left + BUTTON_WIDTH + 24;
        graphics.text(this.font, this.title, left, 10, TEXT_COLOR);
        graphics.text(this.font, Component.translatable(
                "screen.galacticwars.field_command.selection", selectedGroupIds.size(), MAX_SELECTED_SQUADS),
                left, 42, MUTED_COLOR);
        graphics.text(this.font, markerStatus(state), right, 42, MUTED_COLOR);
        if (category == Category.PATROL) {
            graphics.text(this.font, Component.translatable(
                    "screen.galacticwars.field_command.patrol_waypoint",
                    patrolWaypointIndex + 1,
                    sharedPatrolWaypointCount(state)), right, 10, MUTED_COLOR);
        }
        graphics.text(this.font, feedback, left, this.height - 18, FEEDBACK_COLOR);
        graphics.text(this.font, Component.translatable("screen.galacticwars.field_command.marker_hint"),
                left, this.height - 30, MUTED_COLOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void buildWidgets() {
        FieldCommandStatePayload state = FieldCommandClientState.snapshot();
        int left = Math.max(12, (this.width - (BUTTON_WIDTH * 2 + 24)) / 2);
        int right = left + BUTTON_WIDTH + 24;
        int tabWidth = 74;
        int tabsLeft = Math.max(8, (this.width - (tabWidth * Category.values().length + GAP * 3)) / 2);
        for (int index = 0; index < Category.values().length; index++) {
            Category candidate = Category.values()[index];
            Button tab = Button.builder(Component.translatable(candidate.translationKey),
                            ignored -> {
                                category = candidate;
                                this.rebuildWidgets();
                            })
                    .bounds(tabsLeft + index * (tabWidth + GAP), 22, tabWidth, BUTTON_HEIGHT)
                    .build();
            tab.active = candidate != category;
            this.addRenderableWidget(tab);
        }

        List<FieldCommandStatePayload.Squad> squads = state.squads();
        int pageCount = Math.max(1, (squads.size() + SQUADS_PER_PAGE - 1) / SQUADS_PER_PAGE);
        groupPage = Math.max(0, Math.min(groupPage, pageCount - 1));
        int first = groupPage * SQUADS_PER_PAGE;
        int last = Math.min(squads.size(), first + SQUADS_PER_PAGE);
        if (squads.isEmpty()) {
            this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.galacticwars.field_command.no_squads"),
                            ignored -> requestRefresh())
                    .bounds(left, 58, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        } else {
            for (int index = first; index < last; index++) {
                FieldCommandStatePayload.Squad squad = squads.get(index);
                int y = 58 + (index - first) * (BUTTON_HEIGHT + GAP);
                this.addRenderableWidget(Button.builder(squadLabel(squad),
                                ignored -> toggleSquad(squad.id()))
                        .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build());
            }
        }
        if (pageCount > 1) {
            Button previous = Button.builder(Component.translatable("screen.galacticwars.field_command.previous"),
                            ignored -> {
                                groupPage--;
                                this.rebuildWidgets();
                            })
                    .bounds(left, 58 + SQUADS_PER_PAGE * (BUTTON_HEIGHT + GAP), 64, BUTTON_HEIGHT)
                    .build();
            previous.active = groupPage > 0;
            this.addRenderableWidget(previous);
            Button next = Button.builder(Component.translatable("screen.galacticwars.field_command.next"),
                            ignored -> {
                                groupPage++;
                                this.rebuildWidgets();
                            })
                    .bounds(left + 68, 58 + SQUADS_PER_PAGE * (BUTTON_HEIGHT + GAP), 64, BUTTON_HEIGHT)
                    .build();
            next.active = groupPage + 1 < pageCount;
            this.addRenderableWidget(next);
        }

        if (category == Category.PATROL) {
            buildPatrolWidgets(right, state);
            return;
        }

        int actionY = 58;
        for (ActionButton action : category.actions) {
            Button button = Button.builder(Component.translatable(action.translationKey),
                            ignored -> sendAction(action.action))
                    .bounds(right, actionY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = !selectedGroupIds.isEmpty()
                    && action.isAvailable(state);
            this.addRenderableWidget(button);
            actionY += BUTTON_HEIGHT + GAP;
        }
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.galacticwars.field_command.refresh"),
                        ignored -> requestRefresh())
                .bounds(right, actionY + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void syncState() {
        FieldCommandStatePayload state = FieldCommandClientState.snapshot();
        observedRevision = FieldCommandClientState.revision();
        Set<UUID> available = state.squads().stream()
                .map(FieldCommandStatePayload.Squad::id)
                .collect(java.util.stream.Collectors.toSet());
        selectedGroupIds.retainAll(available);
        if (observedRevision > 0L) {
            feedback = Component.translatable(state.result().translationKey());
        }
    }

    /**
     * Patrol editing deliberately remains a compact extension of the field panel:
     * marker coordinates never leave the server-owned Tactical Command Marker,
     * while the only client-authored values are bounded presentation/configuration
     * values (route name, waypoint index, and dwell ticks).
     */
    private void buildPatrolWidgets(int right, FieldCommandStatePayload state) {
        capturePatrolDrafts();
        selectedPatrol(state).ifPresent(patrol -> {
            if (patrolRouteNameDraft.isBlank()) {
                patrolRouteNameDraft = patrol.name();
            }
            patrolWaypointIndex = Math.min(patrolWaypointIndex, sharedPatrolWaypointCount(state) - 1);
        });

        patrolRouteNameInput = new EditBox(
                this.font,
                right,
                58,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatable("screen.galacticwars.field_command.patrol_name"));
        patrolRouteNameInput.setMaxLength(32);
        patrolRouteNameInput.setValue(patrolRouteNameDraft);
        patrolRouteNameInput.setHint(Component.translatable("screen.galacticwars.field_command.patrol_name"));
        this.addRenderableWidget(patrolRouteNameInput);

        patrolWaypointWaitInput = new EditBox(
                this.font,
                right,
                79,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatable("screen.galacticwars.field_command.patrol_wait"));
        patrolWaypointWaitInput.setMaxLength(5);
        patrolWaypointWaitInput.setValue(patrolWaypointWaitDraft);
        patrolWaypointWaitInput.setHint(Component.translatable("screen.galacticwars.field_command.patrol_wait"));
        this.addRenderableWidget(patrolWaypointWaitInput);

        boolean selected = !selectedGroupIds.isEmpty();
        boolean hasRoute = selected && allSelectedHavePatrol(state);
        int actionY = 100;
        addPatrolButton(right, actionY, FieldCommandAction.PATROL_MARKER,
                "screen.galacticwars.field_command.create_patrol_route", selected && state.markedBlockAvailable());
        addPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY, FieldCommandAction.RENAME_PATROL_ROUTE,
                "screen.galacticwars.field_command.rename_patrol_short", hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addLocalPatrolButton(right, actionY, "screen.galacticwars.field_command.patrol_previous_waypoint",
                ignored -> changePatrolWaypoint(-1, state), hasRoute);
        addLocalPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY,
                "screen.galacticwars.field_command.patrol_next_waypoint",
                ignored -> changePatrolWaypoint(1, state), hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.SET_PATROL_WAYPOINT_WAIT,
                "screen.galacticwars.field_command.set_patrol_wait_short", hasRoute);
        addPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY, FieldCommandAction.PAUSE_PATROL,
                "screen.galacticwars.field_command.pause_patrol_short", hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.RESUME_PATROL,
                "screen.galacticwars.field_command.resume_patrol_short", hasRoute);
        addPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY, FieldCommandAction.STOP_PATROL,
                "screen.galacticwars.field_command.stop_patrol_short", hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.CYCLE_PATROL_MODE,
                "screen.galacticwars.field_command.patrol_mode_short", hasRoute);
        addPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY, FieldCommandAction.CYCLE_PATROL_SPEED,
                "screen.galacticwars.field_command.patrol_speed_short", hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.CYCLE_PATROL_ENEMY_POLICY,
                "screen.galacticwars.field_command.patrol_enemy_policy_short", hasRoute);
        addLocalPatrolButton(right + COMPACT_BUTTON_WIDTH + GAP, actionY,
                "screen.galacticwars.field_command.refresh", ignored -> requestRefresh(), true);
    }

    private void addPatrolButton(
            int x,
            int y,
            FieldCommandAction action,
            String translationKey,
            boolean active
    ) {
        Button button = Button.builder(Component.translatable(translationKey), ignored -> sendAction(action))
                .bounds(x, y, COMPACT_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private void addLocalPatrolButton(
            int x,
            int y,
            String translationKey,
            Button.OnPress onPress,
            boolean active
    ) {
        Button button = Button.builder(Component.translatable(translationKey), onPress)
                .bounds(x, y, COMPACT_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private void capturePatrolDrafts() {
        if (patrolRouteNameInput != null) {
            patrolRouteNameDraft = patrolRouteNameInput.getValue();
        }
        if (patrolWaypointWaitInput != null) {
            patrolWaypointWaitDraft = patrolWaypointWaitInput.getValue();
        }
    }

    private java.util.Optional<FieldCommandStatePayload.Squad.Patrol> selectedPatrol(
            FieldCommandStatePayload state
    ) {
        return selectedGroupIds.stream()
                .flatMap(id -> state.squads().stream().filter(squad -> squad.id().equals(id)))
                .map(FieldCommandStatePayload.Squad::patrol)
                .flatMap(java.util.Optional::stream)
                .findFirst();
    }

    private boolean allSelectedHavePatrol(FieldCommandStatePayload state) {
        return selectedGroupIds.stream().allMatch(id -> state.squads().stream()
                .filter(squad -> squad.id().equals(id))
                .findFirst()
                .flatMap(FieldCommandStatePayload.Squad::patrol)
                .isPresent());
    }

    private void changePatrolWaypoint(int direction, FieldCommandStatePayload state) {
        int waypointCount = sharedPatrolWaypointCount(state);
        patrolWaypointIndex = Math.floorMod(patrolWaypointIndex + direction, waypointCount);
        this.rebuildWidgets();
    }

    private int sharedPatrolWaypointCount(FieldCommandStatePayload state) {
        return selectedGroupIds.stream()
                .flatMap(id -> state.squads().stream().filter(squad -> squad.id().equals(id)))
                .map(FieldCommandStatePayload.Squad::patrol)
                .flatMap(java.util.Optional::stream)
                .mapToInt(FieldCommandStatePayload.Squad.Patrol::waypointCount)
                .min()
                .orElse(1);
    }

    private void toggleSquad(UUID squadId) {
        if (selectedGroupIds.remove(squadId)) {
            this.rebuildWidgets();
            return;
        }
        if (selectedGroupIds.size() >= MAX_SELECTED_SQUADS) {
            feedback = Component.translatable("screen.galacticwars.field_command.selection_limit",
                    MAX_SELECTED_SQUADS);
            return;
        }
        selectedGroupIds.add(squadId);
        this.rebuildWidgets();
    }

    private void sendAction(FieldCommandAction action) {
        if (selectedGroupIds.isEmpty()) {
            feedback = Component.translatable("screen.galacticwars.field_command.reason.squad_selection_required");
            return;
        }
        capturePatrolDrafts();
        Integer waitTicks = requestedWaypointWait(action);
        if (waitTicks == null) {
            return;
        }
        try {
            GalacticNetwork.CHANNEL.sendToServer(new FieldCommandRequestPayload(
                    UUID.randomUUID(),
                    action,
                    List.copyOf(selectedGroupIds),
                    patrolRouteNameDraft,
                    patrolWaypointIndex,
                    waitTicks));
        } catch (IllegalArgumentException ignored) {
            feedback = Component.translatable("screen.galacticwars.field_command.reason.invalid_action");
            return;
        }
        feedback = Component.translatable("screen.galacticwars.field_command.request_sent");
    }

    private Integer requestedWaypointWait(FieldCommandAction action) {
        if (action != FieldCommandAction.PATROL_MARKER
                && action != FieldCommandAction.SET_PATROL_WAYPOINT_WAIT) {
            return 0;
        }
        String value = patrolWaypointWaitDraft.trim();
        if (value.isEmpty()) {
            return 0;
        }
        try {
            int waitTicks = Integer.parseInt(value);
            if (waitTicks < 0 || waitTicks > ArmyPatrolPlan.MAX_FIELD_COMMAND_WAIT_TICKS) {
                feedback = Component.translatable("screen.galacticwars.field_command.reason.invalid_patrol_wait");
                return null;
            }
            return waitTicks;
        } catch (NumberFormatException ignored) {
            feedback = Component.translatable("screen.galacticwars.field_command.reason.invalid_patrol_wait");
            return null;
        }
    }

    private void requestRefresh() {
        GalacticNetwork.CHANNEL.sendToServer(new FieldCommandRequestPayload(
                UUID.randomUUID(), FieldCommandAction.REFRESH, List.of()));
        feedback = Component.translatable("screen.galacticwars.field_command.request_sent");
    }

    private Component squadLabel(FieldCommandStatePayload.Squad squad) {
        String selected = selectedGroupIds.contains(squad.id()) ? "[x] " : "[ ] ";
        return Component.literal(selected + squad.name() + " (" + squad.unitCount() + ")");
    }

    private Component markerStatus(FieldCommandStatePayload state) {
        if (state.markedEntityAvailable()) {
            return Component.translatable("screen.galacticwars.field_command.marker.entity");
        }
        if (state.markedBlockAvailable()) {
            return Component.translatable("screen.galacticwars.field_command.marker.block");
        }
        return Component.translatable("screen.galacticwars.field_command.marker.none");
    }

    private enum Category {
        MOVEMENT("screen.galacticwars.field_command.category.movement", List.of(
                new ActionButton(FieldCommandAction.FOLLOW, "screen.galacticwars.field_command.follow"),
                new ActionButton(FieldCommandAction.HOLD, "screen.galacticwars.field_command.hold"),
                new ActionButton(FieldCommandAction.MOVE_TO_MARKER,
                        "screen.galacticwars.field_command.move_marker", true, false),
                new ActionButton(FieldCommandAction.FACE_FORWARD, "screen.galacticwars.field_command.face"),
                new ActionButton(FieldCommandAction.ADVANCE, "screen.galacticwars.field_command.advance"),
                new ActionButton(FieldCommandAction.RETREAT, "screen.galacticwars.field_command.retreat"),
                new ActionButton(FieldCommandAction.RETURN_TO_RALLY,
                        "screen.galacticwars.field_command.return_rally"))),
        COMBAT("screen.galacticwars.field_command.category.combat", List.of(
                new ActionButton(FieldCommandAction.PROTECT_OWNER, "screen.galacticwars.field_command.protect"),
                new ActionButton(FieldCommandAction.PROTECT_MARKED_ENTITY,
                        "screen.galacticwars.field_command.protect_marker", false, true),
                new ActionButton(FieldCommandAction.ATTACK_MARKED_TARGET,
                        "screen.galacticwars.field_command.attack_marker", false, true),
                new ActionButton(FieldCommandAction.CLEAR_TARGET, "screen.galacticwars.field_command.clear"),
                new ActionButton(FieldCommandAction.CYCLE_ENGAGEMENT,
                        "screen.galacticwars.field_command.cycle_engagement"),
                new ActionButton(FieldCommandAction.CYCLE_TARGET_PRIORITY,
                        "screen.galacticwars.field_command.cycle_priority"),
                new ActionButton(FieldCommandAction.CYCLE_RANGED_FIRE,
                        "screen.galacticwars.field_command.cycle_fire_policy"))),
        FORMATION("screen.galacticwars.field_command.category.formation", List.of(
                new ActionButton(FieldCommandAction.CYCLE_FORMATION,
                        "screen.galacticwars.field_command.cycle_formation"),
                new ActionButton(FieldCommandAction.TOGGLE_HOLD_FORMATION,
                        "screen.galacticwars.field_command.toggle_hold_formation"),
                new ActionButton(FieldCommandAction.TOGGLE_TIGHT_FORMATION,
                        "screen.galacticwars.field_command.toggle_tight_formation"))),
        PATROL("screen.galacticwars.field_command.category.patrol", List.of(
                new ActionButton(FieldCommandAction.PATROL_MARKER,
                        "screen.galacticwars.field_command.patrol_marker", true, false),
                new ActionButton(FieldCommandAction.PAUSE_PATROL,
                        "screen.galacticwars.field_command.pause_patrol"),
                new ActionButton(FieldCommandAction.RESUME_PATROL,
                        "screen.galacticwars.field_command.resume_patrol"),
                new ActionButton(FieldCommandAction.STOP_PATROL,
                        "screen.galacticwars.field_command.stop_patrol"),
                new ActionButton(FieldCommandAction.CYCLE_PATROL_MODE,
                        "screen.galacticwars.field_command.cycle_patrol_mode"),
                new ActionButton(FieldCommandAction.CYCLE_PATROL_SPEED,
                        "screen.galacticwars.field_command.cycle_patrol_speed"),
                new ActionButton(FieldCommandAction.CYCLE_PATROL_ENEMY_POLICY,
                        "screen.galacticwars.field_command.cycle_patrol_enemy_policy")));

        private final String translationKey;
        private final List<ActionButton> actions;

        Category(String translationKey, List<ActionButton> actions) {
            this.translationKey = translationKey;
            this.actions = actions;
        }
    }

    private record ActionButton(
            FieldCommandAction action,
            String translationKey,
            boolean requiresMarkedBlock,
            boolean requiresMarkedEntity
    ) {
        private ActionButton(FieldCommandAction action, String translationKey) {
            this(action, translationKey, false, false);
        }

        private boolean isAvailable(FieldCommandStatePayload state) {
            return (!requiresMarkedBlock || state.markedBlockAvailable())
                    && (!requiresMarkedEntity || state.markedEntityAvailable());
        }
    }
}
