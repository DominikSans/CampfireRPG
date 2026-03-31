package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.Location
import org.bukkit.entity.Player

class IntegrationService(
    private val plugin: CampfireRPG,
) {

    private val placeholderHook = PlaceholderHook(plugin)
    private val worldGuardHook = WorldGuardHook(plugin)
    private val clanHooks = listOf(
        ReflectiveGroupHook(plugin, "UltimateClans", listOf("getClanManager", "getApi", "getUltimateClans"), listOf("getClanByPlayer", "getPlayerClan", "getClan", "getByPlayer"), listOf("getName", "getTag", "getId")),
        ReflectiveGroupHook(plugin, "SimpleClans", listOf("getClanManager", "getApi"), listOf("getClanByPlayerUniqueId", "getClanByPlayerName", "getClan"), listOf("getName", "getTagLabel", "getTag")),
        ReflectiveGroupHook(plugin, "Parties", listOf("getPartyManager", "getApi"), listOf("getParty", "getPartyByPlayer", "getPartyPlayer"), listOf("getName", "getId")),
        ReflectiveGroupHook(plugin, "Lands", listOf("getLandsIntegration", "getLandHandler", "getApi"), listOf("getLand", "getLandByPlayer", "getPlayerLand"), listOf("getName", "getId")),
        ReflectiveGroupHook(plugin, "Towny", listOf("getAPI", "getTownyUniverse"), listOf("getResident", "getTown", "getTownByPlayer"), listOf("getName", "getUUID")),
        ReflectiveGroupHook(plugin, "Kingdoms", listOf("getKingdomManager", "getApi"), listOf("getKingdom", "getKingdomByPlayer"), listOf("getName", "getId")),
        ReflectiveGroupHook(plugin, "Factions", listOf("getFactionManager", "getApi"), listOf("getFaction", "getFactionByPlayer"), listOf("getTag", "getId")),
    )

    fun reload() {
        placeholderHook.reload()
        worldGuardHook.reload()
        clanHooks.forEach(ReflectiveGroupHook::reload)
    }

    fun applyPlaceholders(player: Player, text: String): String {
        if (!plugin.settingsLoader.settings.integrations.enablePlaceholderApi) {
            return text
        }
        return placeholderHook.apply(player, text)
    }

    fun isLocationAllowed(location: Location): Boolean {
        val restrictions = plugin.settingsLoader.settings.restrictions
        val worldName = location.world.name.lowercase()
        if (restrictions.allowedWorlds.isNotEmpty() && worldName !in restrictions.allowedWorlds) {
            return false
        }
        if (worldName in restrictions.blockedWorlds) {
            return false
        }
        if (!plugin.settingsLoader.settings.integrations.enableWorldGuard) {
            return true
        }

        return worldGuardHook.isAllowed(location, restrictions.allowedRegions, restrictions.blockedRegions)
    }

    fun resolveGroup(player: Player): String? {
        if (!plugin.settingsLoader.settings.integrations.enableClanHooks) {
            return null
        }
        return clanHooks.firstNotNullOfOrNull { it.resolve(player) }
    }

    fun describeIntegrations(): List<String> {
        return listOf(
            "PlaceholderAPI: ${placeholderHook.isEnabled()}",
            "WorldGuard: ${worldGuardHook.isEnabled()}",
            "Group hooks: ${detectedGroupPluginNames().ifEmpty { listOf("none") }.joinToString()}",
        )
    }

    fun detectedGroupPluginNames(): List<String> = clanHooks.filter(ReflectiveGroupHook::isEnabled).map { it.pluginName }

    fun isWorldGuardEnabled(): Boolean = worldGuardHook.isEnabled()
}
