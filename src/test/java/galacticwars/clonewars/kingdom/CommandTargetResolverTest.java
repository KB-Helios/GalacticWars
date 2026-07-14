package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CommandTargetResolverTest {
    private CommandTargetResolverTest() {
    }

    public static void main(String[] args) {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var empty = CommandTargetResolver.resolve(List.<UUID>of(), Optional.empty(), value -> value);
        require(empty.target().isEmpty() && empty.reason().equals("target_unavailable"),
                "empty candidate set must fail as unavailable");

        var unique = CommandTargetResolver.resolve(List.of(first), Optional.empty(), value -> value);
        require(unique.target().filter(first::equals).isPresent(),
                "one unambiguous candidate should remain backwards compatible");

        var ambiguous = CommandTargetResolver.resolve(
                List.of(first, second), Optional.empty(), value -> value);
        require(ambiguous.target().isEmpty() && ambiguous.reason().equals("selection_required"),
                "multiple candidates must never silently select the first entry");

        var selected = CommandTargetResolver.resolve(
                List.of(first, second), Optional.of(second), value -> value);
        require(selected.target().filter(second::equals).isPresent(),
                "explicit target must resolve deterministically");

        var stale = CommandTargetResolver.resolve(
                List.of(first, second), Optional.of(UUID.randomUUID()), value -> value);
        require(stale.target().isEmpty() && stale.reason().equals("target_unavailable"),
                "stale explicit target must fail closed");
        System.out.println("CommandTargetResolverTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
