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

        when {
            inSlots("previous", event.rawSlot, listOf(38)) -> openPage(player, holder.page - 1, holder.selectedProfileId)
            inSlots("next", event.rawSlot, listOf(42)) -> openPage(player, holder.page + 1, holder.selectedProfileId)
            inSlots("close-menu", event.rawSlot, listOf(40)) -> {
                player.closeInventory()
                return
            }
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
        when {
            inSlots("overview-debug", slot, listOf(30)) -> {
                val enabled = plugin.diagnosticsService.toggleDebug()
                player.sendMessage(if (enabled) plugin.settingsLoader.settings.messages.debugEnabled else plugin.settingsLoader.settings.messages.debugDisabled)
                openPage(player, holder.page, holder.selectedProfileId)
            }
            inSlots("overview-reload", slot, listOf(31)) -> {
                plugin.reloadPlugin()
                player.sendMessage(plugin.languageService.get("command.reload.done"))
                openPage(player, holder.page, holder.selectedProfileId)
            }
            inSlots("overview-rescan", slot, listOf(32)) -> {
                plugin.campfireRegistry.fullRescanLoadedChunks()
                player.sendMessage(plugin.languageService.get("gui.rescan.done", "count" to plugin.campfireRegistry.size().toString()))
                openPage(player, holder.page, holder.selectedProfileId)
            }
            inSlots("overview-help", slot, listOf(33)) -> {
                player.closeInventory()
                player.sendMessage(plugin.languageService.get("gui.commands_hint"))
            }
        }
    }

    private fun handleTogglePage(player: Player, slot: Int, holder: AdminMenuHolder) {
        when {
            inSlots("toggles-night-only", slot, listOf(20)) -> toggleAndReopen(player, "activation.night.only-at-night", "Night only", holder, "night.only-at-night")
            inSlots("toggles-same-group", slot, listOf(21)) -> toggleAndReopen(player, "integrations.groups.require-same-group-for-activation", "Same group activation", holder, "integrations.require-same-group-for-activation")
            inSlots("toggles-worldguard", slot, listOf(22)) -> toggleAndReopen(player, "integrations.hooks.worldguard", "WorldGuard hook", holder, "integrations.worldguard")
            inSlots("toggles-clans", slot, listOf(23)) -> toggleAndReopen(player, "integrations.hooks.clans", "Clan hooks", holder, "integrations.clans")
            inSlots("toggles-placeholderapi", slot, listOf(24)) -> toggleAndReopen(player, "integrations.hooks.placeholderapi", "PlaceholderAPI hook", holder, "integrations.placeholderapi")
            inSlots("toggles-hero-by-group", slot, listOf(29)) -> toggleAndReopen(player, "integrations.groups.use-group-size-for-hero-bonus", "Hero bonus by group size", holder, "integrations.use-group-size-for-hero-bonus")
            inSlots("toggles-xp-pulse", slot, listOf(30)) -> toggleAndReopen(player, "gameplay.support.experience-pulse.enabled", "Experience pulse", holder, "gameplay.experience-pulse.enabled")
            inSlots("toggles-cleanse", slot, listOf(31)) -> toggleAndReopen(player, "gameplay.support.cleanse.enabled", "Cleanse", holder, "gameplay.cleanse.enabled")
            inSlots("toggles-shared-heal", slot, listOf(32)) -> toggleAndReopen(player, "gameplay.support.shared-heal.enabled", "Shared heal", holder, "gameplay.shared-heal.enabled")
        }
    }

    private fun handleNumericPage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        val multiplier = if (click.isShiftClick) 5.0 else 1.0
        when {
            inSlots("numeric-xp-amount", slot, listOf(20)) -> adjustNumber(player, "gameplay.support.experience-pulse.amount", if (click.isLeftClick) -1 else 1, holder, "gameplay.experience-pulse.amount")
            inSlots("numeric-xp-cooldown", slot, listOf(21)) -> adjustNumber(player, "gameplay.support.experience-pulse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.experience-pulse.cooldown-ticks")
            inSlots("numeric-cleanse-cooldown", slot, listOf(22)) -> adjustNumber(player, "gameplay.support.cleanse.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.cleanse.cooldown-ticks")
            inSlots("numeric-shared-heal", slot, listOf(23)) -> adjustDouble(player, "gameplay.support.shared-heal.amount", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder, "gameplay.shared-heal.amount")
            inSlots("numeric-heal-cooldown", slot, listOf(24)) -> adjustNumber(player, "gameplay.support.shared-heal.cooldown-ticks", if (click.isLeftClick) -(20 * multiplier).toInt() else (20 * multiplier).toInt(), holder, "gameplay.shared-heal.cooldown-ticks")
            inSlots("numeric-required-players", slot, listOf(29)) -> adjustNumber(player, "activation.requirements.required-players", if (click.isLeftClick) -1 else 1, holder, "campfire.required-players")
            inSlots("numeric-bonus-threshold", slot, listOf(30)) -> adjustNumber(player, "activation.requirements.bonus-threshold", if (click.isLeftClick) -1 else 1, holder, "campfire.bonus-threshold")
            inSlots("numeric-rest-cycles", slot, listOf(31)) -> adjustNumber(player, "activation.rest.rest-cycles-required", if (click.isLeftClick) -1 else 1, holder, "campfire.rest-cycles-required")
            inSlots("numeric-rest-cooldown", slot, listOf(32)) -> adjustNumber(player, "activation.rest.rest-reward-cooldown-ticks", if (click.isLeftClick) -(100 * multiplier).toInt() else (100 * multiplier).toInt(), holder, "campfire.rest-reward-cooldown-ticks")
            inSlots("numeric-ward-radius", slot, listOf(33)) -> adjustDouble(player, "activation.requirements.monster-ward-radius", if (click.isLeftClick) -0.5 * multiplier else 0.5 * multiplier, holder, "campfire.monster-ward-radius")
        }
    }

    private fun handleClassesPage(player: Player, slot: Int, holder: AdminMenuHolder) {
        if (!plugin.settingsLoader.settings.classes.enabled) {
            return
        }
        val classes = plugin.settingsLoader.settings.classes.classes.keys.toList()
        val classSlots = plugin.guiConfigService.slotsFor("class-entry", listOf(20, 21, 22, 23, 24))
        val index = classSlots.indexOf(slot)
        if (index in classes.indices) {
            val classId = classes[index]
            if (plugin.playerClassService.setSelectedClass(player, classId)) {
                player.sendMessage(plugin.languageService.get("command.class.selected", "class" to classId))
                openPage(player, holder.page, holder.selectedProfileId)
            }
        }
    }

    private fun handleProfilePage(player: Player, slot: Int, click: ClickType, holder: AdminMenuHolder) {
        val effectSlots = plugin.guiConfigService.slotsFor("profile-effect", listOf(29, 30, 31, 32, 33))
        when {
            inSlots("profile-feed-players", slot, listOf(21)) -> toggleAndReopen(player, "profiles.${holder.selectedProfileId}.feed-players", "Feed players", holder)
            inSlots("profile-radius", slot, listOf(22)) -> adjustDouble(player, "profiles.${holder.selectedProfileId}.radius", if (click.isLeftClick) -0.5 else 0.5, holder)
            inSlots("profile-next-profile", slot, listOf(24)) -> {
                val nextProfile = nextProfileId(holder.selectedProfileId)
                openPage(player, holder.page, nextProfile)
            }
            slot in effectSlots -> {
                val effectIndex = effectSlots.indexOf(slot)
                val keys = plugin.runtimeConfigService.merged().getConfigurationSection("profiles.${holder.selectedProfileId}.effects")?.getKeys(false)?.toList().orEmpty()
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

    private fun inSlots(key: String, slot: Int, fallback: List<Int>): Boolean {
        return slot in plugin.guiConfigService.slotsFor(key, fallback)
    }

    private fun nextProfileId(current: String): String {
        val profiles = plugin.settingsLoader.settings.profiles.keys.toList()
        if (profiles.isEmpty()) {
            return current
        }
        val currentIndex = profiles.indexOf(current)
        return profiles[(if (currentIndex >= 0) currentIndex + 1 else 0) % profiles.size]
    }
}
