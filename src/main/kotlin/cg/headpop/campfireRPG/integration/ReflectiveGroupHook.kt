package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.entity.Player
import java.util.UUID

class ReflectiveGroupHook(
    private val plugin: CampfireRPG,
    val pluginName: String,
    private val managerMethods: List<String>,
    private val groupLookupMethods: List<String>,
    private val idMethods: List<String>,
) : GroupResolver {

    private var enabled = false
    private var pluginInstance: Any? = null

    fun reload() {
        pluginInstance = plugin.server.pluginManager.getPlugin(pluginName)
        enabled = pluginInstance != null
    }

    override fun resolve(player: Player): String? {
        if (!enabled) {
            return null
        }

        return runCatching {
            val source = pluginInstance ?: return null
            val manager = managerMethods.firstNotNullOfOrNull { invokeNoArg(source, it) } ?: source
            val group = groupLookupMethods.firstNotNullOfOrNull { method ->
                invokeFlexible(manager, method, player, player.uniqueId, player.name)
            } ?: invokeFlexible(source, "getClan", player, player.uniqueId, player.name)
            val id = idMethods.firstNotNullOfOrNull { invokeNoArg(group ?: return@firstNotNullOfOrNull null, it) }
            id?.toString()?.takeIf(String::isNotBlank)?.let { "$pluginName:$it" }
        }.getOrNull()
    }

    fun isEnabled(): Boolean = enabled

    private fun invokeNoArg(target: Any, name: String): Any? {
        val method = target.javaClass.methods.firstOrNull { it.name.equals(name, ignoreCase = true) && it.parameterCount == 0 } ?: return null
        return method.invoke(target)
    }

    private fun invokeFlexible(target: Any, name: String, player: Player, uuid: UUID, playerName: String): Any? {
        val candidates = target.javaClass.methods.filter { it.name.equals(name, ignoreCase = true) && it.parameterCount == 1 }
        for (method in candidates) {
            val type = method.parameterTypes[0]
            val argument = when {
                type.isAssignableFrom(Player::class.java) -> player
                type == UUID::class.java -> uuid
                type == String::class.java -> playerName
                type.name.equals("java.util.UUID", ignoreCase = true) -> uuid
                else -> null
            } ?: continue

            runCatching { return method.invoke(target, argument) }.getOrNull()
        }
        return null
    }
}
