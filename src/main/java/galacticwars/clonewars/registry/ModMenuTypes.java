package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.menu.RecruitCommandMenu;
import galacticwars.clonewars.menu.CommandCenterNavigationMenu;
import galacticwars.clonewars.menu.FactionSelectionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, GalacticWars.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RecruitCommandMenu>> RECRUIT_COMMAND =
            MENU_TYPES.register("recruit_command", () -> IMenuTypeExtension.create(RecruitCommandMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CommandCenterNavigationMenu>> COMMAND_CENTER_NAVIGATION =
            MENU_TYPES.register("command_center_navigation",
                    () -> IMenuTypeExtension.create(CommandCenterNavigationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FactionSelectionMenu>> FACTION_SELECTION =
            MENU_TYPES.register("faction_selection",
                    () -> IMenuTypeExtension.create(FactionSelectionMenu::new));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
