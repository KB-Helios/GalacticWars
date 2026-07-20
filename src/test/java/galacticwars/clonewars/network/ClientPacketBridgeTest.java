package galacticwars.clonewars.network;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientPacketBridgeTest {
    private ClientPacketBridgeTest() {
    }

    public static void main(String[] args) throws Exception {
        String networkSource = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/network/GalacticNetwork.java"));
        if (networkSource.contains("galacticwars.clonewars.client")) {
            throw new AssertionError("Common packet registration must not link client-only classes");
        }
        String bridgeSource = Files.readString(Path.of(
                "src/main/kotlin/galacticwars/clonewars/network/ClientPacketBridge.kt"));
        String clientSource = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/GalacticWarsClient.java"));
        if (!networkSource.contains("ClientPacketBridge.handleForceHud(payload)")
                || !networkSource.contains("ClientPacketBridge.handleGameplayCatalog(payload)")
                || !networkSource.contains("ClientPacketBridge.handleFieldCommandState(payload)")
                || !bridgeSource.contains("AtomicReference")
                || !bridgeSource.contains("installForceHudHandler")
                || !bridgeSource.contains("installGameplayCatalogHandler")
                || !bridgeSource.contains("installFieldCommandStateHandler")
                || !clientSource.contains("ClientPacketBridge.installForceHudHandler")
                || !clientSource.contains("ClientPacketBridge.installGameplayCatalogHandler")
                || !clientSource.contains("ClientPacketBridge.installFieldCommandStateHandler")) {
            throw new AssertionError("Client packet handoff is not fully installed and dispatched");
        }
        System.out.println("ClientPacketBridgeTest passed");
    }
}
