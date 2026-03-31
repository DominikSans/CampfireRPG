package cg.headpop.campfireRPG.config

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.configuration.ConfigurationSection

class SettingsLoader(
    private val plugin: CampfireRPG,
) {

    lateinit var settings: PluginSettings
        private set

    fun reload() {
        val config = plugin.config
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
            scan = ScanSettings(
                intervalTicks = config.getLong("scan.interval-ticks", 40L).coerceAtLeast(20L),
                rescanIntervalTicks = config.getLong("scan.registry-rescan-interval-ticks", 600L).coerceAtLeast(100L),
            ),
            night = NightSettings(
                onlyAtNight = config.getBoolean("night.only-at-night", true),
                startTick = config.getLong("night.start-tick", 13000L),
                endTick = config.getLong("night.end-tick", 23000L),
            ),
            campfire = CampfireSettings(
                requiredPlayers = config.getInt("campfire.required-players", 2).coerceAtLeast(1),
                restCyclesRequired = config.getInt("campfire.rest-cycles-required", 6).coerceAtLeast(1),
                restRewardCooldownTicks = config.getLong("campfire.rest-reward-cooldown-ticks", 3600L).coerceAtLeast(0L),
                monsterWardRadius = config.getDouble("campfire.monster-ward-radius", 8.0).coerceAtLeast(1.0),
                bonusThreshold = config.getInt("campfire.bonus-threshold", 4).coerceAtLeast(2),
            ),
            gameplay = GameplaySettings(
                enableExperiencePulse = config.getBoolean("gameplay.experience-pulse.enabled", true),
                experiencePulseAmount = config.getInt("gameplay.experience-pulse.amount", 3).coerceAtLeast(0),
                experiencePulseCooldownTicks = config.getLong("gameplay.experience-pulse.cooldown-ticks", 200L).coerceAtLeast(0L),
                enableCleanse = config.getBoolean("gameplay.cleanse.enabled", true),
                cleanseCooldownTicks = config.getLong("gameplay.cleanse.cooldown-ticks", 200L).coerceAtLeast(0L),
                enableSharedHeal = config.getBoolean("gameplay.shared-heal.enabled", true),
                sharedHealAmount = config.getDouble("gameplay.shared-heal.amount", 1.0).coerceAtLeast(0.0),
                sharedHealCooldownTicks = config.getLong("gameplay.shared-heal.cooldown-ticks", 120L).coerceAtLeast(0L),
            ),
            classes = ClassSettings(
                defaultClassId = config.getString("classes.default", "adventurer")!!.lowercase(),
                classes = classPerks,
            ),
            restrictions = RestrictionSettings(
                allowedWorlds = config.getStringList("restrictions.allowed-worlds").map(String::lowercase).toSet(),
                blockedWorlds = config.getStringList("restrictions.blocked-worlds").map(String::lowercase).toSet(),
                allowedRegions = config.getStringList("restrictions.allowed-regions").map(String::lowercase).toSet(),
                blockedRegions = config.getStringList("restrictions.blocked-regions").map(String::lowercase).toSet(),
            ),
            integrations = IntegrationSettings(
                enablePlaceholderApi = config.getBoolean("integrations.placeholderapi", true),
                enableWorldGuard = config.getBoolean("integrations.worldguard", true),
                enableClanHooks = config.getBoolean("integrations.clans", true),
                requireSameGroupForActivation = config.getBoolean("integrations.require-same-group-for-activation", false),
                useGroupSizeForHeroBonus = config.getBoolean("integrations.use-group-size-for-hero-bonus", true),
            ),
            gui = GuiSettings(
                title = colorize(config.getString("gui.title") ?: "&8CampfireRPG Control"),
            ),
            debug = DebugSettings(
                enabledByDefault = config.getBoolean("debug.enabled-by-default", false),
                logIntervalTicks = config.getLong("debug.log-interval-ticks", 1200L).coerceAtLeast(100L),
            ),
            messages = MessageSettings(
                normalActionBar = colorize(config.getString("messages.normal-actionbar") ?: "&6Campfire Aura &7- descanso del aventurero"),
                soulActionBar = colorize(config.getString("messages.soul-actionbar") ?: "&bSoul Campfire &7- poder ancestral"),
                restedMessage = colorize(config.getString("messages.rested") ?: "&6El campfire te concede un descanso reparador."),
                heroMessage = colorize(config.getString("messages.hero") ?: "&dLa fogata responde al grupo y despierta un bonus heroico."),
                noPermission = colorize(config.getString("messages.no-permission") ?: "&cNo tienes permiso."),
                playerOnly = colorize(config.getString("messages.player-only") ?: "&cSolo un jugador puede usar esta opcion."),
                debugEnabled = colorize(config.getString("messages.debug-enabled") ?: "&aDebug activado."),
                debugDisabled = colorize(config.getString("messages.debug-disabled") ?: "&cDebug desactivado."),
                guiOpened = colorize(config.getString("messages.gui-opened") ?: "&aPanel de administracion abierto."),
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
}
