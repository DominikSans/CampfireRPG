package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID

class UClansGroupHook(
    private val plugin: CampfireRPG,
) : GroupResolver {

    private var pluginInstance: Any? = null
    private var available = false

    fun reload() {
        available = false
        pluginInstance = null

        val loadedPlugin = plugin.server.pluginManager.getPlugin("UltimateClans") ?: return
        val uClansClass = runCatching { Class.forName("me.ulrich.clans.interfaces.UClans") }.getOrNull() ?: return
        if (!uClansClass.isInstance(loadedPlugin)) {
            return
        }

        pluginInstance = loadedPlugin
        available = true
    }

    override fun resolve(player: Player): String? = getClanContext(player)?.let { "UltimateClans:${it.id}" }

    fun getClanContext(player: Player): ClanContext? {
        val source = pluginInstance ?: return null
        return runCatching {
            val playerApi = source.javaClass.getMethod("getPlayerAPI").invoke(source)
            val optionalClan = playerApi.javaClass.getMethod("getPlayerClan", UUID::class.java).invoke(playerApi, player.uniqueId)
            val clan = optionalClan.javaClass.getMethod("orElse", Any::class.java).invoke(optionalClan, null) ?: return null

            val optionalPlayerData = playerApi.javaClass.getMethod("getPlayerData", UUID::class.java).invoke(playerApi, player.uniqueId)
            val playerData = optionalPlayerData.javaClass.getMethod("orElse", Any::class.java).invoke(optionalPlayerData, null)
            val role = playerData?.javaClass?.getMethod("getRole")?.invoke(playerData)?.toString()

            val id = clan.javaClass.getMethod("getId").invoke(clan)?.toString()
            val tag = clan.javaClass.getMethod("getTag").invoke(clan)?.toString() ?: "unknown"
            val members = clan.javaClass.getMethod("getMembers").invoke(clan) as? Collection<*>
            val leader = clan.javaClass.getMethod("getLeader").invoke(clan) as? UUID

            ClanContext(
                source = "UltimateClans",
                id = id ?: tag,
                tag = tag,
                size = members?.size ?: 0,
                leader = leader == player.uniqueId,
                role = role,
            )
        }.getOrNull()
    }

    fun isInOwnTerritory(player: Player, location: Location): Boolean {
        val source = pluginInstance ?: return false
        return runCatching {
            val playerApi = source.javaClass.getMethod("getPlayerAPI").invoke(source)
            val optionalClan = playerApi.javaClass.getMethod("getPlayerClan", UUID::class.java).invoke(playerApi, player.uniqueId)
            val clan = optionalClan.javaClass.getMethod("orElse", Any::class.java).invoke(optionalClan, null) ?: return false
            val clanId = clan.javaClass.getMethod("getId").invoke(clan) as? UUID ?: return false

            val claimApi = source.javaClass.getMethod("getClaimAPI").invoke(source)
            val optionalImplement = claimApi.javaClass.getMethod("getPreferentialOrFirstImplement").invoke(claimApi)
            val implement = optionalImplement.javaClass.getMethod("orElse", Any::class.java).invoke(optionalImplement, null) ?: return false
            val optionalOwner = implement.javaClass.getMethod("getClaimOwner", Location::class.java).invoke(implement, location)
            val owner = optionalOwner.javaClass.getMethod("orElse", Any::class.java).invoke(optionalOwner, null) as? UUID ?: return false

            owner == clanId
        }.getOrDefault(false)
    }

    fun getClanTag(player: Player): String = getClanContext(player)?.tag ?: "none"

    fun getClanRole(player: Player): String = getClanContext(player)?.role ?: "none"

    fun getClanSize(player: Player): Int = getClanContext(player)?.size ?: 0

    fun isClanLeader(player: Player): Boolean = getClanContext(player)?.leader == true

    fun isEnabled(): Boolean = available
}
