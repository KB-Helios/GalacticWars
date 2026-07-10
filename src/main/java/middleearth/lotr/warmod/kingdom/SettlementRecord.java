package middleearth.lotr.warmod.kingdom;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import middleearth.lotr.warmod.settlement.KingdomBaseBlueprint;

public record SettlementRecord(
        UUID id,
        String dimensionId,
        int hallX,
        int hallY,
        int hallZ,
        int claimRadius,
        int housingCapacity,
        List<UUID> recruitIds,
        Optional<UUID> commanderId,
        CommanderPolicy commanderPolicy,
        List<WorksiteRecord> worksites,
        List<BuildProject> buildProjects,
        List<WorkOrder> workOrders,
        List<RecruitmentCampaign> recruitmentCampaigns,
        int revision
) {
    public SettlementRecord {
        Objects.requireNonNull(id, "id");
        dimensionId = KingdomNormalizers.normalize(dimensionId, "dimensionId");
        if (claimRadius < 8 || claimRadius > 256) {
            throw new IllegalArgumentException("claimRadius must be between 8 and 256");
        }
        if (housingCapacity < 0) {
            throw new IllegalArgumentException("housingCapacity cannot be negative");
        }
        Objects.requireNonNull(recruitIds, "recruitIds");
        recruitIds = List.copyOf(new LinkedHashSet<>(recruitIds));
        commanderId = commanderId == null ? Optional.empty() : commanderId;
        Objects.requireNonNull(commanderPolicy, "commanderPolicy");
        worksites = List.copyOf(Objects.requireNonNull(worksites, "worksites"));
        buildProjects = List.copyOf(Objects.requireNonNull(buildProjects, "buildProjects"));
        workOrders = List.copyOf(Objects.requireNonNull(workOrders, "workOrders"));
        recruitmentCampaigns = List.copyOf(Objects.requireNonNull(recruitmentCampaigns, "recruitmentCampaigns"));
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
    }

    public static SettlementRecord create(String dimensionId, int x, int y, int z) {
        return new SettlementRecord(UUID.randomUUID(), dimensionId, x, y, z, 48, 4,
                List.of(), Optional.empty(), CommanderPolicy.defaults(), List.of(), List.of(), List.of(), List.of(), 0);
    }

    public boolean hasHousingSpace() {
        return recruitIds.size() < housingCapacity;
    }

    public boolean containsRecruit(UUID recruitId) {
        return recruitIds.contains(recruitId);
    }

    public boolean hasActiveCampaign() {
        return recruitmentCampaigns.stream().anyMatch(RecruitmentCampaign::active);
    }

    public boolean hasCommanderSlot() {
        return buildProjects.stream().anyMatch(project -> project.blueprintId().equals(KingdomBaseBlueprint.STARTER_KEEP_ID));
    }

    public boolean containsCompletedProject(BuildProject project) {
        return buildProjects.stream().anyMatch(existing -> existing.blueprintId().equals(project.blueprintId())
                && existing.dimensionId().equals(project.dimensionId())
                && existing.originX() == project.originX()
                && existing.originY() == project.originY()
                && existing.originZ() == project.originZ());
    }

    public SettlementRecord withRecruit(UUID recruitId) {
        if (containsRecruit(recruitId)) {
            return this;
        }
        LinkedHashSet<UUID> updated = new LinkedHashSet<>(recruitIds);
        updated.add(Objects.requireNonNull(recruitId, "recruitId"));
        return copy(List.copyOf(updated), commanderId, commanderPolicy, recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withoutRecruit(UUID recruitId) {
        Objects.requireNonNull(recruitId, "recruitId");
        if (!containsRecruit(recruitId)) {
            return this;
        }
        LinkedHashSet<UUID> updated = new LinkedHashSet<>(recruitIds);
        updated.remove(recruitId);
        Optional<UUID> updatedCommander = commanderId.filter(id -> !id.equals(recruitId));
        return copy(List.copyOf(updated), updatedCommander, commanderPolicy, recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withHallLocation(String dimensionId, int x, int y, int z) {
        return new SettlementRecord(id, dimensionId, x, y, z, claimRadius, housingCapacity,
                recruitIds, commanderId, commanderPolicy, worksites, buildProjects, workOrders,
                recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withCommander(UUID recruitId) {
        if (!containsRecruit(recruitId)) {
            throw new IllegalArgumentException("commander must be a settlement recruit");
        }
        return copy(recruitIds, Optional.of(recruitId), commanderPolicy, recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withoutCommander(UUID recruitId) {
        if (commanderId.filter(recruitId::equals).isEmpty()) {
            return this;
        }
        return copy(recruitIds, Optional.empty(), commanderPolicy, recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withCommanderPolicy(CommanderPolicy policy) {
        return copy(recruitIds, commanderId, policy, recruitmentCampaigns, revision + 1);
    }

    public SettlementRecord withCampaign(RecruitmentCampaign campaign) {
        if (hasActiveCampaign()) {
            throw new IllegalStateException("settlement already has an active recruitment campaign");
        }
        java.util.ArrayList<RecruitmentCampaign> updated = new java.util.ArrayList<>(recruitmentCampaigns);
        updated.add(campaign);
        return copy(recruitIds, commanderId, commanderPolicy, List.copyOf(updated), revision + 1);
    }

    public SettlementRecord replaceCampaign(RecruitmentCampaign campaign) {
        java.util.ArrayList<RecruitmentCampaign> updated = new java.util.ArrayList<>(recruitmentCampaigns);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).id().equals(campaign.id())) {
                updated.set(i, campaign);
                return copy(recruitIds, commanderId, commanderPolicy, List.copyOf(updated), revision + 1);
            }
        }
        return this;
    }

    public SettlementRecord withCompletedProject(
            BuildProject project,
            int housingReward,
            String worksiteType,
            int worksiteCapacity
    ) {
        Objects.requireNonNull(project, "project");
        if (containsCompletedProject(project)) {
            return this;
        }
        java.util.ArrayList<BuildProject> projects = new java.util.ArrayList<>(buildProjects);
        projects.add(project);
        java.util.ArrayList<WorksiteRecord> updatedWorksites = new java.util.ArrayList<>(worksites);
        if (worksiteType != null && !worksiteType.isBlank() && worksiteCapacity > 0) {
            updatedWorksites.add(new WorksiteRecord(
                    UUID.randomUUID(),
                    worksiteType,
                    project.dimensionId(),
                    project.originX(),
                    project.originY(),
                    project.originZ(),
                    8,
                    worksiteCapacity));
        }
        return new SettlementRecord(id, dimensionId, hallX, hallY, hallZ, claimRadius,
                Math.addExact(housingCapacity, Math.max(0, housingReward)), recruitIds, commanderId,
                commanderPolicy, List.copyOf(updatedWorksites), List.copyOf(projects), workOrders,
                recruitmentCampaigns, revision + 1);
    }

    private SettlementRecord copy(
            List<UUID> recruits,
            Optional<UUID> commander,
            CommanderPolicy policy,
            List<RecruitmentCampaign> campaigns,
            int nextRevision
    ) {
        return new SettlementRecord(id, dimensionId, hallX, hallY, hallZ, claimRadius, housingCapacity,
                recruits, commander, policy, worksites, buildProjects, workOrders, campaigns, nextRevision);
    }
}
