package cg.headpop.campfireRPG.model

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block

data class ActiveCampfire(
    val worldName: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val material: Material,
) {
    fun world(): World? = Bukkit.getWorld(worldName)

    fun location(): Location? = world()?.getBlockAt(x, y, z)?.location?.toCenterLocation()

    fun block(): Block? = world()?.getBlockAt(x, y, z)

    fun key(): String = "$worldName:$x:$y:$z"
}
