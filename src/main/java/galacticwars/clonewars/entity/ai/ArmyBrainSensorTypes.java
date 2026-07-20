package galacticwars.clonewars.entity.ai;

import net.minecraft.world.entity.ai.sensing.SensorType;
import net.tslat.smartbrainlib.registry.SBLSensors;

/**
 * Distinct registered SmartBrainLib sensor identities used as keys by the
 * recruit-local sensors.
 *
 * <p>Minecraft 26.2 hides {@link SensorType}'s constructor from common
 * Fabric code. SmartBrain stores actual sensor instances keyed by their type,
 * so these otherwise-unused library identities give the two transient sensors
 * stable, cross-loader keys without adding a loader-specific registry.</p>
 */
final class ArmyBrainSensorTypes {
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final SensorType<ArmyGroupStateSensor> GROUP_STATE =
            (SensorType)SBLSensors.NEARBY_BLOCKS.get();
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final SensorType<ArmyThreatSensor> THREAT =
            (SensorType)SBLSensors.INCOMING_PROJECTILES.get();

    private ArmyBrainSensorTypes() {
    }
}
