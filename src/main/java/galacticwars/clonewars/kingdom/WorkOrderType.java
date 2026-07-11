package galacticwars.clonewars.kingdom;

import java.util.Locale;
import java.util.Optional;
import galacticwars.clonewars.workforce.WorkerProfession;

public enum WorkOrderType {
    FARM,
    LUMBER,
    MINE,
    BUILD,
    COURIER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static WorkOrderType byId(String id) {
        return valueOf(id.trim().toUpperCase(Locale.ROOT));
    }

    public WorkerProfession profession() {
        return switch (this) {
            case FARM -> WorkerProfession.FARMER;
            case LUMBER -> WorkerProfession.LUMBERJACK;
            case MINE -> WorkerProfession.MINER;
            case BUILD -> WorkerProfession.BUILDER;
            case COURIER -> WorkerProfession.COURIER;
        };
    }

    public static Optional<WorkOrderType> forProfession(WorkerProfession profession) {
        return switch (profession) {
            case FARMER -> Optional.of(FARM);
            case LUMBERJACK -> Optional.of(LUMBER);
            case MINER -> Optional.of(MINE);
            case BUILDER -> Optional.of(BUILD);
            case COURIER -> Optional.of(COURIER);
            case FISHERMAN, ANIMAL_FARMER, COOK, MERCHANT -> Optional.empty();
        };
    }
}
