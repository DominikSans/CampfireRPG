package cg.headpop.campfireRPG.service

import cg.headpop.campfireRPG.CampfireRPG
import cg.headpop.campfireRPG.config.CampfireProfile
import cg.headpop.campfireRPG.config.EffectSpec
import cg.headpop.campfireRPG.model.ActiveCampfire
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class CampfireAuraService(
    private val plugin: CampfireRPG,
    private val registry: CampfireRegistry,
) {

    private val serializer = LegacyComponentSerializer.legacySection()
    private val restProgress = mutableMapOf<UUID, Int>()
    private val lastProfileByPlayer = mutableMapOf<UUID, String>()
    private val currentCampfireByPlayer = mutableMapOf<UUID, String>()
    private val restCampfireByPlayer = mutableMapOf<UUID, String>()
    private val lastRestRewardTick = mutableMapOf<UUID, Long>()
    private var auraTask: BukkitTask? = null
    private var rescanTask: BukkitTask? = null

    fun reload() {
        stop()
        val settings = plugin.settingsLoader.settings

        auraTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { tick() },
            settings.scan.intervalTicks,
            settings.scan.intervalTicks,
        )
        rescanTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable { registry.fullRescanLoadedChunks() },
            settings.scan.rescanIntervalTicks,
            settings.scan.rescanIntervalTicks,
        )
    }

    fun stop() {
        auraTask?.cancel()
        rescanTask?.cancel()
        auraTask = null
        rescanTask = null
        restProgress.clear()
        lastProfileByPlayer.clear()
        currentCampfireByPlayer.clear()
        restCampfireByPlayer.clear()
        lastRestRewardTick.clear()
    }

    private fun tick() {
        val startedAt = System.nanoTime()
        val settings = plugin.settingsLoader.settings
        val energizedPlayers = mutableSetOf<UUID>()
        var activatedCampfires = 0
        var buffedPlayers = 0

        for (campfire in registry.activeCampfires()) {
            val profile = settings.profiles.values.firstOrNull { it.material == campfire.material.name } ?: continue
            val location = campfire.location() ?: continue
            if (!isCampfireStillLit(campfire)) {
                continue
            }
            if (!plugin.integrationService.isLocationAllowed(location)) {
                continue
            }
            if (settings.night.onlyAtNight && !isNight(location.world.time, settings)) {
                continue
            }

            val nearbyPlayers = location.world.getNearbyPlayers(location, profile.radius, profile.radius, profile.radius) {
                it.isValid && !it.isDead && it.hasPermission("campfirerpg.use")
            }.toList()

            val eligiblePlayers = selectEligiblePlayers(nearbyPlayers)
            if (eligiblePlayers.size < settings.campfire.requiredPlayers) {
                continue
            }

            val heroCount = if (settings.integrations.useGroupSizeForHeroBonus) eligiblePlayers.size else nearbyPlayers.size
            val heroBonus = heroCount >= settings.campfire.bonusThreshold
            val campfireKey = campfire.key()

            eligiblePlayers.forEach { player ->
                applyProfile(player, profile, heroBonus)
                energizedPlayers += player.uniqueId
                lastProfileByPlayer[player.uniqueId] = profile.id
                currentCampfireByPlayer[player.uniqueId] = campfireKey
                buffedPlayers++
            }

            applyWard(campfire, profile)
            playFeedback(campfire)
            activatedCampfires++
        }

        updateRestedState(energizedPlayers)
        plugin.diagnosticsService.recordTick(System.nanoTime() - startedAt, activatedCampfires, buffedPlayers)
    }

    private fun applyProfile(player: Player, profile: CampfireProfile, heroBonus: Boolean) {
        val settings = plugin.settingsLoader.settings
        profile.effects.forEach { applyEffect(player, it) }

        if (heroBonus) {
            profile.bonusEffects.forEach { applyEffect(player, it) }
            player.sendActionBar(serializer.deserialize(plugin.integrationService.applyPlaceholders(player, settings.messages.heroMessage)))
        } else {
            val rawMessage = if (profile.material == Material.SOUL_CAMPFIRE.name) settings.messages.soulActionBar else settings.messages.normalActionBar
            player.sendActionBar(serializer.deserialize(plugin.integrationService.applyPlaceholders(player, rawMessage)))
        }

        if (profile.feedPlayers && player.foodLevel < 20) {
            player.foodLevel = (player.foodLevel + 1).coerceAtMost(20)
            player.saturation = (player.saturation + 0.75f).coerceAtMost(20f)
        }
    }

    private fun updateRestedState(energizedPlayers: Set<UUID>) {
        for (player in plugin.server.onlinePlayers) {
            if (player.uniqueId !in energizedPlayers) {
                restProgress.remove(player.uniqueId)
                lastProfileByPlayer.remove(player.uniqueId)
                currentCampfireByPlayer.remove(player.uniqueId)
                restCampfireByPlayer.remove(player.uniqueId)
                continue
            }

            val currentCampfire = currentCampfireByPlayer[player.uniqueId] ?: continue
            if (restCampfireByPlayer[player.uniqueId] != currentCampfire) {
                restCampfireByPlayer[player.uniqueId] = currentCampfire
                restProgress[player.uniqueId] = 0
            }

            val progress = restProgress.getOrDefault(player.uniqueId, 0) + 1
            restProgress[player.uniqueId] = progress

            if (progress % plugin.settingsLoader.settings.campfire.restCyclesRequired != 0) {
                continue
            }

            val worldTick = player.world.fullTime
            val cooldownUntil = lastRestRewardTick[player.uniqueId] ?: 0L
            if (worldTick < cooldownUntil) {
                continue
            }

            val profileId = lastProfileByPlayer[player.uniqueId] ?: continue
            val profile = plugin.settingsLoader.settings.profiles[profileId] ?: continue
            profile.restedEffects.forEach { applyEffect(player, it) }
            player.sendMessage(plugin.integrationService.applyPlaceholders(player, plugin.settingsLoader.settings.messages.restedMessage))
            lastRestRewardTick[player.uniqueId] = worldTick + plugin.settingsLoader.settings.campfire.restRewardCooldownTicks
        }
    }

    private fun selectEligiblePlayers(players: List<Player>): List<Player> {
        val settings = plugin.settingsLoader.settings
        if (!settings.integrations.requireSameGroupForActivation) {
            return players
        }

        return players.groupBy { plugin.integrationService.resolveGroup(it) ?: "solo:${it.uniqueId}" }
            .maxByOrNull { it.value.size }
            ?.value
            ?: emptyList()
    }

    private fun applyWard(campfire: ActiveCampfire, profile: CampfireProfile) {
        val location = campfire.location() ?: return
        val radius = plugin.settingsLoader.settings.campfire.monsterWardRadius
        for (entity in location.world.getNearbyEntities(location, radius, 4.0, radius)) {
            val monster = entity as? Monster ?: continue
            profile.wardEffects.forEach { applyEffect(monster, it) }
        }
    }

    private fun playFeedback(campfire: ActiveCampfire) {
        val location = campfire.location() ?: return
        val soul = campfire.material == Material.SOUL_CAMPFIRE
        location.world.spawnParticle(if (soul) Particle.SOUL_FIRE_FLAME else Particle.FLAME, location.clone().add(0.0, 0.2, 0.0), 10, 0.3, 0.2, 0.3, 0.01)
        location.world.spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0.0, 0.2, 0.0), 2, 0.35, 0.1, 0.35, 0.01)
        location.world.playSound(location, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.25f, if (soul) 0.9f else 1.05f)
    }

    private fun applyEffect(player: Player, spec: EffectSpec) {
        val type = resolveEffectType(spec.type) ?: return
        player.addPotionEffect(PotionEffect(type, spec.durationTicks, spec.amplifier, spec.ambient, spec.particles, spec.icon))
    }

    private fun applyEffect(monster: Monster, spec: EffectSpec) {
        val type = resolveEffectType(spec.type) ?: return
        monster.addPotionEffect(PotionEffect(type, spec.durationTicks, spec.amplifier, spec.ambient, spec.particles, spec.icon))
    }

    private fun resolveEffectType(name: String): PotionEffectType? {
        return Registry.EFFECT.get(NamespacedKey.minecraft(name.lowercase()))
    }

    private fun isCampfireStillLit(campfire: ActiveCampfire): Boolean {
        val block = campfire.block() ?: return false
        if (block.type != campfire.material) {
            return false
        }
        val data = block.blockData as? org.bukkit.block.data.type.Campfire ?: return false
        return data.isLit
    }

    private fun isNight(time: Long, settings: cg.headpop.campfireRPG.config.PluginSettings): Boolean {
        val night = settings.night
        return if (night.startTick <= night.endTick) {
            time in night.startTick..night.endTick
        } else {
            time >= night.startTick || time <= night.endTick
        }
    }
}
