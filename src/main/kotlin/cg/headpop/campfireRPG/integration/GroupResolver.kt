package cg.headpop.campfireRPG.integration

import org.bukkit.entity.Player

fun interface GroupResolver {
    fun resolve(player: Player): String?
}
