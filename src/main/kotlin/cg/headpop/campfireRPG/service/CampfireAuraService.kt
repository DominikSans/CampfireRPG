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
import org.bukkit.attribute.Attribute
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
    private val lastClassByPlayer = mutableMapOf<UUID, String>()
    private val currentCampfireByPlayer = mutableMapOf<UUID, String>()
    private val currentCampfireTypeByPlayer = mutableMapOf<UUID, String>()
    private val heroBonusByPlayer = mutableMapOf<UUID, Boolean>()
    private val auraRemainingByPlayer = mutableMapOf<UUID, Int>()
    private val clanTagByPlayer = mutableMapOf<UUID, String>()
    private val clanRoleByPlayer = mutableMapOf<UUID, String>()
    private val clanSizeByPlayer = mutableMapOf<UUID, Int>()
    private val ownTerritoryByPlayer = mutableMapOf<UUID, Boolean>()
    private val restCampfireByPlayer = mutableMapOf<UUID, String>()
    private val lastRestRewardTick = mutableMapOf<UUID, Long>()
    private val lastExperiencePulseTick = mutableMapOf<UUID, Long>()
    private val lastCleanseTick = mutableMapOf<UUID, Long>()
    private val lastSharedHealTick = mutableMapOf<UUID, Long>()
    private var auraTask: BukkitTask? = null
    private var rescanTask: BukkitTask? = null
    private var auraIntervalTicks: Long = -1L
    private var rescanIntervalTicks: Long = -1L

    fun reload() {
        val settings = plugin.settingsLoader.settings
        if (auraTask != null && auraIntervalTicks == settings.scan.intervalTicks &&
            rescanTask != null && rescanIntervalTicks == settings.scan.rescanIntervalTicks
        ) {
            return
        }

        auraTask?.cancel()
        rescanTask?.cancel()
        auraTask = null
        rescanTask = null

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
        auraIntervalTicks = settings.scan.intervalTicks
        rescanIntervalTicks = settings.scan.rescanIntervalTicks
    }

    fun stop(clearState: Boolean = true) {
        auraTask?.cancel()
        rescanTask?.cancel()
        auraTask = null
        rescanTask = null
        auraIntervalTicks = -1L
        rescanIntervalTicks = -1L
        if (clearState) {
            restProgress.clear()
            lastProfileByPlayer.clear()
            lastClassByPlayer.clear()
            currentCampfireByPlayer.clear()
            currentCampfireTypeByPlayer.clear()
            heroBonusByPlayer.clear()
            auraRemainingByPlayer.clear()
            clanTagByPlayer.clear()
            clanRoleByPlayer.clear()
            clanSizeByPlayer.clear()
            ownTerritoryByPlayer.clear()
            restCampfireByPlayer.clear()
            lastRestRewardTick.clear()
            lastExperiencePulseTick.clear()
            lastCleanseTick.clear()
            lastSharedHealTick.clear()
        }
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
            val sameClanForHero = !settings.clanFeatures.heroBonusRequireSameClan || plugin.integrationService.areSameClan(eligiblePlayers)
            val heroBonus = heroCount >= settings.campfire.bonusThreshold && sameClanForHero
            val campfireKey = campfire.key()
            val soulCampfire = campfire.material == Material.SOUL_CAMPFIRE

            eligiblePlayers.forEach { player ->
                if (settings.clanFeatures.enabled && settings.clanFeatures.territoryRestrictToOwnClan &&
                    plugin.integrationService.detectedGroupPluginNames().isNotEmpty() &&
                    !plugin.integrationService.isInOwnClanTerritory(player, location)
                ) {
                    return@forEach
                }
                applyProfile(player, profile, heroBonus)
                if (settings.classes.enabled) {
                    val classPerk = resolveClassPerk(player)
                    applyClassPerks(player, classPerk.globalEffects, if (soulCampfire) classPerk.soulEffects else classPerk.normalEffects)
                    lastClassByPlayer[player.uniqueId] = classPerk.id
                } else {
                    lastClassByPlayer.remove(player.uniqueId)
                }
                applyClanFeatures(player, location)
                applyGameplayFeatures(player)
                energizedPlayers += player.uniqueId
                lastProfileByPlayer[player.uniqueId] = profile.id
                currentCampfireByPlayer[player.uniqueId] = campfireKey
                currentCampfireTypeByPlayer[player.uniqueId] = if (soulCampfire) "soul" else "normal"
                heroBonusByPlayer[player.uniqueId] = heroBonus
                auraRemainingByPlayer[player.uniqueId] = settings.scan.intervalTicks.toInt() + 40
                plugin.integrationService.getClanContext(player)?.let {
                    clanTagByPlayer[player.uniqueId] = it.tag
                    clanRoleByPlayer[player.uniqueId] = it.role ?: "none"
                    clanSizeByPlayer[player.uniqueId] = it.size
                } ?: run {
                    clanTagByPlayer.remove(player.uniqueId)
                    clanRoleByPlayer.remove(player.uniqueId)
                    clanSizeByPlayer.remove(player.uniqueId)
                }
                ownTerritoryByPlayer[player.uniqueId] = plugin.integrationService.isInOwnClanTerritory(player, location)
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
                lastClassByPlayer.remove(player.uniqueId)
                currentCampfireByPlayer.remove(player.uniqueId)
                currentCampfireTypeByPlayer.remove(player.uniqueId)
                heroBonusByPlayer.remove(player.uniqueId)
                auraRemainingByPlayer.remove(player.uniqueId)
                clanTagByPlayer.remove(player.uniqueId)
                clanRoleByPlayer.remove(player.uniqueId)
                clanSizeByPlayer.remove(player.uniqueId)
                ownTerritoryByPlayer.remove(player.uniqueId)
                restCampfireByPlayer.remove(player.uniqueId)
                lastExperiencePulseTick.remove(player.uniqueId)
                lastCleanseTick.remove(player.uniqueId)
                lastSharedHealTick.remove(player.uniqueId)
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

    fun getCurrentProfileId(player: Player): String = lastProfileByPlayer[player.uniqueId] ?: "none"

    fun getCurrentClassId(player: Player): String = lastClassByPlayer[player.uniqueId] ?: "disabled"

    fun getCurrentClassDisplayName(player: Player): String = if (plugin.settingsLoader.settings.classes.enabled) getCurrentClassId(player) else "disabled"

    fun isPlayerInActiveCampfire(player: Player): Boolean = currentCampfireByPlayer.containsKey(player.uniqueId)

    fun getCurrentCampfireType(player: Player): String = currentCampfireTypeByPlayer[player.uniqueId] ?: "none"

    fun isHeroBonusActive(player: Player): Boolean = heroBonusByPlayer[player.uniqueId] == true

    fun getAuraRemaining(player: Player): Int = auraRemainingByPlayer[player.uniqueId] ?: 0

    fun getClanTag(player: Player): String = clanTagByPlayer[player.uniqueId] ?: plugin.integrationService.getUltimateClanTag(player)

    fun getClanRole(player: Player): String = clanRoleByPlayer[player.uniqueId] ?: plugin.integrationService.getUltimateClanRole(player)

    fun getClanSize(player: Player): Int = clanSizeByPlayer[player.uniqueId] ?: plugin.integrationService.getUltimateClanSize(player)

    fun isInOwnClanTerritory(player: Player): Boolean = ownTerritoryByPlayer[player.uniqueId] == true

    private fun applyGameplayFeatures(player: Player) {
        val gameplay = plugin.settingsLoader.settings.gameplay
        val currentTick = player.world.fullTime

        if (gameplay.enableExperiencePulse && gameplay.experiencePulseAmount > 0) {
            val nextTick = lastExperiencePulseTick[player.uniqueId] ?: 0L
            if (currentTick >= nextTick) {
                player.giveExp(gameplay.experiencePulseAmount)
                lastExperiencePulseTick[player.uniqueId] = currentTick + gameplay.experiencePulseCooldownTicks
            }
        }

        if (gameplay.enableCleanse) {
            val nextTick = lastCleanseTick[player.uniqueId] ?: 0L
            if (currentTick >= nextTick) {
                cleanseNegativeEffect(player)
                lastCleanseTick[player.uniqueId] = currentTick + gameplay.cleanseCooldownTicks
            }
        }

        if (gameplay.enableSharedHeal && gameplay.sharedHealAmount > 0.0) {
            val nextTick = lastSharedHealTick[player.uniqueId] ?: 0L
            if (currentTick >= nextTick) {
                val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                if (player.health < maxHealth) {
                    player.health = (player.health + gameplay.sharedHealAmount).coerceAtMost(maxHealth)
                }
                lastSharedHealTick[player.uniqueId] = currentTick + gameplay.sharedHealCooldownTicks
            }
        }
    }

    private fun applyClassPerks(player: Player, globalEffects: List<EffectSpec>, typeEffects: List<EffectSpec>) {
        globalEffects.forEach { applyEffect(player, it) }
        typeEffects.forEach { applyEffect(player, it) }
    }

    private fun resolveClassPerk(player: Player): cg.headpop.campfireRPG.config.ClassPerk {
        val classes = plugin.settingsLoader.settings.classes
        if (!classes.enabled) {
            return classes.classes[classes.defaultClassId] ?: classes.classes.values.first()
        }
        return classes.classes.values.firstOrNull { player.hasPermission(it.permission) }
            ?: classes.classes[classes.defaultClassId]
            ?: classes.classes.values.first()
    }

    private fun applyClanFeatures(player: Player, location: org.bukkit.Location) {
        val settings = plugin.settingsLoader.settings.clanFeatures
        if (!settings.enabled) {
            return
        }

        val context = plugin.integrationService.getClanContext(player) ?: return
        if (settings.leaderBonusEnabled && context.leader) {
            settings.leaderBonusEffects.forEach { applyEffect(player, it) }
        }
        if (settings.sizeBonusEnabled && context.size >= settings.sizeBonusMinimumMembers) {
            settings.sizeBonusEffects.forEach { applyEffect(player, it) }
        }
        if (settings.territoryBonusEnabled && plugin.integrationService.isInOwnClanTerritory(player, location)) {
            settings.territoryBonusEffects.forEach { applyEffect(player, it) }
        }
    }

    private fun cleanseNegativeEffect(player: Player) {
        val negativeEffects = listOf(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.WEAKNESS,
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.NAUSEA
        )

        val active = negativeEffects.firstOrNull { player.hasPotionEffect(it) } ?: return
        player.removePotionEffect(active)
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
