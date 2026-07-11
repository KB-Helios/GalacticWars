package galacticwars.clonewars.kingdom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import galacticwars.clonewars.recruitment.NpcServiceBranch;

public record KingdomRecord(
        UUID id,
        UUID ownerId,
        String factionId,
        SettlementRecord settlement,
        List<KingdomMember> members,
        List<SettlementRecord> outposts,
        List<KingdomClaim> claims,
        List<KingdomNpcRecord> npcRoster
) {
    public KingdomRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(factionId, "factionId");
        factionId = factionId.trim().toLowerCase(Locale.ROOT);
        if (factionId.isEmpty()) {
            throw new IllegalArgumentException("factionId cannot be blank");
        }
        Objects.requireNonNull(settlement, "settlement");
        LinkedHashMap<UUID, KingdomMember> normalizedMembers = new LinkedHashMap<>();
        Objects.requireNonNull(members, "members").forEach(member ->
                normalizedMembers.putIfAbsent(member.playerId(), member));
        normalizedMembers.put(ownerId, new KingdomMember(ownerId, KingdomMemberRole.OWNER));
        members = List.copyOf(normalizedMembers.values());
        outposts = List.copyOf(Objects.requireNonNull(outposts, "outposts"));
        if (outposts.stream().anyMatch(candidate -> candidate.id().equals(settlement.id()))) {
            throw new IllegalArgumentException("capital cannot also be an outpost");
        }
        claims = List.copyOf(Objects.requireNonNull(claims, "claims"));
        if (claims.isEmpty()) {
            claims = List.of(KingdomClaim.capital(id, settlement));
        }
        if (claims.stream().anyMatch(claim -> !claim.kingdomId().equals(id))) {
            throw new IllegalArgumentException("claim belongs to another kingdom");
        }
        List<SettlementRecord> allSettlements = settlementsForMigration(settlement, outposts);
        java.util.Set<UUID> allRecruitIds = allSettlements.stream()
                .flatMap(candidate -> candidate.recruitIds().stream())
                .collect(java.util.stream.Collectors.toSet());
        LinkedHashMap<UUID, KingdomNpcRecord> normalizedRoster = new LinkedHashMap<>();
        Objects.requireNonNull(npcRoster, "npcRoster").stream()
                .filter(entry -> allRecruitIds.contains(entry.recruitId()))
                .forEach(entry -> normalizedRoster.putIfAbsent(entry.recruitId(), entry));
        for (SettlementRecord candidate : allSettlements) {
            java.util.Set<UUID> civilianIds = candidate.worksites().stream()
                    .flatMap(worksite -> worksite.assignmentIds().stream())
                    .collect(java.util.stream.Collectors.toSet());
            candidate.recruitIds().forEach(recruitId -> normalizedRoster.putIfAbsent(recruitId,
                    new KingdomNpcRecord(recruitId, candidate.id(), civilianIds.contains(recruitId)
                            ? NpcServiceBranch.CIVILIAN : NpcServiceBranch.MILITARY)));
        }
        npcRoster = List.copyOf(normalizedRoster.values());
    }

    public KingdomRecord(
            UUID id,
            UUID ownerId,
            String factionId,
            SettlementRecord settlement,
            List<KingdomMember> members,
            List<SettlementRecord> outposts,
            List<KingdomClaim> claims
    ) {
        this(id, ownerId, factionId, settlement, members, outposts, claims, List.of());
    }

    public KingdomRecord(UUID id, UUID ownerId, String factionId, SettlementRecord settlement) {
        this(id, ownerId, factionId, settlement,
                List.of(new KingdomMember(ownerId, KingdomMemberRole.OWNER)), List.of(), List.of(), List.of());
    }

    public KingdomRecord withSettlement(SettlementRecord settlement) {
        return new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, claims, npcRoster);
    }

    public KingdomRecord withFaction(String factionId) {
        return new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, claims, npcRoster);
    }

    public List<SettlementRecord> settlements() {
        ArrayList<SettlementRecord> all = new ArrayList<>(outposts.size() + 1);
        all.add(settlement);
        all.addAll(outposts);
        return List.copyOf(all);
    }

    private static List<SettlementRecord> settlementsForMigration(
            SettlementRecord capital,
            List<SettlementRecord> outposts
    ) {
        ArrayList<SettlementRecord> all = new ArrayList<>(outposts.size() + 1);
        all.add(capital);
        all.addAll(outposts);
        return all;
    }

    public Optional<KingdomNpcRecord> npc(UUID recruitId) {
        return npcRoster.stream().filter(entry -> entry.recruitId().equals(recruitId)).findFirst();
    }

    public KingdomRecord withNpcBranch(UUID recruitId, NpcServiceBranch branch) {
        ArrayList<KingdomNpcRecord> updated = new ArrayList<>(npcRoster);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).recruitId().equals(recruitId)) {
                updated.set(index, updated.get(index).withServiceBranch(branch));
                return new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, claims, updated);
            }
        }
        return this;
    }

    public Optional<KingdomMember> member(UUID playerId) {
        return members.stream().filter(member -> member.playerId().equals(playerId)).findFirst();
    }

    public boolean allows(UUID playerId, KingdomPermission permission) {
        return member(playerId)
                .map(KingdomMember::role)
                .map(role -> KingdomPermissionPolicy.allows(role, permission))
                .orElse(false);
    }

    public KingdomRecord withMember(UUID playerId, KingdomMemberRole role) {
        if (playerId.equals(ownerId) && role != KingdomMemberRole.OWNER) {
            throw new IllegalArgumentException("owner role cannot be changed");
        }
        ArrayList<KingdomMember> updated = new ArrayList<>(members);
        updated.removeIf(member -> member.playerId().equals(playerId));
        updated.add(new KingdomMember(playerId, role));
        return new KingdomRecord(id, ownerId, factionId, settlement, updated, outposts, claims, npcRoster);
    }

    public KingdomRecord withoutMember(UUID playerId) {
        if (playerId.equals(ownerId)) {
            throw new IllegalArgumentException("owner cannot leave without transferring ownership");
        }
        List<KingdomMember> updated = members.stream()
                .filter(member -> !member.playerId().equals(playerId)).toList();
        return updated.size() == members.size() ? this
                : new KingdomRecord(id, ownerId, factionId, settlement, updated, outposts, claims, npcRoster);
    }

    public KingdomRecord withOutpost(SettlementRecord outpost) {
        if (settlements().stream().anyMatch(existing -> existing.id().equals(outpost.id()))) {
            return this;
        }
        ArrayList<SettlementRecord> updated = new ArrayList<>(outposts);
        updated.add(outpost);
        return new KingdomRecord(id, ownerId, factionId, settlement, members, updated, claims, npcRoster);
    }

    public KingdomRecord replaceSettlement(SettlementRecord updatedSettlement) {
        if (settlement.id().equals(updatedSettlement.id())) {
            return withSettlement(updatedSettlement);
        }
        ArrayList<SettlementRecord> updated = new ArrayList<>(outposts);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(updatedSettlement.id())) {
                updated.set(index, updatedSettlement);
                return new KingdomRecord(id, ownerId, factionId, settlement, members, updated, claims, npcRoster);
            }
        }
        return this;
    }

    public KingdomRecord replaceClaim(KingdomClaim claim) {
        if (!claim.kingdomId().equals(id)) {
            throw new IllegalArgumentException("claim belongs to another kingdom");
        }
        ArrayList<KingdomClaim> updated = new ArrayList<>(claims);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).id().equals(claim.id())) {
                updated.set(index, claim);
                return new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, updated, npcRoster);
            }
        }
        updated.add(claim);
        return new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, updated, npcRoster);
    }

    public KingdomRecord withoutClaim(UUID claimId) {
        List<KingdomClaim> updated = claims.stream().filter(claim -> !claim.id().equals(claimId)).toList();
        return updated.size() == claims.size() ? this
                : new KingdomRecord(id, ownerId, factionId, settlement, members, outposts, updated, npcRoster);
    }
}
