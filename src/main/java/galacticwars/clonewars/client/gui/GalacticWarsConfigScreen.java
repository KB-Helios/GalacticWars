package galacticwars.clonewars.client.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import galacticwars.clonewars.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Creates a fresh YACL screen backed by the existing NeoForge common config. */
public final class GalacticWarsConfigScreen {
    private GalacticWarsConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(text("config.galacticwars.title"))
                .category(ConfigCategory.createBuilder()
                        .name(text("config.galacticwars.category.general"))
                        .group(OptionGroup.createBuilder()
                                .name(text("config.galacticwars.group.foundation"))
                                .description(OptionDescription.of(
                                        text("config.galacticwars.group.foundation.description")))
                                .option(booleanOption("log_startup", Config.LOG_STARTUP))
                                .option(booleanOption("enable_content_seed", Config.ENABLE_CONTENT_SEED))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(text("config.galacticwars.category.combat"))
                        .group(OptionGroup.createBuilder()
                                .name(text("config.galacticwars.group.combat_safety"))
                                .description(OptionDescription.of(
                                        text("config.galacticwars.group.combat_safety.description")))
                                .option(booleanOption(
                                        "allow_blaster_friendly_fire",
                                        Config.ALLOW_BLASTER_FRIENDLY_FIRE))
                                .option(booleanOption("allow_blaster_pvp", Config.ALLOW_BLASTER_PVP))
                                .option(booleanOption("allow_class_pvp", Config.ALLOW_CLASS_PVP))
                                .option(booleanOption("allow_force_pvp", Config.ALLOW_FORCE_PVP))
                                .build())
                        .build())
                .save(Config::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(
            String key,
            ModConfigSpec.BooleanValue value
    ) {
        return Option.<Boolean>createBuilder()
                .name(text("config.galacticwars.option." + key))
                .description(OptionDescription.of(
                        text("config.galacticwars.option." + key + ".description")))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> BooleanControllerBuilder.create(option)
                        .coloured(true)
                        .onOffFormatter())
                .build();
    }

    private static Component text(String key) {
        return Component.translatable(key);
    }
}
