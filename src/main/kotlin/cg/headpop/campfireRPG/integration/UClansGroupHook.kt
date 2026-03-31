package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import me.ulrich.clans.interfaces.UClans
import org.bukkit.entity.Player

class UClansGroupHook(
    private val plugin: CampfireRPG,
) : GroupResolver {

    private var api: UClans? = null

    fun reload() {
        api = plugin.server.pluginManager.plugins
            .firstOrNull { it is UClans }
            ?.let { it as UClans }
    }

    override fun resolve(player: Player): String? {
        val uclans = api ?: return null
        return runCatching {
            val clan = uclans.getPlayerAPI().getPlayerClan(player.uniqueId).orElse(null) ?: return null
            val id = clan.id?.toString() ?: clan.tag ?: return null
            "UltimateClans:$id"
        }.getOrNull()
    }

    fun isEnabled(): Boolean = api != null
}
