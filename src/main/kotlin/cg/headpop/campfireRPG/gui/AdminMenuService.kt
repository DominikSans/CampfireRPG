package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class AdminMenuService(
    private val plugin: CampfireRPG,
) {

    private val serializer = LegacyComponentSerializer.legacySection()

    fun open(player: Player, page: Int = 0, selectedProfileId: String = "normal") {
        val holder = AdminMenuHolder()
        holder.page = page.coerceIn(0, 4)
        holder.selectedProfileId = selectedProfileId
        val inventory = Bukkit.createInventory(holder, 45, serializer.deserialize(plugin.guiConfigService.title()))
        holder.backingInventory = inventory

        fillBorders(inventory)
        fillNavigation(inventory, holder.page)

        when (holder.page) {
            0 -> renderOverviewPage(inventory, player)
            1 -> renderTogglePage(inventory)
            2 -> renderNumericPage(inventory)
            3 -> renderClassesPage(inventory, player)
            4 -> renderProfileEditorPage(inventory, selectedProfileId)
        }

        player.openInventory(inventory)
    }

    private fun renderOverviewPage(inventory: Inventory, player: Player) {
        val snapshot = plugin.diagnosticsService.snapshot()
        val integrations = plugin.integrationService.describeIntegrations()
        val settings = plugin.settingsLoader.settings

        inventory.setItem(10, createItem(Material.CAMPFIRE, "§6Campfire Status", listOf(
            "§7Tracked: §f${plugin.campfireRegistry.size()}",
            "§7Allowed worlds: §f${formatSet(settings.restrictions.allowedWorlds)}",
            "§7Blocked worlds: §f${formatSet(settings.restrictions.blockedWorlds)}",
            "§7Night only: §f${settings.night.onlyAtNight}",
        )))
        inventory.setItem(11, createItem(Material.BEACON, "§bPerformance", listOf(
            "§7Ticks: §f${snapshot.ticksProcessed}",
            "§7Players buffed: §f${snapshot.playersBuffed}",
            "§7Campfires activated: §f${snapshot.campfiresActivated}",
            "§7Avg tick: §f${"%.2f".format(snapshot.averageTickMs)} ms",
            "§7Max tick: §f${"%.2f".format(snapshot.maxTickMs)} ms",
        )))
        inventory.setItem(12, createItem(Material.NAME_TAG, "§dIntegrations", integrations.map { "§7- §f$it" }))
        inventory.setItem(13, createItem(Material.COMPASS, "§eRegions", listOf(
            "§7WorldGuard hook: §f${plugin.integrationService.isWorldGuardEnabled()}",
            "§7Allowed regions: §f${formatSet(settings.restrictions.allowedRegions)}",
            "§7Blocked regions: §f${formatSet(settings.restrictions.blockedRegions)}",
        )))
        inventory.setItem(14, createItem(Material.EXPERIENCE_BOTTLE, "§aPlayer State", listOf(
            "§7Selected class: §f${plugin.playerClassService.getSelectedClassId(player)}",
            "§7Effective class: §f${plugin.auraService.getCurrentClassId(player)}",
            "§7Profile: §f${plugin.auraService.getCurrentProfileId(player)}",
            "§7Aura active: §f${plugin.auraService.isPlayerInActiveCampfire(player)}",
            "§7Campfire type: §f${plugin.auraService.getCurrentCampfireType(player)}",
        )))
        inventory.setItem(15, createItem(Material.BOOK, "§ePlaceholderAPI", listOf(
            "§7%campfirerpg_tracked_campfires%",
            "§7%campfirerpg_profile%",
            "§7%campfirerpg_class%",
            "§7%campfirerpg_aura_remaining%",
        )))
        inventory.setItem(20, createItem(Material.REDSTONE_TORCH, "§cDebug", listOf("§7Enabled: §f${plugin.diagnosticsService.isDebugEnabled()}", "§7Click to toggle debug logging")))
        inventory.setItem(21, createConfiguredItem("reload", Material.EMERALD, "§aReload", listOf("§7Reload configuration and caches")))
        inventory.setItem(22, createConfiguredItem("rescan", Material.SPYGLASS, "§bRescan Campfires", listOf("§7Rescan loaded chunks")))
        inventory.setItem(23, createConfiguredItem("help", Material.BOOK, "§eCommand Help", listOf("§7/crpg help", "§7/crpg gui")))
        inventory.setItem(24, createConfiguredItem("close", Material.BARRIER, "§cClose", listOf("§7Close this panel")))
    }

    private fun renderTogglePage(inventory: Inventory) {
        val settings = plugin.settingsLoader.settings
        inventory.setItem(10, createToggleItem("Night Only", settings.night.onlyAtNight, "Toggle night restriction"))
        inventory.setItem(11, createToggleItem("Same Group", settings.integrations.requireSameGroupForActivation, "Require same clan/party"))
        inventory.setItem(12, createToggleItem("WorldGuard Hook", settings.integrations.enableWorldGuard, "Toggle region filter"))
        inventory.setItem(13, createToggleItem("Clan Hooks", settings.integrations.enableClanHooks, "Toggle group detection"))
        inventory.setItem(14, createToggleItem("PlaceholderAPI", settings.integrations.enablePlaceholderApi, "Toggle placeholders"))
        inventory.setItem(15, createToggleItem("Hero by Group", settings.integrations.useGroupSizeForHeroBonus, "Toggle hero bonus source"))
        inventory.setItem(16, createToggleItem("XP Pulse", settings.gameplay.enableExperiencePulse, "Toggle experience pulse"))
        inventory.setItem(19, createToggleItem("Cleanse", settings.gameplay.enableCleanse, "Toggle debuff cleanse"))
        inventory.setItem(20, createToggleItem("Shared Heal", settings.gameplay.enableSharedHeal, "Toggle campfire healing"))
    }

    private fun renderNumericPage(inventory: Inventory) {
        val settings = plugin.settingsLoader.settings
        inventory.setItem(10, createNumericItem("XP Amount", settings.gameplay.experiencePulseAmount.toString(), "-1 / +1"))
        inventory.setItem(11, createNumericItem("XP Cooldown", settings.gameplay.experiencePulseCooldownTicks.toString(), "-20 / +20"))
        inventory.setItem(12, createNumericItem("Cleanse Cooldown", settings.gameplay.cleanseCooldownTicks.toString(), "-20 / +20"))
        inventory.setItem(13, createNumericItem("Shared Heal", settings.gameplay.sharedHealAmount.toString(), "-0.5 / +0.5"))
        inventory.setItem(14, createNumericItem("Heal Cooldown", settings.gameplay.sharedHealCooldownTicks.toString(), "-20 / +20"))
        inventory.setItem(15, createNumericItem("Required Players", settings.campfire.requiredPlayers.toString(), "-1 / +1"))
        inventory.setItem(16, createNumericItem("Bonus Threshold", settings.campfire.bonusThreshold.toString(), "-1 / +1"))
        inventory.setItem(19, createNumericItem("Rest Cycles", settings.campfire.restCyclesRequired.toString(), "-1 / +1"))
        inventory.setItem(20, createNumericItem("Rest Cooldown", settings.campfire.restRewardCooldownTicks.toString(), "-100 / +100"))
        inventory.setItem(21, createNumericItem("Ward Radius", settings.campfire.monsterWardRadius.toString(), "-0.5 / +0.5"))
        inventory.setItem(22, createItem(Material.OAK_SIGN, "§eUsage", listOf("§7Left click: decrease", "§7Right click: increase", "§7Shift click: stronger step")))
    }

    private fun renderClassesPage(inventory: Inventory, player: Player) {
        val settings = plugin.settingsLoader.settings
        if (!settings.classes.enabled) {
            inventory.setItem(10, createItem(Material.BARRIER, "§cInternal Classes Disabled", listOf(
                "§7This server relies on external class systems.",
                "§7CampfireRPG class perks are disabled by default.",
            )))
            inventory.setItem(11, createItem(Material.NAME_TAG, "§bUltimateClans", listOf(
                "§7Tag: §f${plugin.auraService.getClanTag(player)}",
                "§7Role: §f${plugin.auraService.getClanRole(player)}",
                "§7Size: §f${plugin.auraService.getClanSize(player)}",
                "§7Own territory: §f${plugin.auraService.isInOwnClanTerritory(player)}",
            )))
            inventory.setItem(20, createItem(Material.PAPER, "§bPlaceholders", listOf(
                "§7%campfirerpg_uclans_tag%",
                "§7%campfirerpg_uclans_role%",
                "§7%campfirerpg_uclans_size%",
                "§7%campfirerpg_uclans_own_territory%",
            )))
            return
        }

        val classes = settings.classes.classes.values.toList()
        val materials = listOf(Material.IRON_SWORD, Material.BLAZE_ROD, Material.BOW, Material.LEATHER_CHESTPLATE, Material.BOOK)

        classes.take(5).forEachIndexed { index, classPerk ->
            val selected = plugin.playerClassService.getSelectedClassId(player) == classPerk.id
            val marker = if (selected) "§a[Selected]" else "§7[Click to select]"
            inventory.setItem(10 + index, createItem(materials[index], classPerk.displayName, listOf(
                classPerk.description,
                "§7Permission: §f${classPerk.permission}",
                "§7Global effects: §f${classPerk.globalEffects.size}",
                "§7Normal perks: §f${classPerk.normalEffects.size}",
                "§7Soul perks: §f${classPerk.soulEffects.size}",
                marker,
            )))
        }

        inventory.setItem(19, createItem(Material.NETHER_STAR, "§6Default Class", listOf(
            "§7Current default: §f${settings.classes.defaultClassId}",
            "§7Selected class: §f${plugin.playerClassService.getSelectedClassId(player)}",
            "§7Effective class: §f${plugin.playerClassService.getEffectiveClassId(player)}",
        )))
        inventory.setItem(20, createItem(Material.PAPER, "§bPlaceholders", listOf(
            "§7%campfirerpg_tracked_campfires%",
            "§7%campfirerpg_debug%",
            "§7%campfirerpg_profile%",
            "§7%campfirerpg_class%",
            "§7%campfirerpg_class_display%",
            "§7%campfirerpg_active%",
            "§7%campfirerpg_aura_remaining%",
            "§7%campfirerpg_campfire_type%",
            "§7%campfirerpg_hero_bonus%",
        )))
    }

    private fun renderProfileEditorPage(inventory: Inventory, selectedProfileId: String) {
        val profile = plugin.settingsLoader.settings.profiles[selectedProfileId] ?: plugin.settingsLoader.settings.profiles.values.first()
        val section = plugin.config.getConfigurationSection("profiles.${profile.id}.effects")
        val effectKeys = section?.getKeys(false)?.toList().orEmpty()

        inventory.setItem(10, createItem(Material.CAMPFIRE, "§6Selected Profile", listOf(
            "§7Id: §f${profile.id}",
            "§7Material: §f${profile.material}",
            "§7Radius: §f${profile.radius}",
        )))
        inventory.setItem(11, createToggleItem("Feed Players", profile.feedPlayers, "Toggle feed-players for this profile"))
        inventory.setItem(12, createNumericItem("Profile Radius", profile.radius.toString(), "-0.5 / +0.5"))
        inventory.setItem(13, createItem(Material.OAK_SIGN, "§eEditor Usage", listOf(
            "§7Left click decrease",
            "§7Right click increase",
            "§7Shift = bigger change",
        )))
        inventory.setItem(14, createItem(Material.SOUL_CAMPFIRE, "§bSwap Profile", listOf("§7Click to switch normal/soul profile")))

        effectKeys.take(5).forEachIndexed { index, effectKey ->
            val effectPath = "profiles.${profile.id}.effects.$effectKey"
            val effectSection = plugin.config.getConfigurationSection(effectPath) ?: return@forEachIndexed
            inventory.setItem(19 + index, createItem(Material.POTION, "§dEffect: $effectKey", listOf(
                "§7Type: §f${effectSection.getString("type")}",
                "§7Amplifier: §f${effectSection.getInt("amplifier")}",
                "§7Duration: §f${effectSection.getInt("duration-ticks")}",
                "§8Left/right changes amplifier",
                "§8Shift-left/right changes duration",
            )))
        }
    }

    private fun fillNavigation(inventory: Inventory, page: Int) {
        inventory.setItem(2, createConfiguredItem("nav-overview", Material.MAP, "§eOverview", listOf("§7Page 1")))
        inventory.setItem(3, createConfiguredItem("nav-toggles", Material.LEVER, "§eToggles", listOf("§7Page 2")))
        inventory.setItem(4, createConfiguredItem("nav-numeric", Material.REPEATER, "§eNumeric Tuning", listOf("§7Page 3")))
        inventory.setItem(5, createConfiguredItem("nav-clan", Material.ENCHANTED_BOOK, "§eClan & Placeholders", listOf("§7Page 4")))
        inventory.setItem(6, createConfiguredItem("nav-profile", Material.BREWING_STAND, "§eProfile Editor", listOf("§7Page 5")))

        if (page > 0) {
            inventory.setItem(41, createConfiguredItem("previous", Material.ARROW, "§fPrevious Page", listOf("§7Go to page ${page}")))
        }
        if (page < 4) {
            inventory.setItem(43, createConfiguredItem("next", Material.ARROW, "§fNext Page", listOf("§7Go to page ${page + 2}")))
        }
    }

    private fun createItem(material: Material, name: String, lore: List<String>): ItemStack {
        return createItem(
            cg.headpop.campfireRPG.gui.GuiItemSpec(
                material = material,
                amount = 1,
                name = name,
                lore = lore,
                customModelData = null,
                glow = false,
                itemFlags = emptySet(),
                enchants = emptyMap(),
                skullOwner = null,
            )
        )
    }

    private fun createConfiguredItem(key: String, fallbackMaterial: Material, fallbackName: String, fallbackLore: List<String>): ItemStack {
        return createItem(plugin.guiConfigService.item(key, fallbackMaterial, fallbackName, fallbackLore))
    }

    private fun createItem(spec: cg.headpop.campfireRPG.gui.GuiItemSpec): ItemStack {
        val item = ItemStack(spec.material, spec.amount)
        val meta = item.itemMeta
        meta.displayName(serializer.deserialize(spec.name))
        meta.lore(spec.lore.map(serializer::deserialize))
        spec.customModelData?.let(meta::setCustomModelData)
        plugin.guiConfigService.applyExtraMeta(meta, spec)
        item.itemMeta = meta
        return item
    }

    private fun createToggleItem(name: String, enabled: Boolean, hint: String): ItemStack {
        val material = if (enabled) Material.LIME_DYE else Material.GRAY_DYE
        val state = if (enabled) "§aEnabled" else "§cDisabled"
        return createItem(material, "§e$name", listOf("§7State: $state", "§8$hint"))
    }

    private fun createNumericItem(name: String, value: String, controls: String): ItemStack {
        return createItem(Material.LIGHT_BLUE_DYE, "§b$name", listOf("§7Current: §f$value", "§8$controls"))
    }

    private fun fillBorders(inventory: Inventory) {
        val filler = createConfiguredItem("border", Material.GRAY_STAINED_GLASS_PANE, "§8", emptyList())

        val borderSlots = listOf(0, 1, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 42, 44)
        borderSlots.forEach { inventory.setItem(it, filler) }
    }

    private fun formatSet(values: Set<String>): String {
        return if (values.isEmpty()) "all" else values.joinToString()
    }
}
