package galacticwars.clonewars.entity;

import net.minecraft.network.chat.Component;

import java.util.List;

/** Immutable client-facing view of recruit state. */
public record RecruitStatusSnapshot(List<Component> lines) {
    public RecruitStatusSnapshot {
        lines = List.copyOf(lines);
    }
}
