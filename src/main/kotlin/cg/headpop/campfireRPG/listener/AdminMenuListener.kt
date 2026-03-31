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
            plugin.guiConfigService.slot("navigation.overview", 2) -> openPage(player, 0, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.toggles", 3) -> openPage(player, 1, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.numeric", 4) -> openPage(player, 2, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.clan", 5) -> openPage(player, 3, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.profile", 6) -> openPage(player, 4, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.previous", 41) -> openPage(player, holder.page - 1, holder.selectedProfileId)
            plugin.guiConfigService.slot("navigation.next", 43) -> openPage(player, holder.page + 1, holder.selectedProfileId)
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
            plugin.guiConfigService.slot("pages.overview.debug", 20) -> {
                val enabled = plugin.diagnosticsService.toggleDebug()
                player.sendMessage(if (enabled) plugin.settingsLoader.settings.messages.debugEnabled else plugin.settingsLoader.settings.messages.debugDisabled)
                openPage(player, holder.page, holder.selectedProfileId)
            }
            plugin.guiConfigService.slot("pages.overview.reload", 21) -> {
                plugin.reloadPlugin()
                player.sendMessage(plugin.languageService.get("command.reload.done"))
                openPage(player, holder.page, holder.selectedProfileId)
            }
            plugin.guiConfigService.slot("pages.overview.rescan", 22) -> {
                plugin.campfireRegistry.fullRescanLoadedChunks()
                player.sendMessage(plugin.languageService.get("gui.rescan.done", "count" to plugin.campfireRegistry.size().toString()))
                openPage(player, holder.page, holder.selectedProfileId)
            }
            plugin.guiConfigService.slot("pages.overview.help", 23) -> {
                player.closeInventory()
                player.sendMessage(plugin.languageService.get("gui.commands_hint"))
            }
            plugin.guiConfigService.slot("pages.overview.close", 24) -> player.closeInventory()
        }
    }

    private fun handleTogglePage(player: Player, slot: Int, holder: AdminMenuHolder) {
        when (slot) {
            plugin.guiConfigService.slot("pages.toggles.night-only", 10) -> toggleAndReopen(player, "activation.night.only-at-night", "Night only", holder, "night.only-at-night")
            plugin.guiConfigService.slot("pages.toggles.same-group", 11) -> toggleAndReopen(player, "integrations.groups.require-same-group-for-activation", "Same group activation", holder, "integrations.require-same-group-for-activation")
            plugin.guiConfigService.slot("pages.toggles.worldguard", 12) -> toggleAndReopen(player, "integrations.hooks.worldguard", "WorldGuard hook", holder, "integrations.worldguard")
            plugin.guiConfigService.slot("pages.toggles.clans", 13) -> toggleAndReopen(player, "integrations.hooks.clans", "Clan hooks", holder, "integrations.clans")
            plugin.guiConfigService.slot("pages.toggles.placeholderapi", 14) -> toggleAndReopen(player, "integrations.hooks.placeholderapi", "PlaceholderAPI hook", holder, "integrations.placeholderapi")
            plugin.guiConfigService.slot("pages.toggles.hero-by-group", 15) -> toggleAndReopen(player, "integrations.groups.use-group-size-for-hero-bonus", "Hero bonus by group size", holder, "integrations.use-group-size-for-hero-bonus")
            plugin.guiConfigService.slot("pages.toggles.xp-pulse", 16) -> toggleAndReopen(player, "gameplay.support.experience-pulse.enabled", "Experience pulse", holder, "gameplay.experience-pulse.enabled")
            plugin.guiConfigService.slot("pages.toggles.cleanse", 19) -> toggleAndReopen(player, "gameplay.support.cleanse.enabled", "Cleanse", holder, "gameplay.cleanse.enabled")
            plugin.guiConfigService.slot("pages.toggles.shared-heal", 20) -> toggleAndReopen(player, "gameplay.support.shared-heal.enabled", "Shared heal", holder, "gameplay.shared-heal.enabled")
        }
    }

    private fun handleNumericPage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        val multiplier = if (click.isShiftClick) 5.0 else 1.0
        when (slot) {
            plugin.guiConfigService.slot("pages.numeric.xp-amount", 10) -> adjustNumber(player, "gameplay.support.experience-pulse.amount", if (click.isLeftClick) -1 else 1, holder, "gameplay.experience-pulse.amount")
            plugin.guiConfigService.slot("pages.numeric.xp-cooldown", 11) -> adjustNumber(player, "gameplay.support.experience-pulse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.experience-pulse.cooldown-ticks")
            plugin.guiConfigService.slot("pages.numeric.cleanse-cooldown", 12) -> adjustNumber(player, "gameplay.support.cleanse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.cleanse.cooldown-ticks")
            plugin.guiConfigService.slot("pages.numeric.shared-heal", 13) -> adjustDouble(player, "gameplay.support.shared-heal.amount", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder, "gameplay.shared-heal.amount")
            plugin.guiConfigService.slot("pages.numeric.heal-cooldown", 14) -> adjustNumber(player, "gameplay.support.shared-heal.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.shared-heal.cooldown-ticks")
            plugin.guiConfigService.slot("pages.numeric.required-players", 15) -> adjustNumber(player, "activation.requirements.required-players", if (click.isLeftClick) -1 else 1, holder, "campfire.required-players")
            plugin.guiConfigService.slot("pages.numeric.bonus-threshold", 16) -> adjustNumber(player, "activation.requirements.bonus-threshold", if (click.isLeftClick) -1 else 1, holder, "campfire.bonus-threshold")
            plugin.guiConfigService.slot("pages.numeric.rest-cycles", 19) -> adjustNumber(player, "activation.rest.rest-cycles-required", if (click.isLeftClick) -1 else 1, holder, "campfire.rest-cycles-required")
            plugin.guiConfigService.slot("pages.numeric.rest-cooldown", 20) -> adjustNumber(player, "activation.rest.rest-reward-cooldown-ticks", if (click.isLeftClick) -(100 * multiplier).toInt() else (100 * multiplier).toInt(), holder, "campfire.rest-reward-cooldown-ticks")
            plugin.guiConfigService.slot("pages.numeric.ward-radius", 21) -> adjustDouble(player, "activation.requirements.monster-ward-radius", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder, "campfire.monster-ward-radius")
        }
    }

    private fun handleClassesPage(player: Player, slot: Int, holder: AdminMenuHolder) {
        if (!plugin.settingsLoader.settings.classes.enabled) {
            return
        }
        val classes = plugin.settingsLoader.settings.classes.classes.keys.toList()
        val classSlots = plugin.guiConfigService.slots("pages.classes.class-slots", listOf(10, 11, 12, 13, 14))
        val index = classSlots.indexOf(slot)
        if (index >= 0 && index < classes.size) {
                val classId = classes[index]
                if (plugin.playerClassService.setSelectedClass(player, classId)) {
                    player.sendMessage(plugin.languageService.get("command.class.selected", "class" to classId))
                    openPage(player, holder.page, holder.selectedProfileId)
                }
        }
    }

    private fun handleProfilePage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        val effectSlots = plugin.guiConfigService.slots("pages.profile.effect-slots", listOf(19, 20, 21, 22, 23))
        when (slot) {
            plugin.guiConfigService.slot("pages.profile.feed-players", 11) -> toggleAndReopen(player, "profiles.${holder.selectedProfileId}.feed-players", "Feed players", holder)
            plugin.guiConfigService.slot("pages.profile.radius", 12) -> adjustDouble(player, "profiles.${holder.selectedProfileId}.radius", if (click.isLeftClick) -0.5 else 0.5, holder)
            plugin.guiConfigService.slot("pages.profile.swap-profile", 14) -> {
                val nextProfile = if (holder.selectedProfileId == "normal") "soul" else "normal"
                openPage(player, holder.page, nextProfile)
            }
            in effectSlots -> {
                val effectIndex = effectSlots.indexOf(slot)
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

    private fun toggleAndReopen(player: Player, primaryPath: String, label: String, holder: AdminMenuHolder, legacyPath: String? = null) {
        val enabled = plugin.toggleConfigBoolean(primaryPath, legacyPath)
        player.sendMessage(plugin.languageService.get(
            "toggle.state",
            "label" to label,
            "state" to plugin.languageService.get(if (enabled) "toggle.enabled" else "toggle.disabled"),
        ))
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun adjustNumber(player: Player, primaryPath: String, delta: Int, holder: AdminMenuHolder, legacyPath: String? = null) {
        val value = plugin.updateConfigNumber(primaryPath, delta, legacyPath)
        player.sendMessage("§e$primaryPath: §f$value")
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun adjustDouble(player: Player, primaryPath: String, delta: Double, holder: AdminMenuHolder, legacyPath: String? = null) {
        val value = plugin.updateConfigNumber(primaryPath, delta, legacyPath)
        player.sendMessage("§e$primaryPath: §f$value")
        openPage(player, holder.page, holder.selectedProfileId)
    }

    private fun openPage(player: Player, page: Int, selectedProfileId: String) {
        plugin.adminMenuService.open(player, page, selectedProfileId)
    }
}
