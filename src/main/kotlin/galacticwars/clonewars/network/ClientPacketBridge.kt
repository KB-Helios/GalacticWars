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
    private val noClassHudHandler = Consumer<ClassHudPayload> { }
    private val classHudHandler = AtomicReference(noClassHudHandler)
    private val noGameplayCatalogHandler = Consumer<GameplayCatalogPayload> { }
    private val gameplayCatalogHandler = AtomicReference(noGameplayCatalogHandler)
    private val noFieldCommandStateHandler = Consumer<FieldCommandStatePayload> { }
    private val fieldCommandStateHandler = AtomicReference(noFieldCommandStateHandler)

    @JvmStatic
    fun installForceHudHandler(handler: Consumer<ForceHudPayload>) {
        forceHudHandler.set(handler)
    }

    @JvmStatic
    fun handleForceHud(payload: ForceHudPayload) {
        forceHudHandler.get().accept(payload)
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
    fun clearClientHandlers() {
        forceHudHandler.set(noForceHudHandler)
        classHudHandler.set(noClassHudHandler)
        gameplayCatalogHandler.set(noGameplayCatalogHandler)
        fieldCommandStateHandler.set(noFieldCommandStateHandler)
    }
}
