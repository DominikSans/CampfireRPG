package cg.headpop.campfireRPG.listener

import cg.headpop.campfireRPG.service.CampfireRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class CampfireBlockListener(
    private val registry: CampfireRegistry,
) : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        registry.refreshChunk(event.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onChunkUnload(event: ChunkUnloadEvent) {
        registry.removeChunk(event.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockPlace(event: BlockPlaceEvent) {
        registry.refreshChunk(event.block.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        registry.refreshChunk(event.block.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        registry.refreshChunk(event.block.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockFade(event: BlockFadeEvent) {
        registry.refreshChunk(event.block.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockBurn(event: BlockBurnEvent) {
        registry.refreshChunk(event.block.chunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInteract(event: PlayerInteractEvent) {
        val clicked = event.clickedBlock ?: return
        registry.refreshChunk(clicked.chunk)
    }
}
