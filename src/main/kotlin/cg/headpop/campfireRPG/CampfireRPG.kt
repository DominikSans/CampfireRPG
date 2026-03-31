package cg.headpop.campfireRPG

import cg.headpop.campfireRPG.command.CampfireRpgCommand
import cg.headpop.campfireRPG.config.SettingsLoader
import cg.headpop.campfireRPG.listener.CampfireBlockListener
import cg.headpop.campfireRPG.service.CampfireAuraService
import cg.headpop.campfireRPG.service.CampfireRegistry
import org.bukkit.plugin.java.JavaPlugin

class CampfireRPG : JavaPlugin() {

    lateinit var settingsLoader: SettingsLoader
        private set

    lateinit var campfireRegistry: CampfireRegistry
        private set

    lateinit var auraService: CampfireAuraService
        private set

    override fun onEnable() {
        saveDefaultConfig()

        settingsLoader = SettingsLoader(this)
        campfireRegistry = CampfireRegistry(this)
        auraService = CampfireAuraService(this, campfireRegistry)

        reloadPlugin()

        server.pluginManager.registerEvents(CampfireBlockListener(campfireRegistry), this)
        val command = CampfireRpgCommand(this)
        getCommand("campfirerpg")?.setExecutor(command)
        getCommand("campfirerpg")?.tabCompleter = command

        logger.info("CampfireRPG enabled successfully.")
    }

    override fun onDisable() {
        auraService.stop()
        campfireRegistry.clear()
    }

    fun reloadPlugin() {
        reloadConfig()
        settingsLoader.reload()
        campfireRegistry.reload()
        auraService.reload()
    }
}
