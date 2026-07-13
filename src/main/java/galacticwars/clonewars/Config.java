package galacticwars.clonewars;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_STARTUP = BUILDER
            .comment("Whether to log foundation setup during common setup")
            .define("logStartup", true);

    public static final ModConfigSpec.BooleanValue ENABLE_CONTENT_SEED = BUILDER
            .comment("Whether the initial Galactic Wars content seed should be considered enabled by gameplay systems")
            .define("enableContentSeed", true);

    public static final ModConfigSpec.BooleanValue ALLOW_BLASTER_FRIENDLY_FIRE = BUILDER
            .comment("Whether player blaster bolts may damage owned, same-faction, or allied recruits")
            .define("allowBlasterFriendlyFire", false);

    public static final ModConfigSpec.BooleanValue ALLOW_BLASTER_PVP = BUILDER
            .comment("Whether player and hostile faction recruit blaster bolts may damage players")
            .define("allowBlasterPvp", true);

    public static final ModConfigSpec.BooleanValue ALLOW_FORCE_PVP = BUILDER
            .comment("Whether Force abilities may directly affect other players")
            .define("allowForcePvp", true);

    public static final ModConfigSpec.BooleanValue ALLOW_CLASS_PVP = BUILDER
            .comment("Whether non-Force class abilities may directly affect other players")
            .define("allowClassPvp", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static void save() {
        SPEC.save();
    }
}
