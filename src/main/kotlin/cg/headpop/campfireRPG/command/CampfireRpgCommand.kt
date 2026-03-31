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
            val settings = plugin.settingsLoader.settings
            val diagnostics = plugin.diagnosticsService.snapshot()
            sender.sendMessage("§6CampfireRPG §7| §ftracked campfires: §e${plugin.campfireRegistry.size()}")
            sender.sendMessage("§7Tick interval: §f${settings.scan.intervalTicks} §7| Min players: §f${settings.campfire.requiredPlayers}")
            sender.sendMessage("§7Night only: §f${settings.night.onlyAtNight} §7| Profiles: §f${settings.profiles.keys.joinToString()}")
            sender.sendMessage("§7Avg tick: §f${"%.2f".format(diagnostics.averageTickMs)}ms §7| Max tick: §f${"%.2f".format(diagnostics.maxTickMs)}ms")
            sender.sendMessage("§7Integrations: §f${plugin.integrationService.describeIntegrations().joinToString(" | ")}")
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

        sender.sendMessage("§eUso: /$label [status|reload|debug|gui]")
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
            listOf("status", "reload", "debug", "gui")
        } else {
            listOf("status")
        }

        return options.filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
    }
}
