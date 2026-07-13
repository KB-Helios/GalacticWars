package galacticwars.clonewars.registry;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.combat.BlasterBoltEntity;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModEntityTypes {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(GalacticWars.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BlasterBoltEntity>> BLASTER_BOLT =
            ENTITY_TYPES.registerEntityType(
                    "blaster_bolt",
                    BlasterBoltEntity::new,
                    MobCategory.MISC,
                    builder -> builder.sized(0.18F, 0.18F)
                            .clientTrackingRange(8)
                            .updateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> BARC_SPEEDER =
            registerVehicle("barc_speeder", 1.4F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> AT_RT =
            registerVehicle("at_rt", 1.2F, 2.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> STAP =
            registerVehicle("stap", 1.1F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> AAT =
            registerVehicle("aat", 3.2F, 2.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> LAAT_GUNSHIP =
            registerVehicle("laat_gunship", 5.4F, 3.0F);

    private static final List<DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>>> VEHICLES =
            List.of(BARC_SPEEDER, AT_RT, STAP, AAT, LAAT_GUNSHIP);

    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> CLONE_TROOPER =
            registerRecruit("clone_trooper", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> ARC_TROOPER =
            registerRecruit("arc_trooper", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> JEDI_KNIGHT =
            registerRecruit("jedi_knight", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> MANDALORIAN_WARRIOR =
            registerRecruit("mandalorian_warrior", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> MANDALORIAN_MARKSMAN =
            registerRecruit("mandalorian_marksman", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> MANDALORIAN_HEAVY =
            registerRecruit("mandalorian_heavy", 0.68F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> B1_BATTLE_DROID =
            registerRecruit("b1_battle_droid", 0.70F, 1.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> B2_SUPER_BATTLE_DROID =
            registerRecruit("b2_super_battle_droid", 0.82F, 2.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> COMMANDO_DROID =
            registerRecruit("commando_droid", 0.64F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> HUTT_ENFORCER =
            registerRecruit("hutt_enforcer", 0.75F, 1.55F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> BOUNTY_HUNTER =
            registerRecruit("bounty_hunter", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> SMUGGLER =
            registerRecruit("smuggler", 0.60F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> NIGHTSISTER_ACOLYTE =
            registerRecruit("nightsister_acolyte", 0.60F, 2.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> NIGHTSISTER_ARCHER =
            registerRecruit("nightsister_archer", 0.60F, 2.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> NIGHTBROTHER_BRUTE =
            registerRecruit("nightbrother_brute", 0.72F, 2.10F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> REPUBLIC_CIVILIAN =
            registerRecruit("republic_civilian", 0.60F, 1.90F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> SEPARATIST_TECHNICIAN =
            registerRecruit("separatist_technician", 0.65F, 1.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> MANDALORIAN_CLANSPERSON =
            registerRecruit("mandalorian_clansperson", 0.60F, 1.90F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> HUTT_CIVILIAN =
            registerRecruit("hutt_civilian", 0.60F, 1.90F);
    public static final DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> NIGHTSISTER_CIVILIAN =
            registerRecruit("nightsister_civilian", 0.60F, 2.00F);

    private static final List<DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>>> RECRUITS = List.of(
            CLONE_TROOPER, ARC_TROOPER, JEDI_KNIGHT,
            B1_BATTLE_DROID, B2_SUPER_BATTLE_DROID, COMMANDO_DROID,
            MANDALORIAN_WARRIOR, MANDALORIAN_MARKSMAN, MANDALORIAN_HEAVY,
            HUTT_ENFORCER, BOUNTY_HUNTER, SMUGGLER,
            NIGHTSISTER_ACOLYTE, NIGHTSISTER_ARCHER, NIGHTBROTHER_BRUTE,
            REPUBLIC_CIVILIAN, SEPARATIST_TECHNICIAN, MANDALORIAN_CLANSPERSON,
            HUTT_CIVILIAN, NIGHTSISTER_CIVILIAN);

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    public static List<DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>>> recruits() {
        return RECRUITS;
    }

    public static List<DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>>> vehicles() {
        return VEHICLES;
    }

    private static DeferredHolder<EntityType<?>, EntityType<GalacticVehicleEntity>> registerVehicle(
            String name, float width, float height
    ) {
        return ENTITY_TYPES.registerEntityType(name, GalacticVehicleEntity::new, MobCategory.MISC,
                builder -> builder.sized(width, height).clientTrackingRange(12).updateInterval(1));
    }

    private static DeferredHolder<EntityType<?>, EntityType<GalacticRecruitEntity>> registerRecruit(
            String name,
            float width,
            float height
    ) {
        return ENTITY_TYPES.registerEntityType(
                name,
                GalacticRecruitEntity::new,
                MobCategory.CREATURE,
                builder -> builder.sized(width, height).clientTrackingRange(8).updateInterval(3));
    }
}
