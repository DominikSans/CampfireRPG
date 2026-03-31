package cg.headpop.campfireRPG.service

import cg.headpop.campfireRPG.CampfireRPG
import cg.headpop.campfireRPG.model.ActiveCampfire
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.type.Campfire
import java.util.concurrent.ConcurrentHashMap

class CampfireRegistry(
    private val plugin: CampfireRPG,
) {

    private val campfiresByChunk = ConcurrentHashMap<String, Set<ActiveCampfire>>()

    fun reload() {
        clear()
        fullRescanLoadedChunks()
    }

    fun fullRescanLoadedChunks() {
        for (world in plugin.server.worlds) {
            world.loadedChunks.forEach(::refreshChunk)
        }
    }

    fun refreshChunk(chunk: Chunk) {
        val key = chunkKey(chunk.world, chunk.x, chunk.z)
        val found = linkedSetOf<ActiveCampfire>()

        for (y in chunk.world.minHeight until chunk.world.maxHeight) {
            for (x in 0..15) {
                for (z in 0..15) {
                    val block = chunk.getBlock(x, y, z)
                    if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) {
                        continue
                    }

                    val data = block.blockData as? Campfire ?: continue
                    if (!data.isLit) {
                        continue
                    }

                    found += ActiveCampfire(
                        worldName = chunk.world.name,
                        x = block.x,
                        y = block.y,
                        z = block.z,
                        material = block.type,
                    )
                }
            }
        }

        if (found.isEmpty()) {
            campfiresByChunk.remove(key)
        } else {
            campfiresByChunk[key] = found
        }
    }

    fun removeChunk(chunk: Chunk) {
        campfiresByChunk.remove(chunkKey(chunk.world, chunk.x, chunk.z))
    }

    fun activeCampfires(): List<ActiveCampfire> = campfiresByChunk.values.flatten()

    fun clear() {
        campfiresByChunk.clear()
    }

    fun size(): Int = campfiresByChunk.values.sumOf(Set<ActiveCampfire>::size)

    private fun chunkKey(world: World, x: Int, z: Int): String = "${world.name}:$x:$z"
}
