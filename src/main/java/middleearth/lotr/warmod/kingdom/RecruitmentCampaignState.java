package middleearth.lotr.warmod.kingdom;

import java.util.Locale;

public enum RecruitmentCampaignState {
    RESERVED,
    COMPLETE,
    CANCELLED;

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static RecruitmentCampaignState byId(String value) {
        if (value == null || value.isBlank()) {
            return RESERVED;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return RESERVED;
        }
    }
}
