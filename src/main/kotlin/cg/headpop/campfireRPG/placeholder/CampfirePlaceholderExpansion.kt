package cg.headpop.campfireRPG.placeholder

import cg.headpop.campfireRPG.CampfireRPG
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class CampfirePlaceholderExpansion(
    private val plugin: CampfireRPG,
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "campfirerpg"

    override fun getAuthor(): String = "headpop"

    override fun getVersion(): String = plugin.pluginMeta.version

    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String {
        return when (params.lowercase()) {
            "tracked_campfires" -> plugin.campfireRegistry.size().toString()
            "debug" -> plugin.diagnosticsService.isDebugEnabled().toString()
            "profile" -> player?.let(plugin.auraService::getCurrentProfileId) ?: "none"
            "class" -> player?.let(plugin.auraService::getCurrentClassId) ?: "disabled"
            "class_display" -> player?.let(plugin.auraService::getCurrentClassDisplayName) ?: "disabled"
            "active" -> player?.let { if (plugin.auraService.isPlayerInActiveCampfire(it)) "yes" else "no" } ?: "no"
            "aura_remaining" -> player?.let(plugin.auraService::getAuraRemaining)?.toString() ?: "0"
            "campfire_type" -> player?.let(plugin.auraService::getCurrentCampfireType) ?: "none"
            "hero_bonus" -> player?.let { if (plugin.auraService.isHeroBonusActive(it)) "yes" else "no" } ?: "no"
            "uclans_tag" -> player?.let(plugin.auraService::getClanTag) ?: "none"
            "uclans_role" -> player?.let(plugin.auraService::getClanRole) ?: "none"
            "uclans_size" -> player?.let(plugin.auraService::getClanSize)?.toString() ?: "0"
            "uclans_own_territory" -> player?.let { if (plugin.auraService.isInOwnClanTerritory(it)) "yes" else "no" } ?: "no"
            "integrations" -> plugin.integrationService.detectedGroupPluginNames().joinToString(",").ifBlank { "none" }
            else -> ""
        }
    }
}
