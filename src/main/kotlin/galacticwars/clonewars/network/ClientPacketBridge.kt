package galacticwars.clonewars.network

import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Loader-neutral handoff for clientbound state. Common packet registration never links against
 * classes from the physical client source set, so dedicated servers can load it safely.
 */
object ClientPacketBridge {
    private val noForceHudHandler = Consumer<ForceHudPayload> { }
    private val forceHudHandler = AtomicReference(noForceHudHandler)
    private val noForceProgressionHandler = Consumer<ForceProgressionPayload> { }
    private val forceProgressionHandler = AtomicReference(noForceProgressionHandler)
    private val noClassHudHandler = Consumer<ClassHudPayload> { }
    private val classHudHandler = AtomicReference(noClassHudHandler)
    private val noGameplayCatalogHandler = Consumer<GameplayCatalogPayload> { }
    private val gameplayCatalogHandler = AtomicReference(noGameplayCatalogHandler)
    private val noFieldCommandStateHandler = Consumer<FieldCommandStatePayload> { }
    private val fieldCommandStateHandler = AtomicReference(noFieldCommandStateHandler)
    private val noFieldCommandOpenHandler = Runnable { }
    private val fieldCommandOpenHandler = AtomicReference(noFieldCommandOpenHandler)

    @JvmStatic
    fun installForceHudHandler(handler: Consumer<ForceHudPayload>) {
        forceHudHandler.set(handler)
    }

    @JvmStatic
    fun handleForceHud(payload: ForceHudPayload) {
        forceHudHandler.get().accept(payload)
    }

    @JvmStatic
    fun installForceProgressionHandler(handler: Consumer<ForceProgressionPayload>) {
        forceProgressionHandler.set(handler)
    }

    @JvmStatic
    fun handleForceProgression(payload: ForceProgressionPayload) {
        forceProgressionHandler.get().accept(payload)
    }

    @JvmStatic
    fun installClassHudHandler(handler: Consumer<ClassHudPayload>) {
        classHudHandler.set(handler)
    }

    @JvmStatic
    fun handleClassHud(payload: ClassHudPayload) {
        classHudHandler.get().accept(payload)
    }

    @JvmStatic
    fun installGameplayCatalogHandler(handler: Consumer<GameplayCatalogPayload>) {
        gameplayCatalogHandler.set(handler)
    }

    @JvmStatic
    fun handleGameplayCatalog(payload: GameplayCatalogPayload) {
        gameplayCatalogHandler.get().accept(payload)
    }

    @JvmStatic
    fun installFieldCommandStateHandler(handler: Consumer<FieldCommandStatePayload>) {
        fieldCommandStateHandler.set(handler)
    }

    @JvmStatic
    fun handleFieldCommandState(payload: FieldCommandStatePayload) {
        fieldCommandStateHandler.get().accept(payload)
    }

    @JvmStatic
    fun installFieldCommandOpenHandler(handler: Runnable) {
        fieldCommandOpenHandler.set(handler)
    }

    /** Safe common-code bridge used by the physical Tactical Command Marker. */
    @JvmStatic
    fun openFieldCommandScreen() {
        fieldCommandOpenHandler.get().run()
    }

    @JvmStatic
    fun clearClientHandlers() {
        forceHudHandler.set(noForceHudHandler)
        forceProgressionHandler.set(noForceProgressionHandler)
        classHudHandler.set(noClassHudHandler)
        gameplayCatalogHandler.set(noGameplayCatalogHandler)
        fieldCommandStateHandler.set(noFieldCommandStateHandler)
        fieldCommandOpenHandler.set(noFieldCommandOpenHandler)
    }
}
