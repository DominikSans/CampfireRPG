package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class AdminMenuService(
    private val plugin: CampfireRPG,
) {

    private val serializer = LegacyComponentSerializer.legacySection()

    fun open(player: Player) {
        val holder = AdminMenuHolder()
        val menuInventory = Bukkit.createInventory(holder, 45, serializer.deserialize(plugin.settingsLoader.settings.gui.title))
        holder.backingInventory = menuInventory
        val snapshot = plugin.diagnosticsService.snapshot()
        val integrations = plugin.integrationService.describeIntegrations()
        val settings = plugin.settingsLoader.settings

        fillBorders(menuInventory)

        menuInventory.setItem(10, createItem(Material.CAMPFIRE, "§6Campfire Status", listOf(
            "§7Tracked: §f${plugin.campfireRegistry.size()}",
            "§7Allowed worlds: §f${formatSet(settings.restrictions.allowedWorlds)}",
            "§7Blocked worlds: §f${formatSet(settings.restrictions.blockedWorlds)}",
            "§7Night only: §f${settings.night.onlyAtNight}",
        )))
        menuInventory.setItem(11, createItem(Material.BEACON, "§bPerformance", listOf(
            "§7Ticks: §f${snapshot.ticksProcessed}",
            "§7Players buffed: §f${snapshot.playersBuffed}",
            "§7Campfires activated: §f${snapshot.campfiresActivated}",
            "§7Avg tick: §f${"%.2f".format(snapshot.averageTickMs)} ms",
            "§7Max tick: §f${"%.2f".format(snapshot.maxTickMs)} ms",
        )))
        menuInventory.setItem(12, createItem(Material.NAME_TAG, "§dIntegrations", integrations.map { "§7- §f$it" }))
        menuInventory.setItem(13, createItem(Material.PLAYER_HEAD, "§aGrouping", listOf(
            "§7Same-group activation: §f${settings.integrations.requireSameGroupForActivation}",
            "§7Hero by group size: §f${settings.integrations.useGroupSizeForHeroBonus}",
            "§7Detected hooks: §f${plugin.integrationService.detectedGroupPluginNames().ifEmpty { listOf("none") }.joinToString()}",
        )))
        menuInventory.setItem(14, createItem(Material.COMPASS, "§eRegions", listOf(
            "§7WorldGuard hook: §f${plugin.integrationService.isWorldGuardEnabled()}",
            "§7Allowed regions: §f${formatSet(settings.restrictions.allowedRegions)}",
            "§7Blocked regions: §f${formatSet(settings.restrictions.blockedRegions)}",
        )))
        menuInventory.setItem(15, createItem(Material.WRITABLE_BOOK, "§fProfiles", settings.profiles.values.map {
            "§7- §f${it.id} §8(${it.material}, r=${it.radius})"
        }))
        menuInventory.setItem(16, createItem(Material.CLOCK, "§6Timing", listOf(
            "§7Tick interval: §f${settings.scan.intervalTicks}",
            "§7Registry rescan: §f${settings.scan.rescanIntervalTicks}",
            "§7Rest cycles: §f${settings.campfire.restCyclesRequired}",
            "§7Rest cooldown: §f${settings.campfire.restRewardCooldownTicks}",
        )))
        menuInventory.setItem(20, createItem(Material.REDSTONE_TORCH, "§cDebug", listOf(
            "§7Enabled: §f${plugin.diagnosticsService.isDebugEnabled()}",
            "§7Click to toggle debug logging",
        )))
        menuInventory.setItem(21, createItem(Material.EMERALD, "§aReload", listOf(
            "§7Click to reload configuration",
            "§7and rebuild caches",
        )))
        menuInventory.setItem(22, createItem(Material.SPYGLASS, "§bRescan Campfires", listOf(
            "§7Click to rescan loaded chunks",
            "§7and rebuild tracked campfires",
        )))
        menuInventory.setItem(23, createItem(Material.BOOK, "§eCommand Help", listOf(
            "§7/crpg status",
            "§7/crpg profiles",
            "§7/crpg integrations",
            "§7/crpg gui",
        )))
        menuInventory.setItem(24, createItem(Material.BARRIER, "§cClose", listOf(
            "§7Close this panel",
        )))

        player.openInventory(menuInventory)
    }

    private fun createItem(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(serializer.deserialize(name))
        meta.lore(lore.map(serializer::deserialize))
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        item.itemMeta = meta
        return item
    }

    private fun fillBorders(inventory: org.bukkit.inventory.Inventory) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta: ItemMeta = filler.itemMeta
        meta.displayName(serializer.deserialize("§8"))
        filler.itemMeta = meta

        val borderSlots = listOf(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
        )
        borderSlots.forEach { inventory.setItem(it, filler) }
    }

    private fun formatSet(values: Set<String>): String {
        return if (values.isEmpty()) "all" else values.joinToString()
    }
}
