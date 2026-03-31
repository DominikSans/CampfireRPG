package cg.headpop.campfireRPG.integration

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.Location

class WorldGuardHook(
    private val plugin: CampfireRPG,
) {

    private var enabled = false

    fun reload() {
        enabled = plugin.server.pluginManager.getPlugin("WorldGuard") != null
    }

    fun isAllowed(location: Location, allowedRegions: Set<String>, blockedRegions: Set<String>): Boolean {
        if (!enabled) {
            return true
        }

        return runCatching {
            val wgClass = Class.forName("com.sk89q.worldguard.WorldGuard")
            val bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter")
            val blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3")

            val wgInstance = wgClass.getMethod("getInstance").invoke(null)
            val platform = wgInstance.javaClass.getMethod("getPlatform").invoke(wgInstance)
            val regionContainer = platform.javaClass.getMethod("getRegionContainer").invoke(platform)
            val worldEditWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World::class.java).invoke(null, location.world)
            val regionManager = regionContainer.javaClass.methods
                .firstOrNull { it.name == "get" && it.parameterCount == 1 }
                ?.invoke(regionContainer, worldEditWorld)
                ?: return true

            val vector = blockVector3Class.getMethod("at", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .invoke(null, location.blockX, location.blockY, location.blockZ)
            val applicable = regionManager.javaClass.getMethod("getApplicableRegions", blockVector3Class).invoke(regionManager, vector)
            val regions = applicable.javaClass.getMethod("getRegions").invoke(applicable) as? Collection<*> ?: emptyList<Any>()
            val regionIds = regions.mapNotNull { region ->
                runCatching { region?.javaClass?.getMethod("getId")?.invoke(region)?.toString()?.lowercase() }.getOrNull()
            }.toSet()

            if (blockedRegions.isNotEmpty() && regionIds.any { it in blockedRegions }) {
                return false
            }
            if (allowedRegions.isNotEmpty()) {
                return regionIds.any { it in allowedRegions }
            }
            true
        }.getOrElse {
            if (plugin.diagnosticsService.isDebugEnabled()) {
                plugin.logger.warning("WorldGuard hook failed: ${it.message}")
            }
            true
        }
    }

    fun isEnabled(): Boolean = enabled
}
