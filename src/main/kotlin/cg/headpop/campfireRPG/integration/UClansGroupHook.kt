package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import me.ulrich.clans.data.ClanData
import me.ulrich.clans.interfaces.UClans
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class UClansGroupHook(
    private val plugin: CampfireRPG,
) : GroupResolver {

    private var api: UClans? = null

    fun reload() {
        api = plugin.server.pluginManager.plugins.firstOrNull { it is UClans } as? UClans
    }

    override fun resolve(player: Player): String? = getClanContext(player)?.let { "UltimateClans:${it.id}" }

    fun getClanContext(player: Player): ClanContext? {
        val uclans = api ?: return null
        return runCatching {
            val clan = uclans.getPlayerAPI().getPlayerClan(player.uniqueId).orElse(null) ?: return null
            val role = uclans.getPlayerAPI().getPlayerData(player.uniqueId).orElse(null)?.role
            clan.toContext(player.uniqueId, role)
        }.getOrNull()
    }

    fun isInOwnTerritory(player: Player, location: Location): Boolean {
        val uclans = api ?: return false
        val clan = uclans.getPlayerAPI().getPlayerClan(player.uniqueId).orElse(null) ?: return false
        val claim = uclans.getClaimAPI().getPreferentialOrFirstImplement().orElse(null) ?: return false
        val owner = claim.getClaimOwner(location).orElse(null) ?: return false
        return owner == clan.id
    }

    fun getClanTag(player: Player): String = getClanContext(player)?.tag ?: "none"

    fun getClanRole(player: Player): String = getClanContext(player)?.role ?: "none"

    fun getClanSize(player: Player): Int = getClanContext(player)?.size ?: 0

    fun isClanLeader(player: Player): Boolean = getClanContext(player)?.leader == true

    fun isEnabled(): Boolean = api != null

    private fun ClanData.toContext(playerId: UUID, role: String?): ClanContext {
        return ClanContext(
            source = "UltimateClans",
            id = id?.toString() ?: tag ?: "unknown",
            tag = tag ?: "unknown",
            size = members?.size ?: 0,
            leader = leader == playerId,
            role = role,
        )
    }
}
