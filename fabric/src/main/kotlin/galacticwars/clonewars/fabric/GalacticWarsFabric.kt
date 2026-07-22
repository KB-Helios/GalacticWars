package galacticwars.clonewars.fabric

import galacticwars.clonewars.GalacticWars
import net.fabricmc.api.ModInitializer

class GalacticWarsFabric : ModInitializer {
    override fun onInitialize() {
        FabricPlayerCampaignAttachments.register()
        GalacticWars.init()
        FabricWorldgenFeatures.register()
    }
}
