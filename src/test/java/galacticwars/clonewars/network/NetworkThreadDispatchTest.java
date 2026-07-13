package galacticwars.clonewars.network;

import java.nio.file.Files;
import java.nio.file.Path;

public final class NetworkThreadDispatchTest {
    private NetworkThreadDispatchTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/network/GalacticNetwork.java"));
        int dispatches = source.split("context\\.execute", -1).length - 1;
        if (dispatches != 4) {
            throw new AssertionError("Every Framework payload handler must dispatch to the main thread");
        }
        if (!source.contains("PacketFlow.SERVERBOUND")
                || !source.contains("PacketFlow.CLIENTBOUND")
                || !source.contains("@RegistryContainer")) {
            throw new AssertionError("Framework channel must declare discovery and packet direction");
        }
        System.out.println("NetworkThreadDispatchTest passed");
    }
}
