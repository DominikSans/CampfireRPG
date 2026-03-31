package cg.headpop.campfireRPG.service

import cg.headpop.campfireRPG.CampfireRPG
import cg.headpop.campfireRPG.model.ActiveCampfire
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
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
                    createActiveCampfire(chunk.getBlock(x, y, z))?.let(found::add)
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

    fun refreshBlock(block: Block) {
        val key = chunkKey(block.world, block.chunk.x, block.chunk.z)
        val current = campfiresByChunk[key]?.toMutableSet() ?: linkedSetOf()
        current.removeIf { it.worldName == block.world.name && it.x == block.x && it.y == block.y && it.z == block.z }
        createActiveCampfire(block)?.let(current::add)

        if (current.isEmpty()) {
            campfiresByChunk.remove(key)
        } else {
            campfiresByChunk[key] = current
        }
    }

    fun removeBlock(block: Block) {
        val key = chunkKey(block.world, block.chunk.x, block.chunk.z)
        val current = campfiresByChunk[key]?.toMutableSet() ?: return
        current.removeIf { it.worldName == block.world.name && it.x == block.x && it.y == block.y && it.z == block.z }
        if (current.isEmpty()) {
            campfiresByChunk.remove(key)
        } else {
            campfiresByChunk[key] = current
        }
    }

    fun activeCampfires(): List<ActiveCampfire> = campfiresByChunk.values.flatten()

    fun clear() {
        campfiresByChunk.clear()
    }

    fun size(): Int = campfiresByChunk.values.sumOf(Set<ActiveCampfire>::size)

    private fun chunkKey(world: World, x: Int, z: Int): String = "${world.name}:$x:$z"

    private fun createActiveCampfire(block: Block): ActiveCampfire? {
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) {
            return null
        }

        val data = block.blockData as? Campfire ?: return null
        if (!data.isLit) {
            return null
        }

        return ActiveCampfire(
            worldName = block.world.name,
            x = block.x,
            y = block.y,
            z = block.z,
            material = block.type,
        )
    }
}
