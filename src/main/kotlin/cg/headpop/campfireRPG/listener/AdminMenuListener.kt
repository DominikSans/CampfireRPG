package cg.headpop.campfireRPG.listener

import cg.headpop.campfireRPG.CampfireRPG
import cg.headpop.campfireRPG.gui.AdminMenuHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

class AdminMenuListener(
    private val plugin: CampfireRPG,
) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? AdminMenuHolder ?: return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission("campfirerpg.admin")) {
            return
        }

        when (event.rawSlot) {
            2 -> openPage(player, 0, holder.selectedProfileId)
            3 -> openPage(player, 1, holder.selectedProfileId)
            4 -> openPage(player, 2, holder.selectedProfileId)
            5 -> openPage(player, 3, holder.selectedProfileId)
            6 -> openPage(player, 4, holder.selectedProfileId)
            41 -> openPage(player, holder.page - 1, holder.selectedProfileId)
            43 -> openPage(player, holder.page + 1, holder.selectedProfileId)
        }

        when (holder.page) {
            0 -> handleOverviewPage(player, event.rawSlot, holder)
            1 -> handleTogglePage(player, event.rawSlot, holder)
            2 -> handleNumericPage(player, event.rawSlot, event.click, holder)
            3 -> handleClassesPage(player, event.rawSlot, holder)
            4 -> handleProfilePage(player, event.rawSlot, event.click, holder)
        }
    }

    private fun handleOverviewPage(player: Player, slot: Int, holder: AdminMenuHolder) {
        when (slot) {
            20 -> {
                val enabled = plugin.diagnosticsService.toggleDebug()
                player.sendMessage(if (enabled) plugin.settingsLoader.settings.messages.debugEnabled else plugin.settingsLoader.settings.messages.debugDisabled)
                openPage(player, holder.page, holder.selectedProfileId)
            }
            21 -> {
                plugin.reloadPlugin()
                player.sendMessage("§aCampfireRPG recargado.")
                openPage(player, holder.page, holder.selectedProfileId)
            }
            22 -> {
                plugin.campfireRegistry.fullRescanLoadedChunks()
                player.sendMessage("§aCampfires reescaneados. Total actual: §f${plugin.campfireRegistry.size()}")
                openPage(player, holder.page, holder.selectedProfileId)
            }
            23 -> {
                player.closeInventory()
                player.sendMessage("§6CampfireRPG §7commands: §f/crpg help")
            }
            24 -> player.closeInventory()
        }
    }

    private fun handleTogglePage(player: Player, slot: Int, holder: AdminMenuHolder) {
        when (slot) {
            10 -> toggleAndReopen(player, "night.only-at-night", "Night only", holder)
            11 -> toggleAndReopen(player, "integrations.require-same-group-for-activation", "Same group activation", holder)
            12 -> toggleAndReopen(player, "integrations.worldguard", "WorldGuard hook", holder)
            13 -> toggleAndReopen(player, "integrations.clans", "Clan hooks", holder)
            14 -> toggleAndReopen(player, "integrations.placeholderapi", "PlaceholderAPI hook", holder)
            15 -> toggleAndReopen(player, "integrations.use-group-size-for-hero-bonus", "Hero bonus by group size", holder)
            16 -> toggleAndReopen(player, "gameplay.experience-pulse.enabled", "Experience pulse", holder)
            19 -> toggleAndReopen(player, "gameplay.cleanse.enabled", "Cleanse", holder)
            20 -> toggleAndReopen(player, "gameplay.shared-heal.enabled", "Shared heal", holder)
        }
    }

    private fun handleNumericPage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        val multiplier = if (click.isShiftClick) 5.0 else 1.0
        when (slot) {
            10 -> adjustNumber(player, "gameplay.experience-pulse.amount", if (click.isLeftClick) -1 else 1, holder)
            11 -> adjustNumber(player, "gameplay.experience-pulse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder)
            12 -> adjustNumber(player, "gameplay.cleanse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder)
            13 -> adjustDouble(player, "gameplay.shared-heal.amount", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder)
            14 -> adjustNumber(player, "gameplay.shared-heal.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder)
            15 -> adjustNumber(player, "campfire.required-players", if (click.isLeftClick) -1 else 1, holder)
            16 -> adjustNumber(player, "campfire.bonus-threshold", if (click.isLeftClick) -1 else 1, holder)
            19 -> adjustNumber(player, "campfire.rest-cycles-required", if (click.isLeftClick) -1 else 1, holder)
            20 -> adjustNumber(player, "campfire.rest-reward-cooldown-ticks", if (click.isLeftClick) -(100 * multiplier).toInt() else (100 * multiplier).toInt(), holder)
            21 -> adjustDouble(player, "campfire.monster-ward-radius", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder)
        }
    }

    private fun handleClassesPage(player: Player, slot: Int, holder: AdminMenuHolder) {
        if (!plugin.settingsLoader.settings.classes.enabled) {
            return
        }
        val classes = plugin.settingsLoader.settings.classes.classes.keys.toList()
        if (slot in 10..14) {
            val index = slot - 10
            if (index < classes.size) {
                val classId = classes[index]
                if (plugin.playerClassService.setSelectedClass(player, classId)) {
                    player.sendMessage("§aClase seleccionada: §f$classId")
                    openPage(player, holder.page, holder.selectedProfileId)
                }
            }
        }
    }

    private fun handleProfilePage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        when (slot) {
            11 -> toggleAndReopen(player, "profiles.${holder.selectedProfileId}.feed-players", "Feed players", holder)
            12 -> adjustDouble(player, "profiles.${holder.selectedProfileId}.radius", if (click.isLeftClick) -0.5 else 0.5, holder)
            14 -> {
                val nextProfile = if (holder.selectedProfileId == "normal") "soul" else "normal"
                openPage(player, holder.page, nextProfile)
            }
            in 19..23 -> {
                val effectIndex = slot - 19
                val keys = plugin.config.getConfigurationSection("profiles.${holder.selectedProfileId}.effects")?.getKeys(false)?.toList().orEmpty()
                if (effectIndex >= keys.size) {
                    return
                }
                val effectKey = keys[effectIndex]
                if (click.isShiftClick) {
                    adjustNumber(player, "profiles.${holder.selectedProfileId}.effects.$effectKey.duration-ticks", if (click.isLeftClick) -20 else 20, holder)
                } else {
                    adjustNumber(player, "profiles.${holder.selectedProfileId}.effects.$effectKey.amplifier", if (click.isLeftClick) -1 else 1, holder)
                }
            }
        }
    }

    private fun toggleAndReopen(player: Player, path: String, label: String, holder: AdminMenuHolder) {
        val enabled = plugin.toggleConfigBoolean(path)
        player.sendMessage("§e$label: ${if (enabled) "§aenabled" else "§cdisabled"}")
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun adjustNumber(player: Player, path: String, delta: Int, holder: AdminMenuHolder) {
        val value = plugin.updateConfigNumber(path, delta)
        player.sendMessage("§e$path: §f$value")
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun adjustDouble(player: Player, path: String, delta: Double, holder: AdminMenuHolder) {
        val value = plugin.updateConfigNumber(path, delta)
        player.sendMessage("§e$path: §f$value")
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun openPage(player: Player, page: Int, selectedProfileId: String) {
        plugin.adminMenuService.open(player, page, selectedProfileId)
    }
}
