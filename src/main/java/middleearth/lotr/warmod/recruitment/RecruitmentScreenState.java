package middleearth.lotr.warmod.recruitment;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import middleearth.lotr.warmod.army.ArmyUnitDefinition;
import middleearth.lotr.warmod.army.HiringDecision;
import middleearth.lotr.warmod.army.RecruitState;
import middleearth.lotr.warmod.faction.FactionDefinition;

public record RecruitmentScreenState(
        RecruitmentScreenMode mode,
        String title,
        String factionName,
        int hireCost,
        int ownedRecruitCount,
        int dailyUpkeep,
        String workerProfessionId,
        String reasonCode,
        RecruitmentAction primaryAction,
        List<RecruitmentAction> commandActions,
        List<String> statusLines
) {
    private static final List<RecruitmentAction> OWNED_COMMANDS = List.of(
            RecruitmentAction.FOLLOW_OWNER,
            RecruitmentAction.HOLD_POSITION,
            RecruitmentAction.MOVE_TO_POSITION,
            RecruitmentAction.PROTECT_OWNER,
            RecruitmentAction.ATTACK_TARGET,
            RecruitmentAction.CLEAR_TARGET);

    public RecruitmentScreenState {
        Objects.requireNonNull(mode, "mode");
        title = requireNonBlank(title, "title");
        factionName = factionName == null ? "" : factionName.trim();
        if (hireCost < 0) {
            throw new IllegalArgumentException("hireCost cannot be negative");
        }
        if (ownedRecruitCount < 0) {
            throw new IllegalArgumentException("ownedRecruitCount cannot be negative");
        }
        if (dailyUpkeep < 0) {
            throw new IllegalArgumentException("dailyUpkeep cannot be negative");
        }
        workerProfessionId = workerProfessionId == null ? "" : workerProfessionId.trim();
        reasonCode = requireNonBlank(reasonCode, "reasonCode");
        Objects.requireNonNull(primaryAction, "primaryAction");
        commandActions = List.copyOf(Objects.requireNonNull(commandActions, "commandActions"));
        statusLines = List.copyOf(Objects.requireNonNull(statusLines, "statusLines"));
    }

    public RecruitmentScreenState(
            RecruitmentScreenMode mode,
            String title,
            String factionName,
            int hireCost,
            int ownedRecruitCount,
            String reasonCode,
            RecruitmentAction primaryAction,
            List<RecruitmentAction> commandActions
    ) {
        this(mode, title, factionName, hireCost, ownedRecruitCount, 0, "", reasonCode, primaryAction, commandActions, List.of());
    }

    public static RecruitmentScreenState wildOffer(
            ArmyUnitDefinition unit,
            FactionDefinition faction,
            HiringDecision decision,
            int ownedRecruitCount
    ) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(faction, "faction");
        Objects.requireNonNull(decision, "decision");
        return new RecruitmentScreenState(
                RecruitmentScreenMode.HIRE_OFFER,
                "Hire " + unit.displayName(),
                faction.displayName(),
                decision.cost(),
                ownedRecruitCount,
                0,
                "",
                decision.reasonCode(),
                decision.accepted() ? RecruitmentAction.ACCEPT_HIRE : RecruitmentAction.NONE,
                List.of(),
                List.of("Owned: " + ownedRecruitCount + "/" + faction.maxOwnedRecruits()));
    }

    public static RecruitmentScreenState contractOffer(RecruitmentContractOffer offer, int ownedRecruitCount) {
        Objects.requireNonNull(offer, "offer");
        return new RecruitmentScreenState(
                RecruitmentScreenMode.HIRE_OFFER,
                offer.summaryTitle(),
                offer.faction().displayName(),
                offer.hireCost(),
                ownedRecruitCount,
                offer.dailyUpkeep(),
                offer.workerProfessionId(),
                offer.reasonCode(),
                offer.accepted() ? RecruitmentAction.ACCEPT_HIRE : RecruitmentAction.NONE,
                List.of(),
                contractStatusLines(offer, ownedRecruitCount));
    }

    public static RecruitmentScreenState ownedCommandPanel(RecruitState recruitState, ArmyUnitDefinition unit) {
        Objects.requireNonNull(recruitState, "recruitState");
        Objects.requireNonNull(unit, "unit");
        return new RecruitmentScreenState(
                RecruitmentScreenMode.COMMAND_PANEL,
                unit.displayName(),
                "",
                0,
                0,
                0,
                "",
                "owned_by_player",
                RecruitmentAction.NONE,
                OWNED_COMMANDS,
                List.of("Status: Recruited"));
    }

    public static RecruitmentScreenState lockedByOtherOwner(ArmyUnitDefinition unit, UUID ownerId) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(ownerId, "ownerId");
        return new RecruitmentScreenState(
                RecruitmentScreenMode.LOCKED,
                unit.displayName(),
                "",
                0,
                0,
                0,
                "",
                "owned_by_other_player",
                RecruitmentAction.NONE,
                List.of(),
                List.of("Status: Locked"));
    }

    private static List<String> contractStatusLines(RecruitmentContractOffer offer, int ownedRecruitCount) {
        if (!offer.accepted()) {
            return List.of("Unavailable: " + offer.reasonCode());
        }
        if (offer.workerProfession() == null) {
            return List.of("Owned: " + ownedRecruitCount);
        }
        return List.of(
                "Upkeep: " + offer.dailyUpkeep() + " emeralds/day",
                "Worker: " + offer.workerProfessionName(),
                "Owned: " + ownedRecruitCount);
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
