package cg.headpop.campfireRPG.listener

import cg.headpop.campfireRPG.CampfireRPG
import cg.headpop.campfireRPG.gui.AdminMenuHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class AdminMenuListener(
    private val plugin: CampfireRPG,
) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is AdminMenuHolder) {
            return
        }

        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission("campfirerpg.admin")) {
            return
        }

        when (event.rawSlot) {
            15 -> {
                val enabled = plugin.diagnosticsService.toggleDebug()
                player.sendMessage(if (enabled) plugin.settingsLoader.settings.messages.debugEnabled else plugin.settingsLoader.settings.messages.debugDisabled)
                plugin.adminMenuService.open(player)
            }
            16 -> {
                plugin.reloadPlugin()
                player.sendMessage("§aCampfireRPG recargado.")
                plugin.adminMenuService.open(player)
            }
        }
    }
}
