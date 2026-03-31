package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.entity.Player

class PlaceholderHook(
    private val plugin: CampfireRPG,
) {

    private var enabled = false
    private var method: java.lang.reflect.Method? = null

    fun reload() {
        enabled = false
        method = null

        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return
        }

        runCatching {
            val clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            method = clazz.getMethod("setPlaceholders", Player::class.java, String::class.java)
            enabled = true
        }.onFailure {
            plugin.logger.warning("PlaceholderAPI hook failed: ${it.message}")
        }
    }

    fun apply(player: Player, text: String): String {
        if (!enabled) {
            return text
        }

        return runCatching {
            method?.invoke(null, player, text) as? String ?: text
        }.getOrElse { text }
    }

    fun isEnabled(): Boolean = enabled
}
