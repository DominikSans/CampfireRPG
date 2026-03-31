package cg.headpop.campfireRPG

import cg.headpop.campfireRPG.command.CampfireRpgCommand
import cg.headpop.campfireRPG.config.SettingsLoader
import cg.headpop.campfireRPG.gui.AdminMenuService
import cg.headpop.campfireRPG.integration.IntegrationService
import cg.headpop.campfireRPG.listener.AdminMenuListener
import cg.headpop.campfireRPG.placeholder.CampfirePlaceholderExpansion
import cg.headpop.campfireRPG.listener.CampfireBlockListener
import cg.headpop.campfireRPG.service.CampfireAuraService
import cg.headpop.campfireRPG.service.CampfireRegistry
import cg.headpop.campfireRPG.service.DiagnosticsService
import cg.headpop.campfireRPG.service.PlayerClassService
import org.bukkit.plugin.java.JavaPlugin

class CampfireRPG : JavaPlugin() {

    lateinit var settingsLoader: SettingsLoader
        private set

    lateinit var campfireRegistry: CampfireRegistry
        private set

    lateinit var auraService: CampfireAuraService
        private set

    lateinit var integrationService: IntegrationService
        private set

    lateinit var diagnosticsService: DiagnosticsService
        private set

    lateinit var adminMenuService: AdminMenuService
        private set

    lateinit var playerClassService: PlayerClassService
        private set

    private var placeholderExpansion: CampfirePlaceholderExpansion? = null

    override fun onEnable() {
        saveDefaultConfig()

        settingsLoader = SettingsLoader(this)
        campfireRegistry = CampfireRegistry(this)
        integrationService = IntegrationService(this)
        diagnosticsService = DiagnosticsService(this)
        adminMenuService = AdminMenuService(this)
        playerClassService = PlayerClassService(this)
        auraService = CampfireAuraService(this, campfireRegistry)

        reloadPlugin()

        server.pluginManager.registerEvents(CampfireBlockListener(campfireRegistry), this)
        server.pluginManager.registerEvents(AdminMenuListener(this), this)
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
        integrationService.reload()
        diagnosticsService.reload()
        playerClassService.reload()
        campfireRegistry.reload()
        auraService.reload()
        registerPlaceholderExpansion()
    }

    fun toggleConfigBoolean(path: String): Boolean {
        val nextValue = !config.getBoolean(path, false)
        config.set(path, nextValue)
        saveConfig()
        reloadPlugin()
        return nextValue
    }

    fun updateConfigNumber(path: String, delta: Number): Number {
        val current = config.get(path)
        val next = when (current) {
            is Int -> (current + delta.toInt()).coerceAtLeast(0)
            is Long -> (current + delta.toLong()).coerceAtLeast(0L)
            is Double -> (current + delta.toDouble()).coerceAtLeast(0.0)
            else -> delta
        }
        config.set(path, next)
        saveConfig()
        reloadPlugin()
        return next
    }

    fun setConfigValue(path: String, value: Any) {
        config.set(path, value)
        saveConfig()
        reloadPlugin()
    }

    private fun registerPlaceholderExpansion() {
        placeholderExpansion?.unregister()
        placeholderExpansion = null

        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return
        }

        placeholderExpansion = CampfirePlaceholderExpansion(this).also { it.register() }
    }
}
