package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.army.FieldCommandAction;
import galacticwars.clonewars.army.ArmyPatrolPlan;
import galacticwars.clonewars.client.ArmyFieldCommandKeyMappings;
import galacticwars.clonewars.client.FieldCommandClientState;
import galacticwars.clonewars.network.FieldCommandRequestPayload;
import galacticwars.clonewars.network.FieldCommandStatePayload;
import galacticwars.clonewars.network.GalacticNetwork;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lightweight, in-world squad command panel. It shows only a server-owned projection and never
 * sends target coordinates or entity identities from the client.
 */
public final class ArmyFieldCommandScreen extends Screen {
    private static final int MAX_SELECTED_SQUADS = FieldCommandRequestPayload.MAX_GROUPS;
    private static final int MAX_SQUADS_PER_PAGE = 8;
    private static final int MAX_PANEL_WIDTH = 172;
    private static final int PANEL_GAP = 16;
    private static final int BUTTON_HEIGHT = 18;
    private static final int GAP = 3;
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
        int panelWidth = panelWidth();
        int left = contentLeft(panelWidth);
        int right = left + panelWidth + PANEL_GAP;
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
        if (this.height >= 270) {
            graphics.text(this.font, Component.translatable("screen.galacticwars.field_command.key_hint"),
                    left, this.height - 42, MUTED_COLOR);
            graphics.text(this.font, Component.translatable("screen.galacticwars.field_command.marker_hint"),
                    left, this.height - 30, MUTED_COLOR);
            graphics.text(this.font, feedback, left, this.height - 18, FEEDBACK_COLOR);
        } else {
            graphics.text(this.font, feedback, left, this.height - 12, FEEDBACK_COLOR);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (patrolTextInputFocused()) {
            return super.keyPressed(event);
        }
        if (ArmyFieldCommandKeyMappings.matchesCommandScreen(event)) {
            this.onClose();
            return true;
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_1 -> selectCategory(0);
            case GLFW.GLFW_KEY_2 -> selectCategory(1);
            case GLFW.GLFW_KEY_3 -> selectCategory(2);
            case GLFW.GLFW_KEY_4 -> selectCategory(3);
            case GLFW.GLFW_KEY_A -> selectAllSquads();
            case GLFW.GLFW_KEY_C -> clearSquadSelection();
            case GLFW.GLFW_KEY_R -> requestRefresh();
            case GLFW.GLFW_KEY_Q -> changeGroupPage(-1);
            case GLFW.GLFW_KEY_E -> changeGroupPage(1);
            default -> {
                return super.keyPressed(event);
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() {
        return Math.max(82, Math.min(MAX_PANEL_WIDTH, (this.width - PANEL_GAP - 16) / 2));
    }

    private int contentLeft(int panelWidth) {
        return Math.max(8, (this.width - (panelWidth * 2 + PANEL_GAP)) / 2);
    }

    private int squadsPerPage() {
        int footerSpace = this.height >= 270 ? 58 : 34;
        int availableHeight = Math.max(BUTTON_HEIGHT,
                this.height - 79 - footerSpace - BUTTON_HEIGHT - GAP);
        return Math.max(1, Math.min(MAX_SQUADS_PER_PAGE,
                availableHeight / (BUTTON_HEIGHT + GAP)));
    }

    private boolean patrolTextInputFocused() {
        return category == Category.PATROL
                && ((patrolRouteNameInput != null && patrolRouteNameInput.isFocused())
                || (patrolWaypointWaitInput != null && patrolWaypointWaitInput.isFocused()));
    }

    private void selectCategory(int index) {
        Category[] categories = Category.values();
        if (index >= 0 && index < categories.length) {
            category = categories[index];
            this.rebuildWidgets();
        }
    }

    private void changeGroupPage(int direction) {
        int squadCount = FieldCommandClientState.snapshot().squads().size();
        int pageCount = Math.max(1, (squadCount + squadsPerPage() - 1) / squadsPerPage());
        groupPage = Math.max(0, Math.min(groupPage + direction, pageCount - 1));
        this.rebuildWidgets();
    }

    private void selectAllSquads() {
        selectedGroupIds.clear();
        FieldCommandClientState.snapshot().squads().stream()
                .limit(MAX_SELECTED_SQUADS)
                .map(FieldCommandStatePayload.Squad::id)
                .forEach(selectedGroupIds::add);
        if (FieldCommandClientState.snapshot().squads().size() > MAX_SELECTED_SQUADS) {
            feedback = Component.translatable(
                    "screen.galacticwars.field_command.selection_limit", MAX_SELECTED_SQUADS);
        }
        this.rebuildWidgets();
    }

    private void clearSquadSelection() {
        selectedGroupIds.clear();
        this.rebuildWidgets();
    }

    private void buildWidgets() {
        FieldCommandStatePayload state = FieldCommandClientState.snapshot();
        int panelWidth = panelWidth();
        int compactWidth = (panelWidth - GAP) / 2;
        int left = contentLeft(panelWidth);
        int right = left + panelWidth + PANEL_GAP;
        int tabWidth = Math.max(48, Math.min(74,
                (this.width - 16 - GAP * (Category.values().length - 1)) / Category.values().length));
        int tabsLeft = Math.max(8,
                (this.width - (tabWidth * Category.values().length + GAP * 3)) / 2);
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
        Button selectAll = Button.builder(
                        Component.translatable("screen.galacticwars.field_command.select_all"),
                        ignored -> selectAllSquads())
                .bounds(left, 58, compactWidth, BUTTON_HEIGHT)
                .build();
        selectAll.active = !squads.isEmpty() && selectedGroupIds.size() < Math.min(squads.size(), MAX_SELECTED_SQUADS);
        this.addRenderableWidget(selectAll);
        Button clear = Button.builder(
                        Component.translatable("screen.galacticwars.field_command.clear_selection"),
                        ignored -> clearSquadSelection())
                .bounds(left + compactWidth + GAP, 58, compactWidth, BUTTON_HEIGHT)
                .build();
        clear.active = !selectedGroupIds.isEmpty();
        this.addRenderableWidget(clear);

        int squadsPerPage = squadsPerPage();
        int pageCount = Math.max(1, (squads.size() + squadsPerPage - 1) / squadsPerPage);
        groupPage = Math.max(0, Math.min(groupPage, pageCount - 1));
        int first = groupPage * squadsPerPage;
        int last = Math.min(squads.size(), first + squadsPerPage);
        int squadListY = 79;
        if (squads.isEmpty()) {
            this.addRenderableWidget(Button.builder(
                            Component.translatable("screen.galacticwars.field_command.no_squads"),
                            ignored -> requestRefresh())
                    .bounds(left, squadListY, panelWidth, BUTTON_HEIGHT)
                    .build());
        } else {
            for (int index = first; index < last; index++) {
                FieldCommandStatePayload.Squad squad = squads.get(index);
                int y = squadListY + (index - first) * (BUTTON_HEIGHT + GAP);
                this.addRenderableWidget(Button.builder(squadLabel(squad),
                                ignored -> toggleSquad(squad.id()))
                        .bounds(left, y, panelWidth, BUTTON_HEIGHT)
                        .build());
            }
        }
        if (pageCount > 1) {
            Button previous = Button.builder(Component.translatable("screen.galacticwars.field_command.previous"),
                            ignored -> {
                                groupPage--;
                                this.rebuildWidgets();
                            })
                    .bounds(left, squadListY + squadsPerPage * (BUTTON_HEIGHT + GAP), compactWidth, BUTTON_HEIGHT)
                    .build();
            previous.active = groupPage > 0;
            this.addRenderableWidget(previous);
            Button next = Button.builder(Component.translatable("screen.galacticwars.field_command.next"),
                            ignored -> {
                                groupPage++;
                                this.rebuildWidgets();
                            })
                    .bounds(left + compactWidth + GAP,
                            squadListY + squadsPerPage * (BUTTON_HEIGHT + GAP), compactWidth, BUTTON_HEIGHT)
                    .build();
            next.active = groupPage + 1 < pageCount;
            this.addRenderableWidget(next);
        }

        if (category == Category.PATROL) {
            buildPatrolWidgets(right, state, panelWidth, compactWidth);
            return;
        }

        int actionIndex = 0;
        for (ActionButton action : category.actions) {
            int x = right + (actionIndex % 2) * (compactWidth + GAP);
            int y = 58 + (actionIndex / 2) * (BUTTON_HEIGHT + GAP);
            Button button = Button.builder(actionLabel(action, state),
                            ignored -> sendAction(action.action, action.optionId))
                    .bounds(x, y, compactWidth, BUTTON_HEIGHT)
                    .build();
            button.active = !selectedGroupIds.isEmpty()
                    && action.isAvailable(state);
            String missingRequirement = action.missingRequirement(state);
            if (missingRequirement != null) {
                button.setTooltip(Tooltip.create(Component.translatable(
                        "screen.galacticwars.field_command.unlock_requirement." + missingRequirement)));
            }
            this.addRenderableWidget(button);
            actionIndex++;
        }
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.galacticwars.field_command.refresh"),
                        ignored -> requestRefresh())
                .bounds(right + (actionIndex % 2) * (compactWidth + GAP),
                        58 + (actionIndex / 2) * (BUTTON_HEIGHT + GAP), compactWidth, BUTTON_HEIGHT)
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
    private void buildPatrolWidgets(
            int right,
            FieldCommandStatePayload state,
            int panelWidth,
            int compactWidth
    ) {
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
                panelWidth,
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
                panelWidth,
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
                "screen.galacticwars.field_command.create_patrol_route", compactWidth,
                selected && state.markedBlockAvailable());
        addPatrolButton(right + compactWidth + GAP, actionY, FieldCommandAction.RENAME_PATROL_ROUTE,
                "screen.galacticwars.field_command.rename_patrol_short", compactWidth, hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addLocalPatrolButton(right, actionY, "screen.galacticwars.field_command.patrol_previous_waypoint",
                ignored -> changePatrolWaypoint(-1, state), compactWidth, hasRoute);
        addLocalPatrolButton(right + compactWidth + GAP, actionY,
                "screen.galacticwars.field_command.patrol_next_waypoint",
                ignored -> changePatrolWaypoint(1, state), compactWidth, hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.SET_PATROL_WAYPOINT_WAIT,
                "screen.galacticwars.field_command.set_patrol_wait_short", compactWidth, hasRoute);
        addPatrolButton(right + compactWidth + GAP, actionY, FieldCommandAction.PAUSE_PATROL,
                "screen.galacticwars.field_command.pause_patrol_short", compactWidth, hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.RESUME_PATROL,
                "screen.galacticwars.field_command.resume_patrol_short", compactWidth, hasRoute);
        addPatrolButton(right + compactWidth + GAP, actionY, FieldCommandAction.STOP_PATROL,
                "screen.galacticwars.field_command.stop_patrol_short", compactWidth, hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.CYCLE_PATROL_MODE,
                "screen.galacticwars.field_command.patrol_mode_short", compactWidth, hasRoute);
        addPatrolButton(right + compactWidth + GAP, actionY, FieldCommandAction.CYCLE_PATROL_SPEED,
                "screen.galacticwars.field_command.patrol_speed_short", compactWidth, hasRoute);
        actionY += BUTTON_HEIGHT + GAP;
        addPatrolButton(right, actionY, FieldCommandAction.CYCLE_PATROL_ENEMY_POLICY,
                "screen.galacticwars.field_command.patrol_enemy_policy_short", compactWidth, hasRoute);
        addLocalPatrolButton(right + compactWidth + GAP, actionY,
                "screen.galacticwars.field_command.refresh", ignored -> requestRefresh(), compactWidth, true);
    }

    private void addPatrolButton(
            int x,
            int y,
            FieldCommandAction action,
            String translationKey,
            int width,
            boolean active
    ) {
        Button button = Button.builder(Component.translatable(translationKey), ignored -> sendAction(action))
                .bounds(x, y, width, BUTTON_HEIGHT)
                .build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private void addLocalPatrolButton(
            int x,
            int y,
            String translationKey,
            Button.OnPress onPress,
            int width,
            boolean active
    ) {
        Button button = Button.builder(Component.translatable(translationKey), onPress)
                .bounds(x, y, width, BUTTON_HEIGHT)
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
        sendAction(action, "");
    }

    private void sendAction(FieldCommandAction action, String optionId) {
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
                    waitTicks,
                    optionId));
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

    private Component actionLabel(ActionButton action, FieldCommandStatePayload state) {
        return switch (action.action()) {
            case CYCLE_FORMATION -> Component.translatable(
                    "screen.galacticwars.field_command.state.formation",
                    selectedStateValue(state, FieldCommandStatePayload.Squad::formation));
            case CYCLE_ENGAGEMENT -> Component.translatable(
                    "screen.galacticwars.field_command.state.engagement",
                    selectedStateValue(state, FieldCommandStatePayload.Squad::engagement));
            case CYCLE_TARGET_PRIORITY -> Component.translatable(
                    "screen.galacticwars.field_command.state.priority",
                    selectedStateValue(state, FieldCommandStatePayload.Squad::targetPriority));
            case CYCLE_RANGED_FIRE -> Component.translatable(
                    "screen.galacticwars.field_command.state.fire",
                    selectedStateValue(state, FieldCommandStatePayload.Squad::rangedFirePolicy));
            case TOGGLE_HOLD_FORMATION -> Component.translatable(
                    "screen.galacticwars.field_command.state.hold_formation",
                    selectedToggleValue(state, FieldCommandStatePayload.Squad::holdFormation));
            case TOGGLE_TIGHT_FORMATION -> Component.translatable(
                    "screen.galacticwars.field_command.state.tight_formation",
                    selectedToggleValue(state, FieldCommandStatePayload.Squad::tightFormation));
            default -> Component.translatable(action.translationKey());
        };
    }

    private Component selectedStateValue(
            FieldCommandStatePayload state,
            Function<FieldCommandStatePayload.Squad, String> value
    ) {
        List<String> values = selectedSquads(state).stream().map(value).distinct().toList();
        if (values.isEmpty()) {
            return Component.translatable("screen.galacticwars.field_command.state.none");
        }
        if (values.size() > 1) {
            return Component.translatable("screen.galacticwars.field_command.state.mixed");
        }
        return Component.literal(humanize(values.getFirst()));
    }

    private Component selectedToggleValue(
            FieldCommandStatePayload state,
            Function<FieldCommandStatePayload.Squad, Boolean> value
    ) {
        List<Boolean> values = selectedSquads(state).stream().map(value).distinct().toList();
        if (values.isEmpty()) {
            return Component.translatable("screen.galacticwars.field_command.state.none");
        }
        if (values.size() > 1) {
            return Component.translatable("screen.galacticwars.field_command.state.mixed");
        }
        return Component.translatable(values.getFirst()
                ? "screen.galacticwars.field_command.state.on"
                : "screen.galacticwars.field_command.state.off");
    }

    private List<FieldCommandStatePayload.Squad> selectedSquads(FieldCommandStatePayload state) {
        return state.squads().stream().filter(squad -> selectedGroupIds.contains(squad.id())).toList();
    }

    private static String humanize(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized.isEmpty()
                ? normalized
                : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private Component squadLabel(FieldCommandStatePayload.Squad squad) {
        String selected = selectedGroupIds.contains(squad.id()) ? "[x] " : "[ ] ";
        return Component.literal(selected + squad.name() + " (" + squad.unitCount() + ") "
                + humanize(squad.marchPhase()) + " " + squad.cohesionPercent() + "%");
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
                new ActionButton(FieldCommandAction.SET_ENGAGEMENT,
                        "screen.galacticwars.field_command.engagement.passive", "passive"),
                new ActionButton(FieldCommandAction.SET_ENGAGEMENT,
                        "screen.galacticwars.field_command.engagement.defensive", "defensive"),
                new ActionButton(FieldCommandAction.SET_ENGAGEMENT,
                        "screen.galacticwars.field_command.engagement.aggressive", "aggressive"),
                new ActionButton(FieldCommandAction.SET_TARGET_PRIORITY,
                        "screen.galacticwars.field_command.priority.command", "command_target"),
                new ActionButton(FieldCommandAction.SET_TARGET_PRIORITY,
                        "screen.galacticwars.field_command.priority.owner", "owner_threat"),
                new ActionButton(FieldCommandAction.SET_TARGET_PRIORITY,
                        "screen.galacticwars.field_command.priority.nearest", "nearest_hostile"),
                new ActionButton(FieldCommandAction.SET_TARGET_PRIORITY,
                        "screen.galacticwars.field_command.priority.health", "lowest_health"),
                new ActionButton(FieldCommandAction.SET_RANGED_FIRE,
                        "screen.galacticwars.field_command.fire.hold", "hold_fire"),
                new ActionButton(FieldCommandAction.SET_RANGED_FIRE,
                        "screen.galacticwars.field_command.fire.return", "return_fire"),
                new ActionButton(FieldCommandAction.SET_RANGED_FIRE,
                        "screen.galacticwars.field_command.fire.free", "free_fire"),
                new ActionButton(FieldCommandAction.SET_RANGED_FIRE,
                        "screen.galacticwars.field_command.fire.focus", "focus_command_target"))),
        FORMATION("screen.galacticwars.field_command.category.formation", List.of(
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.line", "line"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.column", "column"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.wedge", "wedge"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.square", "square"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.circle", "circle"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.hollow_circle", "hollow_circle"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.hollow_square", "hollow_square"),
                new ActionButton(FieldCommandAction.SET_FORMATION,
                        "screen.galacticwars.field_command.formation.movement", "movement"),
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
            String optionId,
            boolean requiresMarkedBlock,
            boolean requiresMarkedEntity
    ) {
        private ActionButton(FieldCommandAction action, String translationKey) {
            this(action, translationKey, "", false, false);
        }

        private ActionButton(FieldCommandAction action, String translationKey, String optionId) {
            this(action, translationKey, optionId, false, false);
        }

        private ActionButton(
                FieldCommandAction action,
                String translationKey,
                boolean requiresMarkedBlock,
                boolean requiresMarkedEntity
        ) {
            this(action, translationKey, "", requiresMarkedBlock, requiresMarkedEntity);
        }

        private boolean isAvailable(FieldCommandStatePayload state) {
            return (!requiresMarkedBlock || state.markedBlockAvailable())
                    && (!requiresMarkedEntity || state.markedEntityAvailable())
                    && missingRequirement(state) == null;
        }

        private String missingRequirement(FieldCommandStatePayload state) {
            String requirement = switch (action) {
                case TOGGLE_HOLD_FORMATION -> "formation_advanced";
                case SET_FORMATION -> switch (optionId) {
                    case "wedge", "square", "circle", "hollow_circle", "hollow_square" ->
                            "formation_advanced";
                    default -> "";
                };
                default -> "";
            };
            return requirement.isEmpty() || state.unlocks().contains(requirement) ? null : requirement;
        }
    }
}
