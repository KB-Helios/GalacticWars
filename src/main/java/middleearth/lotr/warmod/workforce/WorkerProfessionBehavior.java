package middleearth.lotr.warmod.workforce;

/** Stable public contract for profession-specific runtime handlers. */
public interface WorkerProfessionBehavior {
    WorkerProfession profession();

    default boolean isEnabled() {
        return WorkerProfessionCatalog.isEnabled(this.profession());
    }
}
