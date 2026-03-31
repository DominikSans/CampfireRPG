package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class AdminMenuService(
    private val plugin: CampfireRPG,
) {

    private val serializer = LegacyComponentSerializer.legacySection()

    fun open(player: Player) {
        val holder = AdminMenuHolder()
        val menuInventory = Bukkit.createInventory(holder, 27, serializer.deserialize(plugin.settingsLoader.settings.gui.title))
        holder.backingInventory = menuInventory
        val snapshot = plugin.diagnosticsService.snapshot()
        val integrations = plugin.integrationService.describeIntegrations()

        menuInventory.setItem(10, createItem(Material.CAMPFIRE, "§6Campfire Status", listOf(
            "§7Tracked: §f${plugin.campfireRegistry.size()}",
            "§7Allowed worlds: §f${formatSet(plugin.settingsLoader.settings.restrictions.allowedWorlds)}",
            "§7Blocked worlds: §f${formatSet(plugin.settingsLoader.settings.restrictions.blockedWorlds)}",
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
            "§7Same-group activation: §f${plugin.settingsLoader.settings.integrations.requireSameGroupForActivation}",
            "§7Hero by group size: §f${plugin.settingsLoader.settings.integrations.useGroupSizeForHeroBonus}",
            "§7Detected hooks: §f${plugin.integrationService.detectedGroupPluginNames().ifEmpty { listOf("none") }.joinToString()}",
        )))
        menuInventory.setItem(14, createItem(Material.COMPASS, "§eRegions", listOf(
            "§7WorldGuard hook: §f${plugin.integrationService.isWorldGuardEnabled()}",
            "§7Allowed regions: §f${formatSet(plugin.settingsLoader.settings.restrictions.allowedRegions)}",
            "§7Blocked regions: §f${formatSet(plugin.settingsLoader.settings.restrictions.blockedRegions)}",
        )))
        menuInventory.setItem(15, createItem(Material.REDSTONE_TORCH, "§cDebug", listOf(
            "§7Enabled: §f${plugin.diagnosticsService.isDebugEnabled()}",
            "§7Click to toggle debug logging",
        )))
        menuInventory.setItem(16, createItem(Material.EMERALD, "§aReload", listOf(
            "§7Click to reload configuration",
            "§7and rebuild caches",
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

    private fun formatSet(values: Set<String>): String {
        return if (values.isEmpty()) "all" else values.joinToString()
    }
}
