package galacticwars.clonewars.kingdom;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Resolves command targets without ever silently selecting among multiple candidates. */
public final class CommandTargetResolver {
    private CommandTargetResolver() {
    }

    public static <T> Resolution<T> resolve(
            List<T> candidates,
            Optional<UUID> requestedId,
            Function<T, UUID> id
    ) {
        Objects.requireNonNull(candidates, "candidates");
        requestedId = requestedId == null ? Optional.empty() : requestedId;
        Objects.requireNonNull(id, "id");
        if (requestedId.isPresent()) {
            UUID targetId = requestedId.orElseThrow();
            return candidates.stream()
                    .filter(candidate -> id.apply(candidate).equals(targetId))
                    .findFirst()
                    .map(Resolution::selected)
                    .orElseGet(Resolution::unavailable);
        }
        if (candidates.size() == 1) {
            return Resolution.selected(candidates.getFirst());
        }
        return candidates.isEmpty() ? Resolution.unavailable() : Resolution.selectionRequired();
    }

    public record Resolution<T>(Optional<T> target, String reason) {
        public Resolution {
            target = target == null ? Optional.empty() : target;
            reason = Objects.requireNonNull(reason, "reason");
            if (target.isPresent() != reason.equals("selected")) {
                throw new IllegalArgumentException("selected resolutions must contain exactly one target");
            }
        }

        public static <T> Resolution<T> selected(T target) {
            return new Resolution<>(Optional.of(Objects.requireNonNull(target, "target")), "selected");
        }

        public static <T> Resolution<T> selectionRequired() {
            return new Resolution<>(Optional.empty(), "selection_required");
        }

        public static <T> Resolution<T> unavailable() {
            return new Resolution<>(Optional.empty(), "target_unavailable");
        }
    }
}
