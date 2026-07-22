package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;

import java.util.function.UnaryOperator;

import java.util.List;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(GalacticWars.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<BlasterBoltEntity>> BLASTER_BOLT =
            registerEntityType(
                    "blaster_bolt",
                    BlasterBoltEntity::new,
                    MobCategory.MISC,
                    builder -> builder.sized(0.18F, 0.18F)
                            .clientTrackingRange(8)
                            .updateInterval(1));
    public static final RegistrySupplier<EntityType<GalacticVehicleEntity>> BARC_SPEEDER =
            registerVehicle("barc_speeder", 1.4F, 1.2F);
    public static final RegistrySupplier<EntityType<GalacticVehicleEntity>> AT_RT =
            registerVehicle("at_rt", 1.2F, 2.7F);
    public static final RegistrySupplier<EntityType<GalacticVehicleEntity>> STAP =
            registerVehicle("stap", 1.1F, 1.5F);
    public static final RegistrySupplier<EntityType<GalacticVehicleEntity>> AAT =
            registerVehicle("aat", 3.2F, 2.5F);
    public static final RegistrySupplier<EntityType<GalacticVehicleEntity>> LAAT_GUNSHIP =
            registerVehicle("laat_gunship", 5.4F, 3.0F);

    private static final List<RegistrySupplier<EntityType<GalacticVehicleEntity>>> VEHICLES =
            List.of(BARC_SPEEDER, AT_RT, STAP, AAT, LAAT_GUNSHIP);

    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> CLONE_TROOPER =
            registerRecruit("clone_trooper", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> ARC_TROOPER =
            registerRecruit("arc_trooper", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> PHASE_I_CLONE_TROOPER =
            registerRecruit("phase_i_clone_trooper", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> PHASE_I_ARC_TROOPER =
            registerRecruit("phase_i_arc_trooper", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> JEDI_KNIGHT =
            registerRecruit("jedi_knight", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> SENATE_COMMANDO =
            registerRecruit("senate_commando", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> REPUBLIC_HONOR_GUARD =
            registerRecruit("republic_honor_guard", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> MANDALORIAN_WARRIOR =
            registerRecruit("mandalorian_warrior", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> MANDALORIAN_MARKSMAN =
            registerRecruit("mandalorian_marksman", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> MANDALORIAN_HEAVY =
            registerRecruit("mandalorian_heavy", 0.68F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> B1_BATTLE_DROID =
            registerRecruit("b1_battle_droid", 0.50F, 1.93F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> B1_SECURITY_DROID =
            registerRecruit("b1_security_droid", 0.50F, 1.93F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> B2_SUPER_BATTLE_DROID =
            registerRecruit("b2_super_battle_droid", 0.82F, 2.05F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> COMMANDO_DROID =
            registerRecruit("commando_droid", 0.58F, 1.91F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> SITH_ACOLYTE =
            registerRecruit("sith_acolyte", 0.62F, 2.05F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> HUTT_ENFORCER =
            registerRecruit("hutt_enforcer", 0.84F, 2.00F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> BOUNTY_HUNTER =
            registerRecruit("bounty_hunter", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> SMUGGLER =
            registerRecruit("smuggler", 0.60F, 1.95F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> NIGHTSISTER_ACOLYTE =
            registerRecruit("nightsister_acolyte", 0.60F, 2.05F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> NIGHTSISTER_ARCHER =
            registerRecruit("nightsister_archer", 0.60F, 2.05F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> NIGHTBROTHER_BRUTE =
            registerRecruit("nightbrother_brute", 0.72F, 2.10F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> REPUBLIC_CIVILIAN =
            registerRecruit("republic_civilian", 0.60F, 1.90F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> TOGRUTA_CIVILIAN =
            registerRecruit("togruta_civilian", 0.60F, 1.90F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> SEPARATIST_TECHNICIAN =
            registerRecruit("separatist_technician", 0.60F, 1.90F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> MANDALORIAN_CLANSPERSON =
            registerRecruit("mandalorian_clansperson", 0.60F, 1.90F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> HUTT_CIVILIAN =
            registerRecruit("hutt_civilian", 0.60F, 1.90F);
    public static final RegistrySupplier<EntityType<GalacticRecruitEntity>> NIGHTSISTER_CIVILIAN =
            registerRecruit("nightsister_civilian", 0.60F, 2.00F);

    private static final List<RegistrySupplier<EntityType<GalacticRecruitEntity>>> RECRUITS = List.of(
            CLONE_TROOPER, ARC_TROOPER, PHASE_I_CLONE_TROOPER, PHASE_I_ARC_TROOPER, JEDI_KNIGHT,
            SENATE_COMMANDO, REPUBLIC_HONOR_GUARD,
            B1_BATTLE_DROID, B1_SECURITY_DROID, B2_SUPER_BATTLE_DROID, COMMANDO_DROID, SITH_ACOLYTE,
            MANDALORIAN_WARRIOR, MANDALORIAN_MARKSMAN, MANDALORIAN_HEAVY,
            HUTT_ENFORCER, BOUNTY_HUNTER, SMUGGLER,
            NIGHTSISTER_ACOLYTE, NIGHTSISTER_ARCHER, NIGHTBROTHER_BRUTE,
            REPUBLIC_CIVILIAN, TOGRUTA_CIVILIAN, SEPARATIST_TECHNICIAN, MANDALORIAN_CLANSPERSON,
            HUTT_CIVILIAN, NIGHTSISTER_CIVILIAN);

    private ModEntityTypes() {
    }

    public static void register() {
        ENTITY_TYPES.register();
    }

    public static List<RegistrySupplier<EntityType<GalacticRecruitEntity>>> recruits() {
        return RECRUITS;
    }

    public static List<RegistrySupplier<EntityType<GalacticVehicleEntity>>> vehicles() {
        return VEHICLES;
    }

    private static RegistrySupplier<EntityType<GalacticVehicleEntity>> registerVehicle(
            String name, float width, float height
    ) {
        return registerEntityType(name, GalacticVehicleEntity::new, MobCategory.MISC,
                builder -> builder.sized(width, height).clientTrackingRange(12).updateInterval(1));
    }

    private static RegistrySupplier<EntityType<GalacticRecruitEntity>> registerRecruit(
            String name,
            float width,
            float height
    ) {
        return registerEntityType(
                name,
                GalacticRecruitEntity::new,
                MobCategory.CREATURE,
                builder -> builder.sized(width, height).clientTrackingRange(8).updateInterval(3));
    }

    private static <T extends Entity> RegistrySupplier<EntityType<T>> registerEntityType(
            String name,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            UnaryOperator<EntityType.Builder<T>> builderFactory
    ) {
        return ENTITY_TYPES.register(name, () -> builderFactory.apply(
                EntityType.Builder.of(factory, category)).build(ResourceKey.create(
                        Registries.ENTITY_TYPE,
                        Identifier.fromNamespaceAndPath(GalacticWars.MODID, name))));
    }

}
