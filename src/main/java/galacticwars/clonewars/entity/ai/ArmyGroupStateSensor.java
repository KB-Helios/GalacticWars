package galacticwars.clonewars.entity.ai;

import galacticwars.clonewars.entity.GalacticRecruitEntity;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.util.BrainUtil;

/** Projects the authoritative SavedData squad state into a loaded recruit brain. */
public final class ArmyGroupStateSensor extends ExtendedSensor<GalacticRecruitEntity> {
    private boolean cleanedInvalidGroup;

    public ArmyGroupStateSensor() {
        scanRate(2);
    }

    @Override
    public SensorType<? extends ExtendedSensor<?>> type() {
        return ArmyBrainSensorTypes.GROUP_STATE;
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(ArmyBrainMemoryTypes.ARMY_STATE, ArmyBrainMemoryTypes.PATH_STATUS);
    }

    @Override
    protected void doTick(ServerLevel level, GalacticRecruitEntity recruit) {
        ArmyBrainState previous = BrainUtil.getMemory(recruit, ArmyBrainMemoryTypes.ARMY_STATE);
        ArmyBrainState next = ArmyBrainSupport.resolveState(recruit, level, previous);
        if (next == null) {
            if (!cleanedInvalidGroup && (previous != null || recruit.hasAuthoritativeArmyGroup())) {
                ArmyBrainSupport.clearGroupExecution(recruit);
            }
            cleanedInvalidGroup = recruit.hasAuthoritativeArmyGroup();
            return;
        }

        boolean changedGroup = previous == null || !previous.group().id().equals(next.group().id());
        if (changedGroup) {
            ArmyBrainSupport.clearGroupExecution(recruit);
        }
        cleanedInvalidGroup = false;
        BrainUtil.setMemory(recruit, ArmyBrainMemoryTypes.ARMY_STATE, next);
    }
}
