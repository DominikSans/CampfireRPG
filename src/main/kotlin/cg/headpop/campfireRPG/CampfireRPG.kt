package cg.headpop.campfireRPG

import cg.headpop.campfireRPG.command.CampfireRpgCommand
import cg.headpop.campfireRPG.config.SettingsLoader
import cg.headpop.campfireRPG.gui.AdminMenuService
import cg.headpop.campfireRPG.gui.GuiConfigService
import cg.headpop.campfireRPG.integration.IntegrationService
import cg.headpop.campfireRPG.lang.LanguageService
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

    lateinit var guiConfigService: GuiConfigService
        private set

    lateinit var playerClassService: PlayerClassService
        private set

    lateinit var languageService: LanguageService
        private set

    private var placeholderExpansion: CampfirePlaceholderExpansion? = null

    override fun onEnable() {
        saveDefaultConfig()

        settingsLoader = SettingsLoader(this)
        campfireRegistry = CampfireRegistry(this)
        integrationService = IntegrationService(this)
        diagnosticsService = DiagnosticsService(this)
        guiConfigService = GuiConfigService(this)
        adminMenuService = AdminMenuService(this)
        playerClassService = PlayerClassService(this)
        languageService = LanguageService(this)
        auraService = CampfireAuraService(this, campfireRegistry)

        reloadPlugin(fullRescan = true, showBanner = true)

        server.pluginManager.registerEvents(CampfireBlockListener(campfireRegistry), this)
        server.pluginManager.registerEvents(AdminMenuListener(this), this)
        val command = CampfireRpgCommand(this)
        getCommand("campfirerpg")?.setExecutor(command)
        getCommand("campfirerpg")?.tabCompleter = command
    }

    override fun onDisable() {
        auraService.stop()
        campfireRegistry.clear()
        printShutdownBanner()
    }

    fun reloadPlugin(fullRescan: Boolean = false, showBanner: Boolean = false) {
        reloadConfig()
        settingsLoader.reload()
        languageService.reload()
        guiConfigService.reload()
        integrationService.reload()
        diagnosticsService.reload()
        playerClassService.reload()
        if (fullRescan) {
            campfireRegistry.reload()
        }
        auraService.reload()
        registerPlaceholderExpansion()
        if (showBanner) {
            printStartupBanner()
        }
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

    private fun printStartupBanner() {
        val console = server.consoleSender
        val version = pluginMeta.version
        val author = pluginMeta.authors.joinToString(", ")
        console.sendMessage("§6   _____                       __ _          _____  _____   _____ ")
        console.sendMessage("§6  / ____|                     / _(_)        |  __ \\|  __ \\ / ____|")
        console.sendMessage("§e | |     __ _ _ __ ___  _ __ | |_ _ _ __ ___| |__) | |__) | |  __ ")
        console.sendMessage("§e | |    / _` | '_ ` _ \\| '_ \\|  _| | '__/ _ \\  _  /|  ___/| | |_ |")
        console.sendMessage("§c | |___| (_| | | | | | | |_) | | | | | |  __/ | \\ \\| |    | |__| |")
        console.sendMessage("§c  \\_____\\__,_|_| |_| |_| .__/|_| |_|_|  \\___|_|  \\_\\_|     \\_____|")
        console.sendMessage("§c                       | |                                        ")
        console.sendMessage("§c                       |_|                                        ")
        console.sendMessage("§7Version: §f$version")
        console.sendMessage("§7Author(s): §f$author")
    }

    private fun printShutdownBanner() {
        val console = server.consoleSender
        val version = pluginMeta.version
        val author = pluginMeta.authors.joinToString(", ")
        console.sendMessage("§8[§6CampfireRPG§8] §cPlugin disabled.")
        console.sendMessage("§7Version: §f$version §8| §7Author(s): §f$author")
    }
}
