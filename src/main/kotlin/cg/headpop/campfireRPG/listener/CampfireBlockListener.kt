package cg.headpop.campfireRPG.listener

import cg.headpop.campfireRPG.service.CampfireRegistry
import org.bukkit.Material
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
        registry.refreshBlock(event.block)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        registry.removeBlock(event.block)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        registry.refreshBlock(event.block)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockFade(event: BlockFadeEvent) {
        registry.refreshBlock(event.block)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockBurn(event: BlockBurnEvent) {
        registry.removeBlock(event.block)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onInteract(event: PlayerInteractEvent) {
        val clicked = event.clickedBlock ?: return
        if (clicked.type == Material.CAMPFIRE || clicked.type == Material.SOUL_CAMPFIRE) {
            registry.refreshBlock(clicked)
        }
    }
}
