package cg.headpop.campfireRPG.config

data class PluginSettings(
    val scan: ScanSettings,
    val night: NightSettings,
    val campfire: CampfireSettings,
    val restrictions: RestrictionSettings,
    val integrations: IntegrationSettings,
    val gui: GuiSettings,
    val debug: DebugSettings,
    val messages: MessageSettings,
    val profiles: Map<String, CampfireProfile>,
)

data class ScanSettings(
    val intervalTicks: Long,
    val rescanIntervalTicks: Long,
)

data class NightSettings(
    val onlyAtNight: Boolean,
    val startTick: Long,
    val endTick: Long,
)

data class CampfireSettings(
    val requiredPlayers: Int,
    val restCyclesRequired: Int,
    val restRewardCooldownTicks: Long,
    val monsterWardRadius: Double,
    val bonusThreshold: Int,
)

data class RestrictionSettings(
    val allowedWorlds: Set<String>,
    val blockedWorlds: Set<String>,
    val allowedRegions: Set<String>,
    val blockedRegions: Set<String>,
)

data class IntegrationSettings(
    val enablePlaceholderApi: Boolean,
    val enableWorldGuard: Boolean,
    val enableClanHooks: Boolean,
    val requireSameGroupForActivation: Boolean,
    val useGroupSizeForHeroBonus: Boolean,
)

data class GuiSettings(
    val title: String,
)

data class DebugSettings(
    val enabledByDefault: Boolean,
    val logIntervalTicks: Long,
)

data class MessageSettings(
    val normalActionBar: String,
    val soulActionBar: String,
    val restedMessage: String,
    val heroMessage: String,
    val noPermission: String,
    val playerOnly: String,
    val debugEnabled: String,
    val debugDisabled: String,
    val guiOpened: String,
)

data class CampfireProfile(
    val id: String,
    val material: String,
    val radius: Double,
    val feedPlayers: Boolean,
    val effects: List<EffectSpec>,
    val bonusEffects: List<EffectSpec>,
    val restedEffects: List<EffectSpec>,
    val wardEffects: List<EffectSpec>,
)

data class EffectSpec(
    val type: String,
    val amplifier: Int,
    val durationTicks: Int,
    val ambient: Boolean,
    val particles: Boolean,
    val icon: Boolean,
)
