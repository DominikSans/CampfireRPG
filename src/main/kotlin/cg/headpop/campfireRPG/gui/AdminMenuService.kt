package cg.headpop.campfireRPG.gui

import cg.headpop.campfireRPG.CampfireRPG
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class AdminMenuService(
    private val plugin: CampfireRPG,
) {

    private val serializer = LegacyComponentSerializer.legacySection()

    fun open(player: Player, page: Int = 0, selectedProfileId: String = defaultProfileId()) {
        val holder = AdminMenuHolder()
        holder.page = page.coerceIn(0, 4)
        holder.selectedProfileId = selectedProfileId.takeIf { it in plugin.settingsLoader.settings.profiles } ?: defaultProfileId()
        val inventory = Bukkit.createInventory(holder, plugin.guiConfigService.rows() * 9, serializer.deserialize(plugin.guiConfigService.title()))
        holder.backingInventory = inventory

        fillDecorations(inventory)
        fillNavigation(inventory, holder.page)

        when (holder.page) {
            0 -> {
                renderPageInfo(inventory, "Campfire Overview", listOf("§7Core plugin status and quick actions"))
                renderOverviewPage(inventory, player)
            }
            1 -> {
                renderPageInfo(inventory, "Core Toggles", listOf("§7Enable or disable the base mechanics"))
                renderTogglePage(inventory)
            }
            2 -> {
                renderPageInfo(inventory, "Numeric Tuning", listOf("§7Adjust values for support and rest"))
                renderNumericPage(inventory)
            }
            3 -> {
                renderPageInfo(inventory, "Groups & Extras", listOf("§7Classes, clans and optional integrations"))
                renderClassesPage(inventory, player)
            }
            4 -> {
                renderPageInfo(inventory, "Profile Editor", listOf("§7Cycle campfire profiles and tweak effects"))
                renderProfileEditorPage(inventory, holder.selectedProfileId)
            }
        }

        player.openInventory(inventory)
    }

    private fun renderOverviewPage(inventory: Inventory, player: Player) {
        val snapshot = plugin.diagnosticsService.snapshot()
        val integrations = plugin.integrationService.describeIntegrations()
        val settings = plugin.settingsLoader.settings

        placeConfiguredItem(inventory, fallback("overview-status", Material.CAMPFIRE, "§6Campfire Status", listOf(
            "§7Tracked: §f${plugin.campfireRegistry.size()}",
            "§7Allowed worlds: §f${formatSet(settings.restrictions.allowedWorlds)}",
            "§7Blocked worlds: §f${formatSet(settings.restrictions.blockedWorlds)}",
            "§7Night only: §f${settings.night.onlyAtNight}",
        ), listOf(20)))
        placeConfiguredItem(inventory, fallback("overview-performance", Material.BEACON, "§bPerformance", listOf(
            "§7Ticks: §f${snapshot.ticksProcessed}",
            "§7Players buffed: §f${snapshot.playersBuffed}",
            "§7Campfires activated: §f${snapshot.campfiresActivated}",
            "§7Avg tick: §f${"%.2f".format(snapshot.averageTickMs)} ms",
            "§7Max tick: §f${"%.2f".format(snapshot.maxTickMs)} ms",
        ), listOf(21)))
        placeConfiguredItem(inventory, fallback("overview-integrations", Material.NAME_TAG, "§dIntegrations", integrations.map { "§7- §f$it" }, listOf(22)))
        placeConfiguredItem(inventory, fallback("overview-regions", Material.COMPASS, "§eRegions", listOf(
            "§7WorldGuard hook: §f${plugin.integrationService.isWorldGuardEnabled()}",
            "§7Allowed regions: §f${formatSet(settings.restrictions.allowedRegions)}",
            "§7Blocked regions: §f${formatSet(settings.restrictions.blockedRegions)}",
        ), listOf(23)))
        placeConfiguredItem(inventory, fallback("overview-player-state", Material.EXPERIENCE_BOTTLE, "§aPlayer State", listOf(
            "§7Selected class: §f${plugin.playerClassService.getSelectedClassId(player)}",
            "§7Effective class: §f${plugin.auraService.getCurrentClassId(player)}",
            "§7Profile: §f${plugin.auraService.getCurrentProfileId(player)}",
            "§7Aura active: §f${plugin.auraService.isPlayerInActiveCampfire(player)}",
            "§7Campfire type: §f${plugin.auraService.getCurrentCampfireType(player)}",
        ), listOf(24)))
        placeConfiguredItem(inventory, fallback("overview-placeholders", Material.BOOK, "§ePlaceholderAPI", listOf(
            "§7%campfirerpg_tracked_campfires%",
            "§7%campfirerpg_profile%",
            "§7%campfirerpg_class%",
            "§7%campfirerpg_aura_remaining%",
        ), listOf(29)))
        placeConfiguredItem(inventory, fallback("overview-debug", Material.REDSTONE_TORCH, "§cDebug", listOf(
            "§7Enabled: §f${plugin.diagnosticsService.isDebugEnabled()}",
            "§7Click to toggle debug logging",
        ), listOf(30)))
        placeConfiguredItem(inventory, fallback("overview-reload", Material.EMERALD, "§aReload", listOf("§7Reload configuration and caches"), listOf(31), glow = true))
        placeConfiguredItem(inventory, fallback("overview-rescan", Material.SPYGLASS, "§bRescan Campfires", listOf("§7Rescan loaded chunks"), listOf(32)))
        placeConfiguredItem(inventory, fallback("overview-help", Material.BOOK, "§eCommand Help", listOf("§7/crpg help", "§7/crpg gui"), listOf(33)))
    }

    private fun renderTogglePage(inventory: Inventory) {
        val settings = plugin.settingsLoader.settings
        placeConfiguredItem(inventory, fallback("toggles-night-only", toggleMaterial(settings.night.onlyAtNight), toggleTitle("Night Only"), toggleLore(settings.night.onlyAtNight, "Toggle night restriction"), listOf(20)))
        placeConfiguredItem(inventory, fallback("toggles-same-group", toggleMaterial(settings.integrations.requireSameGroupForActivation), toggleTitle("Same Group"), toggleLore(settings.integrations.requireSameGroupForActivation, "Require same clan/party"), listOf(21)))
        placeConfiguredItem(inventory, fallback("toggles-worldguard", toggleMaterial(settings.integrations.enableWorldGuard), toggleTitle("WorldGuard Hook"), toggleLore(settings.integrations.enableWorldGuard, "Toggle region filter"), listOf(22)))
        placeConfiguredItem(inventory, fallback("toggles-clans", toggleMaterial(settings.integrations.enableClanHooks), toggleTitle("Clan Hooks"), toggleLore(settings.integrations.enableClanHooks, "Toggle clan and party detection"), listOf(23)))
        placeConfiguredItem(inventory, fallback("toggles-placeholderapi", toggleMaterial(settings.integrations.enablePlaceholderApi), toggleTitle("PlaceholderAPI"), toggleLore(settings.integrations.enablePlaceholderApi, "Toggle placeholders"), listOf(24)))
        placeConfiguredItem(inventory, fallback("toggles-hero-by-group", toggleMaterial(settings.integrations.useGroupSizeForHeroBonus), toggleTitle("Hero by Group"), toggleLore(settings.integrations.useGroupSizeForHeroBonus, "Use group size for hero bonus"), listOf(29)))
        placeConfiguredItem(inventory, fallback("toggles-xp-pulse", toggleMaterial(settings.gameplay.enableExperiencePulse), toggleTitle("XP Pulse"), toggleLore(settings.gameplay.enableExperiencePulse, "Toggle experience pulse"), listOf(30)))
        placeConfiguredItem(inventory, fallback("toggles-cleanse", toggleMaterial(settings.gameplay.enableCleanse), toggleTitle("Cleanse"), toggleLore(settings.gameplay.enableCleanse, "Toggle debuff cleanse"), listOf(31)))
        placeConfiguredItem(inventory, fallback("toggles-shared-heal", toggleMaterial(settings.gameplay.enableSharedHeal), toggleTitle("Shared Heal"), toggleLore(settings.gameplay.enableSharedHeal, "Toggle campfire healing"), listOf(32)))
    }

    private fun renderNumericPage(inventory: Inventory) {
        val settings = plugin.settingsLoader.settings
        placeConfiguredItem(inventory, fallback("numeric-xp-amount", Material.PAPER, "§bXP Amount", listOf("§7Current: §f${settings.gameplay.experiencePulseAmount}", "§8-1 / +1"), listOf(20)))
        placeConfiguredItem(inventory, fallback("numeric-xp-cooldown", Material.PAPER, "§bXP Cooldown", listOf("§7Current: §f${settings.gameplay.experiencePulseCooldownTicks}", "§8-20 / +20"), listOf(21)))
        placeConfiguredItem(inventory, fallback("numeric-cleanse-cooldown", Material.PAPER, "§bCleanse Cooldown", listOf("§7Current: §f${settings.gameplay.cleanseCooldownTicks}", "§8-20 / +20"), listOf(22)))
        placeConfiguredItem(inventory, fallback("numeric-shared-heal", Material.PAPER, "§bShared Heal", listOf("§7Current: §f${settings.gameplay.sharedHealAmount}", "§8-0.5 / +0.5"), listOf(23)))
        placeConfiguredItem(inventory, fallback("numeric-heal-cooldown", Material.PAPER, "§bHeal Cooldown", listOf("§7Current: §f${settings.gameplay.sharedHealCooldownTicks}", "§8-20 / +20"), listOf(24)))
        placeConfiguredItem(inventory, fallback("numeric-required-players", Material.PAPER, "§bRequired Players", listOf("§7Current: §f${settings.campfire.requiredPlayers}", "§8-1 / +1"), listOf(29)))
        placeConfiguredItem(inventory, fallback("numeric-bonus-threshold", Material.PAPER, "§bBonus Threshold", listOf("§7Current: §f${settings.campfire.bonusThreshold}", "§8-1 / +1"), listOf(30)))
        placeConfiguredItem(inventory, fallback("numeric-rest-cycles", Material.PAPER, "§bRest Cycles", listOf("§7Current: §f${settings.campfire.restCyclesRequired}", "§8-1 / +1"), listOf(31)))
        placeConfiguredItem(inventory, fallback("numeric-rest-cooldown", Material.PAPER, "§bRest Cooldown", listOf("§7Current: §f${settings.campfire.restRewardCooldownTicks}", "§8-100 / +100"), listOf(32)))
        placeConfiguredItem(inventory, fallback("numeric-ward-radius", Material.PAPER, "§bWard Radius", listOf("§7Current: §f${settings.campfire.monsterWardRadius}", "§8-0.5 / +0.5"), listOf(33)))
    }

    private fun renderClassesPage(inventory: Inventory, player: Player) {
        val settings = plugin.settingsLoader.settings
        if (!settings.classes.enabled) {
            placeConfiguredItem(inventory, fallback("classes-disabled", Material.BARRIER, "§cAdvanced systems disabled", listOf(
                "§7Internal classes: §f${settings.classes.enabled}",
                "§7Clan features: §f${settings.clanFeatures.enabled}",
                "§7Group hooks: §f${settings.integrations.enableClanHooks}",
                "§7Base mode focuses on campfire support only.",
            ), listOf(20)))
            placeConfiguredItem(inventory, fallback("classes-clan-info", Material.COMPASS, "§bAdvanced hooks", listOf(
                "§7Detected: §f${plugin.integrationService.detectedGroupPluginNames().ifEmpty { listOf("none") }.joinToString()}",
                "§7WorldGuard: §f${plugin.integrationService.isWorldGuardEnabled()}",
                "§7PlaceholderAPI: §f${settings.integrations.enablePlaceholderApi}",
                "§7Use this page only when enabling advanced features.",
            ), listOf(21)))
            placeConfiguredItem(inventory, fallback("classes-placeholders", Material.PAPER, "§bPlaceholders", listOf(
                "§7%campfirerpg_tracked_campfires%",
                "§7%campfirerpg_profile%",
                "§7%campfirerpg_active%",
                "§7%campfirerpg_aura_remaining%",
                "§7%campfirerpg_uclans_tag%",
                "§7%campfirerpg_uclans_role%",
                "§7%campfirerpg_uclans_size%",
                "§7Advanced placeholders require hooks.",
            ), listOf(22)))
            return
        }

        val classSlots = plugin.guiConfigService.slotsFor("class-entry", listOf(20, 21, 22, 23, 24))
        val classes = settings.classes.classes.values.toList()
        val materials = listOf(Material.IRON_SWORD, Material.BLAZE_ROD, Material.BOW, Material.LEATHER_CHESTPLATE, Material.BOOK)

        classes.take(classSlots.size).forEachIndexed { index, classPerk ->
            val selected = plugin.playerClassService.getSelectedClassId(player) == classPerk.id
            val marker = if (selected) "§a[Selected]" else "§7[Click to select]"
            placeSpecificItem(inventory, fallback("class-entry", materials.getOrElse(index) { Material.BOOK }, classPerk.displayName, listOf(
                classPerk.description,
                "§7Permission: §f${classPerk.permission}",
                "§7Global effects: §f${classPerk.globalEffects.size}",
                "§7Normal perks: §f${classPerk.normalEffects.size}",
                "§7Soul perks: §f${classPerk.soulEffects.size}",
                marker,
            ), classSlots), classSlots[index])
        }

        placeConfiguredItem(inventory, fallback("classes-default-class", Material.NETHER_STAR, "§6Default Class", listOf(
            "§7Current default: §f${settings.classes.defaultClassId}",
            "§7Selected class: §f${plugin.playerClassService.getSelectedClassId(player)}",
            "§7Effective class: §f${plugin.playerClassService.getEffectiveClassId(player)}",
        ), listOf(29)))
        placeConfiguredItem(inventory, fallback("classes-placeholders", Material.PAPER, "§bPlaceholders", listOf(
            "§7%campfirerpg_tracked_campfires%",
            "§7%campfirerpg_debug%",
            "§7%campfirerpg_profile%",
            "§7%campfirerpg_class%",
            "§7%campfirerpg_class_display%",
            "§7%campfirerpg_active%",
            "§7%campfirerpg_aura_remaining%",
            "§7%campfirerpg_campfire_type%",
            "§7%campfirerpg_hero_bonus%",
        ), listOf(30)))
    }

    private fun renderProfileEditorPage(inventory: Inventory, selectedProfileId: String) {
        val profileIds = plugin.settingsLoader.settings.profiles.keys.toList()
        val profileId = selectedProfileId.takeIf { it in plugin.settingsLoader.settings.profiles } ?: profileIds.first()
        val profile = plugin.settingsLoader.settings.profiles.getValue(profileId)
        val section = plugin.runtimeConfigService.merged().getConfigurationSection("profiles.$profileId.effects")
        val effectKeys = section?.getKeys(false)?.toList().orEmpty()
        val effectSlots = plugin.guiConfigService.slotsFor("profile-effect", listOf(29, 30, 31, 32, 33))

        placeConfiguredItem(inventory, fallback("profile-selected", Material.CAMPFIRE, "§6Selected Profile", listOf(
            "§7Id: §f${profile.id}",
            "§7Material: §f${profile.material}",
            "§7Radius: §f${profile.radius}",
            "§7Available: §f${profileIds.joinToString()}",
        ), listOf(20)))
        placeConfiguredItem(inventory, fallback("profile-feed-players", toggleMaterial(profile.feedPlayers), toggleTitle("Feed Players"), toggleLore(profile.feedPlayers, "Toggle feed-players for this profile"), listOf(21)))
        placeConfiguredItem(inventory, fallback("profile-radius", Material.PAPER, "§bProfile Radius", listOf("§7Current: §f${profile.radius}", "§8-0.5 / +0.5"), listOf(22)))
        placeConfiguredItem(inventory, fallback("profile-usage", Material.OAK_SIGN, "§eEditor Usage", listOf(
            "§7Left click decrease",
            "§7Right click increase",
            "§7Shift = bigger change",
        ), listOf(23)))
        placeConfiguredItem(inventory, fallback("profile-next-profile", Material.SOUL_CAMPFIRE, "§bNext Profile", listOf(
            "§7Current: §f$profileId",
            "§7Click to cycle available profiles",
        ), listOf(24)))

        effectKeys.take(effectSlots.size).forEachIndexed { index, effectKey ->
            val effectPath = "profiles.$profileId.effects.$effectKey"
            val effectSection = plugin.runtimeConfigService.merged().getConfigurationSection(effectPath) ?: return@forEachIndexed
            placeSpecificItem(inventory, fallback("profile-effect", Material.POTION, "§dEffect: $effectKey", listOf(
                "§7Type: §f${effectSection.getString("type")}",
                "§7Amplifier: §f${effectSection.getInt("amplifier")}",
                "§7Duration: §f${effectSection.getInt("duration-ticks")}",
                "§8Left/right changes amplifier",
                "§8Shift-left/right changes duration",
            ), effectSlots), effectSlots[index])
        }
    }

    private fun fillNavigation(inventory: Inventory, page: Int) {
        if (page > 0) {
            placeConfiguredItem(inventory, fallback("previous", Material.ARROW, "§fPrevious Page", listOf("§7Go to previous page"), listOf(38)))
        }
        if (page < 4) {
            placeConfiguredItem(inventory, fallback("next", Material.ARROW, "§fNext Page", listOf("§7Go to next page"), listOf(42)))
        }
        placeConfiguredItem(inventory, fallback("close-menu", Material.BARRIER, "§cClose Menu", listOf("§7Close this panel"), listOf(40)))
    }

    private fun renderPageInfo(inventory: Inventory, title: String, lore: List<String>) {
        placeConfiguredItem(inventory, fallback("page-info", Material.PIGLIN_HEAD, "§6$title", lore, listOf(13)))
    }

    private fun fillDecorations(inventory: Inventory) {
        plugin.guiConfigService.decorationItems().forEach { spec ->
            placeConfiguredItem(inventory, spec)
        }
    }

    private fun placeConfiguredItem(inventory: Inventory, fallback: GuiItemSpec) {
        val spec = plugin.guiConfigService.item(fallback.key, fallback)
        val item = createItem(spec)
        spec.slots.filter { it in 0 until inventory.size }.forEach { inventory.setItem(it, item.clone()) }
    }

    private fun placeSpecificItem(inventory: Inventory, fallback: GuiItemSpec, slot: Int) {
        if (slot !in 0 until inventory.size) {
            return
        }
        val spec = plugin.guiConfigService.item(fallback.key, fallback)
        inventory.setItem(slot, createItem(spec))
    }

    private fun createItem(spec: GuiItemSpec): ItemStack {
        val item = ItemStack(spec.material, spec.amount)
        val meta = item.itemMeta
        meta.displayName(serializer.deserialize(spec.name))
        meta.lore(spec.lore.map(serializer::deserialize))
        plugin.guiConfigService.applyExtraMeta(meta, spec)
        item.itemMeta = meta
        return item
    }

    private fun toggleMaterial(enabled: Boolean): Material = if (enabled) Material.LIME_DYE else Material.GRAY_DYE

    private fun toggleTitle(name: String): String = "§e$name"

    private fun toggleLore(enabled: Boolean, hint: String): List<String> {
        val state = if (enabled) "§aEnabled" else "§cDisabled"
        return listOf("§7State: $state", "§8$hint")
    }

    private fun fallback(
        key: String,
        material: Material,
        name: String,
        lore: List<String>,
        slots: List<Int>,
        glow: Boolean = false,
    ): GuiItemSpec {
        return GuiItemSpec(
            key = key,
            material = material,
            amount = 1,
            name = name,
            lore = lore,
            glow = glow,
            itemFlags = emptySet(),
            enchants = emptyMap(),
            skullOwner = null,
            itemModel = null,
            slots = slots,
        )
    }

    private fun formatSet(values: Set<String>): String = if (values.isEmpty()) "all" else values.joinToString()

    private fun defaultProfileId(): String = plugin.settingsLoader.settings.profiles.keys.firstOrNull() ?: "normal"
}
