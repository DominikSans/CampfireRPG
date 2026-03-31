package cg.headpop.campfireRPG

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.math.roundToInt

class CampfireRPG : JavaPlugin() {

    private var checkIntervalTicks = 40L
    private var campfireRadius = 7.0
    private var searchVerticalRadius = 3
    private var requiredPlayers = 2
    private var restoreFood = true
    private var restCyclesRequired = 6
    private var monsterWardRadius = 8.0
    private var nightStart = 13000L
    private var nightEnd = 23000L

    private val campfireTicks = mutableMapOf<UUID, Int>()

    override fun onEnable() {
        saveDefaultConfig()
        loadSettings()

        server.scheduler.runTaskTimer(this, Runnable { tickCampfires() }, 40L, checkIntervalTicks)
        logger.info("CampfireRPG enabled.")
    }

    override fun onDisable() {
        campfireTicks.clear()
    }

    private fun loadSettings() {
        checkIntervalTicks = config.getLong("scan.check-interval-ticks", 40L).coerceAtLeast(20L)
        campfireRadius = config.getDouble("campfire.radius", 7.0).coerceAtLeast(2.0)
        searchVerticalRadius = config.getInt("campfire.search-vertical-radius", 3).coerceAtLeast(1)
        requiredPlayers = config.getInt("campfire.required-players", 2).coerceAtLeast(2)
        restoreFood = config.getBoolean("campfire.restore-food", true)
        restCyclesRequired = config.getInt("campfire.rest-cycles-required", 6).coerceAtLeast(2)
        monsterWardRadius = config.getDouble("campfire.monster-ward-radius", 8.0).coerceAtLeast(2.0)
        nightStart = config.getLong("night.start-tick", 13000L)
        nightEnd = config.getLong("night.end-tick", 23000L)
    }

    private fun tickCampfires() {
        val grouped = mutableMapOf<CampfireSpot, MutableList<Player>>()

        for (player in server.onlinePlayers) {
            if (!player.isValid || player.isDead || !isNight(player)) {
                continue
            }

            for (spot in findNearbyCampfires(player)) {
                grouped.getOrPut(spot) { mutableListOf() }.add(player)
            }
        }

        val energizedPlayers = mutableSetOf<UUID>()
        for ((spot, players) in grouped) {
            val uniquePlayers = players.distinctBy(Player::getUniqueId)
            if (uniquePlayers.size < requiredPlayers) {
                continue
            }

            val block = spot.resolveBlock() ?: continue
            if (!isLitCampfire(block)) {
                continue
            }

            uniquePlayers.forEach { player ->
                applyCampfireAura(player, block.type == Material.SOUL_CAMPFIRE)
                energizedPlayers += player.uniqueId
            }

            applyMonsterWard(block)
            playCampfireFeedback(block, block.type == Material.SOUL_CAMPFIRE)
        }

        updateRestProgress(energizedPlayers)
    }

    private fun findNearbyCampfires(player: Player): List<CampfireSpot> {
        val found = mutableSetOf<CampfireSpot>()
        val radiusInt = campfireRadius.roundToInt()
        val base = player.location.block

        for (x in -radiusInt..radiusInt) {
            for (y in -searchVerticalRadius..searchVerticalRadius) {
                for (z in -radiusInt..radiusInt) {
                    val block = base.getRelative(x, y, z)
                    if (!isLitCampfire(block)) {
                        continue
                    }

                    val center = block.location.toCenterLocation()
                    if (center.world == player.world && center.distanceSquared(player.location) <= campfireRadius * campfireRadius) {
                        found += CampfireSpot.fromBlock(block)
                    }
                }
            }
        }

        return found.toList()
    }

    private fun isLitCampfire(block: Block): Boolean {
        if (block.type != Material.CAMPFIRE && block.type != Material.SOUL_CAMPFIRE) {
            return false
        }

        val data = block.blockData as? Campfire ?: return false
        return data.isLit
    }

    private fun applyCampfireAura(player: Player, soulCampfire: Boolean) {
        val baseDuration = (checkIntervalTicks + 40L).toInt()

        giveEffect(player, PotionEffectType.REGENERATION, baseDuration, 0)
        giveEffect(player, PotionEffectType.RESISTANCE, baseDuration, 0)
        giveEffect(player, PotionEffectType.SPEED, baseDuration, if (soulCampfire) 1 else 0)

        if (soulCampfire) {
            giveEffect(player, PotionEffectType.NIGHT_VISION, baseDuration, 0)
            giveEffect(player, PotionEffectType.STRENGTH, baseDuration, 0)
            player.sendActionBar(Component.text("Soul Campfire: poder ancestral"))
        } else {
            player.sendActionBar(Component.text("Campfire Aura: descanso del aventurero"))
        }

        if (restoreFood && player.foodLevel < 20) {
            player.foodLevel = (player.foodLevel + 1).coerceAtMost(20)
            player.saturation = (player.saturation + 0.5f).coerceAtMost(20f)
        }
    }

    private fun updateRestProgress(energizedPlayers: Set<UUID>) {
        val restedDuration = 20 * 90
        val cycles = campfireTicks.keys.toList()

        for (player in Bukkit.getOnlinePlayers()) {
            if (player.uniqueId !in energizedPlayers) {
                campfireTicks.remove(player.uniqueId)
                continue
            }

            val newValue = campfireTicks.getOrDefault(player.uniqueId, 0) + 1
            campfireTicks[player.uniqueId] = newValue

            if (newValue % restCyclesRequired == 0) {
                giveEffect(player, PotionEffectType.ABSORPTION, restedDuration, 0)
                giveEffect(player, PotionEffectType.LUCK, restedDuration, 0)
                player.sendMessage("§6El campfire te ha concedido un descanso reparador.")
            }
        }

        for (uuid in cycles) {
            if (uuid !in energizedPlayers) {
                campfireTicks.remove(uuid)
            }
        }
    }

    private fun applyMonsterWard(block: Block) {
        val world = block.world
        val center = block.location.toCenterLocation()
        val duration = (checkIntervalTicks + 20L).toInt()

        for (entity in world.getNearbyEntities(center, monsterWardRadius, 4.0, monsterWardRadius)) {
            val monster = entity as? Monster ?: continue
            giveEffect(monster, PotionEffectType.WEAKNESS, duration, 0)
            giveEffect(monster, PotionEffectType.SLOWNESS, duration, 0)
        }
    }

    private fun playCampfireFeedback(block: Block, soulCampfire: Boolean) {
        val center = block.location.toCenterLocation().add(0.0, 0.2, 0.0)
        val particle = if (soulCampfire) Particle.SOUL_FIRE_FLAME else Particle.FLAME
        block.world.spawnParticle(particle, center, 12, 0.35, 0.25, 0.35, 0.01)
        block.world.spawnParticle(Particle.HAPPY_VILLAGER, center, 3, 0.4, 0.1, 0.4, 0.01)
        block.world.playSound(center, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.35f, if (soulCampfire) 0.85f else 1.05f)
    }

    private fun giveEffect(player: Player, type: PotionEffectType, duration: Int, amplifier: Int) {
        player.addPotionEffect(PotionEffect(type, duration, amplifier, true, false, true))
    }

    private fun giveEffect(monster: Monster, type: PotionEffectType, duration: Int, amplifier: Int) {
        monster.addPotionEffect(PotionEffect(type, duration, amplifier, true, true, true))
    }

    private fun isNight(player: Player): Boolean {
        val time = player.world.time
        return if (nightStart <= nightEnd) {
            time in nightStart..nightEnd
        } else {
            time >= nightStart || time <= nightEnd
        }
    }

    private data class CampfireSpot(
        val worldName: String,
        val x: Int,
        val y: Int,
        val z: Int,
    ) {
        fun resolveBlock(): Block? = Bukkit.getWorld(worldName)?.getBlockAt(x, y, z)

        companion object {
            fun fromBlock(block: Block): CampfireSpot {
                return CampfireSpot(block.world.name, block.x, block.y, block.z)
            }
        }
    }
}
