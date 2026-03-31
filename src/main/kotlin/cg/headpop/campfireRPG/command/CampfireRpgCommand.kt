package cg.headpop.campfireRPG.command

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CampfireRpgCommand(
    private val plugin: CampfireRPG,
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.isEmpty() || args[0].equals("status", ignoreCase = true)) {
            sendStatus(sender)
            return true
        }

        if (args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender, label)
            return true
        }

        if (args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission("campfirerpg.admin")) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.noPermission)
                return true
            }

            plugin.reloadPlugin()
            sender.sendMessage("§aCampfireRPG recargado.")
            return true
        }

        if (args[0].equals("debug", ignoreCase = true)) {
            if (!sender.hasPermission("campfirerpg.admin")) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.noPermission)
                return true
            }

            val enabled = plugin.diagnosticsService.toggleDebug()
            sender.sendMessage(if (enabled) plugin.settingsLoader.settings.messages.debugEnabled else plugin.settingsLoader.settings.messages.debugDisabled)
            return true
        }

        if (args[0].equals("scan", ignoreCase = true)) {
            if (!sender.hasPermission("campfirerpg.admin")) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.noPermission)
                return true
            }

            plugin.campfireRegistry.fullRescanLoadedChunks()
            sender.sendMessage("§aCampfires cargados reescaneados. Total actual: §f${plugin.campfireRegistry.size()}")
            return true
        }

        if (args[0].equals("profiles", ignoreCase = true)) {
            val settings = plugin.settingsLoader.settings
            sender.sendMessage("§6CampfireRPG Profiles")
            settings.profiles.values.forEach { profile ->
                sender.sendMessage(
                    "§7- §f${profile.id} §8| §7material: §f${profile.material} §8| §7radius: §f${profile.radius} §8| §7effects: §f${profile.effects.size}"
                )
            }
            return true
        }

        if (args[0].equals("class", ignoreCase = true)) {
            val player = sender as? Player
            if (player == null) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.playerOnly)
                return true
            }

            if (args.size == 1 || args[1].equals("list", ignoreCase = true)) {
                sender.sendMessage("§6CampfireRPG Classes")
                plugin.settingsLoader.settings.classes.classes.values.forEach {
                    sender.sendMessage("§7- §f${it.id} §8| ${it.displayName} §8| §7perm: §f${it.permission}")
                }
                sender.sendMessage("§7Selected: §f${plugin.playerClassService.getSelectedClassId(player)}")
                sender.sendMessage("§7Effective: §f${plugin.playerClassService.getEffectiveClassId(player)}")
                return true
            }

            val classId = args[1].lowercase()
            if (!plugin.playerClassService.setSelectedClass(player, classId)) {
                player.sendMessage("§cClase invalida. Usa /$label class list")
                return true
            }

            player.sendMessage("§aClase seleccionada: §f$classId")
            return true
        }

        if (args[0].equals("integrations", ignoreCase = true)) {
            sender.sendMessage("§6CampfireRPG Integrations")
            plugin.integrationService.describeIntegrations().forEach { sender.sendMessage("§7- §f$it") }
            return true
        }

        if (args[0].equals("gui", ignoreCase = true)) {
            if (!sender.hasPermission("campfirerpg.admin")) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.noPermission)
                return true
            }

            val player = sender as? Player
            if (player == null) {
                sender.sendMessage(plugin.settingsLoader.settings.messages.playerOnly)
                return true
            }

            plugin.adminMenuService.open(player)
            player.sendMessage(plugin.settingsLoader.settings.messages.guiOpened)
            return true
        }

        sendHelp(sender, label)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        if (args.size != 1) {
            return mutableListOf()
        }

        val options = if (sender.hasPermission("campfirerpg.admin")) {
            listOf("help", "status", "profiles", "integrations", "class", "reload", "debug", "scan", "gui")
        } else {
            listOf("help", "status", "profiles", "integrations", "class")
        }

        if (args.size == 1) {
            return options.filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }

        if (args.size == 2 && args[0].equals("class", ignoreCase = true) && sender is Player) {
            val classOptions = mutableListOf("list")
            classOptions += plugin.settingsLoader.settings.classes.classes.keys
            return classOptions.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
        }

        return mutableListOf()
    }

    private fun sendStatus(sender: CommandSender) {
        val settings = plugin.settingsLoader.settings
        val diagnostics = plugin.diagnosticsService.snapshot()
        sender.sendMessage("§6CampfireRPG §7| §ftracked campfires: §e${plugin.campfireRegistry.size()}")
        sender.sendMessage("§7Tick interval: §f${settings.scan.intervalTicks} §7| Min players: §f${settings.campfire.requiredPlayers}")
        sender.sendMessage("§7Night only: §f${settings.night.onlyAtNight} §7| Profiles: §f${settings.profiles.keys.joinToString()}")
        sender.sendMessage("§7Avg tick: §f${"%.2f".format(diagnostics.averageTickMs)}ms §7| Max tick: §f${"%.2f".format(diagnostics.maxTickMs)}ms")
        sender.sendMessage("§7Integrations: §f${plugin.integrationService.describeIntegrations().joinToString(" | ")}")
    }

    private fun sendHelp(sender: CommandSender, label: String) {
        sender.sendMessage("§6CampfireRPG Commands")
        sender.sendMessage("§7/$label status §8- §festado general del plugin")
        sender.sendMessage("§7/$label profiles §8- §fperfiles de campfire activos")
        sender.sendMessage("§7/$label integrations §8- §fintegraciones detectadas")
        sender.sendMessage("§7/$label class [list|id] §8- §fselecciona tu clase de campfire")
        if (sender.hasPermission("campfirerpg.admin")) {
            sender.sendMessage("§7/$label gui §8- §fpanel administrativo")
            sender.sendMessage("§7/$label reload §8- §frecarga la configuracion")
            sender.sendMessage("§7/$label debug §8- §factiva o desactiva debug")
            sender.sendMessage("§7/$label scan §8- §freescanea campfires cargados")
        }
    }
}
