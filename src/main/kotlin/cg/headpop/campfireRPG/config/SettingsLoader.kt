package cg.headpop.campfireRPG.config

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.configuration.ConfigurationSection

class SettingsLoader(
    private val plugin: CampfireRPG,
) {

    lateinit var settings: PluginSettings
        private set

    fun reload() {
        val config = plugin.runtimeConfigService.merged()
        val profilesSection = requireNotNull(config.getConfigurationSection("profiles")) {
            "profiles section is required in config.yml"
        }

        val profiles = profilesSection.getKeys(false).associateWith { key ->
            loadProfile(key, requireNotNull(profilesSection.getConfigurationSection(key)))
        }
        val classesSection = requireNotNull(config.getConfigurationSection("classes.types")) {
            "classes.types section is required in config.yml"
        }
        val classPerks = classesSection.getKeys(false).associateWith { key ->
            loadClassPerk(key, requireNotNull(classesSection.getConfigurationSection(key)))
        }

        settings = PluginSettings(
            language = LanguageSettings(
                locale = string(config, "language.default", "language", "EN").uppercase(),
            ),
            scan = ScanSettings(
                intervalTicks = long(config, "timers.aura-interval-ticks", "scan.interval-ticks", 40L).coerceAtLeast(20L),
                rescanIntervalTicks = long(config, "timers.registry-rescan-interval-ticks", "scan.registry-rescan-interval-ticks", 600L).coerceAtLeast(100L),
            ),
            night = NightSettings(
                onlyAtNight = bool(config, "activation.night.only-at-night", "night.only-at-night", true),
                startTick = long(config, "activation.night.start-tick", "night.start-tick", 13000L),
                endTick = long(config, "activation.night.end-tick", "night.end-tick", 23000L),
            ),
            campfire = CampfireSettings(
                requiredPlayers = int(config, "activation.requirements.required-players", "campfire.required-players", 2).coerceAtLeast(1),
                restCyclesRequired = int(config, "activation.rest.rest-cycles-required", "campfire.rest-cycles-required", 6).coerceAtLeast(1),
                restRewardCooldownTicks = long(config, "activation.rest.rest-reward-cooldown-ticks", "campfire.rest-reward-cooldown-ticks", 3600L).coerceAtLeast(0L),
                monsterWardRadius = double(config, "activation.requirements.monster-ward-radius", "campfire.monster-ward-radius", 8.0).coerceAtLeast(1.0),
                bonusThreshold = int(config, "activation.requirements.bonus-threshold", "campfire.bonus-threshold", 4).coerceAtLeast(2),
            ),
            gameplay = GameplaySettings(
                enableExperiencePulse = bool(config, "gameplay.support.experience-pulse.enabled", "gameplay.experience-pulse.enabled", true),
                experiencePulseAmount = int(config, "gameplay.support.experience-pulse.amount", "gameplay.experience-pulse.amount", 3).coerceAtLeast(0),
                experiencePulseCooldownTicks = long(config, "gameplay.support.experience-pulse.cooldown-ticks", "gameplay.experience-pulse.cooldown-ticks", 200L).coerceAtLeast(0L),
                enableCleanse = bool(config, "gameplay.support.cleanse.enabled", "gameplay.cleanse.enabled", true),
                cleanseCooldownTicks = long(config, "gameplay.support.cleanse.cooldown-ticks", "gameplay.cleanse.cooldown-ticks", 200L).coerceAtLeast(0L),
                enableSharedHeal = bool(config, "gameplay.support.shared-heal.enabled", "gameplay.shared-heal.enabled", true),
                sharedHealAmount = double(config, "gameplay.support.shared-heal.amount", "gameplay.shared-heal.amount", 1.0).coerceAtLeast(0.0),
                sharedHealCooldownTicks = long(config, "gameplay.support.shared-heal.cooldown-ticks", "gameplay.shared-heal.cooldown-ticks", 120L).coerceAtLeast(0L),
            ),
            classes = ClassSettings(
                enabled = config.getBoolean("classes.enabled", false),
                defaultClassId = config.getString("classes.default", "adventurer")!!.lowercase(),
                classes = classPerks,
            ),
            clanFeatures = ClanFeatureSettings(
                enabled = config.getBoolean("clan-features.enabled", false),
                heroBonusRequireSameClan = config.getBoolean("clan-features.hero-bonus-require-same-clan", false),
                leaderBonusEnabled = config.getBoolean("clan-features.leader-bonus.enabled", false),
                leaderBonusEffects = loadEffects(config.getConfigurationSection("clan-features.leader-bonus.effects")),
                sizeBonusEnabled = config.getBoolean("clan-features.size-bonus.enabled", false),
                sizeBonusMinimumMembers = config.getInt("clan-features.size-bonus.minimum-members", 4).coerceAtLeast(2),
                sizeBonusEffects = loadEffects(config.getConfigurationSection("clan-features.size-bonus.effects")),
                territoryRestrictToOwnClan = config.getBoolean("clan-features.territory.restrict-to-own-clan-territory", false),
                territoryBonusEnabled = config.getBoolean("clan-features.territory.bonus-in-own-territory", false),
                territoryBonusEffects = loadEffects(config.getConfigurationSection("clan-features.territory.effects")),
            ),
            restrictions = RestrictionSettings(
                allowedWorlds = strings(config, "restrictions.worlds.allowed", "restrictions.allowed-worlds").map(String::lowercase).toSet(),
                blockedWorlds = strings(config, "restrictions.worlds.blocked", "restrictions.blocked-worlds").map(String::lowercase).toSet(),
                allowedRegions = strings(config, "restrictions.regions.allowed", "restrictions.allowed-regions").map(String::lowercase).toSet(),
                blockedRegions = strings(config, "restrictions.regions.blocked", "restrictions.blocked-regions").map(String::lowercase).toSet(),
            ),
            integrations = IntegrationSettings(
                enablePlaceholderApi = bool(config, "integrations.hooks.placeholderapi", "integrations.placeholderapi", true),
                enableWorldGuard = bool(config, "integrations.hooks.worldguard", "integrations.worldguard", false),
                enableClanHooks = bool(config, "integrations.hooks.clans", "integrations.clans", false),
                requireSameGroupForActivation = bool(config, "integrations.groups.require-same-group-for-activation", "integrations.require-same-group-for-activation", false),
                useGroupSizeForHeroBonus = bool(config, "integrations.groups.use-group-size-for-hero-bonus", "integrations.use-group-size-for-hero-bonus", false),
            ),
            gui = GuiSettings(
                title = colorize(config.getString("gui.title") ?: "&8CampfireRPG Control"),
            ),
            debug = DebugSettings(
                enabledByDefault = bool(config, "debug.enabled-by-default", default = false),
                logIntervalTicks = long(config, "debug.log-interval-ticks", default = 1200L).coerceAtLeast(100L),
            ),
            messages = MessageSettings(
                normalActionBar = colorize(string(config, "messages.actionbar.normal", "messages.normal-actionbar", "&6Campfire Rest &7- warm support aura")),
                soulActionBar = colorize(string(config, "messages.actionbar.soul", "messages.soul-actionbar", "&bSoul Campfire &7- stronger support aura")),
                restedMessage = colorize(string(config, "messages.chat.rested", "messages.rested", "&6The campfire leaves you well rested.")),
                heroMessage = colorize(string(config, "messages.chat.hero", "messages.hero", "&dYour group awakened the campfire bonus.")),
                noPermission = colorize(string(config, "messages.system.no-permission", "messages.no-permission", "&cNo tienes permiso.")),
                playerOnly = colorize(string(config, "messages.system.player-only", "messages.player-only", "&cSolo un jugador puede usar esta opcion.")),
                debugEnabled = colorize(string(config, "messages.system.debug-enabled", "messages.debug-enabled", "&aDebug activado.")),
                debugDisabled = colorize(string(config, "messages.system.debug-disabled", "messages.debug-disabled", "&cDebug desactivado.")),
                guiOpened = colorize(string(config, "messages.system.gui-opened", "messages.gui-opened", "&aPanel de administracion abierto.")),
            ),
            profiles = profiles,
        )
    }

    private fun loadProfile(key: String, section: ConfigurationSection): CampfireProfile {
        return CampfireProfile(
            id = key,
            material = section.getString("material", key.uppercase())!!.uppercase(),
            radius = section.getDouble("radius", 7.0).coerceAtLeast(2.0),
            feedPlayers = section.getBoolean("feed-players", true),
            effects = loadEffects(section.getConfigurationSection("effects")),
            bonusEffects = loadEffects(section.getConfigurationSection("bonus-effects")),
            restedEffects = loadEffects(section.getConfigurationSection("rested-effects")),
            wardEffects = loadEffects(section.getConfigurationSection("ward-effects")),
        )
    }

    private fun loadClassPerk(key: String, section: ConfigurationSection): ClassPerk {
        return ClassPerk(
            id = key,
            displayName = colorize(section.getString("display-name") ?: key.replaceFirstChar(Char::uppercase)),
            permission = section.getString("permission", "campfirerpg.class.$key")!!,
            description = colorize(section.getString("description") ?: "Campfire class perk"),
            globalEffects = loadEffects(section.getConfigurationSection("global-effects")),
            normalEffects = loadEffects(section.getConfigurationSection("normal-effects")),
            soulEffects = loadEffects(section.getConfigurationSection("soul-effects")),
        )
    }

    private fun loadEffects(section: ConfigurationSection?): List<EffectSpec> {
        if (section == null) {
            return emptyList()
        }

        return section.getKeys(false).mapNotNull { key ->
            val child = section.getConfigurationSection(key) ?: return@mapNotNull null
            EffectSpec(
                type = child.getString("type", "SPEED")!!.uppercase(),
                amplifier = child.getInt("amplifier", 0).coerceAtLeast(0),
                durationTicks = child.getInt("duration-ticks", 100).coerceAtLeast(20),
                ambient = child.getBoolean("ambient", true),
                particles = child.getBoolean("particles", false),
                icon = child.getBoolean("icon", true),
            )
        }
    }

    private fun colorize(text: String): String = text.replace('&', '§')

    private fun string(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null, default: String): String {
        return when {
            config.contains(primaryPath) -> config.getString(primaryPath, default) ?: default
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getString(fallbackPath, default) ?: default
            else -> default
        }
    }

    private fun bool(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null, default: Boolean): Boolean {
        return when {
            config.contains(primaryPath) -> config.getBoolean(primaryPath, default)
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getBoolean(fallbackPath, default)
            else -> default
        }
    }

    private fun int(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null, default: Int): Int {
        return when {
            config.contains(primaryPath) -> config.getInt(primaryPath, default)
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getInt(fallbackPath, default)
            else -> default
        }
    }

    private fun long(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null, default: Long): Long {
        return when {
            config.contains(primaryPath) -> config.getLong(primaryPath, default)
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getLong(fallbackPath, default)
            else -> default
        }
    }

    private fun double(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null, default: Double): Double {
        return when {
            config.contains(primaryPath) -> config.getDouble(primaryPath, default)
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getDouble(fallbackPath, default)
            else -> default
        }
    }

    private fun strings(config: org.bukkit.configuration.file.FileConfiguration, primaryPath: String, fallbackPath: String? = null): List<String> {
        return when {
            config.contains(primaryPath) -> config.getStringList(primaryPath)
            !fallbackPath.isNullOrBlank() && config.contains(fallbackPath) -> config.getStringList(fallbackPath)
            else -> emptyList()
        }
    }
}
