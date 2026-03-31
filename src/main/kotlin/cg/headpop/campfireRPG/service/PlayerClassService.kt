package cg.headpop.campfireRPG.service

import cg.headpop.campfireRPG.CampfireRPG
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID

class PlayerClassService(
    private val plugin: CampfireRPG,
) {

    private lateinit var file: File
    private lateinit var config: YamlConfiguration
    private val selectedClasses = mutableMapOf<UUID, String>()

    fun reload() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        file = File(plugin.dataFolder, "player-classes.yml")
        if (!file.exists()) {
            file.createNewFile()
        }

        config = YamlConfiguration.loadConfiguration(file)
        selectedClasses.clear()

        val section = config.getConfigurationSection("players") ?: return
        for (key in section.getKeys(false)) {
            runCatching { UUID.fromString(key) }.getOrNull()?.let { uuid ->
                selectedClasses[uuid] = section.getString(key, plugin.settingsLoader.settings.classes.defaultClassId)!!.lowercase()
            }
        }
    }

    fun setSelectedClass(player: Player, classId: String): Boolean {
        val normalized = classId.lowercase()
        if (normalized !in plugin.settingsLoader.settings.classes.classes) {
            return false
        }

        selectedClasses[player.uniqueId] = normalized
        config.set("players.${player.uniqueId}", normalized)
        config.save(file)
        return true
    }

    fun getSelectedClassId(player: Player): String {
        return selectedClasses[player.uniqueId] ?: plugin.settingsLoader.settings.classes.defaultClassId
    }

    fun getEffectiveClassId(player: Player): String {
        val selected = getSelectedClassId(player)
        val selectedPerk = plugin.settingsLoader.settings.classes.classes[selected]
        if (selectedPerk != null && player.hasPermission(selectedPerk.permission)) {
            return selected
        }

        return plugin.settingsLoader.settings.classes.classes.values
            .firstOrNull { player.hasPermission(it.permission) }
            ?.id
            ?: plugin.settingsLoader.settings.classes.defaultClassId
    }
}
