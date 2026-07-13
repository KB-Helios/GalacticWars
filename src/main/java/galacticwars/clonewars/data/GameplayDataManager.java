package galacticwars.clonewars.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.ability.AbilityActivation;
import galacticwars.clonewars.ability.AbilityDefinition;
import galacticwars.clonewars.ability.AbilityId;
import galacticwars.clonewars.ability.AbilityKind;
import galacticwars.clonewars.army.ArmyEquipmentLoadout;
import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyUnitCatalog;
import galacticwars.clonewars.army.ArmyUnitDefinition;
import galacticwars.clonewars.army.ArmyUnitId;
import galacticwars.clonewars.army.ArmyUnitRole;
import galacticwars.clonewars.classes.ProgressionRequirement;
import galacticwars.clonewars.classes.UnitClassDefinition;
import galacticwars.clonewars.classes.UnitClassId;
import galacticwars.clonewars.faction.FactionCatalog;
import galacticwars.clonewars.faction.FactionDefinition;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.faction.FactionRuntimePolicy;
import galacticwars.clonewars.faction.FactionStrategyDefinition;
import galacticwars.clonewars.recruitment.NpcServiceBranch;
import galacticwars.clonewars.settlement.BaseBlockPlacement;
import galacticwars.clonewars.settlement.BlueprintAnchor;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import galacticwars.clonewars.world.OverworldFactionSpawnProfile;
import galacticwars.clonewars.world.CivilianArchetypeDefinition;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ListenerKey;

@EventBusSubscriber(modid = GalacticWars.MODID)
public final class GameplayDataManager extends SimplePreparableReloadListener<GameplayDataManager.LoadResult> {
    private static final Gson GSON = new Gson();
    private static final GameplayDataManager INSTANCE = new GameplayDataManager();
    private static final ListenerKey<GameplayDataManager> LISTENER_KEY = ListenerKey.create(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "gameplay_data"));
    private static volatile GameplayDataSnapshot snapshot = defaults();
    private static volatile long generation;

    private GameplayDataManager() {
    }

    public static GameplayDataSnapshot snapshot() {
        return snapshot;
    }

    public static long generation() {
        return generation;
    }

    @SubscribeEvent
    public static void addReloadListener(AddServerReloadListenersEvent event) {
        event.addRetainedListener(LISTENER_KEY, INSTANCE);
    }

    @Override
    protected LoadResult prepare(ResourceManager manager, ProfilerFiller profiler) {
        try {
            Map<FactionId, FactionDefinition> factions = loadFactions(manager);
            List<ArmyUnitDefinition> units = loadUnits(manager, factions);
            Map<AbilityId, AbilityDefinition> abilities = loadAbilities(manager);
            Map<UnitClassId, UnitClassDefinition> unitClasses = loadUnitClasses(
                    manager, factions, units, abilities);
            Map<FactionId, FactionRuntimePolicy> factionPolicies = loadFactionPolicies(manager, factions);
            Map<String, KingdomBaseBlueprint> blueprints = loadBlueprints(manager);
            Map<String, CivilianArchetypeDefinition> civilianArchetypes = loadCivilianArchetypes(
                    manager, factions);
            Map<String, OverworldFactionSpawnProfile> overworldSpawnProfiles = loadOverworldSpawnProfiles(
                    manager, factions, units, civilianArchetypes);
            LaunchContentDefinitions launchContent = LaunchContentValidator.load(
                    manager,
                    factions.keySet().stream().map(FactionId::path)
                            .collect(java.util.stream.Collectors.toSet()));
            validateRelations(factions);
            LinkedHashMap<String, ArmyUnitId> byEntityType = new LinkedHashMap<>();
            for (ArmyUnitDefinition unit : units) {
                if (unit.entityTypeId().isBlank()) {
                    throw new IllegalArgumentException("Unit " + unit.id() + " is missing entity_type");
                }
                ArmyUnitId previous = byEntityType.putIfAbsent(unit.entityTypeId(), unit.id());
                if (previous != null) {
                    throw new IllegalArgumentException("Entity type " + unit.entityTypeId()
                            + " is mapped by both " + previous + " and " + unit.id());
                }
                requireRegistered(BuiltInRegistries.ENTITY_TYPE, unit.entityTypeId(),
                        "entity type for " + unit.id());
                validateEquipment(unit);
            }
            for (FactionDefinition faction : factions.values()) {
                requireRegistered(BuiltInRegistries.ITEM, faction.pledgeTokenItemId(),
                        "pledge token for " + faction.id());
            }
            for (LaunchContentDefinitions.TradeDefinition trade : launchContent.trades().values()) {
                requireRegistered(BuiltInRegistries.ITEM, trade.itemId(),
                        "stock item for trade " + trade.id());
            }
            for (LaunchContentDefinitions.VehicleDefinition vehicle : launchContent.vehicles().values()) {
                for (String itemId : vehicle.fabricationInputs().keySet()) {
                    requireRegistered(BuiltInRegistries.ITEM, itemId,
                            "fabrication input for vehicle " + vehicle.id());
                }
            }
            for (LaunchContentDefinitions.ForceAbilityDefinition ability
                    : launchContent.forceAbilities().values()) {
                if (ability.enabled() && !galacticwars.clonewars.force.ForceEffectExecutorCatalog
                        .supports(ability.effect())) {
                    throw new IllegalArgumentException(
                            "Enabled Force ability has no executor: " + ability.id());
                }
            }
            for (KingdomBaseBlueprint blueprint : blueprints.values()) {
                for (BaseBlockPlacement placement : blueprint.placements()) {
                    requireRegistered(BuiltInRegistries.BLOCK, placement.blockId(),
                            "blueprint block for " + blueprint.id());
                    requireRegistered(BuiltInRegistries.ITEM, placement.itemId(),
                            "blueprint item for " + blueprint.id());
                }
            }
            GameplayDataSnapshot loaded = new GameplayDataSnapshot(
                    new FactionCatalog(factions),
                    new ArmyUnitCatalog(units),
                    byEntityType,
                    Map.of("galacticwars:mandalorian_rider", ArmyUnitId.of("galacticwars:mandalorian_warrior")),
                    blueprints,
                    overworldSpawnProfiles,
                    civilianArchetypes,
                    abilities,
                    unitClasses,
                    factionPolicies,
                    launchContent);
            return LoadResult.success(loaded);
        } catch (RuntimeException | IOException exception) {
            return LoadResult.failure(exception);
        }
    }

    @Override
    protected void apply(LoadResult result, ResourceManager manager, ProfilerFiller profiler) {
        if (result.snapshot().isPresent()) {
            snapshot = result.snapshot().orElseThrow();
            LinkedHashMap<String, List<String>> launchUnits = new LinkedHashMap<>();
            snapshot.factions().definitions().keySet().forEach(faction -> launchUnits.put(
                    faction.path(), snapshot.units().definitions().values().stream()
                            .filter(unit -> unit.factionId().equals(faction))
                            .map(unit -> unit.id().path()).toList()));
            LaunchContentRuntime.install(
                    snapshot.launchContent(),
                    snapshot.selectableFactions().stream().map(faction -> faction.id().toString()).toList(),
                    launchUnits);
            generation++;
            GalacticWars.LOGGER.info(
                    "Loaded {} factions, {} units, {} classes, {} abilities, {} faction policies, {} civilian archetypes, {} base blueprints, {} Overworld spawn profiles, {} quests, {} trades, {} vehicles, and {} planets",
                    snapshot.factions().definitions().size(),
                    snapshot.units().definitions().size(),
                    snapshot.unitClasses().size(),
                    snapshot.abilities().size(),
                    snapshot.factionPolicies().size(),
                    snapshot.civilianArchetypesByEntityType().size(),
                    snapshot.blueprints().size(),
                    snapshot.overworldSpawnProfiles().size(),
                    snapshot.launchContent().quests().size(),
                    snapshot.launchContent().trades().size(),
                    snapshot.launchContent().vehicles().size(),
                    snapshot.launchContent().planets().size());
        } else {
            GalacticWars.LOGGER.error(
                    "Rejected gameplay data reload; retaining the previous valid snapshot",
                    result.failure().orElse(null));
        }
    }

    private static Map<FactionId, FactionDefinition> loadFactions(ResourceManager manager) throws IOException {
        LinkedHashMap<FactionId, FactionDefinition> definitions = new LinkedHashMap<>();
        for (ResourceJson resource : resources(manager, "galacticwars/factions")) {
            JsonObject json = resource.json();
            FactionId id = FactionId.of(requiredString(json, "id", resource.id()));
            Set<FactionId> allies = factionIds(json, "allies");
            Set<FactionId> enemies = factionIds(json, "enemies");
            FactionDefinition.validateRelationSets(resource.id().toString(), id, allies, enemies);
            JsonObject pledge = object(json, "pledge", new JsonObject());
            JsonObject strategy = object(json, "strategy", new JsonObject());
            FactionDefinition definition = new FactionDefinition(
                    id,
                    requiredString(json, "display_name", resource.id()),
                    requiredInteger(json, "hire_cost", resource.id()),
                    integer(json, "minimum_hiring_alignment", 0),
                    integer(json, "max_owned_recruits", 0),
                    allies,
                    enemies,
                    integer(json, "selection_order", definitions.size()),
                    string(pledge, "token_item", "galacticwars:" + id.path() + "_identity_chip"),
                    integer(pledge, "direct_delta", 10),
                    integer(pledge, "ally_delta", 2),
                    integer(pledge, "enemy_delta", -5),
                    new FactionStrategyDefinition(
                            string(strategy, "archetype", "shared"),
                            integer(strategy, "recruitment_capacity_bonus", 0),
                            integer(strategy, "upkeep_percent", 100),
                            integer(strategy, "production_percent", 100),
                            integer(strategy, "morale_bonus", 0),
                            string(strategy, "strength", "combined_arms"),
                            string(strategy, "weakness", "none")));
            if (definitions.putIfAbsent(id, definition) != null) {
                throw new IllegalArgumentException("Duplicate faction id " + id + " in " + resource.id());
            }
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("No faction definitions were loaded");
        }
        return definitions;
    }

    private static List<ArmyUnitDefinition> loadUnits(
            ResourceManager manager,
            Map<FactionId, FactionDefinition> factions
    ) throws IOException {
        ArrayList<ArmyUnitDefinition> definitions = new ArrayList<>();
        LinkedHashSet<ArmyUnitId> seen = new LinkedHashSet<>();
        for (ResourceJson resource : resources(manager, "galacticwars/units")) {
            JsonObject json = resource.json();
            ArmyUnitId id = ArmyUnitId.of(requiredString(json, "id", resource.id()));
            FactionId factionId = FactionId.of(requiredString(json, "faction", resource.id()));
            FactionDefinition faction = factions.get(factionId);
            if (faction == null) {
                throw new IllegalArgumentException("Unit " + id + " references unknown faction " + factionId);
            }
            JsonObject attributes = object(json, "attributes", json);
            JsonObject equipment = object(json, "equipment", new JsonObject());
            ArmyUnitDefinition definition = new ArmyUnitDefinition(
                    id,
                    requiredString(json, "display_name", resource.id()),
                    factionId,
                    ArmyUnitRole.valueOf(requiredString(json, "role", resource.id()).toUpperCase(Locale.ROOT)),
                    integer(json, "hire_cost", faction.hireCost()),
                    integer(attributes, "max_health", integer(json, "max_health", 20)),
                    integer(attributes, "attack_damage", integer(json, "attack_damage", 5)),
                    ArmyFormation.valueOf(requiredString(json, "default_formation", resource.id())
                            .toUpperCase(Locale.ROOT)),
                    requiredString(json, "entity_type", resource.id()),
                    decimal(attributes, "movement_speed", 0.28D),
                    decimal(attributes, "follow_range", 24.0D),
                    decimal(attributes, "armor", 0.0D),
                    new ArmyEquipmentLoadout(
                            string(equipment, "main_hand", "minecraft:iron_sword"),
                            string(equipment, "head", ""),
                            string(equipment, "chest", ""),
                            string(equipment, "legs", ""),
                            string(equipment, "feet", "")));
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate unit id " + id + " in " + resource.id());
            }
            definitions.add(definition);
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("No army unit definitions were loaded");
        }
        return List.copyOf(definitions);
    }

    private static Map<AbilityId, AbilityDefinition> loadAbilities(ResourceManager manager) throws IOException {
        LinkedHashMap<AbilityId, AbilityDefinition> definitions = new LinkedHashMap<>();
        for (ResourceJson resource : resources(manager, "galacticwars/abilities")) {
            requireSchema(resource, 1);
            for (JsonObject json : definitionObjects(resource, "abilities")) {
                AbilityDefinition definition = new AbilityDefinition(
                        AbilityId.of(requiredString(json, "id", resource.id())),
                        requiredString(json, "display_name", resource.id()),
                        AbilityKind.valueOf(requiredString(json, "kind", resource.id())
                                .toUpperCase(Locale.ROOT)),
                        AbilityActivation.valueOf(requiredString(json, "activation", resource.id())
                                .toUpperCase(Locale.ROOT)),
                        integer(json, "cooldown_ticks", 0),
                        integer(json, "resource_cost", 0),
                        decimal(json, "range", 0.0D),
                        integer(json, "ai_interval_ticks", 20),
                        bool(json, "enabled", true));
                if (definitions.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("Duplicate ability id " + definition.id()
                            + " in " + resource.id());
                }
                if (definition.enabled()
                        && !galacticwars.clonewars.classes.ClassAbilityEffectRegistry.registered(
                        definition.id().toString())) {
                    throw new IllegalArgumentException("Enabled ability lacks runtime executor "
                            + definition.id() + " in " + resource.id());
                }
            }
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("No ability definitions were loaded");
        }
        return Map.copyOf(definitions);
    }

    private static Map<UnitClassId, UnitClassDefinition> loadUnitClasses(
            ResourceManager manager,
            Map<FactionId, FactionDefinition> factions,
            List<ArmyUnitDefinition> units,
            Map<AbilityId, AbilityDefinition> abilities
    ) throws IOException {
        LinkedHashMap<ArmyUnitId, ArmyUnitDefinition> unitsById = new LinkedHashMap<>();
        for (ArmyUnitDefinition unit : units) {
            unitsById.put(unit.id(), unit);
        }
        LinkedHashMap<UnitClassId, UnitClassDefinition> definitions = new LinkedHashMap<>();
        LinkedHashSet<ArmyUnitId> representedUnits = new LinkedHashSet<>();
        for (ResourceJson resource : resources(manager, "galacticwars/classes")) {
            requireSchema(resource, 1);
            for (JsonObject json : definitionObjects(resource, "classes")) {
                FactionId factionId = FactionId.of(requiredString(json, "faction", resource.id()));
                ArmyUnitId unitId = ArmyUnitId.of(requiredString(json, "unit", resource.id()));
                ArmyUnitDefinition unit = unitsById.get(unitId);
                if (!factions.containsKey(factionId)) {
                    throw new IllegalArgumentException("Class references unknown faction " + factionId);
                }
                if (unit == null || !unit.factionId().equals(factionId)) {
                    throw new IllegalArgumentException("Class unit " + unitId
                            + " is missing or does not belong to " + factionId);
                }
                List<AbilityId> abilityIds = strings(json, "abilities").stream().map(AbilityId::of).toList();
                for (AbilityId abilityId : abilityIds) {
                    if (!abilities.containsKey(abilityId)) {
                        throw new IllegalArgumentException("Class references unknown ability " + abilityId);
                    }
                }
                ArrayList<ProgressionRequirement> requirements = new ArrayList<>();
                JsonArray requirementArray = json.has("requirements")
                        ? json.getAsJsonArray("requirements") : new JsonArray();
                for (JsonElement element : requirementArray) {
                    JsonObject requirement = element.getAsJsonObject();
                    requirements.add(new ProgressionRequirement(
                            requiredString(requirement, "type", resource.id()),
                            requiredString(requirement, "subject", resource.id()),
                            integer(requirement, "amount", 1)));
                }
                UnitClassDefinition definition = new UnitClassDefinition(
                        UnitClassId.of(requiredString(json, "id", resource.id())),
                        requiredString(json, "display_name", resource.id()),
                        factionId,
                        unitId,
                        bool(json, "player_assignable", false),
                        abilityIds,
                        requirements,
                        string(json, "force_path_slot", ""));
                if (definitions.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("Duplicate class id " + definition.id()
                            + " in " + resource.id());
                }
                if (!representedUnits.add(unitId)) {
                    throw new IllegalArgumentException("Multiple classes target unit " + unitId);
                }
            }
        }
        if (!representedUnits.equals(unitsById.keySet())) {
            LinkedHashSet<ArmyUnitId> missing = new LinkedHashSet<>(unitsById.keySet());
            missing.removeAll(representedUnits);
            throw new IllegalArgumentException("Every launch unit requires one class; missing " + missing);
        }
        return Map.copyOf(definitions);
    }

    private static Map<FactionId, FactionRuntimePolicy> loadFactionPolicies(
            ResourceManager manager,
            Map<FactionId, FactionDefinition> factions
    ) throws IOException {
        LinkedHashMap<FactionId, FactionRuntimePolicy> policies = new LinkedHashMap<>();
        for (ResourceJson resource : resources(manager, "galacticwars/faction_policies")) {
            requireSchema(resource, 1);
            for (JsonObject json : definitionObjects(resource, "policies")) {
                FactionId factionId = FactionId.of(requiredString(json, "faction", resource.id()));
                if (!factions.containsKey(factionId)) {
                    throw new IllegalArgumentException("Policy references unknown faction " + factionId);
                }
                FactionRuntimePolicy policy = new FactionRuntimePolicy(
                        factionId,
                        Set.copyOf(strings(json, "traits")),
                        integerMap(json, "modifiers", resource.id()));
                if (policies.putIfAbsent(factionId, policy) != null) {
                    throw new IllegalArgumentException("Duplicate faction policy for " + factionId);
                }
            }
        }
        if (!policies.keySet().equals(factions.keySet())) {
            LinkedHashSet<FactionId> missing = new LinkedHashSet<>(factions.keySet());
            missing.removeAll(policies.keySet());
            throw new IllegalArgumentException("Every faction requires one runtime policy; missing " + missing);
        }
        return Map.copyOf(policies);
    }

    private static Map<String, KingdomBaseBlueprint> loadBlueprints(ResourceManager manager) throws IOException {
        LinkedHashMap<String, KingdomBaseBlueprint> blueprints = new LinkedHashMap<>();
        for (ResourceJson resource : resources(manager, "galacticwars/blueprints")) {
            KingdomBaseBlueprint blueprint = parseBlueprint(resource.id(), resource.json());
            if (blueprints.putIfAbsent(blueprint.id(), blueprint) != null) {
                throw new IllegalArgumentException("Duplicate blueprint id " + blueprint.id() + " in " + resource.id());
            }
        }
        if (!blueprints.containsKey(KingdomBaseBlueprint.STARTER_KEEP_ID)) {
            throw new IllegalArgumentException("Missing required forward_base blueprint");
        }
        return blueprints;
    }

    private static Map<String, OverworldFactionSpawnProfile> loadOverworldSpawnProfiles(
            ResourceManager manager,
            Map<FactionId, FactionDefinition> factions,
            List<ArmyUnitDefinition> units,
            Map<String, CivilianArchetypeDefinition> civilianArchetypes
    ) throws IOException {
        Map<String, ArmyUnitDefinition> unitsByEntity = units.stream().collect(java.util.stream.Collectors.toMap(
                ArmyUnitDefinition::entityTypeId, unit -> unit));
        LinkedHashMap<String, OverworldFactionSpawnProfile> profiles = new LinkedHashMap<>();
        for (ResourceJson resource : resources(manager, "galacticwars/overworld_faction_spawns")) {
            JsonArray definitions = requiredArray(resource.json(), "profiles", resource.id());
            for (JsonElement element : definitions) {
                JsonObject json = element.getAsJsonObject();
                String factionId = requiredString(json, "faction", resource.id());
                FactionId faction = FactionId.of(factionId);
                if (!factions.containsKey(faction)) {
                    throw new IllegalArgumentException("Overworld spawn profile references unknown faction " + faction);
                }
                LinkedHashMap<String, NpcServiceBranch> branches = new LinkedHashMap<>();
                JsonArray entities = requiredArray(json, "entities", resource.id());
                for (JsonElement entityElement : entities) {
                    JsonObject entity = entityElement.getAsJsonObject();
                    String entityTypeId = requiredString(entity, "entity_type", resource.id());
                    ArmyUnitDefinition unit = unitsByEntity.get(entityTypeId);
                    CivilianArchetypeDefinition civilian = civilianArchetypes.get(entityTypeId);
                    boolean matchingUnit = unit != null && unit.factionId().equals(faction);
                    boolean matchingCivilian = civilian != null && civilian.factionId().equals(faction.toString());
                    if (!matchingUnit && !matchingCivilian) {
                        throw new IllegalArgumentException("Overworld spawn entity " + entityTypeId
                                + " does not belong to " + faction);
                    }
                    NpcServiceBranch branch = NpcServiceBranch.byId(
                            requiredString(entity, "service_branch", resource.id()));
                    if (branches.putIfAbsent(entityTypeId, branch) != null) {
                        throw new IllegalArgumentException("Duplicate Overworld spawn entity " + entityTypeId);
                    }
                }
                OverworldFactionSpawnProfile profile = new OverworldFactionSpawnProfile(
                        faction.toString(), branches,
                        integer(json, "outpost_radius", 96),
                        integer(json, "minimum_outpost_spacing", 256),
                        integer(json, "military_capacity", 12),
                        integer(json, "civilian_capacity", 4));
                if (profiles.putIfAbsent(profile.factionId(), profile) != null) {
                    throw new IllegalArgumentException("Duplicate Overworld spawn profile for " + faction);
                }
            }
        }
        if (profiles.size() != factions.size()) {
            throw new IllegalArgumentException("Every faction requires one Overworld spawn profile");
        }
        return Map.copyOf(profiles);
    }

    private static Map<String, CivilianArchetypeDefinition> loadCivilianArchetypes(
            ResourceManager manager,
            Map<FactionId, FactionDefinition> factions
    ) throws IOException {
        LinkedHashMap<String, CivilianArchetypeDefinition> definitions = new LinkedHashMap<>();
        LinkedHashSet<FactionId> representedFactions = new LinkedHashSet<>();
        for (ResourceJson resource : resources(manager, "galacticwars/civilian_archetypes")) {
            JsonObject json = resource.json();
            FactionId faction = FactionId.of(requiredString(json, "faction", resource.id()));
            if (!factions.containsKey(faction)) {
                throw new IllegalArgumentException("Civilian archetype references unknown faction " + faction);
            }
            JsonArray professionsJson = requiredArray(json, "professions", resource.id());
            ArrayList<String> professions = new ArrayList<>();
            professionsJson.forEach(element -> professions.add(element.getAsString()));
            CivilianArchetypeDefinition definition = new CivilianArchetypeDefinition(
                    requiredString(json, "id", resource.id()),
                    requiredString(json, "display_name", resource.id()),
                    faction.toString(),
                    requiredString(json, "entity_type", resource.id()),
                    professions,
                    integer(json, "max_health", 20),
                    decimal(json, "movement_speed", 0.26D),
                    integer(json, "base_morale", 60),
                    string(json, "home_type", "housing"));
            requireRegistered(BuiltInRegistries.ENTITY_TYPE, definition.entityTypeId(),
                    "civilian entity type for " + definition.id());
            if (definitions.putIfAbsent(definition.entityTypeId(), definition) != null
                    || !representedFactions.add(faction)) {
                throw new IllegalArgumentException("Duplicate civilian archetype for " + faction);
            }
        }
        if (representedFactions.size() != factions.size()) {
            throw new IllegalArgumentException("Every faction requires one civilian archetype");
        }
        return Map.copyOf(definitions);
    }

    static KingdomBaseBlueprint parseBlueprint(Identifier resourceId, JsonObject json) {
        if (integer(json, "schema_version", -1) != 1) {
            throw new IllegalArgumentException("Unsupported blueprint schema in " + resourceId);
        }
        String id = GameplayDataSnapshot.normalizeBlueprintId(requiredString(json, "id", resourceId));
        String canonicalResourceId = GameplayDataSnapshot.normalizeBlueprintId(resourceId.toString());
        if (!id.equals(canonicalResourceId)) {
            throw new IllegalArgumentException("Blueprint " + resourceId + " declares mismatched id " + id);
        }
        JsonObject anchorJson = requiredObject(json, "anchor", resourceId);
        BlueprintAnchor anchor = new BlueprintAnchor(
                integer(anchorJson, "x", 0),
                integer(anchorJson, "y", 0),
                integer(anchorJson, "z", 0));
        JsonArray rotationJson = requiredArray(json, "allowed_rotations", resourceId);
        ArrayList<Integer> allowedRotations = new ArrayList<>(rotationJson.size());
        for (JsonElement element : rotationJson) {
            allowedRotations.add(element.getAsInt());
        }
        JsonArray placementJson = requiredArray(json, "placements", resourceId);
        ArrayList<BaseBlockPlacement> placements = new ArrayList<>(placementJson.size());
        for (JsonElement element : placementJson) {
            JsonObject placement = element.getAsJsonObject();
            placements.add(new BaseBlockPlacement(
                    integer(placement, "x", 0),
                    integer(placement, "y", 0),
                    integer(placement, "z", 0),
                    requiredString(placement, "block", resourceId),
                    requiredString(placement, "item", resourceId)));
        }
        JsonObject rewards = object(json, "rewards", new JsonObject());
        return new KingdomBaseBlueprint(
                id,
                requiredString(json, "display_name", resourceId),
                anchor,
                allowedRotations,
                placements,
                integer(rewards, "housing", 0),
                integer(rewards, "storage_slots", 0),
                string(rewards, "worksite", ""),
                integer(rewards, "worksite_capacity", 0),
                integer(rewards, "commander_slots", 0));
    }

    private static void validateRelations(Map<FactionId, FactionDefinition> factions) {
        for (FactionDefinition definition : factions.values()) {
            for (FactionId ally : definition.allies()) {
                FactionDefinition related = factions.get(ally);
                if (related == null || !related.allies().contains(definition.id())) {
                    throw new IllegalArgumentException("Alliance must be reciprocal: " + definition.id() + " <-> " + ally);
                }
            }
            for (FactionId enemy : definition.enemies()) {
                FactionDefinition related = factions.get(enemy);
                if (related == null || !related.enemies().contains(definition.id())) {
                    throw new IllegalArgumentException("Enemy relation must be reciprocal: " + definition.id() + " <-> " + enemy);
                }
            }
        }
    }

    private static List<ResourceJson> resources(ResourceManager manager, String path) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json(path);
        ArrayList<ResourceJson> result = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : converter.listMatchingResourcesFromNamespace(
                manager, GalacticWars.MODID).entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                result.add(new ResourceJson(converter.fileToId(entry.getKey()), JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Could not parse gameplay data resource " + entry.getKey(), exception);
            }
        }
        result.sort(java.util.Comparator.comparing(resource -> resource.id().toString()));
        return List.copyOf(result);
    }

    private static Set<FactionId> factionIds(JsonObject json, String key) {
        LinkedHashSet<FactionId> result = new LinkedHashSet<>();
        JsonArray values = json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
        for (JsonElement value : values) {
            result.add(FactionId.of(value.getAsString()));
        }
        return Set.copyOf(result);
    }

    private static List<JsonObject> definitionObjects(ResourceJson resource, String arrayKey) {
        if (!resource.json().has(arrayKey)) {
            return List.of(resource.json());
        }
        JsonArray array = requiredArray(resource.json(), arrayKey, resource.id());
        ArrayList<JsonObject> definitions = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Resource " + resource.id()
                        + " contains a non-object in " + arrayKey);
            }
            definitions.add(element.getAsJsonObject());
        }
        return List.copyOf(definitions);
    }

    private static void requireSchema(ResourceJson resource, int supported) {
        int schema = integer(resource.json(), "schema_version", -1);
        if (schema != supported) {
            throw new IllegalArgumentException("Unsupported schema " + schema + " in " + resource.id());
        }
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        ArrayList<String> values = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            String value = element.getAsString().trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                throw new IllegalArgumentException(key + " cannot contain a blank value");
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private static Map<String, Integer> integerMap(
            JsonObject json,
            String key,
            Identifier resourceId
    ) {
        JsonObject object = requiredObject(json, key, resourceId);
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getAsInt());
        }
        return Map.copyOf(values);
    }

    private static String requiredString(JsonObject json, String key, Identifier resourceId) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) {
            throw new IllegalArgumentException("Resource " + resourceId + " is missing " + key);
        }
        return json.get(key).getAsString();
    }

    private static JsonArray requiredArray(JsonObject json, String key, Identifier resourceId) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalArgumentException("Resource " + resourceId + " is missing array " + key);
        }
        return json.getAsJsonArray(key);
    }

    private static JsonObject requiredObject(JsonObject json, String key, Identifier resourceId) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalArgumentException("Resource " + resourceId + " is missing object " + key);
        }
        return json.getAsJsonObject(key);
    }

    private static JsonObject object(JsonObject json, String key, JsonObject fallback) {
        return json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : fallback;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static int requiredInteger(JsonObject json, String key, Identifier resourceId) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Resource " + resourceId + " is missing integer " + key);
        }
        return json.get(key).getAsInt();
    }

    private static double decimal(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static void validateEquipment(ArmyUnitDefinition unit) {
        ArmyEquipmentLoadout equipment = unit.equipment();
        for (String itemId : List.of(
                equipment.mainHandItemId(), equipment.headItemId(), equipment.chestItemId(),
                equipment.legsItemId(), equipment.feetItemId())) {
            if (!itemId.isBlank()) {
                requireRegistered(BuiltInRegistries.ITEM, itemId, "equipment for " + unit.id());
            }
        }
    }

    private static <T> void requireRegistered(Registry<T> registry, String value, String label) {
        Identifier id;
        try {
            id = Identifier.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + label + " resource id " + value, exception);
        }
        T registered = registry.getValue(id);
        if (registered == null || !id.equals(registry.getKey(registered))) {
            throw new IllegalArgumentException("Unknown " + label + " resource id " + id);
        }
    }

    private static GameplayDataSnapshot defaults() {
        List<KingdomBaseBlueprint> blueprints = KingdomBaseBlueprint.builtIns();
        LinkedHashMap<String, KingdomBaseBlueprint> indexed = new LinkedHashMap<>();
        for (KingdomBaseBlueprint blueprint : blueprints) {
            indexed.put(blueprint.id(), blueprint);
        }
        return new GameplayDataSnapshot(
                new FactionCatalog(Map.of()),
                new ArmyUnitCatalog(List.of()),
                Map.of(),
                Map.of("galacticwars:mandalorian_rider", ArmyUnitId.of("galacticwars:mandalorian_warrior")),
                indexed,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                LaunchContentDefinitions.empty());
    }

    record LoadResult(Optional<GameplayDataSnapshot> snapshot, Optional<Throwable> failure) {
        static LoadResult success(GameplayDataSnapshot snapshot) {
            return new LoadResult(Optional.of(snapshot), Optional.empty());
        }

        static LoadResult failure(Throwable failure) {
            return new LoadResult(Optional.empty(), Optional.of(failure));
        }
    }

    private record ResourceJson(Identifier id, JsonObject json) {
    }
}
