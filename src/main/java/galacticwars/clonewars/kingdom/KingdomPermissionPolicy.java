package galacticwars.clonewars.kingdom;

import java.util.EnumSet;
import java.util.Set;

public final class KingdomPermissionPolicy {
    private static final Set<KingdomPermission> OWNER = EnumSet.allOf(KingdomPermission.class);
    private static final Set<KingdomPermission> OFFICER = EnumSet.of(
            KingdomPermission.MANAGE_MEMBERS,
            KingdomPermission.MANAGE_DIPLOMACY,
            KingdomPermission.MANAGE_CLAIMS,
            KingdomPermission.COMMAND_ARMY,
            KingdomPermission.BUILD,
            KingdomPermission.TRAVEL,
            KingdomPermission.USE_STORAGE,
            KingdomPermission.MANAGE_WORKSITES,
            KingdomPermission.RECRUIT);
    private static final Set<KingdomPermission> BUILDER = EnumSet.of(
            KingdomPermission.BUILD,
            KingdomPermission.TRAVEL,
            KingdomPermission.USE_STORAGE,
            KingdomPermission.MANAGE_WORKSITES);
    private static final Set<KingdomPermission> QUARTERMASTER = EnumSet.of(
            KingdomPermission.USE_STORAGE,
            KingdomPermission.TRAVEL,
            KingdomPermission.MANAGE_WORKSITES,
            KingdomPermission.RECRUIT);
    private static final Set<KingdomPermission> MEMBER = EnumSet.of(
            KingdomPermission.USE_STORAGE, KingdomPermission.TRAVEL);

    private KingdomPermissionPolicy() {
    }

    public static boolean allows(KingdomMemberRole role, KingdomPermission permission) {
        return permissions(role).contains(permission);
    }

    public static Set<KingdomPermission> permissions(KingdomMemberRole role) {
        return switch (role) {
            case OWNER -> Set.copyOf(OWNER);
            case OFFICER -> Set.copyOf(OFFICER);
            case BUILDER -> Set.copyOf(BUILDER);
            case QUARTERMASTER -> Set.copyOf(QUARTERMASTER);
            case MEMBER -> Set.copyOf(MEMBER);
        };
    }
}
